package com.witos.vpp.event.domain;

import java.math.BigDecimal;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * VPP 资源分钟级功率对象 dr_resource_power_minute
 * 充电负荷实时口径：桩每分钟负荷功率，由定时任务模拟写入（同桩同一分钟重算一致，刷新不变）
 *
 * @author witos
 */
@Data
@TableName("dr_resource_power_minute")
public class DrPowerMinute
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

    /** 功率时间（分钟） */
    private Date powerTime;

    /** 充电负荷功率kW */
    private BigDecimal loadPower;

    /** 创建时间 */
    private Date createTime;
}