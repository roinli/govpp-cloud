package com.witos.vpp.event.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.witos.common.core.text.Convert;
import com.witos.common.core.utils.ServletUtils;
import com.witos.common.core.utils.StringUtils;
import com.witos.common.mybatisplus.constant.MybatisPageConstants;
import com.witos.common.security.utils.SecurityUtils;
import com.witos.system.api.domain.SysUser;
import com.witos.vpp.event.domain.DrPowerDaily;
import com.witos.vpp.event.domain.DrPowerMinute;
import com.witos.vpp.event.domain.DrResource;
import com.witos.vpp.event.domain.DrStation;
import com.witos.vpp.event.mapper.DrPowerDailyMapper;
import com.witos.vpp.event.mapper.DrPowerMinuteMapper;
import com.witos.vpp.event.mapper.DrResourceMapper;
import com.witos.vpp.event.mapper.DrStationMapper;
import com.witos.vpp.event.service.IDrResourceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/**
 * VPP 资源台账 Service业务层处理
 *
 * @author witos
 */
@Service
public class DrResourceServiceImpl implements IDrResourceService
{
    @Autowired
    private DrResourceMapper drResourceMapper;

    @Autowired
    private DrStationMapper drStationMapper;

    @Autowired
    private DrPowerDailyMapper drPowerDailyMapper;

    @Autowired
    private DrPowerMinuteMapper drPowerMinuteMapper;

