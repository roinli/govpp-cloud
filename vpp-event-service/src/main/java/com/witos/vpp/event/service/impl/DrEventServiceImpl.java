package com.witos.vpp.event.service.impl;

import java.text.SimpleDateFormat;
import com.witos.common.core.utils.DateUtils;
import java.util.TimeZone;
import java.util.Calendar;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Comparator;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.witos.common.core.text.Convert;
import com.witos.common.core.utils.ServletUtils;
import com.witos.common.core.utils.StringUtils;
import com.witos.common.mybatisplus.constant.MybatisPageConstants;
import com.witos.common.security.utils.SecurityUtils;
import com.witos.system.api.domain.SysUser;
import com.witos.vpp.event.domain.DrApply;
import com.witos.vpp.event.domain.DrAssess;
import com.witos.vpp.event.domain.DrDispatch;
import com.witos.vpp.event.domain.DrEvent;
import com.witos.vpp.event.domain.DrExecute;
import com.witos.vpp.event.domain.DrSettlement;
import com.witos.vpp.event.domain.DrStation;
import com.witos.vpp.event.mapper.DrApplyMapper;
import com.witos.vpp.event.mapper.DrAssessMapper;
import com.witos.vpp.event.mapper.DrDispatchMapper;
import com.witos.vpp.event.mapper.DrEventMapper;
import com.witos.vpp.event.mapper.DrExecuteMapper;
import com.witos.vpp.event.mapper.DrSettlementMapper;
import com.witos.vpp.event.mapper.DrStationMapper;
import com.witos.vpp.event.service.IDrEventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/**
 * 需求响应事件Service业务层处理
 *
 * @author witos
 * @date 2026-08-02
 */
@Service
public class DrEventServiceImpl implements IDrEventService
{
    @Autowired
    private DrEventMapper drEventMapper;

    @Autowired
    private DrApplyMapper drApplyMapper;

    @Autowired
    private DrStationMapper drStationMapper;

    @Autowired
    private DrExecuteMapper drExecuteMapper;

    @Autowired
    private DrDispatchMapper drDispatchMapper;

    @Autowired
    private DrAssessMapper drAssessMapper;

    @Autowired
    private DrSettlementMapper drSettlementMapper;

    @Override
    public DrEvent selectDrEventByEventId(Long eventId)
    {
        DrEvent e = drEventMapper.selectById(eventId);
        if (e != null)
        {
            fillApplyPower(Collections.singletonList(e));
            fillActualPower(Collections.singletonList(e));
            fillResultStats(Collections.singletonList(e));
        }
        return e;
    }

    @Override
    public IPage<DrEvent> selectDrEventPage(DrEvent event)
    {
        Page<DrEvent> page = new Page<>(Convert.toLong(ServletUtils.getParameterToInt(MybatisPageConstants.PAGE_NUM), 1L),
                Convert.toLong(ServletUtils.getParameterToInt(MybatisPageConstants.PAGE_SIZE), 10L));
        IPage<DrEvent> result = drEventMapper.selectPage(page, buildWrapper(event));
        if (!result.getRecords().isEmpty())
        {
            fillApplyPower(result.getRecords());
            fillActualPower(result.getRecords());
            fillResultStats(result.getRecords());
        }
        return result;
    }

    @Override
    public List<DrEvent> selectDrEventList(DrEvent event)
    {
        List<DrEvent> list = drEventMapper.selectList(buildWrapper(event));
        if (!list.isEmpty())
        {
            fillApplyPower(list);
            fillActualPower(list);
        }
        return list;
    }

    /**
     * 大屏用：按角色过滤事件列表（场站用户只看自己场站参与的，运营商只看本租户的，平台看全部）
     */
    @Override
    public List<DrEvent> selectDrEventListByRole(DrEvent event)
    {
        LambdaQueryWrapper<DrEvent> wrapper = buildWrapper(event);
        applyRoleFilter(wrapper);
        List<DrEvent> list = drEventMapper.selectList(wrapper);
        if (!list.isEmpty())
        {
            fillApplyPower(list);
            fillActualPower(list);
            fillResultStats(list);
        }
        return list;
    }

