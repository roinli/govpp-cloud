package com.witos.vpp.event.domain;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.witos.common.core.web.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * VPP执行监测对象 dr_execute（只读，供事件列表聚合实时功率）
 *
 * @author witos
 * @date 2026-08-21
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

    /** 分配ID（关联 dr_dispatch 取基线功率） */
    private Long dispatchId;

    /** 实际功率kW */
    private BigDecimal actualPower;

    /** 数据时间点 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date recordTime;
}