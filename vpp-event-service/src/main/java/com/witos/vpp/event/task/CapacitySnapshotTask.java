package com.witos.vpp.event.task;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.witos.vpp.event.domain.DrCapacitySnapshot;
import com.witos.vpp.event.mapper.DrCapacitySnapshotMapper;
import com.witos.vpp.event.mapper.DrResourceMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * VPP 首页容量快照定时任务
 * 每小时对资源台账的可调容量做一次快照，支撑首页环比昨日与每小时趋势曲线
 *
 * @author witos
 */
@Component
public class CapacitySnapshotTask implements ApplicationRunner
{
    private static final Logger log = LoggerFactory.getLogger(CapacitySnapshotTask.class);

    /** 削峰可调系数（下标=小时）：白天充电高峰可调能力大，凌晨最低 */
    private static final double[] PEAK_COEFF = {
        0.58, 0.52, 0.49, 0.47, 0.50, 0.57, 0.66, 0.78,
        0.88, 0.95, 1.00, 1.03, 1.02, 0.99, 1.04, 1.05,
        1.01, 0.96, 0.90, 0.86, 0.82, 0.76, 0.69, 0.63 };

    /** 填谷可调系数（下标=小时）：夜间低谷可调能力大，白天最低 */
    private static final double[] VALLEY_COEFF = {
        1.06, 1.09, 1.11, 1.11, 1.08, 1.01, 0.92, 0.82,
        0.74, 0.69, 0.66, 0.63, 0.61, 0.63, 0.66, 0.67,
        0.70, 0.74, 0.80, 0.86, 0.92, 0.96, 1.01, 1.04 };

    @Autowired
    private DrResourceMapper drResourceMapper;

    @Autowired
    private DrCapacitySnapshotMapper snapshotMapper;

    /** 启动时按缺口补写快照，避免服务停机导致首页趋势曲线断档 */
    @Override
    public void run(ApplicationArguments args)
    {
        try
        {
            backfillMissingHours();
        }
        catch (Exception e)
        {
            log.error("capacity snapshot backfill failed", e);
        }
    }

    /** 每小时整点过5分写入一条快照 */
    @Scheduled(cron = "0 5 * * * ?")
    public void snapshotHourly()
    {
        try
        {
            writeSnapshot(truncateToHour(new Date()));
        }
        catch (Exception e)
        {
            log.error("capacity snapshot failed", e);
        }
    }

    /**
     * 启动时补写缺口：从最后快照小时的下一小时补到当前小时，且至少覆盖最近24小时
     * （表空补24小时；超长停机只补最近24小时，首页环比/趋势窗口用不到更早的数据）
     */
    public void backfillMissingHours()
    {
        Date now = truncateToHour(new Date());
        Date last = snapshotMapper.selectMaxSnapshotHour();
        Date from = last == null ? new Date(now.getTime() - 23L * 3600000L)
                : new Date(last.getTime() + 3600000L);
        if (from.after(now))
        {
            return;
        }
        Date lowerBound = new Date(now.getTime() - 23L * 3600000L);
        if (from.before(lowerBound))
        {
            from = lowerBound;
        }
        int written = 0;
        for (Date hour = from; !hour.after(now); hour = new Date(hour.getTime() + 3600000L))
        {
            writeSnapshot(hour);
            written++;
        }
        log.info("capacity snapshot backfilled {} hours from {} to {}", written, from, now);
    }

    /** 按当前资源台账逐场站写入指定小时的快照（叠加昼夜曲线系数与每日抖动，与回填脚本口径一致） */
    public void writeSnapshot(Date hour)
    {
        List<Map<String, Object>> stationStats = drResourceMapper.selectResourceStatsGroupByStation();
        Calendar cal = Calendar.getInstance();
        cal.setTime(hour);
        int hr = cal.get(Calendar.HOUR_OF_DAY);
        // 每日抖动 ±3%：按天确定性伪随机，同一天一致、天与天不同
        Random rand = new Random(hour.getTime() / 86400000L);
        double jitter = 0.97 + rand.nextDouble() * 0.06;
        // 额定逐小时抖动 ±3%：按小时做种子，每个小时都不一样（与回填脚本口径一致）
        Random ratedRand = new Random(hour.getTime() / 3600000L);
        double ratedJitter = 0.97 + ratedRand.nextDouble() * 0.06;
        for (Map<String, Object> stats : stationStats)
        {
            DrCapacitySnapshot snapshot = new DrCapacitySnapshot();
            snapshot.setTenantId(toLong(stats.get("tenantId")));
            snapshot.setStationId(toLong(stats.get("stationId")));
            snapshot.setSnapshotHour(hour);
            snapshot.setPeakPower(scale(toDecimal(stats.get("adjustablePower")).multiply(
                BigDecimal.valueOf(PEAK_COEFF[hr] * jitter))));
            snapshot.setValleyPower(scale(toDecimal(stats.get("valleyPower")).multiply(
                BigDecimal.valueOf(VALLEY_COEFF[hr] * jitter))));
            snapshot.setRatedPower(scale(toDecimal(stats.get("ratedPower")).multiply(
                BigDecimal.valueOf(ratedJitter))));
            // 每条快照对应一个场站，聚合时 sum 即为场站数
            snapshot.setStationCount(1);
            snapshot.setPileCount(toInt(stats.get("pileCount")));
            snapshot.setOnlineCount(toInt(stats.get("onlineCount")));
            snapshot.setOfflineCount(toInt(stats.get("offlineCount")));
            snapshot.setFaultCount(toInt(stats.get("faultCount")));
            snapshotMapper.upsertSnapshot(snapshot);
        }
        log.info("capacity snapshot written for {} ({} stations)", hour, stationStats.size());
    }

    public static Date truncateToHour(Date date)
    {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    private static BigDecimal scale(BigDecimal v)
    {
        return v.setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal toDecimal(Object v)
    {
        return v == null ? BigDecimal.ZERO : new BigDecimal(v.toString());
    }

    private static Integer toInt(Object v)
    {
        return v == null ? 0 : ((Number) v).intValue();
    }

    private static Long toLong(Object v)
    {
        return v == null ? null : ((Number) v).longValue();
    }
}
