package com.witos.vpp.dispatch.domain;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.witos.common.core.web.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * VPP执行监测对象 dr_execute
 *
 * @author witos
 * @date 2026-08-02
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dr_execute")
public class DrExecute extends TenantEntity
{
    private static final long serialVersionUID = 1L;

    /** 执行记录ID */
    @TableId
    private Long executeId;

    /** 事件ID */
    private Long eventId;

    /** 分配ID */
    private Long dispatchId;

    /** 场站ID */
    private Long stationId;

    /** 桩编号 */
    private String pileNo;

    /** 场站名称 */
    private String stationName;

    /** 目标功率kW */
    private BigDecimal planPower;

    /** 实际功率kW */
    private BigDecimal actualPower;

    /** 数据时间点 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date recordTime;
}

