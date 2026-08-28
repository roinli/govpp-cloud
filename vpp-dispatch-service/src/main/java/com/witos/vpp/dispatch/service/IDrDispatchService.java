package com.witos.vpp.dispatch.service;

import java.util.List;
import com.witos.vpp.dispatch.domain.DrDispatch;
import com.baomidou.mybatisplus.core.metadata.IPage;

/**
 * VPP调度分配Service接口
 *
 * @author witos
 * @date 2026-08-02
 */
public interface IDrDispatchService
{
    DrDispatch selectDrDispatchByDispatchId(Long dispatchId);

    IPage<DrDispatch> selectDrDispatchPage(DrDispatch dispatch);

    List<DrDispatch> selectDrDispatchList(DrDispatch dispatch);

    /** 一键分配：按可调容量比例（有确认申报则按申报容量比例）生成场站/桩目标功率 */
    int allocate(Long eventId);

    /** 分配结果人工确认（保存微调后的目标功率并校验） */
    int confirm(List<DrDispatch> list);

    /** 生成并下发指令（预留：调用现有控制通道） */
    int send(Long[] dispatchIds);

    int insertDrDispatch(DrDispatch dispatch);

    int updateDrDispatch(DrDispatch dispatch);

    int deleteDrDispatchByDispatchIds(Long[] dispatchIds);
}
