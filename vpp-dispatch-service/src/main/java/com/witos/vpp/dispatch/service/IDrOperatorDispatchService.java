package com.witos.vpp.dispatch.service;

import java.util.List;
import com.witos.vpp.dispatch.domain.DrOperatorDispatch;

/**
 * VPP运营商分配Service接口（平台 → 运营商）
 *
 * @author witos
 * @date 2026-08-14
 */
public interface IDrOperatorDispatchService
{
    /** 平台一键分配：按运营商申报量占比拆分目标功率 */
    int allocateByOperator(Long eventId);

    /** 平台确认分配：保存微调后的目标功率并 0待确认 -> 1已分配 */
    int confirm(List<DrOperatorDispatch> list);

    /** 查询运营商分配列表 */
    List<DrOperatorDispatch> selectDrOperatorDispatchList(DrOperatorDispatch query);
}
