package com.witos.system.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.witos.common.core.annotation.Excel;
import com.witos.common.core.web.domain.BaseEntity;
import lombok.Data;

/**
 * 平台用户管理 sys_platform
 */
@Data
@TableName("sys_platform")
public class SysPlatform extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @Excel(name = "编号")
    @TableId
    private Long id;

    /** 对应的 sys_user 用户ID（虚拟字段，不对应数据库列） */
    @TableField(exist = false)
    private Long userId;

    /** 用户名称 */
    @Excel(name = "用户名称")
    private String userName;

    /** 用户昵称 */
    @Excel(name = "用户昵称")
    private String nickName;

    /** 手机号码 */
    @Excel(name = "手机号码")
    private String userPhone;

    /** 密码 */
    private String password;

    /** 平台套餐ID */
    @Excel(name = "平台套餐ID")
    private Long packageId;

    /** 状态（0正常 1停用） */
    @Excel(name = "状态", readConverterExp = "0=正常,1=停用")
    private String status;

    /** 删除标志（0代表存在 1代表删除） */
    private String delFlag;
}