    /**
     * 角色数据隔离：场站用户只看自己场站参与的事件，运营商只看本租户下场站参与的事件，平台看全部
     */
    private void applyRoleFilter(LambdaQueryWrapper<DrEvent> wrapper) {
        Set<Long> myEventIds = getMyEventIds();
        if (myEventIds != null) {
            if (myEventIds.isEmpty()) {
                wrapper.apply("1=0"); // 无事件，返回空
            } else {
                wrapper.in(DrEvent::getEventId, myEventIds);
            }
        } else {
            Set<Long> tenantEventIds = getTenantEventIds();
            if (tenantEventIds != null) {
                if (tenantEventIds.isEmpty()) {
                    wrapper.apply("1=0");
                } else {
                    wrapper.in(DrEvent::getEventId, tenantEventIds);
                }
            }
        }
    }

    private void fillApplyPower(List<DrEvent> events) {
        Set<Long> eventIds = events.stream().map(DrEvent::getEventId).collect(Collectors.toSet());
        List<DrApply> applies = drApplyMapper.selectList(
                new LambdaQueryWrapper<DrApply>()
                        .in(DrApply::getEventId, eventIds));
        Map<Long, BigDecimal> sumMap = new HashMap<>();
        Map<Long, Set<Long>> stationMap = new HashMap<>();
        for (DrApply a : applies) {
            sumMap.merge(a.getEventId(), a.getApplyPower(), BigDecimal::add);
            stationMap.computeIfAbsent(a.getEventId(), k -> new HashSet<>()).add(a.getStationId());
        }
        events.forEach(e -> e.setApplyPower(sumMap.getOrDefault(e.getEventId(), BigDecimal.ZERO)));
        events.forEach(e -> e.setApplyCount(stationMap.getOrDefault(e.getEventId(), Collections.emptySet()).size()));
    }

