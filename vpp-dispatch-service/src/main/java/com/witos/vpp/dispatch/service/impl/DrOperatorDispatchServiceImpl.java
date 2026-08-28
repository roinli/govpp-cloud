package com.witos.vpp.dispatch.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.witos.common.core.exception.ServiceException;
import com.witos.common.core.utils.StringUtils;
import com.witos.common.security.utils.SecurityUtils;
import com.witos.vpp.dispatch.domain.DrApply;
import com.witos.vpp.dispatch.domain.DrEvent;
import com.witos.vpp.dispatch.domain.DrOperatorDispatch;
import com.witos.vpp.dispatch.domain.DrResource;
import com.witos.vpp.dispatch.mapper.DrApplyMapper;
import com.witos.vpp.dispatch.mapper.DrEventMapper;
import com.witos.vpp.dispatch.mapper.DrOperatorDispatchMapper;
import com.witos.vpp.dispatch.mapper.DrResourceMapper;
import com.witos.vpp.dispatch.service.IDrOperatorDispatchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * VPP运营商分配Service业务层处理（平台 → 运营商）
 *
 * @author witos
 * @date 2026-08-14
 */
@Service
public class DrOperatorDispatchServiceImpl implements IDrOperatorDispatchService
{
    private static final BigDecimal ZERO = BigDecimal.ZERO;

    @Autowired
    private DrOperatorDispatchMapper drOperatorDispatchMapper;
    @Autowired
    private DrEventMapper drEventMapper;
    @Autowired
    private DrApplyMapper drApplyMapper;
    @Autowired
    private DrResourceMapper drResourceMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int allocateByOperator(Long eventId)
    {
        DrEvent event = drEventMapper.selectById(eventId);
        if (event == null)
        {
            throw new ServiceException("事件不存在：" + eventId);
        }
        if ("2".equals(event.getStatus()))
        {
            throw new ServiceException("事件已结束，不允许再分配");
        }
        // 重新分配前清掉旧记录
        drOperatorDispatchMapper.delete(new LambdaQueryWrapper<DrOperatorDispatch>()
                .eq(DrOperatorDispatch::getEventId, eventId));

        // 已确认申报，按运营商（tenant_id）聚合申报量
        List<DrApply> applies = drApplyMapper.selectList(new LambdaQueryWrapper<DrApply>()
                .eq(DrApply::getEventId, eventId)
                .eq(DrApply::getStatus, "1"));
        Map<Long, BigDecimal> operatorApplyMap = new HashMap<>();
        BigDecimal totalApply = ZERO;
        for (DrApply apply : applies)
        {
            BigDecimal power = apply.getApplyPower() == null ? ZERO : apply.getApplyPower();
            Long operatorId = apply.getTenantId();
            if (operatorId != null)
            {
                operatorApplyMap.merge(operatorId, power, BigDecimal::add);
                totalApply = totalApply.add(power);
            }
        }
        if (totalApply.signum() == 0)
        {
            throw new ServiceException("该事件暂无已确认申报，无法分配。请先让运营商申报");
        }

        // 各运营商可调容量（削峰=削峰功率，填谷=填谷功率）与基线（7日平均功率）合计
        List<DrResource> resources = drResourceMapper.selectList(new LambdaQueryWrapper<DrResource>()
                .eq(DrResource::getParticipateFlag, "1")
                .eq(DrResource::getParticipateStatus, "1"));
        boolean isPeak = !"2".equals(event.getEventType());
        Map<Long, BigDecimal> operatorCapMap = new HashMap<>();
        Map<Long, BigDecimal> operatorBaselineMap = new HashMap<>();
        for (DrResource r : resources)
        {
            BigDecimal cap = isPeak ? (r.getAdjustablePower() == null ? ZERO : r.getAdjustablePower())
                    : (r.getValleyPower() == null ? ZERO : r.getValleyPower());
            if (cap.signum() < 0) cap = ZERO;
            BigDecimal baseline = r.getAvg7dPower() == null ? ZERO : r.getAvg7dPower();
            if (r.getTenantId() != null)
            {
                operatorCapMap.merge(r.getTenantId(), cap, BigDecimal::add);
                operatorBaselineMap.merge(r.getTenantId(), baseline, BigDecimal::add);
            }
        }

        BigDecimal target = event.getTargetPower() == null ? ZERO : event.getTargetPower();

        // 各运营商份额：目标调节量按已确认申报占比拆分（保留4位），申报不足时不超额
        List<Long> operatorIds = new ArrayList<>(operatorApplyMap.keySet());
        Collections.sort(operatorIds);
        Map<Long, BigDecimal> reduceFull = new HashMap<>();
        BigDecimal allocSum = ZERO;
        for (Long operatorId : operatorIds)
        {
            BigDecimal applyPower = operatorApplyMap.get(operatorId);
            BigDecimal reduce = target.multiply(applyPower)
                    .divide(totalApply, 4, RoundingMode.HALF_UP);
            // 申报不足时不超额：压降量不超过该运营商申报量
            if (reduce.compareTo(applyPower) > 0) reduce = applyPower;
            reduceFull.put(operatorId, reduce);
            allocSum = allocSum.add(reduce);
        }

        // 尾差修正（最大余数法）：先向下取整到2位，再把差额按余数从大到小逐份补0.01，
        // 保证各运营商压降量合计与目标一致（与场站/桩分配的尾差修正口径相同），且不超过各自申报量
        Map<Long, BigDecimal> reduceFinal = new HashMap<>();
        BigDecimal baseSum = ZERO;
        for (Long operatorId : operatorIds)
        {
            BigDecimal base = reduceFull.get(operatorId).setScale(2, RoundingMode.FLOOR);
            reduceFinal.put(operatorId, base);
            baseSum = baseSum.add(base);
        }
        long units = allocSum.subtract(baseSum).setScale(2, RoundingMode.HALF_UP)
                .movePointRight(2).longValue();
        while (units-- > 0)
        {
            Long best = null;
            BigDecimal bestRemainder = null;
            for (Long operatorId : operatorIds)
            {
                // 已到申报上限（2位）的运营商不再补足
                if (reduceFinal.get(operatorId).compareTo(operatorApplyMap.get(operatorId)) >= 0) continue;
                BigDecimal remainder = reduceFull.get(operatorId).subtract(reduceFinal.get(operatorId));
                if (remainder.signum() <= 0) continue;
                if (best == null || remainder.compareTo(bestRemainder) > 0)
                {
                    best = operatorId;
                    bestRemainder = remainder;
                }
            }
            if (best == null) break;
            reduceFinal.put(best, reduceFinal.get(best).add(new BigDecimal("0.01")));
        }

        int count = 0;
        for (Long operatorId : operatorIds)
        {
            BigDecimal reduce = reduceFinal.get(operatorId);
            if (reduce.signum() == 0) continue;

            BigDecimal cap = operatorCapMap.getOrDefault(operatorId, ZERO);
            BigDecimal baseline = operatorBaselineMap.getOrDefault(operatorId, ZERO);
            // 目标功率 = 基线（削峰功率）± 压降/拉升量
            BigDecimal targetPower = isPeak ? baseline.subtract(reduce) : baseline.add(reduce);
            if (targetPower.signum() < 0) targetPower = ZERO;

            DrOperatorDispatch d = new DrOperatorDispatch();
            d.setEventId(eventId);
            d.setEventNo(event.getEventNo());
            d.setOperatorId(operatorId);
            d.setTenantId(operatorId);
            d.setAdjustablePower(cap.setScale(2, RoundingMode.HALF_UP));
            d.setBaselinePower(baseline.setScale(2, RoundingMode.HALF_UP));
            d.setTargetPower(targetPower.setScale(2, RoundingMode.HALF_UP));
            d.setReducePower(reduce);
            d.setStatus("0");
            drOperatorDispatchMapper.insert(d);
            count++;
        }
        return count;
    }

