package com.witos.vpp.dispatch.domain;

import java.math.BigDecimal;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.witos.common.core.web.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 需求响应事件对象 dr_event（调度侧读取）
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

    @TableId
    private Long eventId;

    private String eventNo;

    private String eventType;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date endTime;

    private BigDecimal targetPower;

    private BigDecimal targetEnergy;

    private String organizer;

    private String status;

    /** 申报总功率kW（非表字段，查询时从 dr_apply 聚合，与 event 服务口径一致） */
    @TableField(exist = false)
    private BigDecimal applyPower;

    /** 参与场站数（非表字段，查询时从 dr_apply 去重统计） */
    @TableField(exist = false)
    private Integer applyCount;
}

