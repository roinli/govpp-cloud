package com.witos.vpp.dispatch.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.witos.common.core.text.Convert;
import com.witos.common.core.utils.DateUtils;
import com.witos.common.core.utils.ServletUtils;
import com.witos.common.core.utils.StringUtils;
import com.witos.common.mybatisplus.constant.MybatisPageConstants;
import com.witos.vpp.dispatch.domain.DrDispatch;
import com.witos.vpp.dispatch.domain.DrEvent;
import com.witos.vpp.dispatch.domain.DrExecute;
import com.witos.vpp.dispatch.domain.DrStation;
import com.witos.vpp.dispatch.mapper.DrDispatchMapper;
import com.witos.vpp.dispatch.mapper.DrEventMapper;
import com.witos.vpp.dispatch.mapper.DrStationMapper;
import com.witos.vpp.dispatch.mapper.DrExecuteMapper;
import com.witos.vpp.dispatch.service.IDrExecuteService;
import com.witos.common.security.utils.SecurityUtils;
import com.witos.system.api.domain.SysUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/**
 * VPP执行监测Service业务层处理
 *
 * @author witos
 * @date 2026-08-02
 */
@Service
public class DrExecuteServiceImpl implements IDrExecuteService
{
    @Autowired
    private DrExecuteMapper drExecuteMapper;
    @Autowired
    private DrDispatchMapper drDispatchMapper;
    @Autowired
    private DrEventMapper drEventMapper;

    @Autowired
    private DrStationMapper drStationMapper;

    @Override
    public DrExecute selectDrExecuteByExecuteId(Long executeId)
    {
        return drExecuteMapper.selectById(executeId);
    }

    @Override
    public IPage<DrExecute> selectDrExecutePage(DrExecute execute)
    {
        Page<DrExecute> page = new Page<>(Convert.toLong(ServletUtils.getParameterToInt(MybatisPageConstants.PAGE_NUM), 1L),
                Convert.toLong(ServletUtils.getParameterToInt(MybatisPageConstants.PAGE_SIZE), 10L));
        return drExecuteMapper.selectPage(page, buildWrapper(execute));
    }

    @Override
    public List<DrExecute> selectDrExecuteList(DrExecute execute)
    {
        return drExecuteMapper.selectList(buildWrapper(execute));
    }

    private <T> void addStationFilter(LambdaQueryWrapper<T> wrapper, SFunction<T, ?> stationIdGetter) {
        try {
            SysUser sysUser = SecurityUtils.getLoginUser().getSysUser();
            if (sysUser != null && sysUser.getIsStation() != null && sysUser.getIsStation() == 1) {
                Set<Long> ids = drStationMapper.selectList(new LambdaQueryWrapper<DrStation>()
                        .eq(DrStation::getUserId, SecurityUtils.getUserId()))
                        .stream().map(DrStation::getStationId).collect(Collectors.toSet());
                if (!ids.isEmpty()) wrapper.in(stationIdGetter, ids);
            }
        } catch (Exception ignored) {}
    }

    private LambdaQueryWrapper<DrExecute> buildWrapper(DrExecute execute)
    {
        LambdaQueryWrapper<DrExecute> wrapper = new LambdaQueryWrapper<>();
        if (execute != null)
        {
            if (execute.getEventId() != null)
            {
                wrapper.eq(DrExecute::getEventId, execute.getEventId());
            }
            if (execute.getStationId() != null)
            {
                wrapper.eq(DrExecute::getStationId, execute.getStationId());
            }
            if (StringUtils.isNotBlank(execute.getPileNo()))
            {
                wrapper.like(DrExecute::getPileNo, execute.getPileNo());
            }
        }
        // 场站方只查自己场站
        try {
            SysUser sysUser = SecurityUtils.getLoginUser().getSysUser();
            if (sysUser != null && sysUser.getIsStation() != null && sysUser.getIsStation() == 1) {
                List<DrStation> myStations = drStationMapper.selectList(new LambdaQueryWrapper<DrStation>()
                        .eq(DrStation::getUserId, SecurityUtils.getUserId()));
                if (!myStations.isEmpty()) {
                    wrapper.in(DrExecute::getStationId, myStations.stream().map(DrStation::getStationId).collect(Collectors.toList()));
                }
            }
        } catch (Exception ignored) {}
        addStationFilter(wrapper, DrExecute::getStationId);
        wrapper.orderByAsc(DrExecute::getRecordTime);
        return wrapper;
    }

    @Override
    public int insertDrExecute(DrExecute execute)
    {
        if (execute.getRecordTime() == null)
        {
            execute.setRecordTime(new Date());
        }
        // 自动带出目标功率
        if (execute.getPlanPower() == null && execute.getDispatchId() != null)
        {
            DrDispatch dispatch = drDispatchMapper.selectById(execute.getDispatchId());
            if (dispatch != null)
            {
                execute.setPlanPower(dispatch.getTargetPower());
            }
        }
        return drExecuteMapper.insert(execute);
    }

