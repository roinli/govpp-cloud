package com.witos.vpp.event.domain;

import java.math.BigDecimal;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * VPP 资源每日功率对象 dr_resource_power_daily
 * 充电负荷口径：当日充电平均/峰值功率，即削峰事件中的基线功率（非削峰填谷可调功率）
 *
 * @author witos
 */
@Data
@TableName("dr_resource_power_daily")
public class DrPowerDaily
{
    /** 主键ID */
    @TableId
    private Long id;

    /** 租户ID（运营商） */
    private Long tenantId;

    /** 场站ID */
    private Long stationId;

    /** 资源ID */
    private Long resourceId;

    /** 功率日期 */
    private Date powerDate;

    /** 当日充电平均功率kW（基线口径） */
    private BigDecimal avgPower;

    /** 当日充电峰值功率kW */
    private BigDecimal peakPower;

    /** 创建时间 */
    private Date createTime;
}