    /**
     * 填充响应中事件的实时功率、实时响应电量与完成进度：
     * 实时功率 = dr_execute 最新遥测时间点上全部桩 actual_power 合计；
     * 响应电量 = 平均压降/拉升功率 × 已持续小时；
     * 完成进度 = 响应电量 ÷ 目标电量，四舍五入取整，大于等于100显示100；
     * 压降/拉升量按桩计算（削峰=基线-实际，填谷=实际-基线，负值归0），取全部遥测批次平均得平均功率，
     * 时间取 min(now, endTime) - startTime；基线取已下发桩（status=2）baseline_power
     * （dr_execute/dr_dispatch 不在忽略表，自动按租户隔离）
     */
    private void fillActualPower(List<DrEvent> events) {
        List<DrEvent> running = events.stream()
                .filter(e -> "1".equals(e.getStatus()))
                .collect(Collectors.toList());
        if (running.isEmpty())
        {
            return;
        }
        Set<Long> eventIds = running.stream().map(DrEvent::getEventId).collect(Collectors.toSet());
        Map<Long, Boolean> fillValley = running.stream()
                .collect(Collectors.toMap(DrEvent::getEventId, e -> "2".equals(e.getEventType())));

        // 已下发桩基线（status=2），遥测只对已下发桩生成
        List<DrDispatch> dispatches = drDispatchMapper.selectList(
                new LambdaQueryWrapper<DrDispatch>()
                        .in(DrDispatch::getEventId, eventIds)
                        .eq(DrDispatch::getStatus, "2"));
        Map<Long, BigDecimal> baselineByDispatch = new HashMap<>();
        for (DrDispatch d : dispatches)
        {
            if (d.getDispatchId() != null && d.getBaselinePower() != null)
            {
                baselineByDispatch.put(d.getDispatchId(), d.getBaselinePower());
            }
        }
        if (baselineByDispatch.isEmpty())
        {
            return;
        }

        // 全部遥测点按 (事件, 时间点) 分组：每批压降/拉升量合计 + 实际功率合计
        Map<Long, TreeMap<Long, BigDecimal[]>> batchMap = new HashMap<>();
        for (DrExecute x : drExecuteMapper.selectList(
                new LambdaQueryWrapper<DrExecute>().in(DrExecute::getEventId, eventIds)))
        {
            if (x.getRecordTime() == null || x.getDispatchId() == null || x.getActualPower() == null)
            {
                continue;
            }
            BigDecimal baseline = baselineByDispatch.get(x.getDispatchId());
            if (baseline == null)
            {
                continue;
            }
            Boolean valley = fillValley.get(x.getEventId());
            BigDecimal reduce = valley != null && valley
                    ? x.getActualPower().subtract(baseline)   // 填谷：拉升量 = 实际 - 基线
                    : baseline.subtract(x.getActualPower());   // 削峰：压降量 = 基线 - 实际
            if (reduce.signum() < 0) reduce = BigDecimal.ZERO;
            TreeMap<Long, BigDecimal[]> groups = batchMap.computeIfAbsent(x.getEventId(), k -> new TreeMap<>());
            long t = x.getRecordTime().getTime();
            BigDecimal[] agg = groups.get(t);
            if (agg == null)
            {
                agg = new BigDecimal[]{reduce, x.getActualPower()};
                groups.put(t, agg);
            }
            else
            {
                agg[0] = agg[0].add(reduce);
                agg[1] = agg[1].add(x.getActualPower());
            }
        }

        BigDecimal hundred = new BigDecimal("100");
        long nowMs = System.currentTimeMillis();
        for (DrEvent e : running)
        {
            TreeMap<Long, BigDecimal[]> groups = batchMap.get(e.getEventId());
            if (groups == null || groups.isEmpty())
            {
                e.setActualPower(null);
                e.setProgress(null);
                e.setRespEnergy(null);
                continue;
            }
            // 最后一批的实际功率合计作为实时功率
            e.setActualPower(groups.lastEntry().getValue()[1]);

            // 平均压降/拉升功率 = 全部遥测批次压降量合计 ÷ 批次个数
            BigDecimal totalReduce = BigDecimal.ZERO;
            for (BigDecimal[] agg : groups.values())
            {
                totalReduce = totalReduce.add(agg[0]);
            }
            BigDecimal avgReduce = totalReduce.divide(BigDecimal.valueOf(groups.size()), 2, RoundingMode.HALF_UP);

            // 响应电量 = 平均压降/拉升功率 × 已持续小时（min(now, endTime) - startTime）
            double hours = 0;
            if (e.getStartTime() != null)
            {
                long end = (e.getEndTime() != null && e.getEndTime().getTime() < nowMs)
                        ? e.getEndTime().getTime() : nowMs;
                long ms = end - e.getStartTime().getTime();
                if (ms > 0) hours = ms / 3600000.0;
            }
            BigDecimal energy = avgReduce.multiply(BigDecimal.valueOf(hours));

            // 实时响应电量（保留2位小数随接口返回）
            e.setRespEnergy(energy.setScale(2, RoundingMode.HALF_UP));

            BigDecimal targetEnergy = e.getTargetEnergy() == null ? BigDecimal.ZERO : e.getTargetEnergy();
            Integer progress = null;
            if (targetEnergy.signum() > 0)
            {
                int pct = energy.multiply(hundred).divide(targetEnergy, 0, RoundingMode.HALF_UP).intValue();
                progress = Math.max(0, Math.min(pct, 100)); // 大于等于100显示100
            }
            e.setProgress(progress);
        }
    }

