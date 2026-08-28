package com.witos.vpp.dispatch.domain;

import java.math.BigDecimal;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.witos.common.core.web.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * VPP资源台账对象 dr_resource（调度侧读取，用于分配）
 *
 * @author witos
 * @date 2026-08-02
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dr_resource")
public class DrResource extends TenantEntity
{
    private static final long serialVersionUID = 1L;

    @TableId
    private Long resourceId;

    private String resourceCode;

    private Long stationId;

    private String stationName;

    private String pileNo;

    private String pileType;

    private BigDecimal ratedPower;

    private BigDecimal adjustableRate;

    private BigDecimal adjustablePower;

    private BigDecimal avg7dPower;

    private BigDecimal valleyRate;

    private BigDecimal valleyPower;

    /** 归属运营商（JOIN 查出来，不存表） */
    @TableField(exist = false)
    private String tenantName;

    private String participateFlag;

    private String participateStatus;
}
