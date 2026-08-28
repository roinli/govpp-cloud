package com.witos.vpp.event.domain;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.witos.common.core.web.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 需求响应事件对象 dr_event
 *
 * @author witos
 * @date 2026-08-02
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dr_event")
public class DrEvent extends TenantEntity
{
    private static final long serialVersionUID = 1L;

    /** 事件ID */
    @TableId
    private Long eventId;

    /** 事件编号 */
    private String eventNo;

    /** 事件类型：1削峰 2填谷 */
    private String eventType;

    /** 开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date startTime;

    /** 结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date endTime;

    /** 目标调节量kW */
    private BigDecimal targetPower;

    /** 目标响应电量kWh */
    private BigDecimal targetEnergy;

    /** 组织方 */
    private String organizer;

    /** 状态：0待响应 1响应中 2已结束 */
    private String status;

    /** 关键词搜索（非表字段，用于接收前端 keyword 参数） */
    @TableField(exist = false)
    private String keyword;

    /** 已申报容量(kW)（非表字段，查询时从 dr_apply 聚合） */
    @TableField(exist = false)
    private BigDecimal applyPower;

    /** 参与申报场站数（非表字段，查询时从 dr_apply 去重统计） */
    @TableField(exist = false)
    private Integer applyCount;

    /** 实时功率kW（非表字段，响应中事件从 dr_execute 最新遥测点聚合） */
    @TableField(exist = false)
    private BigDecimal actualPower;

    /** 完成进度百分比0-100（非表字段，响应中事件平均压降/拉升功率×已持续小时÷目标电量，无遥测数据时为空） */
    @TableField(exist = false)
    private Integer progress;

    /** 实时响应电量kWh（非表字段，响应中事件平均压降/拉升功率×已持续小时，无遥测数据时为空） */
    @TableField(exist = false)
    private BigDecimal respEnergy;

    /** 合格场站数（非表字段，已结束事件从 dr_assess 聚合 qualified_flag='1' 记录数） */
    @TableField(exist = false)
    private Integer qualifiedCount;

    /** 结算金额元（非表字段，已结束事件从 dr_settlement 聚合 total_amount 合计，未结算时为空） */
    @TableField(exist = false)
    private BigDecimal settleAmount;
}

