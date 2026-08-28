package com.witos.vpp.event.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.witos.vpp.event.domain.DrStation;
import com.baomidou.mybatisplus.core.metadata.IPage;

/**
 * VPP 场站管理 Service接口
 *
 * @author witos
 */
public interface IDrStationService
{
    DrStation selectDrStationByStationId(Long stationId);

    IPage<DrStation> selectDrStationPage(DrStation station);

    List<DrStation> selectDrStationList(DrStation station);

    int insertDrStation(DrStation station);

    int updateDrStation(DrStation station);

    int deleteDrStationByIds(Long[] stationIds);

    /**
     * 场站统计
     */
    Map<String, Object> getStationStats(Collection<Long> stationIds);

    Set<Long> getMyStationIds();
}
