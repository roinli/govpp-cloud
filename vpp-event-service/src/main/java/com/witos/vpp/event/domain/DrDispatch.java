package com.witos.vpp.event.domain;

import java.math.BigDecimal;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.witos.common.core.web.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * VPP调度分配对象 dr_dispatch（只读，供事件列表聚合基线功率）
 *
 * @author witos
 * @date 2026-08-21
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

    /** 基线功率kW(7日平均功率) */
    private BigDecimal baselinePower;

    /** 状态：0待确认 1已确认 2已下发 */
    private String status;
}