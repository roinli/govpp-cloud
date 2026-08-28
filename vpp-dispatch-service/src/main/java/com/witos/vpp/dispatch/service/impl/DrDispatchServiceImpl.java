package com.witos.vpp.dispatch.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.witos.common.core.exception.ServiceException;
import com.witos.common.core.text.Convert;
import com.witos.common.core.utils.ServletUtils;
import com.witos.common.core.utils.StringUtils;
import com.witos.common.mybatisplus.constant.MybatisPageConstants;
import com.witos.vpp.dispatch.domain.DrApply;
import com.witos.vpp.dispatch.domain.DrDispatch;
import com.witos.vpp.dispatch.domain.DrEvent;
import com.witos.vpp.dispatch.domain.DrOperatorDispatch;
import com.witos.vpp.dispatch.domain.DrResource;
import com.witos.vpp.dispatch.domain.DrStation;
import com.witos.vpp.dispatch.mapper.DrApplyMapper;
import com.witos.vpp.dispatch.mapper.DrDispatchMapper;
import com.witos.vpp.dispatch.mapper.DrStationMapper;
import com.witos.vpp.dispatch.mapper.DrEventMapper;
import com.witos.vpp.dispatch.mapper.DrOperatorDispatchMapper;
import com.witos.vpp.dispatch.mapper.DrResourceMapper;
import com.witos.common.security.utils.SecurityUtils;
import com.witos.system.api.domain.SysUser;
import com.witos.vpp.dispatch.service.IDrDispatchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/**
 * VPP调度分配Service业务层处理
 *
 * @author witos
 * @date 2026-08-02
 */
@Service
public class DrDispatchServiceImpl implements IDrDispatchService
{
    private static final BigDecimal ZERO = BigDecimal.ZERO;

    @Autowired
    private DrDispatchMapper drDispatchMapper;
    @Autowired
    private DrEventMapper drEventMapper;

    @Autowired
    private DrApplyMapper drApplyMapper;

    @Autowired
    private DrStationMapper drStationMapper;

    @Autowired
    private DrResourceMapper drResourceMapper;

    @Autowired
    private DrOperatorDispatchMapper drOperatorDispatchMapper;

    @Override
    public DrDispatch selectDrDispatchByDispatchId(Long dispatchId)
    {
        return drDispatchMapper.selectById(dispatchId);
    }

    @Override
    public IPage<DrDispatch> selectDrDispatchPage(DrDispatch dispatch)
    {
        Page<DrDispatch> page = new Page<>(Convert.toLong(ServletUtils.getParameterToInt(MybatisPageConstants.PAGE_NUM), 1L),
                Convert.toLong(ServletUtils.getParameterToInt(MybatisPageConstants.PAGE_SIZE), 10L));
        return drDispatchMapper.selectPage(page, buildWrapper(dispatch));
    }

