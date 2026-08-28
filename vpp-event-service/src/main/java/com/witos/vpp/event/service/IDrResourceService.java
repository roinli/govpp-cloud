package com.witos.vpp.event.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import com.witos.vpp.event.domain.DrResource;
import com.baomidou.mybatisplus.core.metadata.IPage;

/**
 * VPP 资源台账 Service接口
 *
 * @author witos
 */
public interface IDrResourceService
{
    DrResource selectDrResourceByResourceId(Long resourceId);

    IPage<DrResource> selectDrResourcePage(DrResource resource);

    List<DrResource> selectDrResourceList(DrResource resource);

    int insertDrResource(DrResource resource);

    int updateDrResource(DrResource resource);

    int deleteDrResourceByIds(Long[] resourceIds);

    int deleteDrResourceById(Long resourceId);

    /**
     * 批量设置参与状态
     *
     * @param resourceIds 资源ID集合
     * @param participateStatus 目标状态 0停用 1启用
     * @return 结果
     */
    int updateParticipateStatus(Long[] resourceIds, String participateStatus);

    /**
     * 场站可调容量汇总（用于事件申报选择场站）
     */
    List<Map<String, Object>> getStationCapacityList();

    /**
     * 场站可调TOP排行（type：peak=削峰可调 valley=填谷可调）
     */
    List<Map<String, Object>> getStationRank(String type, int limit);

    /**
     * 运营商参与事件饼图（按租户聚合事件数）
     */
    List<Map<String, Object>> getOperatorEventPie();

    /**
     * 资源7日功率趋势（充电负荷每日均值，非削峰填谷可调功率）
     *
     * @param resourceIds 资源ID集合
     * @return resourceId -> [{date, power, peakPower}]（按日期升序，缺失日期以基线回退）
     */
    Map<Long, List<Map<String, Object>>> getPower7dTrend(Long[] resourceIds);

    /**
     * 总负荷曲线（充电负荷实时口径，分钟级数据按15分钟聚合）
     *
     * @param date 查询日期 yyyy-MM-dd，空取当天
     * @param ownerType 资源归属过滤：operator=运营商桩 private=私有桩，空=全部
     * @return [{time, load}]（按时间升序）
     */
    List<Map<String, Object>> getLoadCurve(String date, String ownerType);

    /**
     * 资源统计
     */
    Map<String, Object> getResourceStats(Collection<Long> stationIds);
}