package com.witos.vpp.event.mapper;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import com.witos.vpp.event.domain.DrPowerDaily;
import com.witos.common.mybatisplus.mapper.BaseMapperX;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * VPP 资源每日功率 Mapper接口
 *
 * @author witos
 */
public interface DrPowerDailyMapper extends BaseMapperX<DrPowerDaily>
{
    /**
     * 写入当日功率（resource_id + power_date 唯一键，重复写入时覆盖；
     * 显式带 tenant_id 列，租户插件不会重复注入）
     */
    @Insert("insert into dr_resource_power_daily (tenant_id, station_id, resource_id, power_date, avg_power, peak_power, create_time) " +
            "values (#{tenantId}, #{stationId}, #{resourceId}, #{powerDate}, #{avgPower}, #{peakPower}, now()) " +
            "on duplicate key update avg_power = values(avg_power), peak_power = values(peak_power)")
    int upsertDailyPower(DrPowerDaily daily);

    /**
     * 查询指定资源在日期区间内的每日功率（按资源、日期升序；
     * 非平台租户由租户插件自动追加 tenant_id 过滤）
     */
    @Select("<script>select id, tenant_id, station_id, resource_id, power_date, avg_power, peak_power, create_time " +
            "from dr_resource_power_daily " +
            "where resource_id in " +
            "<foreach collection='resourceIds' item='id' open='(' separator=',' close=')'>#{id}</foreach> " +
            "and power_date &gt;= #{startDate} and power_date &lt;= #{endDate} " +
            "order by resource_id, power_date</script>")
    List<DrPowerDaily> selectByResourceRange(@Param("resourceIds") Collection<Long> resourceIds,
            @Param("startDate") Date startDate, @Param("endDate") Date endDate);

    /**
     * 逐资源统计最后一条功率记录日期（定时任务按缺口补写用）
     */
    @Select("select resource_id, max(power_date) as power_date from dr_resource_power_daily group by resource_id")
    List<DrPowerDaily> selectMaxDateGroupByResource();
}