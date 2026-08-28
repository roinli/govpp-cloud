package com.witos.vpp.event.service.impl;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.witos.common.core.exception.ServiceException;
import com.witos.common.core.text.Convert;
import com.witos.common.core.utils.ServletUtils;
import com.witos.common.core.utils.StringUtils;
import com.witos.common.mybatisplus.constant.MybatisPageConstants;
import com.witos.common.security.utils.SecurityUtils;
import com.witos.system.api.domain.SysUser;
import com.witos.vpp.event.domain.DrApply;
import com.witos.vpp.event.domain.DrEvent;
import com.witos.vpp.event.domain.DrStation;
import com.witos.vpp.event.mapper.DrApplyMapper;
import com.witos.vpp.event.mapper.DrEventMapper;
import com.witos.vpp.event.mapper.DrStationMapper;
import com.witos.vpp.event.service.IDrApplyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

@Service
public class DrApplyServiceImpl implements IDrApplyService
{
    @Autowired
    private DrApplyMapper drApplyMapper;

    @Autowired
    private DrEventMapper drEventMapper;

    @Autowired
    private DrStationMapper drStationMapper;

    private Set<Long> getMyStationIds() {
        Set<Long> ids = new HashSet<>();
        Long userId = SecurityUtils.getUserId();
        if (userId == null) return ids;
        SysUser sysUser = SecurityUtils.getLoginUser().getSysUser();
        if (sysUser == null || sysUser.getIsStation() == null || sysUser.getIsStation() != 1) return ids;
        List<DrStation> myStations = drStationMapper.selectList(new LambdaQueryWrapper<DrStation>().eq(DrStation::getUserId, userId));
        for (DrStation s : myStations) ids.add(s.getStationId());
        return ids;
    }

    @Override
    public DrApply selectDrApplyByApplyId(Long applyId)
    {
        return drApplyMapper.selectById(applyId);
    }

    @Override
    public IPage<DrApply> selectDrApplyPage(DrApply apply)
    {
        Page<DrApply> page = new Page<>(
                Convert.toLong(ServletUtils.getParameterToInt(MybatisPageConstants.PAGE_NUM), 1L),
                Convert.toLong(ServletUtils.getParameterToInt(MybatisPageConstants.PAGE_SIZE), 10L));
        Set<Long> myIds = getMyStationIds();
        if (!myIds.isEmpty()) apply.setStationId(myIds.iterator().next());
        IPage<DrApply> result = drApplyMapper.selectPage(page, buildWrapper(apply));
        if (myIds.size() > 1) result.setRecords(result.getRecords().stream().filter(a -> myIds.contains(a.getStationId())).collect(Collectors.toList()));
        return result;
    }

    @Override
    public List<DrApply> selectDrApplyList(DrApply apply)
    {
        Set<Long> myIds = getMyStationIds();
        if (!myIds.isEmpty()) apply.setStationId(myIds.iterator().next());
        List<DrApply> result = drApplyMapper.selectList(buildWrapper(apply));
        if (myIds.size() > 1) result = result.stream().filter(a -> myIds.contains(a.getStationId())).collect(Collectors.toList());
        return result;
    }

    private LambdaQueryWrapper<DrApply> buildWrapper(DrApply apply)
    {
        LambdaQueryWrapper<DrApply> wrapper = new LambdaQueryWrapper<>();
        if (apply != null)
        {
            if (apply.getEventId() != null)
            {
                wrapper.eq(DrApply::getEventId, apply.getEventId());
            }
            if (apply.getStationId() != null)
            {
                wrapper.eq(DrApply::getStationId, apply.getStationId());
            }
            if (StringUtils.isNotBlank(apply.getStationName()))
            {
                wrapper.like(DrApply::getStationName, apply.getStationName());
            }
            if (StringUtils.isNotBlank(apply.getStatus()))
            {
                wrapper.eq(DrApply::getStatus, apply.getStatus());
            }
        }
        wrapper.orderByDesc(DrApply::getApplyId);
        return wrapper;
    }

    @Override
    public int insertDrApply(DrApply apply)
    {
        if (apply.getEventId() != null)
        {
            DrEvent event = drEventMapper.selectById(apply.getEventId());
            if (event != null)
            {
                if (!"0".equals(event.getStatus()))
                {
                    throw new ServiceException("事件" + event.getEventNo() + "已进入" + ("1".equals(event.getStatus()) ? "响应中" : "已结束") + "阶段，不再接受申报");
                }
                if (event.getStartTime() != null && event.getStartTime().before(new Date()))
                {
                    throw new ServiceException("事件" + event.getEventNo() + "已到开始时间，不再接受申报");
                }
                if (StringUtils.isBlank(apply.getEventNo()))
                {
                    apply.setEventNo(event.getEventNo());
                }
            }
        }
        if (StringUtils.isBlank(apply.getStatus()))
        {
            apply.setStatus("0");
        }
        return drApplyMapper.insert(apply);
    }

    @Override
    public int updateDrApply(DrApply apply)
    {
        return drApplyMapper.updateById(apply);
    }

    @Override
    public int confirmApply(Long[] applyIds)
    {
        if (applyIds == null || applyIds.length == 0) return 0;
        int count = 0;
        for (Long id : applyIds)
        {
            DrApply apply = drApplyMapper.selectById(id);
            if (apply != null && "0".equals(apply.getStatus()))
            {
                apply.setStatus("1");
                count += drApplyMapper.updateById(apply);
            }
        }
        return count;
    }

    @Override
    public int deleteDrApplyByApplyIds(Long[] applyIds)
    {
        if (applyIds == null || applyIds.length == 0)
        {
            return 0;
        }
        return drApplyMapper.deleteBatchIds(Arrays.asList(applyIds));
    }
}
