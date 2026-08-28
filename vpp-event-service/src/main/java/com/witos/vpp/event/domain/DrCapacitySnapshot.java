package com.witos.vpp.event.domain;

import java.math.BigDecimal;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * VPP 首页每小时容量快照对象 dr_capacity_snapshot
 *
 * @author witos
 */
@Data
@TableName("dr_capacity_snapshot")
public class DrCapacitySnapshot
{
    /** 快照ID */
    @TableId
    private Long snapshotId;

    /** 快照小时（整点） */
    private Date snapshotHour;

    /** 租户ID（运营商） */
    private Long tenantId;

    /** 场站ID */
    private Long stationId;

    /** 削峰可调容量kW */
    private BigDecimal peakPower;

    /** 填谷可调容量kW */
    private BigDecimal valleyPower;

    /** 额定装机容量kW */
    private BigDecimal ratedPower;

    /** 参与场站数 */
    private Integer stationCount;

    /** 参与桩数 */
    private Integer pileCount;

    /** 在线设备数 */
    private Integer onlineCount;

    /** 离线设备数 */
    private Integer offlineCount;

    /** 故障设备数 */
    private Integer faultCount;

    /** 创建时间 */
    private Date createTime;
}
