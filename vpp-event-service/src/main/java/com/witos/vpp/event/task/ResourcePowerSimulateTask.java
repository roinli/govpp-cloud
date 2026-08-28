package com.witos.vpp.event.task;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.witos.vpp.event.domain.DrPowerDaily;
import com.witos.vpp.event.domain.DrResource;
import com.witos.vpp.event.mapper.DrPowerDailyMapper;
import com.witos.vpp.event.mapper.DrResourceMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * VPP 资源每日功率模拟任务（充电负荷口径，即削峰基线功率，非削峰填谷可调功率）
 *
 * 差异化模拟，让每个桩更像真实运行：
 *  - 桩级“负荷个性”由资源ID种子决定：利用率水平 / 本桩每周高峰星期 / 每日波动幅度 / 意外停运概率，
 *    不同桩的曲线形态各不相同；
 *  - 日级因子由 资源ID+日期 种子决定：偶发故障消缺日功率骤降，且同一天重算结果不变；
 *  - 今日记录联动当前状态：设备离线/故障当天功率走低、停用桩长期低负荷（已写入的历史日不动）。
 *
 * 写盘策略：每天 00:10 只写今日一天；启动时按缺口补写（表空补最近7天，
 * 否则从各桩最后记录日+1补到今天），不重复刷写历史。
 *
 * @author witos
 */
@Component
public class ResourcePowerSimulateTask implements ApplicationRunner
{
    private static final Logger log = LoggerFactory.getLogger(ResourcePowerSimulateTask.class);

    @Autowired
    private DrResourceMapper drResourceMapper;

    @Autowired
    private DrPowerDailyMapper drPowerDailyMapper;

    /** 启动时按缺口补写历史数据，并初始化一轮设备状态 */
    @Override
    public void run(ApplicationArguments args)
    {
        try
        {
            backfillMissing();
        }
        catch (Exception e)
        {
            log.error("resource power backfill failed", e);
        }
        simulateDeviceStatus();
    }

    /** 每天 00:10 只写今日一天的模拟功率（幂等 upsert，不刷写历史） */
    @Scheduled(cron = "0 10 0 * * ?")
    public void simulateDaily()
    {
        try
        {
            writeDay(new Date());
        }
        catch (Exception e)
        {
            log.error("resource power simulate task failed", e);
        }
    }

    /**
     * 每小时整点模拟一次设备状态演变（仅启用桩）：在线为主（≥75%），
     * 各桩按桩级故障/离线体质偏离；同一小时内多次执行结果一致（刷新不跳动），跨小时才演变。
     * 停用桩不参与（启停动作已由接口联动置为在线/离线），模拟不会覆盖人工启停结果
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void simulateDeviceStatus()
    {
        try
        {
            List<DrResource> resources = drResourceMapper.selectList(null);
            int changed = 0;
            for (DrResource r : resources)
            {
                // 只模拟启用桩；停用桩保持启停联动后的状态
                if (!"1".equals(r.getParticipateStatus()))
                {
                    continue;
                }
                String status = rollDeviceStatus(r);
                if (!status.equals(r.getDeviceStatus()))
                {
                    drResourceMapper.update(null, new LambdaUpdateWrapper<DrResource>()
                            .eq(DrResource::getResourceId, r.getResourceId())
                            .set(DrResource::getDeviceStatus, status));
                    changed++;
                }
            }
            // 今日功率与最新状态联动重写，历史日不重写
            writeDay(new Date());
            log.info("device status simulated ({} changed of {} resources)", changed, resources.size());
        }
        catch (Exception e)
        {
            log.error("device status simulate task failed", e);
        }
    }

    /**
     * 每小时为启用桩抽一次设备状态：
     * 故障 3%~9%，离线 8%~16%，其余在线（在线始终占大头）；概率由资源ID的桩级种子决定，
     * 时变种子用 资源ID+当前小时，同一小时内重算结果一致
     */
    private String rollDeviceStatus(DrResource r)
    {
        long resourceId = r.getResourceId() == null ? 0L : r.getResourceId();
        // 桩级故障/离线体质（只与资源ID相关）
        Random profile = new Random(resourceId * 27733L + 7L);
        double faultProb = 0.03 + profile.nextDouble() * 0.06;   // 3%~9%
        double offlineProb = 0.08 + profile.nextDouble() * 0.08; // 8%~16%
        // 时变抽签：同一小时结果一致
        Random hourly = new Random(resourceId * 104729L + System.currentTimeMillis() / 3600000L);
        double roll = hourly.nextDouble();
        if (roll < faultProb)
        {
            return "3";
        }
        if (roll < faultProb + offlineProb)
        {
            return "2";
        }
        return "1";
    }

    /** 为所有资源写入指定一天的模拟功率 */
    public void writeDay(Date day)
    {
        List<DrResource> resources = drResourceMapper.selectList(null);
        Date d = truncateToDay(day);
        for (DrResource r : resources)
        {
            drPowerDailyMapper.upsertDailyPower(buildDaily(r, d));
        }
        log.info("resource daily power simulated for {} ({} resources)", d, resources.size());
    }

