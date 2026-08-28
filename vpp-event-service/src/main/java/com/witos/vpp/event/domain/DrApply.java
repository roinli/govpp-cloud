package com.witos.vpp.event.domain;

import java.math.BigDecimal;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.witos.common.core.web.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 需求响应申报对象 dr_apply
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

    /** 申报ID */
    @TableId
    private Long applyId;

    /** 事件ID */
    private Long eventId;

    /** 事件编号 */
    private String eventNo;

    /** 场站ID */
    private Long stationId;

    /** 场站名称 */
    private String stationName;

    /** 申报容量kW */
    private BigDecimal applyPower;

    /** 状态：0待确认 1已确认 2已拒绝 */
    private String status;
}
