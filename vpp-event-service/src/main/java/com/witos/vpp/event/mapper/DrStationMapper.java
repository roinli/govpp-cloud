package com.witos.vpp.event.mapper;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import com.witos.vpp.event.domain.DrStation;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.witos.common.mybatisplus.mapper.BaseMapperX;
import org.apache.ibatis.annotations.Param;

/**
 * VPP 场站管理 Mapper接口
 *
 * @author witos
 */
public interface DrStationMapper extends BaseMapperX<DrStation>
{
    /**
     * 查询场站列表-分页
     */
    IPage<DrStation> selectDrStationPage(Page page, @Param("query") DrStation station);

    /**
     * 查询场站列表
     */
    List<DrStation> selectDrStationList(@Param("query") DrStation station);

    /**
     * 场站统计（场站数/总容量/桩数）
     */
    Map<String, Object> selectStationStats(@Param("stationIds") Collection<Long> stationIds);
}
