package com.witos.vpp.dispatch.mapper;

import java.util.List;

import com.witos.vpp.dispatch.domain.DrOperatorDispatch;
import com.witos.common.mybatisplus.mapper.BaseMapperX;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * VPP运营商分配Mapper接口
 *
 * @author witos
 * @date 2026-08-14
 */
public interface DrOperatorDispatchMapper extends BaseMapperX<DrOperatorDispatch>
{
    /** 查询运营商（租户）名称 */
    @Select("select tenant_name from sys_tenant where id = #{tenantId}")
    String selectTenantName(@Param("tenantId") Long tenantId);

    /**
     * 批量更新运营商分配结果（人工确认后的微调目标/压降量与状态、确认时间）
     */
    @Update("<script>" +
            "update dr_operator_dispatch set " +
            "target_power = case dispatch_id " +
            "<foreach collection='list' item='d'>when #{d.dispatchId} then #{d.targetPower} </foreach>" +
            "end, " +
            "reduce_power = case dispatch_id " +
            "<foreach collection='list' item='d'>when #{d.dispatchId} then #{d.reducePower} </foreach>" +
            "end, " +
            "status = '1', confirm_time = now(), update_time = now()" +
            "<if test='updateBy != null'>, update_by = #{updateBy}</if>" +
            " where dispatch_id in " +
            "<foreach collection='list' item='d' open='(' separator=',' close=')'>#{d.dispatchId}</foreach>" +
            "</script>")
    int updateBatchConfirm(@Param("list") List<DrOperatorDispatch> list, @Param("updateBy") String updateBy);
}
