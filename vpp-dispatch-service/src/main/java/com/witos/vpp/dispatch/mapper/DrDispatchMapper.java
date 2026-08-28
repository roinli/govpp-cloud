package com.witos.vpp.dispatch.mapper;

import java.util.List;

import com.witos.vpp.dispatch.domain.DrDispatch;
import com.witos.common.mybatisplus.mapper.BaseMapperX;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * VPP调度分配Mapper接口
 *
 * @author witos
 * @date 2026-08-02
 */
public interface DrDispatchMapper extends BaseMapperX<DrDispatch>
{
    /**
     * 批量插入调度分配记录（一条SQL，避免逐桩insert导致分配接口慢）
     * tenant_id/update_by/update_time/del_flag/remark 不写，走库表默认值，与单条insert行为一致
     */
    @Insert("<script>" +
            "insert into dr_dispatch (event_id, event_no, station_id, station_name, pile_no, adjustable_power, " +
            "baseline_power, target_power, reduce_power, status, create_by, create_time) values " +
            "<foreach collection='list' item='d' separator=','>" +
            "(#{d.eventId}, #{d.eventNo}, #{d.stationId}, #{d.stationName}, #{d.pileNo}, #{d.adjustablePower}, " +
            "#{d.baselinePower}, #{d.targetPower}, #{d.reducePower}, #{d.status}, #{d.createBy}, #{d.createTime})" +
            "</foreach>" +
            "</script>")
    int insertBatch(@Param("list") List<DrDispatch> list);

    /**
     * 批量更新分配结果（人工确认后的微调目标/压降量与状态），桩多时避免逐条update
     * update_time/update_by 手动维护，与单条update的自动填充一致
     */
    @Update("<script>" +
            "update dr_dispatch set " +
            "target_power = case dispatch_id " +
            "<foreach collection='list' item='d'>when #{d.dispatchId} then #{d.targetPower} </foreach>" +
            "end, " +
            "reduce_power = case dispatch_id " +
            "<foreach collection='list' item='d'>when #{d.dispatchId} then #{d.reducePower} </foreach>" +
            "end, " +
            "status = '1', update_time = now()" +
            "<if test='updateBy != null'>, update_by = #{updateBy}</if>" +
            " where dispatch_id in " +
            "<foreach collection='list' item='d' open='(' separator=',' close=')'>#{d.dispatchId}</foreach>" +
            "</script>")
    int updateBatchConfirm(@Param("list") List<DrDispatch> list, @Param("updateBy") String updateBy);
}
