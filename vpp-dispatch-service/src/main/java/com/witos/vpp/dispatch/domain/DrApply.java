package com.witos.vpp.dispatch.domain;

import java.math.BigDecimal;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.witos.common.core.web.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 需求响应申报对象 dr_apply（调度侧读取）
 *
 * @author witos
 * @date 2026-08-02
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dr_apply")
public class DrApply extends TenantEntity
{
    private static final long serialVersionUID = 1L;

    @TableId
    private Long applyId;

    private Long eventId;

    private String eventNo;

    private Long stationId;

    private String stationName;

    private BigDecimal applyPower;

    private String status;
}
