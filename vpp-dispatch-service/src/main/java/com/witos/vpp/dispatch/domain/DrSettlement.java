package com.witos.vpp.dispatch.domain;

import java.math.BigDecimal;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.witos.common.core.annotation.Excel;
import com.witos.common.core.web.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * VPP结算分账对象 dr_settlement
 *
 * @author witos
 * @date 2026-08-02
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

    /** 结算单编号 */
    @Excel(name = "结算单编号")
    private String settlementNo;

    /** 事件ID */
    private Long eventId;

    /** 事件编号 */
    @Excel(name = "事件编号")
    private String eventNo;

    /** 场站ID */
    private Long stationId;

    /** 场站名称 */
    @Excel(name = "场站名称")
    private String stationName;

    /** 响应电量kWh */
    @Excel(name = "响应电量(kWh)")
    private BigDecimal responseEnergy;

    /** 结算单价 元/kWh */
    @Excel(name = "单价(元/kWh)")
    private BigDecimal unitPrice;

    /** 收益总额 元 */
    @Excel(name = "总金额(元)")
    private BigDecimal totalAmount;

    /** 平台分成比例 */
    private BigDecimal platformRate;

    /** 运营商分成比例 */
    private BigDecimal operatorRate;

    /** 场站分成比例 */
    private BigDecimal stationRate;

    /** 平台金额 */
    @Excel(name = "平台分成(元)")
    private BigDecimal platformAmount;

    /** 运营商金额 */
    @Excel(name = "运营商分成(元)")
    private BigDecimal operatorAmount;

    /** 场站金额 */
    @Excel(name = "场站分成(元)")
    private BigDecimal stationAmount;

    /** 状态：0待确认 1已结算 2已打款 */
    @Excel(name = "状态", readConverterExp = "0=待确认,1=已结算,2=已打款")
    private String status;

    /** 确认结算时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date confirmTime;

    /** 打款时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date payTime;

    /** 事件类型，展示用 */
    @TableField(exist = false)
    private String eventType;
}
