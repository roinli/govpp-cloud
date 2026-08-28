package com.witos.vpp.event.domain;

import java.math.BigDecimal;
import java.util.List;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.witos.common.core.annotation.Excel;
import com.witos.common.core.annotation.Excel.ColumnType;
import com.witos.common.core.web.domain.TenantEntity;
import lombok.Data;

/**
 * VPP 资源台账对象 dr_resource
 *
 * @author witos
 */
@Data
@TableName("dr_resource")
public class DrResource extends TenantEntity
{
    private static final long serialVersionUID = 1L;

    /** 资源ID */
    @TableId
    private Long resourceId;

    /** 资源编码 */
    @Excel(name = "资源编码")
    private String resourceCode;

    /** 场站ID */
    private Long stationId;

    /** 场站名称 */
    @Excel(name = "场站名称")
    private String stationName;

    /** 桩编号 */
    @Excel(name = "桩编号")
    private String pileNo;

    /** 类型：DC直流/AC交流 */
    @Excel(name = "类型", readConverterExp = "DC=直流,AC=交流")
    private String pileType;
    /** 资源归属：1运营商 2私有 */
    private String resourceOwnerType;


    /** 额定功率kW */
    @Excel(name = "额定功率(kW)", cellType = ColumnType.NUMERIC)
    private BigDecimal ratedPower;

    /** 削峰可调系数 */
    @Excel(name = "削峰系数", cellType = ColumnType.NUMERIC)
    private BigDecimal adjustableRate;

    /** 削峰可调功率kW(=额定×系数) */
    @Excel(name = "削峰可调功率(kW)", cellType = ColumnType.NUMERIC)
    private BigDecimal adjustablePower;

    /** 7日平均功率kW（基线，24小时平均，=额定×削峰系数） */
    @Excel(name = "7日平均功率(kW)", cellType = ColumnType.NUMERIC)
    private BigDecimal avg7dPower;

    /** 填谷可调系数 */
    @Excel(name = "填谷系数", cellType = ColumnType.NUMERIC)
    private BigDecimal valleyRate;

    /** 填谷可调功率kW(=额定×填谷系数) */
    @Excel(name = "填谷可调功率(kW)", cellType = ColumnType.NUMERIC)
    private BigDecimal valleyPower;

    /** 归属运营商（JOIN 查出来，不存表） */
    @TableField(exist = false)
    @Excel(name = "归属运营商")
    private String tenantName;

    /** 是否参与VPP：0否 1是 */
    private String participateFlag;

    /** 参与状态：0停用 1启用 */
    @Excel(name = "参与状态", readConverterExp = "0=停用,1=启用")
    private String participateStatus;

    /** 设备状态：1在线 2离线 3故障 */
    @Excel(name = "设备状态", readConverterExp = "1=在线,2=离线,3=故障")
    private String deviceStatus;

    /** 参与事件次数（关联统计，不存表） */
    @TableField(exist = false)
    @Excel(name = "参与事件次数", cellType = ColumnType.NUMERIC)
    private Long eventCount;

    /** 多场站过滤（非表字段） */
    @TableField(exist = false)
    private List<Long> stationIds;

    /** 关键词搜索（非表字段） */
    @TableField(exist = false)
    private String keyword;
}