    @Override
    public int confirm(List<DrOperatorDispatch> list)
    {
        if (list == null || list.isEmpty())
        {
            return 0;
        }
        // 校验：压降量总和必须等于目标调节量（事件目标）
        DrOperatorDispatch first = list.get(0);
        if (first.getEventId() != null)
        {
            DrEvent event = drEventMapper.selectById(first.getEventId());
            if (event != null)
            {
                if ("2".equals(event.getStatus()))
                {
                    throw new ServiceException("事件已结束，不允许确认分配");
                }
                BigDecimal sumReduce = list.stream()
                        .map(d -> d.getReducePower() == null ? ZERO : d.getReducePower())
                        .reduce(ZERO, BigDecimal::add);
                BigDecimal eventTarget = event.getTargetPower() == null ? ZERO : event.getTargetPower();
                if (sumReduce.subtract(eventTarget).abs().compareTo(new BigDecimal("0.01")) > 0)
                {
                    throw new ServiceException("压降量总和必须等于目标调节量 " + eventTarget + " kW");
                }
            }
        }
        // 保存微调后的目标功率/压降量，并改状态：一次性批量更新
        List<DrOperatorDispatch> valid = list.stream()
                .filter(d -> d.getDispatchId() != null)
                .collect(Collectors.toList());
        if (valid.isEmpty())
        {
            return 0;
        }
        return drOperatorDispatchMapper.updateBatchConfirm(valid, SecurityUtils.getUsername());
    }

    @Override
    public List<DrOperatorDispatch> selectDrOperatorDispatchList(DrOperatorDispatch query)
    {
        LambdaQueryWrapper<DrOperatorDispatch> wrapper = new LambdaQueryWrapper<>();
        // 运营商自动只看自己收到的分配；平台(9999)看全部
        Long tenantId = SecurityUtils.getTenantId();
        if (tenantId != null && tenantId != 9999L)
        {
            wrapper.eq(DrOperatorDispatch::getOperatorId, tenantId);
        }
        if (query != null)
        {
            if (query.getEventId() != null)
            {
                wrapper.eq(DrOperatorDispatch::getEventId, query.getEventId());
            }
            if (query.getOperatorId() != null)
            {
                wrapper.eq(DrOperatorDispatch::getOperatorId, query.getOperatorId());
            }
            if (StringUtils.isNotBlank(query.getStatus()))
            {
                wrapper.eq(DrOperatorDispatch::getStatus, query.getStatus());
            }
        }
        wrapper.orderByDesc(DrOperatorDispatch::getDispatchId);
        List<DrOperatorDispatch> list = drOperatorDispatchMapper.selectList(wrapper);
        // 回填运营商名称
        for (DrOperatorDispatch d : list)
        {
            if (d.getOperatorId() != null)
            {
                d.setOperatorName(drOperatorDispatchMapper.selectTenantName(d.getOperatorId()));
            }
        }
        return list;
    }
}
