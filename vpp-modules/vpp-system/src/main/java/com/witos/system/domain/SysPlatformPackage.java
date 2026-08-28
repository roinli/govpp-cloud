package com.witos.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.witos.common.core.annotation.Excel;
import com.witos.common.core.web.domain.BaseEntity;
import lombok.Data;

/**
 * 平台套餐对象 sys_platform_package
 *
 * @author witos
 */
@Data
public class SysPlatformPackage extends BaseEntity
{
    /** 套餐编号 */
    @Excel(name = "套餐编号")
    @TableId
    private Long id;

    /** 套餐名 */
    @Excel(name = "套餐名")
    private String name;

    /** 关联的菜单编号（逗号分隔） */
    private String menuIds;

    /** 删除标志（0代表存在 1代表删除） */
    private String delFlag;

    /** 状态:0正常,1停用 */
    private Integer status;
}
