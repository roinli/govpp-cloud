package com.witos.vpp.dispatch.mapper;

import com.witos.vpp.dispatch.domain.DrSettlement;
import com.witos.common.mybatisplus.mapper.BaseMapperX;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * VPP结算分账Mapper接口
 *
 * @author witos
 * @date 2026-08-02
 */
public interface DrSettlementMapper extends BaseMapperX<DrSettlement>
{
    /** 根据租户ID查租户名称（分成结构图用） */
    @Select("select tenant_name from sys_tenant where id = #{tenantId}")
    String selectTenantNameById(@Param("tenantId") Long tenantId);
}
