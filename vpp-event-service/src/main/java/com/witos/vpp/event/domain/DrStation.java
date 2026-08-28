package com.witos.vpp.event.domain;

import java.math.BigDecimal;
import java.util.Collection;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.witos.common.core.annotation.Excel;
import com.witos.common.core.web.domain.TenantEntity;
import lombok.Data;

/**
 * VPP 场站对象 dr_station
 *
 * @author witos
 */
@Data
@TableName("dr_station")
public class DrStation extends TenantEntity
{
    private static final long serialVersionUID = 1L;

    /** 场站ID */
    @Excel(name = "场站ID")
    @TableId
    private Long stationId;

    /** 场站名称 */
    @Excel(name = "场站名称")
    private String stationName;

    /** 地址 */
    @Excel(name = "地址")
    private String address;
    /** 省 */
    private String province;

    /** 市 */
    private String city;

    /** 经度 */
    private BigDecimal longitude;

    /** 纬度 */
    private BigDecimal latitude;


    /** 总容量kW */
    @Excel(name = "总容量(kW)")
    private BigDecimal capacity;

    /** 场站方账号（关联sys_user） */
    private Long userId;

    /** 联系人 */
    @Excel(name = "联系人")
    private String contact;

    /** 联系电话 */
    @Excel(name = "联系电话")
    private String phone;

    /** 场站图片 */
    private String imageUrl;

    /** 状态：0停用 1启用 */
    @Excel(name = "状态", readConverterExp = "0=停用,1=启用")
    private String status;

    /** 运营商分成比例（运营商在运营商+场站里的占比，NULL=默认0.01，即运营商:场站=1:99） */
    private BigDecimal operatorRate;

    /** 场站ID集合（用于IN查询，非表字段） */
    @TableField(exist = false)
    private Collection<Long> stationIds;

    /** 总收益金额（结算场站金额汇总，非表字段） */
    @TableField(exist = false)
    private BigDecimal totalIncome;

    /** 在线设备数（非表字段） */
    @TableField(exist = false)
    private Long onlineCount;

    /** 离线设备数（非表字段） */
    @TableField(exist = false)
    private Long offlineCount;

    /** 故障设备数（非表字段） */
    @TableField(exist = false)
    private Long faultCount;
}
