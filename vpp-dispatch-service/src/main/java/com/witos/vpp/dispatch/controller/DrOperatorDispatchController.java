package com.witos.vpp.dispatch.controller;

import java.util.List;

import com.witos.vpp.dispatch.domain.DrOperatorDispatch;
import com.witos.vpp.dispatch.service.IDrOperatorDispatchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.witos.common.log.annotation.Log;
import com.witos.common.log.enums.BusinessType;
import com.witos.common.security.annotation.RequiresPermissions;
import com.witos.common.core.web.controller.BaseController;
import com.witos.common.core.web.domain.AjaxResult;

/**
 * VPP运营商分配Controller（平台 → 运营商）
 *
 * @author witos
 * @date 2026-08-14
 */
@RestController
@RequestMapping("/dispatch")
public class DrOperatorDispatchController extends BaseController
{
    @Autowired
    private IDrOperatorDispatchService drOperatorDispatchService;

    /**
     * 查询运营商分配列表（平台：全部；运营商：自己收到的）
     */
    @RequiresPermissions("vpp:dispatch:list")
    @GetMapping("/operatorList")
    public AjaxResult operatorList(DrOperatorDispatch query)
    {
        List<DrOperatorDispatch> list = drOperatorDispatchService.selectDrOperatorDispatchList(query);
        return AjaxResult.success(list);
    }

    /**
     * 平台一键分配：目标调节量按运营商申报量占比分配到运营商
     */
    @RequiresPermissions("vpp:dispatch:add")
    @Log(title = "VPP运营商分配", businessType = BusinessType.INSERT)
    @PostMapping("/allocateByOperator/{eventId}")
    public AjaxResult allocateByOperator(@PathVariable("eventId") Long eventId)
    {
        return toAjax(drOperatorDispatchService.allocateByOperator(eventId));
    }

    /**
     * 平台确认运营商分配：保存微调后的目标功率并 0待确认 -> 1已分配
     */
    @RequiresPermissions("vpp:dispatch:confirm")
    @Log(title = "VPP运营商分配确认", businessType = BusinessType.UPDATE)
    @PutMapping("/confirmOperator")
    public AjaxResult confirm(@RequestBody List<DrOperatorDispatch> list)
    {
        return toAjax(drOperatorDispatchService.confirm(list));
    }
}
