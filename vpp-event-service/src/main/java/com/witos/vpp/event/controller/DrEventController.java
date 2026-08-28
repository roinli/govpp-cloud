package com.witos.vpp.event.controller;

import java.util.List;
import java.util.Map;

import com.witos.vpp.event.domain.DrEvent;
import com.witos.vpp.event.service.IDrEventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.witos.common.log.annotation.Log;
import com.witos.common.log.enums.BusinessType;
import com.witos.common.security.annotation.RequiresPermissions;
import com.witos.common.core.web.controller.BaseController;
import com.witos.common.core.web.domain.AjaxResult;
import com.baomidou.mybatisplus.core.metadata.IPage;

/**
 * 需求响应事件Controller
 *
 * @author witos
 * @date 2026-08-02
 */
@RestController
@RequestMapping("/event")
public class DrEventController extends BaseController
{
    @Autowired
    private IDrEventService drEventService;

    /**
     * 查询需求响应事件列表
     */
    @RequiresPermissions("vpp:event:list")
    @GetMapping("/list")
    public AjaxResult list(DrEvent event)
    {
        IPage<DrEvent> list = drEventService.selectDrEventPage(event);
        return AjaxResult.success(list);
    }

    /**
     * 查询需求响应事件列表（不分页，大屏用：按角色过滤）
     */
    @RequiresPermissions("vpp:event:list")
    @GetMapping("/listAll")
    public AjaxResult listAll(DrEvent event)
    {
        List<DrEvent> list = drEventService.selectDrEventListByRole(event);
        return AjaxResult.success(list);
    }

    /**
     * 事件统计：待响应/响应中/已结束/本月
     */
    @RequiresPermissions("vpp:event:list")
    @GetMapping("/stats")
    public AjaxResult stats()
    {
        Map<String, Object> stats = drEventService.getEventStats();
        return AjaxResult.success(stats);
    }

    /**
     * 获取需求响应事件详细信息
     */
    @RequiresPermissions("vpp:event:info")
    @GetMapping(value = "/{eventId}")
    public AjaxResult getInfo(@PathVariable("eventId") Long eventId)
    {
        return AjaxResult.success(drEventService.selectDrEventByEventId(eventId));
    }

    /**
     * 新增需求响应事件（支持手动录入，对接电网系统留接口）
     */
    @RequiresPermissions("vpp:event:add")
    @Log(title = "需求响应事件", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody DrEvent event)
    {
        return toAjax(drEventService.insertDrEvent(event));
    }

    /**
     * 修改需求响应事件
     */
    @RequiresPermissions("vpp:event:edit")
    @Log(title = "需求响应事件", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody DrEvent event)
    {
        return toAjax(drEventService.updateDrEvent(event));
    }

    /**
     * 更新事件状态（手动流转：0待响应→1响应中→2已结束）
     */
    @RequiresPermissions("vpp:event:edit")
    @Log(title = "需求响应事件", businessType = BusinessType.UPDATE)
    @PutMapping("/status/{eventId}")
    public AjaxResult updateStatus(@PathVariable("eventId") Long eventId, @RequestParam("status") String status)
    {
        return toAjax(drEventService.updateEventStatus(eventId, status));
    }

    /**
     * 删除需求响应事件
     */
    @RequiresPermissions("vpp:event:remove")
    @Log(title = "需求响应事件", businessType = BusinessType.DELETE)
    @DeleteMapping("/{eventIds}")
    public AjaxResult remove(@PathVariable("eventIds") Long[] eventIds)
    {
        return toAjax(drEventService.deleteDrEventByEventIds(eventIds));
    }
}