    /**
     * 当前登录场站用户名下的场站ID集合；非场站用户返回 null（不做场站过滤）
     */
    private Set<Long> getMyStationIds() {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) return null;
        SysUser sysUser = SecurityUtils.getLoginUser().getSysUser();
        if (sysUser == null || sysUser.getIsStation() == null || sysUser.getIsStation() != 1) return null;
        Set<Long> ids = new HashSet<>();
        List<DrStation> myStations = drStationMapper.selectList(new LambdaQueryWrapper<DrStation>().eq(DrStation::getUserId, userId));
        for (DrStation s : myStations) ids.add(s.getStationId());
        return ids;
    }

    @Override
    public DrResource selectDrResourceByResourceId(Long resourceId)
    {
        return drResourceMapper.selectById(resourceId);
    }

    @Override
    public IPage<DrResource> selectDrResourcePage(DrResource resource)
    {
        Page<DrResource> mpPage = new Page<>(
                Convert.toLong(ServletUtils.getParameterToInt(MybatisPageConstants.PAGE_NUM), 1L),
                Convert.toLong(ServletUtils.getParameterToInt(MybatisPageConstants.PAGE_SIZE), 10L));
        Set<Long> myIds = getMyStationIds();
        if (myIds != null) {
            // 场站方未绑定场站时直接返回空，避免误查全量数据
            if (myIds.isEmpty()) return mpPage;
            if (resource.getStationId() == null) resource.setStationIds(new ArrayList<>(myIds));
        }
        return drResourceMapper.selectDrResourcePage(mpPage, resource);
    }

    @Override
    public List<DrResource> selectDrResourceList(DrResource resource)
    {
        Set<Long> myIds = getMyStationIds();
        if (myIds != null) {
            // 场站方未绑定场站时直接返回空，避免误查全量数据
            if (myIds.isEmpty()) return new ArrayList<>();
            if (resource.getStationId() == null) resource.setStationIds(new ArrayList<>(myIds));
        }
        return drResourceMapper.selectDrResourceList(resource);
    }

    @Override
    public int insertDrResource(DrResource resource)
    {
        if (StringUtils.isBlank(resource.getResourceCode()))
        {
            resource.setResourceCode("RES-" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date())
                    + (int) (Math.random() * 900 + 100));
        }
        // 根据场站ID自动填充场站名称
        if (resource.getStationId() != null)
        {
            DrStation station = drStationMapper.selectById(resource.getStationId());
            if (station != null)
            {
                resource.setStationName(station.getStationName());
            }
        }
        resource.setAdjustablePower(calcAdjustablePower(resource));
        resource.setAvg7dPower(calcAdjustablePower(resource));
        resource.setValleyRate(calcValleyRate(resource));
        resource.setValleyPower(calcValleyPower(resource));
        resource.setParticipateFlag("1");
        if (resource.getAdjustableRate() == null)
        {
            resource.setAdjustableRate(new BigDecimal("0.80"));
        }
        return drResourceMapper.insert(resource);
    }

    @Override
    public int updateDrResource(DrResource resource)
    {
        resource.setAdjustablePower(calcAdjustablePower(resource));
        resource.setAvg7dPower(calcAdjustablePower(resource));
        resource.setValleyRate(calcValleyRate(resource));
        resource.setValleyPower(calcValleyPower(resource));
        return drResourceMapper.updateById(resource);
    }

    @Override
    public int deleteDrResourceByIds(Long[] resourceIds)
    {
        return drResourceMapper.deleteDrResourceByIds(resourceIds);
    }

    @Override
    public int deleteDrResourceById(Long resourceId)
    {
        return drResourceMapper.deleteById(resourceId);
    }

    @Override
    public int updateParticipateStatus(Long[] resourceIds, String participateStatus)
    {
        LambdaUpdateWrapper<DrResource> wrapper = new LambdaUpdateWrapper<>();
        wrapper.in(DrResource::getResourceId, Arrays.asList(resourceIds));
        wrapper.set(DrResource::getParticipateStatus, participateStatus);
        // 启停联动设备状态：启用即上线，停用即离线，避免启用后仍显示离线/故障、停用后仍显示在线
        wrapper.set(DrResource::getDeviceStatus, "1".equals(participateStatus) ? "1" : "2");
        return drResourceMapper.update(null, wrapper);
    }

    @Override
    public Map<String, Object> getResourceStats(Collection<Long> stationIds)
    {
        // 场站方用户限定只看自己场站的资源
        if (isStationUser()) {
            if (stationIds == null) stationIds = getMyStationIds();
            if (stationIds.isEmpty()) {
                Map<String, Object> empty = new HashMap<>();
                empty.put("totalAdjustablePower", BigDecimal.ZERO);
                empty.put("totalValleyPower", BigDecimal.ZERO);
                empty.put("stationCount", 0L);
                empty.put("pileCount", 0L);
                return empty;
            }
        }
        return drResourceMapper.selectResourceStats(stationIds);
    }

    /**
     * 总负荷曲线（充电负荷实时口径）：当前用户可见场站下所有资源的分钟级负荷按15分钟聚合。
     * 平台/运营商由租户插件自动过滤；场站用户只允许查自己场站；分钟数据由
     * {@link com.witos.vpp.event.task.ResourcePowerMinuteSimulateTask} 模拟写入。
     */
    @Override
    public List<Map<String, Object>> getLoadCurve(String date, String ownerType)
    {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        if (StringUtils.isNotBlank(date))
        {
            try
            {
                cal.setTime(new SimpleDateFormat("yyyy-MM-dd").parse(date));
            }
            catch (ParseException e)
            {
                // 日期格式非法时按今天处理
            }
        }
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date startTime = cal.getTime();
        cal.add(Calendar.DAY_OF_MONTH, 1);
        Date endTime = cal.getTime();

        // 场站用户只能看自己场站的资源；未绑定场站返回空，避免误查全量
        Set<Long> myIds = getMyStationIds();
        LambdaQueryWrapper<DrResource> query = new LambdaQueryWrapper<>();
        if (myIds != null)
        {
            if (myIds.isEmpty())
            {
                return new ArrayList<>();
            }
            query.in(DrResource::getStationId, myIds);
        }
        // 资源归属过滤：运营商桩（非私有）、私有桩；空表示全部
        if ("operator".equals(ownerType))
        {
            query.and(w -> w.ne(DrResource::getResourceOwnerType, "2")
                    .or().isNull(DrResource::getResourceOwnerType));
        }
        else if ("private".equals(ownerType))
        {
            query.eq(DrResource::getResourceOwnerType, "2");
        }
        List<DrResource> resources = drResourceMapper.selectList(query);
        if (resources.isEmpty())
        {
            return new ArrayList<>();
        }
        List<Long> resourceIds = new ArrayList<>();
        for (DrResource r : resources)
        {
            resourceIds.add(r.getResourceId());
        }

        List<DrPowerMinute> rows = drPowerMinuteMapper.selectLoadGroupByQuarter(resourceIds, startTime, endTime);
        SimpleDateFormat hmFmt = new SimpleDateFormat("HH:mm");
        List<Map<String, Object>> points = new ArrayList<>();
        for (DrPowerMinute row : rows)
        {
            Map<String, Object> point = new HashMap<>();
            point.put("time", hmFmt.format(row.getPowerTime()));
            point.put("load", row.getLoadPower() == null ? BigDecimal.ZERO : row.getLoadPower());
            points.add(point);
        }
        return points;
    }

    private boolean isStationUser() {
        try {
            SysUser sysUser = SecurityUtils.getLoginUser().getSysUser();
            return sysUser != null && sysUser.getIsStation() != null && sysUser.getIsStation() == 1;
        } catch (Exception e) { return false; }
    }

    @Override
    public List<Map<String, Object>> getStationCapacityList()
    {
        return drResourceMapper.selectStationCapacityList();
    }

    @Override
    public List<Map<String, Object>> getStationRank(String type, int limit)
    {
        Set<Long> myIds = getMyStationIds();
        if (myIds != null) {
            // 场站方未绑定场站时直接返回空，避免误查全量数据
            if (myIds.isEmpty()) return new ArrayList<>();
        }
        return drResourceMapper.selectStationRank(myIds, "valley".equals(type) ? "valley" : "peak", limit);
    }

    @Override
    public List<Map<String, Object>> getOperatorEventPie()
    {
        // 场站用户：只看自己场站的场站级占比
        Set<Long> myIds = getMyStationIds();
        if (myIds != null) {
            if (myIds.isEmpty()) return new ArrayList<>();
            List<Map<String, Object>> list = drResourceMapper.selectStationEventPie(myIds, null);
            for (Map<String, Object> m : list) m.put("pieType", "station");
            return list;
        }
        // 平台(9999)：运营商级占比
        Long tenantId = SecurityUtils.getTenantId();
        if (tenantId != null && tenantId == 9999L) {
            List<Map<String, Object>> list = drResourceMapper.selectOperatorEventPie(null);
            for (Map<String, Object> m : list) m.put("pieType", "operator");
            return list;
        }
        // 运营商：场站级占比（只看自己租户下场站）
        List<Map<String, Object>> list = drResourceMapper.selectStationEventPie(null, tenantId);
        for (Map<String, Object> m : list) m.put("pieType", "station");
        return list;
    }

    @Override
    public Map<Long, List<Map<String, Object>>> getPower7dTrend(Long[] resourceIds)
    {
        Map<Long, List<Map<String, Object>>> result = new HashMap<>();
        if (resourceIds == null || resourceIds.length == 0)
        {
            return result;
        }
        List<Long> idList = Arrays.asList(resourceIds);
        Set<Long> myIds = getMyStationIds();

        // 场站用户只能看自己场站的资源；平台/运营商由租户插件自动过滤
        LambdaQueryWrapper<DrResource> query = new LambdaQueryWrapper<>();
        query.in(DrResource::getResourceId, idList);
        if (myIds != null)
        {
            if (myIds.isEmpty())
            {
                return result;
            }
            query.in(DrResource::getStationId, myIds);
        }
        List<DrResource> resources = drResourceMapper.selectList(query);
        if (resources.isEmpty())
        {
            return result;
        }

        // 最近7天（今日 + 前6天），起止均取当日0点
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date endDate = cal.getTime();
        cal.add(Calendar.DAY_OF_MONTH, -6);
        Date startDate = cal.getTime();

        List<DrPowerDaily> rows = drPowerDailyMapper.selectByResourceRange(idList, startDate, endDate);
        SimpleDateFormat dayFmt = new SimpleDateFormat("yyyy-MM-dd");
        Map<Long, Map<String, DrPowerDaily>> rowsByResource = new HashMap<>();
        for (DrPowerDaily row : rows)
        {
            rowsByResource.computeIfAbsent(row.getResourceId(), k -> new HashMap<>())
                    .put(dayFmt.format(row.getPowerDate()), row);
        }

        // 缺失日期（如新增资源、任务未跑）以资源7日基线回退，保证折线完整且刷新不变
        for (DrResource r : resources)
        {
            Map<String, DrPowerDaily> dayMap = rowsByResource.get(r.getResourceId());
            BigDecimal baseline = defaultBaseline(r);
            List<Map<String, Object>> points = new ArrayList<>();
            for (int i = 0; i < 7; i++)
            {
                cal.setTime(startDate);
                cal.add(Calendar.DAY_OF_MONTH, i);
                String day = dayFmt.format(cal.getTime());
                DrPowerDaily row = dayMap == null ? null : dayMap.get(day);
                Map<String, Object> point = new HashMap<>();
                point.put("date", day);
                point.put("power", row != null ? row.getAvgPower() : baseline);
                point.put("peakPower", row == null ? baseline : row.getPeakPower());
                points.add(point);
            }
            result.put(r.getResourceId(), points);
        }
        return result;
    }

    /**
     * 无历史功率记录时回退使用的基线（7日平均功率口径）
     */
    private BigDecimal defaultBaseline(DrResource r)
    {
        if (r.getAvg7dPower() != null && r.getAvg7dPower().signum() > 0)
        {
            return r.getAvg7dPower();
        }
        return r.getRatedPower() == null ? BigDecimal.ZERO : r.getRatedPower();
    }

    /**
     * 可调功率 = 额定功率 × 可调系数（保留两位小数）
     */
    private BigDecimal calcAdjustablePower(DrResource resource)
    {
        if (resource.getRatedPower() == null) return BigDecimal.ZERO;
        BigDecimal rate = resource.getAdjustableRate() == null ? new BigDecimal("0.80") : resource.getAdjustableRate();
        return resource.getRatedPower().multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calcValleyRate(DrResource resource)
    {
        BigDecimal peak = resource.getAdjustableRate() == null ? new BigDecimal("0.80") : resource.getAdjustableRate();
        BigDecimal valley = BigDecimal.ONE.subtract(peak).setScale(2, RoundingMode.HALF_UP);
        if (valley.signum() < 0) valley = BigDecimal.ZERO;
        return valley;
    }

    private BigDecimal calcValleyPower(DrResource resource)
    {
        if (resource.getRatedPower() == null) return BigDecimal.ZERO;
        return resource.getRatedPower().multiply(calcValleyRate(resource)).setScale(2, RoundingMode.HALF_UP);
    }
}