    @Override
    public List<Map<String, Object>> selectDeviationList(Long eventId, BigDecimal thresholdPercent, Long stationId, String pileNo)
    {
        BigDecimal threshold = thresholdPercent == null ? new BigDecimal("20") : thresholdPercent;
        DrEvent event = drEventMapper.selectById(eventId);
        boolean isPeak = event == null || !"2".equals(event.getEventType()); // 削峰(1)或默认

        LambdaQueryWrapper<DrExecute> wrapper = new LambdaQueryWrapper<DrExecute>()
                .eq(DrExecute::getEventId, eventId);
        if (stationId != null)
        {
            wrapper.eq(DrExecute::getStationId, stationId);
        }
        if (StringUtils.isNotBlank(pileNo))
        {
            wrapper.eq(DrExecute::getPileNo, pileNo);
        }
        wrapper.orderByAsc(DrExecute::getRecordTime);
        addStationFilter(wrapper, DrExecute::getStationId);
        List<DrExecute> list = drExecuteMapper.selectList(wrapper);

        List<Map<String, Object>> result = new ArrayList<>();
        for (DrExecute e : list)
        {
            Map<String, Object> row = new HashMap<>();
            row.put("pileNo", e.getPileNo());
            row.put("stationName", e.getStationName());
            row.put("recordTime", DateUtils.parseDateToStr("yyyy-MM-dd HH:mm:ss", e.getRecordTime()));
            row.put("planPower", e.getPlanPower());
            row.put("actualPower", e.getActualPower());
            BigDecimal plan = e.getPlanPower() == null ? BigDecimal.ZERO : e.getPlanPower();
            BigDecimal actual = e.getActualPower() == null ? BigDecimal.ZERO : e.getActualPower();
            BigDecimal deviation = plan.signum() == 0 ? BigDecimal.ZERO
                    : actual.subtract(plan).multiply(new BigDecimal("100")).divide(plan, 2, RoundingMode.HALF_UP);
            row.put("deviationPercent", deviation);
            // 削峰：实际>目标才告警（没压够）；填谷：实际<目标才告警（没抬够）
            boolean alert = isPeak
                    ? deviation.compareTo(threshold) > 0
                    : deviation.negate().compareTo(threshold) > 0;
            row.put("alertFlag", alert ? "1" : "0");
            result.add(row);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> selectPileOptions(Long eventId)
    {
        // 桩清单来自已下发分配（行数≈桩数，轻量），按场站分组排序
        LambdaQueryWrapper<DrDispatch> dw = new LambdaQueryWrapper<DrDispatch>()
                .eq(DrDispatch::getEventId, eventId)
                .eq(DrDispatch::getStatus, "2")
                .orderByAsc(DrDispatch::getStationId);
        addStationFilter(dw, DrDispatch::getStationId);
        List<DrDispatch> dispatches = drDispatchMapper.selectList(dw);

        // 去重：保留场站名+桩号组合（不同场站可能撞桩号）
        Map<String, Map<String, Object>> seen = new LinkedHashMap<>();
        for (DrDispatch d : dispatches)
        {
            String key = (d.getStationName() == null ? "" : d.getStationName()) + "|" + (d.getPileNo() == null ? "" : d.getPileNo());
            if (seen.containsKey(key))
            {
                continue;
            }
            Map<String, Object> row = new HashMap<>();
            row.put("stationId", d.getStationId());
            row.put("stationName", d.getStationName());
            row.put("pileNo", d.getPileNo());
            seen.put(key, row);
        }
        return new ArrayList<>(seen.values());
    }

    @Override
    public int updateDrExecute(DrExecute execute)
    {
        return drExecuteMapper.updateById(execute);
    }

    @Override
    public int deleteDrExecuteByExecuteIds(Long[] executeIds)
    {
        if (executeIds == null || executeIds.length == 0)
        {
            return 0;
        }
        return drExecuteMapper.deleteBatchIds(Arrays.asList(executeIds));
    }

    @Override
    public int generateSimulatedData(Long eventId)
    {
        DrEvent event = drEventMapper.selectById(eventId);
        if (event == null || !"1".equals(event.getStatus()))
        {
            return 0; // 只有响应中的事件才造数据
        }

        // 获取该事件下所有已下发的分配记录
        List<DrDispatch> dispatches = drDispatchMapper.selectList(new LambdaQueryWrapper<DrDispatch>()
                .eq(DrDispatch::getEventId, eventId)
                .eq(DrDispatch::getStatus, "2")); // 已下发

        if (dispatches.isEmpty())
        {
            return 0;
        }

        // 生成本时刻的遥测数据点
        Date now = new Date();
        int count = 0;
        Random rand = new Random();

        for (DrDispatch d : dispatches)
        {
            // 实际功率 = 目标功率 ± 随机波动（±20%以内，模拟真实桩的响应偏差）
            BigDecimal target = d.getTargetPower() == null ? BigDecimal.ZERO : d.getTargetPower();
            double fluctuation = (rand.nextDouble() * 0.40 - 0.20); // -20% ~ +20%
            BigDecimal actual = target.multiply(BigDecimal.valueOf(1 + fluctuation)).setScale(2, RoundingMode.HALF_UP);
            if (actual.compareTo(BigDecimal.ZERO) < 0)
            {
                actual = BigDecimal.ZERO;
            }

            DrExecute exec = new DrExecute();
            exec.setEventId(eventId);
            exec.setDispatchId(d.getDispatchId());
            exec.setStationId(d.getStationId());
            exec.setStationName(d.getStationName());
            exec.setPileNo(d.getPileNo());
            exec.setPlanPower(target);
            exec.setActualPower(actual);
            exec.setRecordTime(now);
            exec.setTenantId(d.getTenantId());
            drExecuteMapper.insert(exec);
            count++;
        }
        return count;
    }

    @Override
    @Scheduled(cron = "0 */1 * * * ?") // 每分钟一次
    public int autoGenerateForActiveEvents()
    {
        // 找出所有"响应中"的事件
        List<DrEvent> activeEvents = drEventMapper.selectList(new LambdaQueryWrapper<DrEvent>()
                .eq(DrEvent::getStatus, "1"));

        int total = 0;
        for (DrEvent event : activeEvents)
        {
            total += generateSimulatedData(event.getEventId());
        }
        return total;
    }
}
