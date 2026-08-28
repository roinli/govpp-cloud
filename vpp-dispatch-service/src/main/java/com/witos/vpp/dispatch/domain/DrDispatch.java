package com.witos.vpp.dispatch.domain;

import java.math.BigDecimal;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.witos.common.core.web.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * VPP调度分配对象 dr_dispatch
 *
 * @author witos
 * @date 2026-08-02
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dr_dispatch")
public class DrDispatch extends TenantEntity
{
    private static final long serialVersionUID = 1L;

    /** 分配ID */
    @TableId
    private Long dispatchId;

    /** 事件ID */
    private Long eventId;

    /** 事件编号 */
    private String eventNo;

    /** 场站ID */
    private Long stationId;

    /** 场站名称 */
    private String stationName;

    /** 桩编号 */
    private String pileNo;

    /** 可调容量kW（削峰=削峰功率，填谷=填谷功率） */
    private BigDecimal adjustablePower;

    /** 基线功率kW（削峰功率，24小时平均，削峰/填谷都用它做基准） */
    private BigDecimal baselinePower;

    /** 目标功率kW（绝对功率，降到这个值） */
    private BigDecimal targetPower;

    /** 压降量kW（可调功率 − 目标功率，要求少用多少） */
    private BigDecimal reducePower;

    /** 状态：0待确认 1已确认 2已下发 */
    private String status;
}
