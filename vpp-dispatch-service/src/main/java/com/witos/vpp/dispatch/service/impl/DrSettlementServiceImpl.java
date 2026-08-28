package com.witos.vpp.dispatch.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import com.witos.common.core.utils.DateUtils;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.witos.common.core.exception.ServiceException;
import com.witos.common.core.text.Convert;
import com.witos.common.core.utils.ServletUtils;
import com.witos.common.core.utils.StringUtils;
import com.witos.common.mybatisplus.constant.MybatisPageConstants;
import com.witos.common.security.utils.SecurityUtils;
import com.witos.vpp.dispatch.domain.DrAssess;
import com.witos.vpp.dispatch.domain.DrEvent;
import com.witos.vpp.dispatch.domain.DrSettlement;
import com.witos.vpp.dispatch.mapper.DrAssessMapper;
import com.witos.vpp.dispatch.mapper.DrEventMapper;
import com.witos.vpp.dispatch.mapper.DrSettlementMapper;
import com.witos.vpp.dispatch.domain.DrStation;
import com.witos.vpp.dispatch.mapper.DrStationMapper;
import com.witos.vpp.dispatch.service.IDrSettlementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/**
 * VPP结算分账Service业务层处理
 *
 * @author witos
 * @date 2026-08-02
 */
@Service
public class DrSettlementServiceImpl implements IDrSettlementService
{
    private static final BigDecimal ZERO = BigDecimal.ZERO;

    @Autowired
    private DrSettlementMapper drSettlementMapper;

    @Autowired
    private DrAssessMapper drAssessMapper;

    @Autowired
    private DrEventMapper drEventMapper;

    @Autowired
    private DrStationMapper drStationMapper;

    @Override
    public DrSettlement selectDrSettlementBySettlementId(Long settlementId)
    {
        return drSettlementMapper.selectById(settlementId);
    }

    @Override
    public IPage<DrSettlement> selectDrSettlementPage(DrSettlement settlement)
    {
        Page<DrSettlement> page = new Page<>(Convert.toLong(ServletUtils.getParameterToInt(MybatisPageConstants.PAGE_NUM), 1L),
                Convert.toLong(ServletUtils.getParameterToInt(MybatisPageConstants.PAGE_SIZE), 10L));
        return drSettlementMapper.selectPage(page, buildWrapper(settlement));
    }

    @Override
    public List<DrSettlement> selectDrSettlementList(DrSettlement settlement)
    {
        return drSettlementMapper.selectList(buildWrapper(settlement));
    }

    private LambdaQueryWrapper<DrSettlement> buildWrapper(DrSettlement settlement)
    {
        LambdaQueryWrapper<DrSettlement> wrapper = new LambdaQueryWrapper<>();
        if (settlement != null)
        {
            if (settlement.getEventId() != null)
            {
                wrapper.eq(DrSettlement::getEventId, settlement.getEventId());
            }
            if (StringUtils.isNotBlank(settlement.getEventNo()))
            {
                wrapper.like(DrSettlement::getEventNo, settlement.getEventNo());
            }
            if (StringUtils.isNotBlank(settlement.getSettlementNo()))
            {
                wrapper.like(DrSettlement::getSettlementNo, settlement.getSettlementNo());
            }
            if (settlement.getStationId() != null)
            {
                wrapper.eq(DrSettlement::getStationId, settlement.getStationId());
            }
            if (StringUtils.isNotBlank(settlement.getStatus()))
            {
                wrapper.eq(DrSettlement::getStatus, settlement.getStatus());
            }
        }
        wrapper.orderByDesc(DrSettlement::getSettlementId);
        return wrapper;
    }

    @Override
    public int insertDrSettlement(DrSettlement settlement)
    {
        if (StringUtils.isBlank(settlement.getStatus()))
        {
            settlement.setStatus("0");
        }
        return drSettlementMapper.insert(settlement);
    }

    @Override
    public int updateDrSettlement(DrSettlement settlement)
    {
        return drSettlementMapper.updateById(settlement);
    }

    @Override
    public int deleteDrSettlementBySettlementIds(Long[] settlementIds)
    {
        if (settlementIds == null || settlementIds.length == 0)
        {
            return 0;
        }
        return drSettlementMapper.deleteBatchIds(Arrays.asList(settlementIds));
    }

    private int updateStatus(Long[] settlementIds, String fromStatus, String toStatus)
    {
        if (settlementIds == null || settlementIds.length == 0)
        {
            return 0;
        }
        List<DrSettlement> list = drSettlementMapper.selectList(new LambdaQueryWrapper<DrSettlement>()
                .in(DrSettlement::getSettlementId, Arrays.asList(settlementIds)));
        int count = 0;
        for (DrSettlement s : list)
        {
            if (!fromStatus.equals(s.getStatus()))
            {
                throw new ServiceException("结算单 " + s.getSettlementNo() + " 状态不是待" + ("0".equals(fromStatus) ? "确认" : "打款") + "，无法流转");
            }
            DrSettlement upd = new DrSettlement();
            upd.setSettlementId(s.getSettlementId());
            upd.setStatus(toStatus);
            if ("1".equals(toStatus))
            {
                upd.setConfirmTime(new Date());
            }
            else if ("2".equals(toStatus))
            {
                upd.setPayTime(new Date());
            }
            count += drSettlementMapper.updateById(upd);
        }
        return count;
    }

}