package com.witos.vpp.dispatch.domain;

import java.math.BigDecimal;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.witos.common.core.web.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * VPP效果评估对象 dr_assess
 *
 * @author witos
 * @date 2026-08-02
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dr_assess")
public class DrAssess extends TenantEntity
{
    private static final long serialVersionUID = 1L;

    /** 评估ID */
    @TableId
    private Long assessId;

    /** 事件ID */
    private Long eventId;

    /** 事件编号 */
    private String eventNo;

    /** 场站ID */
    private Long stationId;

    /** 场站名称 */
    private String stationName;

    /** 基线功率kW */
    private BigDecimal basePower;

    /** 实际平均功率kW */
    private BigDecimal actualPower;

    /** 响应电量kWh */
    private BigDecimal responseEnergy;

    /** 是否合格：0否 1是 */
    private String qualifiedFlag;

    /** 充电桩数 */
    private Integer chargePiles;

    /** 超差记录 */
    private String points;

    /** 合格线(%) */
    private BigDecimal qualRate;

    /** 备注 */
    private String note;
}
