package com.witos.vpp.event.service;

import java.util.List;
import java.util.Map;
import com.witos.vpp.event.domain.DrEvent;
import com.baomidou.mybatisplus.core.metadata.IPage;

/**
 * 需求响应事件Service接口
 *
 * @author witos
 * @date 2026-08-02
 */
public interface IDrEventService
{
    DrEvent selectDrEventByEventId(Long eventId);

    IPage<DrEvent> selectDrEventPage(DrEvent event);

    List<DrEvent> selectDrEventList(DrEvent event);

    /** 大屏用：按角色过滤事件列表 */
    List<DrEvent> selectDrEventListByRole(DrEvent event);

    int insertDrEvent(DrEvent event);

    int updateDrEvent(DrEvent event);

    int deleteDrEventByEventIds(Long[] eventIds);

    /** 事件统计：待响应/响应中/已结束/本月 */
    Map<String, Object> getEventStats();

    /** 手动更新事件状态（0→1→2 顺序流转） */
    int updateEventStatus(Long eventId, String status);

    /** 定时自动更新事件状态 */
    int autoUpdateEventStatus();
}
