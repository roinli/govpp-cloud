package com.witos.vpp.dispatch.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import com.witos.vpp.dispatch.domain.DrExecute;
import com.baomidou.mybatisplus.core.metadata.IPage;

/**
 * VPP执行监测Service接口
 *
 * @author witos
 * @date 2026-08-02
 */
public interface IDrExecuteService
{
    DrExecute selectDrExecuteByExecuteId(Long executeId);

    IPage<DrExecute> selectDrExecutePage(DrExecute execute);

    List<DrExecute> selectDrExecuteList(DrExecute execute);

    /** 记录执行反馈点（桩上报实际功率） */
    int insertDrExecute(DrExecute execute);

    /** 偏差监测：返回目标vs实际及偏差率（超出阈值告警），可选按场站/桩过滤，避免全量传输 */
    List<Map<String, Object>> selectDeviationList(Long eventId, BigDecimal thresholdPercent, Long stationId, String pileNo);

    /** 事件的桩清单（轻量，来自已下发分配），供前端先选桩再按需请求偏差数据 */
    List<Map<String, Object>> selectPileOptions(Long eventId);

    int updateDrExecute(DrExecute execute);

    int deleteDrExecuteByExecuteIds(Long[] executeIds);

    /** 模拟生成执行遥测数据（根据分配的目标功率，实际值在目标附近随机波动） */
    int generateSimulatedData(Long eventId);

    /** 定时：为所有响应中的事件自动造模拟数据 */
    int autoGenerateForActiveEvents();
}
