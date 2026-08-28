package com.witos.vpp.event.mapper;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import com.witos.vpp.event.domain.DrResource;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.witos.common.mybatisplus.mapper.BaseMapperX;
import org.apache.ibatis.annotations.Param;

/**
 * VPP 资源台账 Mapper接口
 *
 * @author witos
 */
public interface DrResourceMapper extends BaseMapperX<DrResource>
{
    /**
     * 查询资源列表-分页
     */
    IPage<DrResource> selectDrResourcePage(Page page, @Param("query") DrResource resource);

    /**
     * 查询资源列表
     */
    List<DrResource> selectDrResourceList(@Param("query") DrResource resource);

    /**
     * 批量删除资源
     */
    int deleteDrResourceByIds(Long[] resourceIds);

    /**
     * 资源统计（参与场站/参与桩数/总额定功率/总可调功率）
     */
    Map<String, Object> selectResourceStats(@Param("stationIds") Collection<Long> stationIds);

    /**
     * 按场站分组的资源统计（容量快照任务逐场站写入用）
     */
    List<Map<String, Object>> selectResourceStatsGroupByStation();

    /**
     * 场站可调容量汇总（用于事件申报选择场站）
     */
    List<Map<String, Object>> selectStationCapacityList();

    /**
     * 场站可调TOP排行（按可调/填谷功率聚合）
     */
    List<Map<String, Object>> selectStationRank(@Param("stationIds") Collection<Long> stationIds,
            @Param("type") String type, @Param("limit") int limit);

    /**
     * 运营商参与事件饼图（按租户聚合事件数）
     */
    List<Map<String, Object>> selectOperatorEventPie(@Param("stationIds") Collection<Long> stationIds);

    /**
     * 场站参与事件饼图（按场站聚合事件数，tenantId 可选过滤运营商）
     */
    List<Map<String, Object>> selectStationEventPie(@Param("stationIds") Collection<Long> stationIds,
            @Param("tenantId") Long tenantId);

    /**
     * 根据租户ID查租户名称
     */
    String selectTenantNameById(@Param("tenantId") Long tenantId);
}