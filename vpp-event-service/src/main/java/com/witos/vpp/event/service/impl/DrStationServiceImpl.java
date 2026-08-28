package com.witos.vpp.event.service.impl;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.witos.common.core.text.Convert;
import com.witos.common.core.utils.ServletUtils;
import com.witos.common.mybatisplus.constant.MybatisPageConstants;
import com.witos.common.security.utils.SecurityUtils;
import com.witos.system.api.domain.SysUser;
import com.witos.vpp.event.domain.DrStation;
import com.witos.vpp.event.mapper.DrStationMapper;
import com.witos.vpp.event.service.IDrStationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/**
 * VPP 场站管理 Service业务层处理
 *
 * @author witos
 */
@Service
public class DrStationServiceImpl implements IDrStationService
{
    @Autowired
    private DrStationMapper drStationMapper;

    @Override
    public Set<Long> getMyStationIds() {
        Set<Long> ids = new HashSet<>();
        Long userId = SecurityUtils.getUserId();
        if (userId == null) return ids;
        try {
            SysUser sysUser = SecurityUtils.getLoginUser().getSysUser();
            if (sysUser == null || sysUser.getIsStation() == null || sysUser.getIsStation() != 1) return ids;
        } catch (Exception e) { return ids; }
        List<DrStation> myStations = drStationMapper.selectList(new LambdaQueryWrapper<DrStation>()
                .eq(DrStation::getUserId, userId));
        for (DrStation s : myStations) ids.add(s.getStationId());
        return ids;
    }

    @Override
    public DrStation selectDrStationByStationId(Long stationId)
    {
        return drStationMapper.selectById(stationId);
    }

    @Override
    public IPage<DrStation> selectDrStationPage(DrStation station)
    {
        Page<DrStation> mpPage = new Page<>(
                Convert.toLong(ServletUtils.getParameterToInt(MybatisPageConstants.PAGE_NUM), 1L),
                Convert.toLong(ServletUtils.getParameterToInt(MybatisPageConstants.PAGE_SIZE), 10L));
        // 场站方用户只查自己的场站
        if (isStationUser()) station.setUserId(SecurityUtils.getUserId());
        return drStationMapper.selectDrStationPage(mpPage, station);
    }

    @Override
    public List<DrStation> selectDrStationList(DrStation station)
    {
        if (isStationUser()) station.setUserId(SecurityUtils.getUserId());
        return drStationMapper.selectDrStationList(station);
    }

    private boolean isStationUser() {
        try {
            SysUser sysUser = SecurityUtils.getLoginUser().getSysUser();
            return sysUser != null && sysUser.getIsStation() != null && sysUser.getIsStation() == 1;
        } catch (Exception e) { return false; }
    }

    @Override
    public int insertDrStation(DrStation station)
    {
        return drStationMapper.insert(station);
    }

    @Override
    public int updateDrStation(DrStation station)
    {
        return drStationMapper.updateById(station);
    }

    @Override
    public int deleteDrStationByIds(Long[] stationIds)
    {
        return drStationMapper.deleteBatchIds(Arrays.asList(stationIds));
    }

    @Override
    public Map<String, Object> getStationStats(Collection<Long> stationIds)
    {
        // 场站方用户没有场站时直接返回 0，防止查到别人数据
        if (isStationUser() && (stationIds == null || stationIds.isEmpty())) {
            Map<String, Object> empty = new HashMap<>();
            empty.put("stationCount", 0L);
            empty.put("totalCapacity", BigDecimal.ZERO);
            empty.put("pileCount", 0L);
            return empty;
        }
        return drStationMapper.selectStationStats(stationIds);
    }
}