    /**
     * 填充已结束事件的合格场站数与结算金额：
     * 合格场站数 = dr_assess 中该事件 qualified_flag='1' 的记录数（评估每场站一条）；
     * 结算金额 = dr_settlement 中该事件 total_amount 合计，未生成结算单时为空；
     * dr_assess/dr_settlement 不在租户忽略表，自动按租户隔离
     */
    private void fillResultStats(List<DrEvent> events) {
        List<DrEvent> ended = events.stream()
                .filter(e -> "2".equals(e.getStatus()))
                .collect(Collectors.toList());
        if (ended.isEmpty())
        {
            return;
        }
        Set<Long> eventIds = ended.stream().map(DrEvent::getEventId).collect(Collectors.toSet());
        // 合格场站数：效果评估合格记录数
        Map<Long, Integer> qualifiedMap = new HashMap<>();
        for (DrAssess a : drAssessMapper.selectList(
                new LambdaQueryWrapper<DrAssess>().in(DrAssess::getEventId, eventIds)))
        {
            if ("1".equals(a.getQualifiedFlag()))
            {
                qualifiedMap.merge(a.getEventId(), 1, Integer::sum);
            }
        }
        // 结算金额：结算单收益总额合计
        Map<Long, BigDecimal> settleMap = new HashMap<>();
        for (DrSettlement s : drSettlementMapper.selectList(
                new LambdaQueryWrapper<DrSettlement>().in(DrSettlement::getEventId, eventIds)))
        {
            if (s.getTotalAmount() != null)
            {
                settleMap.merge(s.getEventId(), s.getTotalAmount(), BigDecimal::add);
            }
        }
        for (DrEvent e : ended)
        {
            e.setQualifiedCount(qualifiedMap.getOrDefault(e.getEventId(), 0));
            e.setSettleAmount(settleMap.get(e.getEventId()));
        }
    }

    private LambdaQueryWrapper<DrEvent> buildWrapper(DrEvent event)
    {
        LambdaQueryWrapper<DrEvent> wrapper = new LambdaQueryWrapper<>();
        if (event != null)
        {
            // keyword: 模糊搜索事件编号/组织方
            if (StringUtils.isNotBlank(event.getKeyword()))
            {
                String kw = event.getKeyword();
                wrapper.and(w -> w.like(DrEvent::getEventNo, kw).or().like(DrEvent::getOrganizer, kw));
            }
            if (StringUtils.isNotBlank(event.getEventNo()))
            {
                wrapper.like(DrEvent::getEventNo, event.getEventNo());
            }
            if (StringUtils.isNotBlank(event.getEventType()))
            {
                wrapper.eq(DrEvent::getEventType, event.getEventType());
            }
            if (StringUtils.isNotBlank(event.getStatus()))
            {
                wrapper.eq(DrEvent::getStatus, event.getStatus());
            }
            if (StringUtils.isNotBlank(event.getOrganizer()))
            {
                wrapper.like(DrEvent::getOrganizer, event.getOrganizer());
            }
            // 事件开始时间范围筛选
            Object beginTime = event.getParams().get("beginTime");
            Object endTime = event.getParams().get("endTime");
            if (beginTime != null && StringUtils.isNotBlank(beginTime.toString()))
            {
                wrapper.ge(DrEvent::getStartTime, beginTime.toString());
            }
            if (endTime != null && StringUtils.isNotBlank(endTime.toString()))
            {
                wrapper.le(DrEvent::getStartTime, endTime.toString() + " 23:59:59");
            }
        }
        wrapper.orderByDesc(DrEvent::getEventId);
        return wrapper;
    }

    @Override
    public int insertDrEvent(DrEvent event)
    {
        if (StringUtils.isBlank(event.getEventNo()))
        {
            event.setEventNo("DR" + DateUtils.parseDateToStr(DateUtils.YYYYMMDDHHMMSS, new Date())
                    + (int) (Math.random() * 900 + 100));
        }
        if (StringUtils.isBlank(event.getStatus()))
        {
            event.setStatus("0");
        }
        if (StringUtils.isBlank(event.getEventType()))
        {
            event.setEventType("1");
        }
        return drEventMapper.insert(event);
    }

    @Override
    public int updateDrEvent(DrEvent event)
    {
        return drEventMapper.updateById(event);
    }

    @Override
    public int deleteDrEventByEventIds(Long[] eventIds)
    {
        if (eventIds == null || eventIds.length == 0)
        {
            return 0;
        }
        return drEventMapper.deleteBatchIds(Arrays.asList(eventIds));
    }

