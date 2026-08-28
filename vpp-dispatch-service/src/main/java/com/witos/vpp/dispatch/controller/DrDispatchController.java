package com.witos.vpp.dispatch.controller;

import java.util.List;

import com.witos.vpp.dispatch.domain.DrDispatch;
import com.witos.vpp.dispatch.service.IDrDispatchService;
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
 * VPP调度分配Controller
 *
 * @author witos
 * @date 2026-08-02
 */
@RestController
@RequestMapping("/dispatch")
public class DrDispatchController extends BaseController
{
    @Autowired
    private IDrDispatchService drDispatchService;

    /**
     * 查询VPP调度分配列表
     */
    @RequiresPermissions("vpp:dispatch:list")
    @GetMapping("/list")
    public AjaxResult list(DrDispatch dispatch)
    {
        IPage<DrDispatch> list = drDispatchService.selectDrDispatchPage(dispatch);
        return AjaxResult.success(list);
    }

    /**
     * 查询VPP调度分配列表（不分页）
     */
    @RequiresPermissions("vpp:dispatch:list")
    @GetMapping("/listAll")
    public AjaxResult listAll(DrDispatch dispatch)
    {
        List<DrDispatch> list = drDispatchService.selectDrDispatchList(dispatch);
        return AjaxResult.success(list);
    }

    /**
     * 获取VPP调度分配详细信息
     */
    @RequiresPermissions("vpp:dispatch:list")
    @GetMapping(value = "/{dispatchId}")
    public AjaxResult getInfo(@PathVariable("dispatchId") Long dispatchId)
    {
        return AjaxResult.success(drDispatchService.selectDrDispatchByDispatchId(dispatchId));
    }

    /**
     * 一键分配：目标调节量按可调容量/申报容量比例分配到场站、桩
     */
    @RequiresPermissions("vpp:dispatch:add")
    @Log(title = "VPP调度分配", businessType = BusinessType.INSERT)
    @PostMapping("/allocate/{eventId}")
    public AjaxResult allocate(@PathVariable("eventId") Long eventId)
    {
        return toAjax(drDispatchService.allocate(eventId));
    }

    /**
     * 分配结果人工确认（保存微调后的目标功率并校验）
     */
    @RequiresPermissions("vpp:dispatch:confirm")
    @Log(title = "VPP调度分配确认", businessType = BusinessType.UPDATE)
    @PutMapping("/confirm")
    public AjaxResult confirm(@RequestBody List<DrDispatch> list)
    {
        return toAjax(drDispatchService.confirm(list));
    }

    /**
     * 生成并下发指令（预留：复用现有控制通道）
     */
    @RequiresPermissions("vpp:dispatch:send")
    @Log(title = "VPP指令下发", businessType = BusinessType.UPDATE)
    @PutMapping("/send/{dispatchIds}")
    public AjaxResult send(@PathVariable("dispatchIds") Long[] dispatchIds)
    {
        return toAjax(drDispatchService.send(dispatchIds));
    }

    /**
     * 新增VPP调度分配
     */
    @RequiresPermissions("vpp:dispatch:add")
    @Log(title = "VPP调度分配", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody DrDispatch dispatch)
    {
        return toAjax(drDispatchService.insertDrDispatch(dispatch));
    }

    /**
     * 修改VPP调度分配
     */
    @RequiresPermissions("vpp:dispatch:edit")
    @Log(title = "VPP调度分配", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody DrDispatch dispatch)
    {
        return toAjax(drDispatchService.updateDrDispatch(dispatch));
    }

    /**
     * 删除VPP调度分配
     */
    @RequiresPermissions("vpp:dispatch:remove")
    @Log(title = "VPP调度分配", businessType = BusinessType.DELETE)
    @DeleteMapping("/{dispatchIds}")
    public AjaxResult remove(@PathVariable("dispatchIds") Long[] dispatchIds)
    {
        return toAjax(drDispatchService.deleteDrDispatchByDispatchIds(dispatchIds));
    }
}
