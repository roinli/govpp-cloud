package com.witos.vpp.event.task;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.witos.vpp.event.domain.DrPowerMinute;
import com.witos.vpp.event.domain.DrResource;
import com.witos.vpp.event.mapper.DrPowerMinuteMapper;
import com.witos.vpp.event.mapper.DrResourceMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * VPP 资源分钟级负荷模拟任务（总负荷曲线"实时负荷"口径，15 分钟刻钟粒度）
 *
 * 写盘策略：
 *  - 每 15 分钟整点（xx:00/15/30/45）跑一次，只写当前刻钟所有桩的负荷
 *    （批量 upsert，同桩同一刻钟重算一致，刷新不跳动；
 *    总负荷曲线接口本就按 15 分钟聚合展示，存刻钟粒度即可，单桩一天 96 条，避免数据量失控）；
 *  - 启动时后台异步回补昨日整天缺口（总负荷曲线需要 T-1 日对比线）+ 当天 00:00 到当前刻钟的缺口
 *    （幂等覆盖，不阻塞服务启动；更早历史与停机期间不补）；
 *  - 每天凌晨 00:20 清理前天之前的过期数据，表内恒定保留最近 3 天（前天/昨天/今天），数据量封顶。
 *
 * 口径与日模拟 {@link ResourcePowerSimulateTask} 保持同源：
 *  - 桩级"负荷个性"、日级因子与日模拟共用同一套种子，实时曲线全天均值口径与每日功率接近；
 *  - 今日刻钟数据联动当前设备状态：离线/故障打折、停用桩低负荷（已写入的历史刻钟不重写）。
 *
 * @author witos
 */
@Component
public class ResourcePowerMinuteSimulateTask implements ApplicationRunner
{
    private static final Logger log = LoggerFactory.getLogger(ResourcePowerMinuteSimulateTask.class);

    /** 充电桩日内负荷形状因子（按小时）：夜间低谷、早晚双高峰，与真实充电行为贴近 */
    private static final double[] HOUR_FACTOR = {
            0.35, 0.30, 0.26, 0.24, 0.26, 0.33, // 0-5点 夜间低谷
            0.55, 0.80, 1.00, 1.10, 1.05, 1.00, // 6-11点 早高峰前后
            1.20, 1.10, 1.00, 1.05, 1.10, 1.28, // 12-17点 午间平稳
            1.40, 1.42, 1.35, 1.20, 0.85, 0.55  // 18-23点 晚高峰后回落
    };

    /** 一天的刻钟数（96），用于判断昨日刻钟数据是否完整 */
    private static final int QUARTERS_PER_DAY = 24 * 4;

    @Autowired
    private DrResourceMapper drResourceMapper;

    @Autowired
    private DrPowerMinuteMapper drPowerMinuteMapper;