    @Override
    public List<DrDispatch> selectDrDispatchList(DrDispatch dispatch)
    {
        return drDispatchMapper.selectList(buildWrapper(dispatch));
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

    private LambdaQueryWrapper<DrDispatch> buildWrapper(DrDispatch dispatch)
    {
        LambdaQueryWrapper<DrDispatch> wrapper = new LambdaQueryWrapper<>();
        if (dispatch != null)
        {
            if (dispatch.getEventId() != null)
            {
                wrapper.eq(DrDispatch::getEventId, dispatch.getEventId());
            }
            if (StringUtils.isNotBlank(dispatch.getEventNo()))
            {
                wrapper.like(DrDispatch::getEventNo, dispatch.getEventNo());
            }
            if (dispatch.getStationId() != null)
            {
                wrapper.eq(DrDispatch::getStationId, dispatch.getStationId());
            }
            if (StringUtils.isNotBlank(dispatch.getPileNo()))
            {
                wrapper.like(DrDispatch::getPileNo, dispatch.getPileNo());
            }
            if (StringUtils.isNotBlank(dispatch.getStatus()))
            {
                wrapper.eq(DrDispatch::getStatus, dispatch.getStatus());
            }
        }
        // 场站方只查自己场站
        try {
            SysUser sysUser = SecurityUtils.getLoginUser().getSysUser();
            if (sysUser != null && sysUser.getIsStation() != null && sysUser.getIsStation() == 1) {
                List<DrStation> myStations = drStationMapper.selectList(new LambdaQueryWrapper<DrStation>()
                        .eq(DrStation::getUserId, SecurityUtils.getUserId()));
                if (!myStations.isEmpty()) {
                    wrapper.in(DrDispatch::getStationId, myStations.stream().map(DrStation::getStationId).collect(Collectors.toList()));
                }
            }
        } catch (Exception ignored) {}
        addStationFilter(wrapper, DrDispatch::getStationId);
        wrapper.orderByDesc(DrDispatch::getDispatchId);
        return wrapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int allocate(Long eventId)
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
        drDispatchMapper.delete(new LambdaQueryWrapper<DrDispatch>().eq(DrDispatch::getEventId, eventId));

        List<DrResource> resources = drResourceMapper.selectList(new LambdaQueryWrapper<DrResource>()
                .eq(DrResource::getParticipateFlag, "1")
                .eq(DrResource::getParticipateStatus, "1"));
        if (resources.isEmpty())
        {
            throw new ServiceException("暂无参与VPP的资源，请先在资源台账中勾选参与");
        }

        // 已确认申报（作为分配前置校验）
        List<DrApply> applies = drApplyMapper.selectList(new LambdaQueryWrapper<DrApply>()
                .eq(DrApply::getEventId, eventId)
                .eq(DrApply::getStatus, "1"));
        if (applies.isEmpty())
        {
            throw new ServiceException("该事件暂无申报记录，无法分配。请先让运营商申报");
        }

        boolean isPeak = !"2".equals(event.getEventType());

        // 运营商：从平台分配里拿本运营商的目标压降量；平台请走「按运营商分配」
        Long tenantId = SecurityUtils.getTenantId();
        BigDecimal target;
        if (tenantId != null && tenantId == 9999L)
        {
            throw new ServiceException("平台请使用「按运营商分配」");
        }
        List<DrOperatorDispatch> myAlloc = drOperatorDispatchMapper.selectList(new LambdaQueryWrapper<DrOperatorDispatch>()
                .eq(DrOperatorDispatch::getEventId, eventId)
                .eq(DrOperatorDispatch::getOperatorId, tenantId)
                .eq(DrOperatorDispatch::getStatus, "1"));
        if (myAlloc.isEmpty())
        {
            throw new ServiceException("平台尚未分配或未确认分配给您，无法一键分配");
        }
        target = myAlloc.get(0).getReducePower() == null ? ZERO : myAlloc.get(0).getReducePower();

        // 总可调容量（所有参与桩，削峰用可调、填谷用谷值可调）
        BigDecimal totalCapacity = ZERO;
        for (DrResource r : resources)
        {
            BigDecimal cap = isPeak ? (r.getAdjustablePower() == null ? ZERO : r.getAdjustablePower())
                    : (r.getValleyPower() == null ? ZERO : r.getValleyPower());
            if (cap.signum() < 0) cap = ZERO;
            totalCapacity = totalCapacity.add(cap);
        }
        if (totalCapacity.signum() == 0)
        {
            throw new ServiceException("暂无可调容量，无法分配");
        }

        List<DrDispatch> allocated = new ArrayList<>();
        // 批量插入绕过 MetaObjectHandler，手动补 create_by/create_time（与单条insert的自动填充一致）
        String username = SecurityUtils.getUsername();
        Date now = new Date();
        for (DrResource r : resources)
        {
            BigDecimal pileCap = isPeak ? (r.getAdjustablePower() == null ? ZERO : r.getAdjustablePower())
                    : (r.getValleyPower() == null ? ZERO : r.getValleyPower());
            if (pileCap.signum() < 0) pileCap = ZERO;
            if (pileCap.signum() == 0) continue;

            // 桩级压降：按桩可调容量占比分摊 target（保留2位，和数据库 decimal(12,2) 一致）
            BigDecimal pileReduce = target.multiply(pileCap).divide(totalCapacity, 2, RoundingMode.HALF_UP);

            if (pileReduce.signum() == 0) continue;

            // 基线 = 7日平均功率（24小时平均）：削峰往下压、填谷往上抬
            // 削峰：目标 = 基线 − 压降量；填谷：目标 = 基线 + 拉升量（不超过额定）
            BigDecimal pileAdj = r.getAvg7dPower() == null ? ZERO : r.getAvg7dPower();
            BigDecimal pileTarget = isPeak ? pileAdj.subtract(pileReduce) : pileAdj.add(pileReduce);
            if (pileTarget.signum() < 0) pileTarget = ZERO;
            BigDecimal rated = r.getRatedPower() == null ? pileTarget : r.getRatedPower();
            if (!isPeak && pileTarget.compareTo(rated) > 0) pileTarget = rated;
            pileTarget = pileTarget.setScale(2, RoundingMode.HALF_UP);

            DrDispatch dispatch = new DrDispatch();
            dispatch.setEventId(eventId);
            dispatch.setEventNo(event.getEventNo());
            dispatch.setStationId(r.getStationId());
            dispatch.setStationName(r.getStationName());
            dispatch.setPileNo(r.getPileNo());
            // 可调容量：削峰 = 削峰功率，填谷 = 填谷功率（能压降/拉升的量）
            dispatch.setAdjustablePower(pileCap);
            // 基线功率 = 削峰功率（24小时平均）
            dispatch.setBaselinePower(pileAdj.setScale(2, RoundingMode.HALF_UP));
            dispatch.setTargetPower(pileTarget);
            dispatch.setReducePower(pileReduce);
            dispatch.setStatus("0");
            dispatch.setCreateBy(StringUtils.isNotBlank(username) ? username : "");
            dispatch.setCreateTime(now);
            allocated.add(dispatch);
        }

        // 尾差修正：差额精确补到最后一条压降量，保证总和 = target
        if (!allocated.isEmpty())
        {
            BigDecimal sumReduce = allocated.stream().map(d -> d.getReducePower() == null ? ZERO : d.getReducePower()).reduce(ZERO, BigDecimal::add);
            BigDecimal diff = target.subtract(sumReduce);
            if (diff.signum() != 0)
            {
                DrDispatch last = allocated.get(allocated.size() - 1);
                BigDecimal oldTarget = last.getTargetPower() == null ? ZERO : last.getTargetPower();
                BigDecimal newReduce = last.getReducePower().add(diff).setScale(2, RoundingMode.HALF_UP);
                if (newReduce.signum() < 0) newReduce = ZERO;
                last.setReducePower(newReduce);
                BigDecimal lastTarget = isPeak ? oldTarget.subtract(diff) : oldTarget.add(diff);
                if (lastTarget.signum() < 0) lastTarget = ZERO;
                last.setTargetPower(lastTarget.setScale(2, RoundingMode.HALF_UP));
                // 尾差已在内存修正，随批量插入一起入库
            }
        }
        // 一次性批量入库：桩多时避免逐条insert的多次网络往返
        if (!allocated.isEmpty())
        {
            drDispatchMapper.insertBatch(allocated);
        }
        return allocated.size();
    }

    @Override
    public int confirm(List<DrDispatch> list)
    {
        if (list == null || list.isEmpty())
        {
            return 0;
        }
        // 校验：微调后的场站/桩目标功率总和不能超过平台分配的目标功率
        DrDispatch first = list.get(0);
        if (first.getEventId() != null)
        {
            Long eventId = first.getEventId();
            // 事件已结束不允许确认
            DrEvent event = drEventMapper.selectById(eventId);
            if (event != null && "2".equals(event.getStatus()))
            {
                throw new ServiceException("事件已结束，不允许确认分配");
            }
            Long tenantId = SecurityUtils.getTenantId();
            BigDecimal sumTarget = list.stream().map(d -> d.getTargetPower() == null ? ZERO : d.getTargetPower())
                    .reduce(ZERO, BigDecimal::add);
            List<DrOperatorDispatch> opAlloc = drOperatorDispatchMapper.selectList(new LambdaQueryWrapper<DrOperatorDispatch>()
                    .eq(DrOperatorDispatch::getEventId, eventId)
                    .eq(DrOperatorDispatch::getOperatorId, tenantId));
            if (!opAlloc.isEmpty())
            {
                BigDecimal opTarget = opAlloc.get(0).getTargetPower() == null ? ZERO : opAlloc.get(0).getTargetPower();
                if (sumTarget.compareTo(opTarget) > 0)
                {
                    throw new ServiceException("场站/桩目标功率总和超过平台分配，无法确认分配");
                }
            }
        }
        // 保存微调后的目标功率/压降量，并改状态：一次性批量更新，桩多时避免逐条update
        List<DrDispatch> valid = list.stream()
                .filter(d -> d.getDispatchId() != null)
                .collect(Collectors.toList());
        if (valid.isEmpty())
        {
            return 0;
        }
        return drDispatchMapper.updateBatchConfirm(valid, SecurityUtils.getUsername());
    }

    @Override
    public int send(Long[] dispatchIds)
    {
        if (dispatchIds == null || dispatchIds.length == 0)
        {
            return 0;
        }
        DrDispatch update = new DrDispatch();
        update.setStatus("2");
        int rows = drDispatchMapper.update(update, new LambdaQueryWrapper<DrDispatch>()
                .in(DrDispatch::getDispatchId, Arrays.asList(dispatchIds))
                .eq(DrDispatch::getStatus, "1"));
        // TODO 复用现有控制通道：调用 witos 远程控制接口下发功率限制指令
        return rows;
    }

    @Override
    public int insertDrDispatch(DrDispatch dispatch)
    {
        if (StringUtils.isBlank(dispatch.getStatus()))
        {
            dispatch.setStatus("0");
        }
        return drDispatchMapper.insert(dispatch);
    }

    @Override
    public int updateDrDispatch(DrDispatch dispatch)
    {
        return drDispatchMapper.updateById(dispatch);
    }

    @Override
    public int deleteDrDispatchByDispatchIds(Long[] dispatchIds)
    {
        if (dispatchIds == null || dispatchIds.length == 0)
        {
            return 0;
        }
        return drDispatchMapper.deleteBatchIds(Arrays.asList(dispatchIds));
    }
}
