package com.witos.vpp.event.mapper;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import com.witos.vpp.event.domain.DrCapacitySnapshot;
import com.witos.common.mybatisplus.mapper.BaseMapperX;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * VPP 首页每小时容量快照 Mapper接口
 *
 * @author witos
 */
public interface DrCapacitySnapshotMapper extends BaseMapperX<DrCapacitySnapshot>
{
    /**
     * 写入快照（station_id + snapshot_hour 唯一键，每场站每小时一条，重复写入时覆盖；
     * 显式带 tenant_id/station_id 列，租户插件不会重复注入）
     */
    @Insert("insert into dr_capacity_snapshot (tenant_id, station_id, snapshot_hour, peak_power, valley_power, rated_power, " +
            "station_count, pile_count, online_count, offline_count, fault_count, create_time) " +
            "values (#{tenantId}, #{stationId}, #{snapshotHour}, #{peakPower}, #{valleyPower}, #{ratedPower}, " +
            "#{stationCount}, #{pileCount}, #{onlineCount}, #{offlineCount}, #{faultCount}, now()) " +
            "on duplicate key update peak_power = values(peak_power), valley_power = values(valley_power), " +
            "rated_power = values(rated_power), station_count = values(station_count), pile_count = values(pile_count), " +
            "online_count = values(online_count), offline_count = values(offline_count), fault_count = values(fault_count)")
    int upsertSnapshot(DrCapacitySnapshot snapshot);

    /**
     * 查询指定小时区间内的快照（含边界，按小时聚合升序；
     * 非平台租户由租户插件自动追加 tenant_id 过滤，场站方用户再传 stationIds 限定自己的场站，
     * 超管传 null 查全部）
     */
    @Select("<script>select snapshot_hour, sum(peak_power) as peak_power, sum(valley_power) as valley_power, " +
            "sum(rated_power) as rated_power, sum(station_count) as station_count, sum(pile_count) as pile_count, " +
            "sum(online_count) as online_count, sum(offline_count) as offline_count, sum(fault_count) as fault_count " +
            "from dr_capacity_snapshot " +
            "where snapshot_hour &gt;= #{startHour} and snapshot_hour &lt;= #{endHour} " +
            "<if test='stationIds != null and stationIds.size() > 0'>and station_id in " +
            "<foreach collection='stationIds' item='id' open='(' separator=',' close=')'>#{id}</foreach> </if>" +
            "group by snapshot_hour order by snapshot_hour</script>")
    List<DrCapacitySnapshot> selectByHourRange(@Param("startHour") Date startHour, @Param("endHour") Date endHour,
                                               @Param("stationIds") Collection<Long> stationIds);

    /**
     * 查询不晚于指定小时的最近一条快照（环比取昨日同点用，按小时聚合；场站限定同 selectByHourRange）
     */
    @Select("<script>select snapshot_hour, sum(peak_power) as peak_power, sum(valley_power) as valley_power, " +
            "sum(rated_power) as rated_power, sum(station_count) as station_count, sum(pile_count) as pile_count, " +
            "sum(online_count) as online_count, sum(offline_count) as offline_count, sum(fault_count) as fault_count " +
            "from dr_capacity_snapshot " +
            "where snapshot_hour &lt;= #{hour} " +
            "<if test='stationIds != null and stationIds.size() > 0'>and station_id in " +
            "<foreach collection='stationIds' item='id' open='(' separator=',' close=')'>#{id}</foreach> </if>" +
            "group by snapshot_hour order by snapshot_hour desc limit 1</script>")
    DrCapacitySnapshot selectLatestBefore(@Param("hour") Date hour,
                                          @Param("stationIds") Collection<Long> stationIds);

    /**
     * 取全局最新一条快照的小时（所有场站同一小时统一写盘，全局最大值即最后写入小时；
     * 表空返回 null，供启动补写缺口判断用）
     */
    @Select("select max(snapshot_hour) from dr_capacity_snapshot")
    Date selectMaxSnapshotHour();
}