    @Override
    public Map<String, Object> getEventStats()
    {
        Map<String, Object> map = new HashMap<>();
        // 场站方用户只看自己参与的事件
        Set<Long> myEventIds = getMyEventIds();
        if (myEventIds != null && myEventIds.isEmpty()) {
            map.put("totalCount", 0L);
            map.put("waitCount", 0L); map.put("runCount", 0L); map.put("endCount", 0L); map.put("monthCount", 0L);
            map.put("peakCount", 0L); map.put("valleyCount", 0L);
            map.put("totalTargetPower", BigDecimal.ZERO); map.put("totalTargetEnergy", BigDecimal.ZERO);
            map.put("totalApplyPower", BigDecimal.ZERO); map.put("totalApplyCount", 0L);
            map.put("avgTargetPower", BigDecimal.ZERO); map.put("avgTargetEnergy", BigDecimal.ZERO);
            return map;
        }
        LambdaQueryWrapper<DrEvent> eventWrapper = new LambdaQueryWrapper<>();
        if (myEventIds != null) {
            eventWrapper.in(DrEvent::getEventId, myEventIds);
        } else {
            // 运营商：只看自己租户下参与的事件
            Set<Long> tenantEventIds = getTenantEventIds();
            if (tenantEventIds != null) {
                if (tenantEventIds.isEmpty()) {
                    map.put("totalCount", 0L);
                    map.put("waitCount", 0L); map.put("runCount", 0L); map.put("endCount", 0L); map.put("monthCount", 0L);
                    map.put("peakCount", 0L); map.put("valleyCount", 0L);
                    map.put("totalTargetPower", BigDecimal.ZERO); map.put("totalTargetEnergy", BigDecimal.ZERO);
                    map.put("totalApplyPower", BigDecimal.ZERO); map.put("totalApplyCount", 0L);
                    map.put("avgTargetPower", BigDecimal.ZERO); map.put("avgTargetEnergy", BigDecimal.ZERO);
                    return map;
                }
                eventWrapper.in(DrEvent::getEventId, tenantEventIds);
            }
        }
        List<DrEvent> events = drEventMapper.selectList(eventWrapper);

        long waitCount = 0, runCount = 0, endCount = 0, monthCount = 0, peakCount = 0, valleyCount = 0;
        BigDecimal totalTargetPower = BigDecimal.ZERO;
        BigDecimal totalTargetEnergy = BigDecimal.ZERO;
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date monthStart = cal.getTime();
        cal.add(Calendar.MONTH, 1);
        Date monthEnd = cal.getTime();
        for (DrEvent e : events) {
            if ("0".equals(e.getStatus())) waitCount++;
            else if ("1".equals(e.getStatus())) runCount++;
            else if ("2".equals(e.getStatus())) endCount++;
            if ("1".equals(e.getEventType())) peakCount++;
            else if ("2".equals(e.getEventType())) valleyCount++;
            if (e.getTargetPower() != null) totalTargetPower = totalTargetPower.add(e.getTargetPower());
            if (e.getTargetEnergy() != null) totalTargetEnergy = totalTargetEnergy.add(e.getTargetEnergy());
            if (e.getStartTime() != null && !e.getStartTime().before(monthStart) && e.getStartTime().before(monthEnd)) monthCount++;
        }
        map.put("totalCount", (long) events.size());
        map.put("waitCount", waitCount); map.put("runCount", runCount); map.put("endCount", endCount); map.put("monthCount", monthCount);
        map.put("peakCount", peakCount); map.put("valleyCount", valleyCount);
        map.put("totalTargetPower", totalTargetPower); map.put("totalTargetEnergy", totalTargetEnergy);
        int cnt = events.size();
        map.put("avgTargetPower", cnt > 0 ? totalTargetPower.divide(BigDecimal.valueOf(cnt), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO);
        map.put("avgTargetEnergy", cnt > 0 ? totalTargetEnergy.divide(BigDecimal.valueOf(cnt), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO);

        // 累计申报 / 累计参与场站
        BigDecimal totalApplyPower = BigDecimal.ZERO;
        long totalApplyCount = 0;
        if (!events.isEmpty()) {
            Set<Long> eventIds = events.stream().map(DrEvent::getEventId).collect(Collectors.toSet());
            List<DrApply> applies = drApplyMapper.selectList(
                    new LambdaQueryWrapper<DrApply>().in(DrApply::getEventId, eventIds));
            Set<Long> stationIds = new HashSet<>();
            for (DrApply a : applies) {
                if (a.getApplyPower() != null) totalApplyPower = totalApplyPower.add(a.getApplyPower());
                if (a.getStationId() != null) stationIds.add(a.getStationId());
            }
            totalApplyCount = stationIds.size();
        }
        map.put("totalApplyPower", totalApplyPower);
        map.put("totalApplyCount", totalApplyCount);
        return map;
    }

    /**
     * 运营商用户：获取本租户下所有场站参与过的事件ID集合；平台/非租户用户返回 null
     */
    private Set<Long> getTenantEventIds() {
        try {
            Long tenantId = SecurityUtils.getTenantId();
            if (tenantId == null || tenantId == 9999L) return null;
        } catch (Exception e) { return null; }
        // 查本租户下所有场站
        List<DrStation> stations = drStationMapper.selectList(
                new LambdaQueryWrapper<DrStation>()
                        .eq(DrStation::getTenantId, SecurityUtils.getTenantId()));
        if (stations.isEmpty()) return Collections.emptySet();
        Set<Long> stationIds = stations.stream().map(DrStation::getStationId).collect(Collectors.toSet());
        List<DrApply> applies = drApplyMapper.selectList(
                new LambdaQueryWrapper<DrApply>()
                        .in(DrApply::getStationId, stationIds));
        return applies.stream().map(DrApply::getEventId).collect(Collectors.toSet());
    }

    private Set<Long> getMyEventIds() {
        try {
            SysUser sysUser = SecurityUtils.getLoginUser().getSysUser();
            if (sysUser == null || sysUser.getIsStation() == null || sysUser.getIsStation() != 1) return null;
        } catch (Exception e) { return null; }
        Long userId = SecurityUtils.getUserId();
        if (userId == null) return null;
        List<DrStation> myStations = drStationMapper.selectList(
                new LambdaQueryWrapper<DrStation>()
                        .eq(DrStation::getUserId, userId));
        if (myStations.isEmpty()) return Collections.emptySet();
        Set<Long> stationIds = myStations.stream().map(DrStation::getStationId).collect(Collectors.toSet());
        List<DrApply> applies = drApplyMapper.selectList(
                new LambdaQueryWrapper<DrApply>()
                        .in(DrApply::getStationId, stationIds));
        return applies.stream().map(DrApply::getEventId).collect(Collectors.toSet());
    }

    @Override
    public int updateEventStatus(Long eventId, String status)
    {
        DrEvent event = drEventMapper.selectById(eventId);
        if (event == null)
        {
            return 0;
        }
        // 只允许按顺序流转：0→1→2
        String currentStatus = event.getStatus();
        if ("0".equals(currentStatus) && "1".equals(status))
        {
            event.setStatus("1");
            return drEventMapper.updateById(event);
        }
        if ("1".equals(currentStatus) && "2".equals(status))
        {
            event.setStatus("2");
            return drEventMapper.updateById(event);
        }
        return 0;
    }

    @Override
    @Scheduled(cron = "0 */1 * * * ?") // 每分钟执行一次
    public int autoUpdateEventStatus()
    {
        int count = 0;
        Date now = new Date();
        // 待响应→响应中：startTime <= now && status='0'
        LambdaUpdateWrapper<DrEvent> toRunning = new LambdaUpdateWrapper<>();
        toRunning.eq(DrEvent::getStatus, "0").le(DrEvent::getStartTime, now);
        count += drEventMapper.update(null, toRunning.set(DrEvent::getStatus, "1"));
        // 响应中→已结束：endTime <= now && status='1'
        LambdaUpdateWrapper<DrEvent> toEnd = new LambdaUpdateWrapper<>();
        toEnd.eq(DrEvent::getStatus, "1").le(DrEvent::getEndTime, now);
        count += drEventMapper.update(null, toEnd.set(DrEvent::getStatus, "2"));
        return count;
    }
}