    /** 按各桩已有记录的最后日期补写缺口：表空补最近7天，否则从最后记录日+1补到今天 */
    public void backfillMissing()
    {
        List<DrResource> resources = drResourceMapper.selectList(null);
        if (resources.isEmpty())
        {
            return;
        }
        Date today = truncateToDay(new Date());
        Map<Long, Date> lastByResource = new HashMap<>();
        for (DrPowerDaily d : drPowerDailyMapper.selectMaxDateGroupByResource())
        {
            if (d.getResourceId() != null && d.getPowerDate() != null)
            {
                lastByResource.put(d.getResourceId(), d.getPowerDate());
            }
        }

        int written = 0;
        Calendar cal = Calendar.getInstance();
        for (DrResource r : resources)
        {
            Date last = lastByResource.get(r.getResourceId());
            if (last == null)
            {
                // 新桩/表空：补最近7天
                cal.setTime(today);
                cal.add(Calendar.DAY_OF_MONTH, -6);
            }
            else if (last.before(today))
            {
                // 已有历史：只从最后记录日的下一天补到今天
                cal.setTime(last);
                cal.add(Calendar.DAY_OF_MONTH, 1);
            }
            else
            {
                continue;
            }
            while (!cal.getTime().after(today))
            {
                drPowerDailyMapper.upsertDailyPower(buildDaily(r, cal.getTime()));
                written++;
                cal.add(Calendar.DAY_OF_MONTH, 1);
            }
        }
        log.info("resource daily power backfilled {} rows to {}", written, today);
    }

    /**
     * 生成指定桩、指定日的充电功率。
     * 桩级参数只与资源ID相关（各桩曲线形态不同），日级参数由 资源ID+日期 决定，
     * 同一桩同一天重算结果不变，保证历史数据稳定。
     */
    private DrPowerDaily buildDaily(DrResource r, Date day)
    {
        BigDecimal base = r.getAvg7dPower() != null && r.getAvg7dPower().signum() > 0
                ? r.getAvg7dPower()
                : (r.getRatedPower() == null ? BigDecimal.ZERO : r.getRatedPower());
        long resourceId = r.getResourceId() == null ? 0L : r.getResourceId();
        long daySeed = resourceId * 131L + day.getTime() / 86400000L;

        // ---- 桩级“负荷个性”（只与资源ID相关，各桩不同） ----
        Random profile = new Random(resourceId * 10007L + 13L);
        double util = 0.45 + profile.nextDouble() * 0.55;       // 桩利用率水平 0.45~1.00
        int peakDow = profile.nextInt(7);                       // 本桩每周用电高峰（0=周日）
        double jitterAmp = 0.15 + profile.nextDouble() * 0.40;  // 每日波动幅度 0.15~0.55
        double outageProb = 0.06 + profile.nextDouble() * 0.16; // 意外停运概率 6%~22%
        boolean lowUsage = "0".equals(r.getParticipateStatus()); // 停用桩长期低负荷

        // ---- 日级因子（同一天重算一致，历史写入后不变） ----
        Calendar cal = Calendar.getInstance();
        cal.setTime(day);
        int dow = cal.get(Calendar.DAY_OF_WEEK) - 1;
        int diff = Math.abs(dow - peakDow);
        diff = Math.min(diff, 7 - diff);
        double dowFactor = 1.0 - diff * 0.09;                   // 靠近本桩高峰日越高
        double noise = (1 - jitterAmp) + new Random(daySeed * 3L + 1L).nextDouble() * jitterAmp * 2;
        boolean outage = new Random(daySeed * 3L + 2L).nextDouble() < outageProb;

        double factor;
        if (outage)
        {
            // 故障/消缺日：功率骤降至基线的2%~30%
            factor = 0.02 + new Random(daySeed * 3L + 3L).nextDouble() * 0.28;
        }
        else
        {
            factor = util * dowFactor * noise;
            // 今日联动当前状态；已写入的历史日不受影响
            boolean isToday = day.equals(truncateToDay(new Date()));
            if (isToday && ("2".equals(r.getDeviceStatus()) || "3".equals(r.getDeviceStatus())))
            {
                factor *= 0.10 + new Random(daySeed * 3L + 4L).nextDouble() * 0.25;
            }
            if (lowUsage)
            {
                factor *= 0.05 + new Random(daySeed * 3L + 5L).nextDouble() * 0.20;
            }
        }

        BigDecimal avgPower = base.multiply(BigDecimal.valueOf(factor)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal peakPower = avgPower.multiply(
                BigDecimal.valueOf(1.10 + new Random(daySeed * 3L + 6L).nextDouble() * 0.50))
                .setScale(2, RoundingMode.HALF_UP);

        DrPowerDaily daily = new DrPowerDaily();
        daily.setTenantId(r.getTenantId());
        daily.setStationId(r.getStationId());
        daily.setResourceId(r.getResourceId());
        daily.setPowerDate(day);
        daily.setAvgPower(avgPower);
        daily.setPeakPower(peakPower);
        return daily;
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