package com.witos.vpp.event.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.witos.common.core.web.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * VPP效果评估对象 dr_assess（只读，供事件列表聚合合格场站数）
 *
 * @author witos
 * @date 2026-08-23
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dr_assess")
public class DrAssess extends TenantEntity
{
    private static final long serialVersionUID = 1L;

    /** 评估ID */
    @TableId
    private Long assessId;

    /** 事件ID */
    private Long eventId;

    /** 是否合格：0否 1是 */
    private String qualifiedFlag;
}
