package com.witos.vpp.dispatch.domain;

import java.math.BigDecimal;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.witos.common.core.web.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * VPP运营商分配对象 dr_operator_dispatch（平台 → 运营商）
 *
 * @author witos
 * @date 2026-08-14
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dr_operator_dispatch")
public class DrOperatorDispatch extends TenantEntity
{
    private static final long serialVersionUID = 1L;

    /** 分配ID */
    @TableId
    private Long dispatchId;

    /** 事件ID */
    private Long eventId;

    /** 事件编号 */
    private String eventNo;

    /** 运营商ID（=租户ID） */
    private Long operatorId;

    /** 运营商名称（JOIN 查出来，不存表） */
    @TableField(exist = false)
    private String operatorName;

    /** 可调功率kW（削峰=削峰功率，填谷=填谷功率） */
    private BigDecimal adjustablePower;

    /** 基线功率kW（削峰功率，24小时平均，削峰/填谷都用它做基准） */
    private BigDecimal baselinePower;

    /** 目标功率kW（绝对功率） */
    private BigDecimal targetPower;

    /** 压降量kW */
    private BigDecimal reducePower;

    /** 状态：0待确认 1已分配 */
    private String status;
    /** 确认分配时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date confirmTime;
}
