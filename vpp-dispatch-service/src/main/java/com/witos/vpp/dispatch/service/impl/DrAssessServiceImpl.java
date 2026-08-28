package com.witos.vpp.dispatch.service.impl;

import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.witos.common.core.utils.StringUtils;
import com.witos.vpp.dispatch.domain.DrAssess;
import com.witos.vpp.dispatch.mapper.DrAssessMapper;
import com.witos.vpp.dispatch.service.IDrAssessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * VPP效果评估Service业务层处理
 *
 * @author witos
 * @date 2026-08-02
 */
@Service
public class DrAssessServiceImpl implements IDrAssessService
{
    @Autowired
    private DrAssessMapper drAssessMapper;

    @Override
    public List<DrAssess> selectDrAssessList(DrAssess assess)
    {
        return drAssessMapper.selectList(buildWrapper(assess));
    }

    private LambdaQueryWrapper<DrAssess> buildWrapper(DrAssess assess)
    {
        LambdaQueryWrapper<DrAssess> wrapper = new LambdaQueryWrapper<>();
        if (assess != null)
        {
            if (assess.getEventId() != null)
            {
                wrapper.eq(DrAssess::getEventId, assess.getEventId());
            }
            if (StringUtils.isNotBlank(assess.getEventNo()))
            {
                wrapper.like(DrAssess::getEventNo, assess.getEventNo());
            }
            if (assess.getStationId() != null)
            {
                wrapper.eq(DrAssess::getStationId, assess.getStationId());
            }
            if (StringUtils.isNotBlank(assess.getQualifiedFlag()))
            {
                wrapper.eq(DrAssess::getQualifiedFlag, assess.getQualifiedFlag());
            }
        }
        wrapper.orderByDesc(DrAssess::getAssessId);
        return wrapper;
    }
}