    /** 回补专用单线程（daemon）：补数放后台执行，避免大量回补写入阻塞服务启动 */
    private final ExecutorService backfillExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "resource-minute-backfill");
        t.setDaemon(true);
        return t;
    });

    /** 启动时后台异步清理过期数据 + 回补昨日整天缺口 + 当天 00:00 到当前刻钟的缺口，不阻塞服务就绪 */
    @Override
    public void run(ApplicationArguments args)
    {
        backfillExecutor.execute(() -> {
            try
            {
                cleanExpiredQuarters();
            }
            catch (Exception e)
            {
                log.error("resource minute power clean task failed", e);
            }
            try
            {
                backfillYesterday();
            }
            catch (Exception e)
            {
                log.error("resource minute power yesterday backfill failed", e);
            }
            try
            {
                backfillToday();
            }
            catch (Exception e)
            {
                log.error("resource minute power backfill failed", e);
            }
        });
    }

    /** 每 15 分钟整点（xx:00/15/30/45）写入当前刻钟的模拟负荷 */
    @Scheduled(cron = "0 0/15 * * * ?")
    public void simulateCurrentQuarter()
    {
        try
        {
            Date nowQuarter = truncateToQuarter(new Date());
            writeMinutes(nowQuarter, nowQuarter);
        }
        catch (Exception e)
        {
            log.error("resource minute power simulate task failed", e);
        }
    }

    /**
     * 每天凌晨 00:20 清理前天之前的过期数据：总负荷曲线只看今天与 T-1，
     * 表内恒定保留最近 3 天（前天/昨天/今天），数据量封顶不再增长。
     * 与 00:10 的日功率任务、00:15 的刻钟写入错开，删除条件只命中历史数据，不影响当日写入。
     */
    @Scheduled(cron = "0 20 0 * * ?")
    public void cleanExpiredQuarters()
    {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        cal.add(Calendar.DAY_OF_MONTH, -2);
        Date expireBefore = cal.getTime();
        int deleted = drPowerMinuteMapper.deleteBeforeTime(expireBefore);
        log.info("resource minute power cleaned {} rows before {}", deleted, expireBefore);
    }

    /**
     * 回补昨日全天的刻钟缺口（幂等 upsert，重算结果一致）。
     * 总负荷曲线需要 T-1 日对比线：服务今日才首次启动/表新建/新增桩时昨日无数据，
     * 按"每桩昨日应有 96 条刻钟记录"判断缺口后整日补写，只补一次，不反复重写历史刻钟。
     */
    public void backfillYesterday()
    {
        List<DrResource> resources = drResourceMapper.selectList(null);
        if (resources.isEmpty())
        {
            return;
        }
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date todayStart = cal.getTime();
        cal.add(Calendar.DAY_OF_MONTH, -1);
        Date yesterdayStart = cal.getTime();
        // 昨日最后一个刻钟 23:45 = 今日 00:00 回退一刻钟（不能在已减一天的 cal 上继续减，否则比起始还早，循环体一次都不执行）
        Calendar endCal = Calendar.getInstance();
        endCal.setTime(todayStart);
        endCal.add(Calendar.MINUTE, -15);
        Date yesterdayEnd = endCal.getTime();

        Map<Long, Long> countByResource = new HashMap<>();
        for (Map<String, Object> row : drPowerMinuteMapper.selectCountGroupByResource(yesterdayStart, todayStart))
        {
            Object id = row.get("resourceId");
            Object cnt = row.get("cnt");
            if (id instanceof Number && cnt instanceof Number)
            {
                countByResource.put(((Number) id).longValue(), ((Number) cnt).longValue());
            }
        }
        for (DrResource r : resources)
        {
            Long cnt = r.getResourceId() == null ? null : countByResource.get(r.getResourceId());
            if (cnt == null || cnt < QUARTERS_PER_DAY)
            {
                writeResourceMinutes(r, yesterdayStart, yesterdayEnd);
            }
        }
    }

    /** 回填当天 00:00 到当前刻钟的缺口（幂等 upsert，重复回填不产生脏数据） */
    public void backfillToday()
    {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date dayStart = cal.getTime();
        Date nowQuarter = truncateToQuarter(new Date());
        if (dayStart.after(nowQuarter))
        {
            return;
        }
        writeMinutes(dayStart, nowQuarter);
    }

    /** 为所有资源写入指定刻钟区间的模拟负荷（含两端，步长15分钟），按每批1000行批量 upsert；
     * synchronized 串行化写库，避免后台回补与刻钟 cron 并发 upsert 同一键 */
    private synchronized void writeMinutes(Date fromMinute, Date toMinute)
    {
        List<DrResource> resources = drResourceMapper.selectList(null);
        if (resources.isEmpty())
        {
            return;
        }
        List<DrPowerMinute> batch = new ArrayList<>(1000);
        int written = 0;
        Calendar cal = Calendar.getInstance();
        cal.setTime(fromMinute);
        while (!cal.getTime().after(toMinute))
        {
            Date minute = cal.getTime();
            for (DrResource r : resources)
            {
                batch.add(buildMinute(r, minute));
                if (batch.size() >= 1000)
                {
                    drPowerMinuteMapper.insertBatchMinutes(batch);
                    written += batch.size();
                    batch.clear();
                }
            }
            cal.add(Calendar.MINUTE, 15);
        }
        if (!batch.isEmpty())
        {
            drPowerMinuteMapper.insertBatchMinutes(batch);
            written += batch.size();
        }
        log.info("resource minute power simulated {} rows [{}, {}]", written, fromMinute, toMinute);
    }

    /** 为单个资源写入指定刻钟区间的模拟负荷（含两端，步长15分钟），按每批1000行批量 upsert，回补昨日整天用；
     * synchronized 串行化写库，避免后台回补与刻钟 cron 并发 upsert 同一键 */
    private synchronized void writeResourceMinutes(DrResource r, Date fromMinute, Date toMinute)
    {
        List<DrPowerMinute> batch = new ArrayList<>(1000);
        int written = 0;
        Calendar cal = Calendar.getInstance();
        cal.setTime(fromMinute);
        while (!cal.getTime().after(toMinute))
        {
            batch.add(buildMinute(r, cal.getTime()));
            if (batch.size() >= 1000)
            {
                drPowerMinuteMapper.insertBatchMinutes(batch);
                written += batch.size();
                batch.clear();
            }
            cal.add(Calendar.MINUTE, 15);
        }
        if (!batch.isEmpty())
        {
            drPowerMinuteMapper.insertBatchMinutes(batch);
            written += batch.size();
        }
        // 无条件打日志：回补量 0 行时便于发现区间算错等问题
        log.info("resource {} minute power backfilled {} rows [{}, {}]", r.getResourceId(), written, fromMinute, toMinute);
    }

    /**
     * 生成指定桩、指定分钟的充电负荷。
     * 桩级/日级因子与日模拟（ResourcePowerSimulateTask 的 buildDaily）同源，
     * 叠加小时时段因子与分钟抖动（种子带分钟戳，同一分钟重算结果一致）。
     */
    private DrPowerMinute buildMinute(DrResource r, Date minute)
    {
        BigDecimal base = r.getAvg7dPower() != null && r.getAvg7dPower().signum() > 0
                ? r.getAvg7dPower()
                : (r.getRatedPower() == null ? BigDecimal.ZERO : r.getRatedPower());
        long resourceId = r.getResourceId() == null ? 0L : r.getResourceId();
        long dayEpoch = truncateToDay(minute).getTime() / 86400000L;
        long daySeed = resourceId * 131L + dayEpoch;

        // ---- 桩级"负荷个性"（与日模拟同一套种子） ----
        Random profile = new Random(resourceId * 10007L + 13L);
        double util = 0.45 + profile.nextDouble() * 0.55;
        int peakDow = profile.nextInt(7);
        double jitterAmp = 0.15 + profile.nextDouble() * 0.40;
        double outageProb = 0.06 + profile.nextDouble() * 0.16;
        boolean lowUsage = "0".equals(r.getParticipateStatus());

        // ---- 日级因子（与日模拟一致） ----
        Calendar cal = Calendar.getInstance();
        cal.setTime(minute);
        int dow = cal.get(Calendar.DAY_OF_WEEK) - 1;
        int diff = Math.abs(dow - peakDow);
        diff = Math.min(diff, 7 - diff);
        double dowFactor = 1.0 - diff * 0.09;
        double noise = (1 - jitterAmp) + new Random(daySeed * 3L + 1L).nextDouble() * jitterAmp * 2;
        boolean outage = new Random(daySeed * 3L + 2L).nextDouble() < outageProb;

        double factor;
        if (outage)
        {
            factor = 0.02 + new Random(daySeed * 3L + 3L).nextDouble() * 0.28;
        }
        else
        {
            factor = util * dowFactor * noise;
            // 分钟数据只写当天，当前设备状态即时联动（历史分钟不重写）
            if ("2".equals(r.getDeviceStatus()) || "3".equals(r.getDeviceStatus()))
            {
                factor *= 0.10 + new Random(daySeed * 3L + 4L).nextDouble() * 0.25;
            }
            if (lowUsage)
            {
                factor *= 0.05 + new Random(daySeed * 3L + 5L).nextDouble() * 0.20;
            }
        }

        // ---- 时段因子 + 分钟抖动（各因子种子带日戳/分钟戳，同一刻钟重算结果一致） ----
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int minuteOfHour = cal.get(Calendar.MINUTE);
        // 日级全局峰时偏移（-3~+3 小时）：种子只含日期不含资源 ID，全桩同向偏移；
        // 若按桩各自偏移，百余根桩聚合求和后互相抵消，总曲线形态仍与 T-1 重合
        int peakShift = new Random(dayEpoch * 7L + 6L).nextInt(7) - 3;
        int shapeHour = ((hour + peakShift) % 24 + 24) % 24;
        // 小时因子按刻钟线性插值，消除整点处台阶跳变（相邻小时因子首尾连续）
        double hourFrac = minuteOfHour / 60.0;
        double f0 = HOUR_FACTOR[shapeHour];
        double f1 = HOUR_FACTOR[(shapeHour + 1) % 24];
        double hourFactor = f0 + (f1 - f0) * hourFrac;
        // 日级平滑波动：正弦波叠加（周期12h/8h，相位由日期种子决定），替代逐小时独立随机——
        // 连续无跳变、全桩一致，当天与 T-1 相位不同形态自然错开；正弦均值为零不影响全天均值口径
        double phase1 = new Random(dayEpoch * 11L + 1L).nextDouble() * Math.PI * 2;
        double phase2 = new Random(dayEpoch * 11L + 2L).nextDouble() * Math.PI * 2;
        double hourWave = 1.0
                + 0.10 * Math.sin(Math.PI * 2 * (hour + hourFrac) / 12.0 + phase1)
                + 0.06 * Math.sin(Math.PI * 2 * (hour + hourFrac) / 8.0 + phase2);
        long minuteEpoch = minute.getTime() / 60000L;
        double minuteJitter = 0.90 + new Random(resourceId * 104729L + minuteEpoch).nextDouble() * 0.20;

        BigDecimal load = base.multiply(BigDecimal.valueOf(factor * hourFactor * hourWave * minuteJitter))
                .setScale(2, RoundingMode.HALF_UP);
        if (load.signum() < 0)
        {
            load = BigDecimal.ZERO;
        }

        DrPowerMinute m = new DrPowerMinute();
        m.setTenantId(r.getTenantId());
        m.setStationId(r.getStationId());
        m.setResourceId(r.getResourceId());
        m.setPowerTime(minute);
        m.setLoadPower(load);
        return m;
    }

    public static Date truncateToQuarter(Date date)
    {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.MINUTE, cal.get(Calendar.MINUTE) / 15 * 15);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    public static Date truncateToDay(Date date)
    {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }
}