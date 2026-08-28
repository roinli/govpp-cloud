package com.witos.vpp.dispatch.mapper;

import java.math.BigDecimal;

import com.witos.vpp.dispatch.domain.DrStation;
import com.witos.common.mybatisplus.mapper.BaseMapperX;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface DrStationMapper extends BaseMapperX<DrStation> {

    /** 查询运营商（租户）的平台分成比例 */
    @Select("select platform_rate from sys_tenant where id = #{tenantId}")
    BigDecimal selectPlatformRate(@Param("tenantId") Long tenantId);
}
