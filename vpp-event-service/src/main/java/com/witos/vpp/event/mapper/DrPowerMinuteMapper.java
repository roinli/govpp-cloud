package com.witos.vpp.event.mapper;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.witos.vpp.event.domain.DrPowerMinute;
import com.witos.common.mybatisplus.mapper.BaseMapperX;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * VPP 资源分钟级功率 Mapper接口
 *
 * @author witos
 */
public interface DrPowerMinuteMapper extends BaseMapperX<DrPowerMinute>
{
    /**
     * 写入桩指定分钟的负载功率（resource_id + power_time 唯一键，重复写入时覆盖；
     * 显式带 tenant_id 列，租户插件不会重复注入）
     */
    @Insert("insert into dr_resource_power_minute (tenant_id, station_id, resource_id, power_time, load_power, create_time) " +
            "values (#{tenantId}, #{stationId}, #{resourceId}, #{powerTime}, #{loadPower}, now()) " +
            "on duplicate key update load_power = values(load_power)")
    int upsertMinutePower(DrPowerMinute minute);

    /**
     * 批量写入桩分钟级负载功率（resource_id + power_time 唯一键，重复写入时覆盖；
     * 显式带 tenant_id 列，租户插件不会重复注入），回填当天缺口用
     */
    @Insert("<script>insert into dr_resource_power_minute (tenant_id, station_id, resource_id, power_time, load_power, create_time) values " +
            "<foreach collection='list' item='m' separator=','>(#{m.tenantId}, #{m.stationId}, #{m.resourceId}, #{m.powerTime}, #{m.loadPower}, now())</foreach> " +
            "on duplicate key update load_power = values(load_power)</script>")
    int insertBatchMinutes(@Param("list") List<DrPowerMinute> minutes);

    /**
     * 指定资源集合在时间区间内的负荷合计，按15分钟粒度聚合（升序；
     * 非平台租户由租户插件自动追加 tenant_id 过滤）
     */
    @Select("<script>select from_unixtime(floor(unix_timestamp(power_time) / 900) * 900) as power_time, " +
            "sum(load_power) as load_power " +
            "from dr_resource_power_minute " +
            "where resource_id in " +
            "<foreach collection='resourceIds' item='id' open='(' separator=',' close=')'>#{id}</foreach> " +
            "and power_time &gt;= #{startTime} and power_time &lt; #{endTime} " +
            "group by floor(unix_timestamp(power_time) / 900) " +
            "order by power_time</script>")
    List<DrPowerMinute> selectLoadGroupByQuarter(@Param("resourceIds") Collection<Long> resourceIds,
            @Param("startTime") Date startTime, @Param("endTime") Date endTime);

    /**
     * 逐资源统计指定时间区间内的分钟记录数（定时任务判断昨日缺口是否需要回补；
     * 查询列不含租户字段，租户插件不会追加过滤）
     */
    @Select("select resource_id as resourceId, count(*) as cnt from dr_resource_power_minute " +
            "where power_time >= #{startTime} and power_time < #{endTime} group by resource_id")
    List<Map<String, Object>> selectCountGroupByResource(@Param("startTime") Date startTime,
            @Param("endTime") Date endTime);

    /**
     * 删除指定时间之前的分钟数据（凌晨定时清理用，避免表无限增长；
     * 定时线程无租户上下文时默认平台租户，不做租户过滤，可清理全部租户数据）
     */
    @Delete("delete from dr_resource_power_minute where power_time < #{beforeTime}")
    int deleteBeforeTime(@Param("beforeTime") Date beforeTime);
}