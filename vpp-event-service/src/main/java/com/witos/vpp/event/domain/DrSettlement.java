package com.witos.vpp.event.domain;

import java.math.BigDecimal;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.witos.common.core.web.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * VPP结算分账对象 dr_settlement（只读，供事件列表聚合结算金额）
 *
 * @author witos
 * @date 2026-08-23
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dr_settlement")
public class DrSettlement extends TenantEntity
{
    private static final long serialVersionUID = 1L;

    /** 结算单ID */
    @TableId
    private Long settlementId;

    /** 事件ID */
    private Long eventId;

    /** 收益总额 元 */
    private BigDecimal totalAmount;
}
