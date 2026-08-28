package com.witos.vpp.event.controller;

import java.util.List;

import com.witos.vpp.event.domain.DrApply;
import com.witos.vpp.event.service.IDrApplyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.witos.common.log.annotation.Log;
import com.witos.common.log.enums.BusinessType;
import com.witos.common.security.annotation.RequiresPermissions;
import com.witos.common.core.web.controller.BaseController;
import com.witos.common.core.web.domain.AjaxResult;
import com.baomidou.mybatisplus.core.metadata.IPage;

/**
 * 需求响应申报Controller
 *
 * @author witos
 * @date 2026-08-02
 */
@RestController
@RequestMapping("/apply")
public class DrApplyController extends BaseController
{
    @Autowired
    private IDrApplyService drApplyService;

    /**
     * 查询需求响应申报列表
     */
    @RequiresPermissions("vpp:event:list")
    @GetMapping("/list")
    public AjaxResult list(DrApply apply)
    {
        IPage<DrApply> list = drApplyService.selectDrApplyPage(apply);
        return AjaxResult.success(list);
    }

    /**
     * 查询需求响应申报列表（不分页）
     */
    @RequiresPermissions("vpp:event:list")
    @GetMapping("/listAll")
    public AjaxResult listAll(DrApply apply)
    {
        List<DrApply> list = drApplyService.selectDrApplyList(apply);
        return AjaxResult.success(list);
    }

    /**
     * 获取需求响应申报详细信息
     */
    @RequiresPermissions("vpp:event:list")
    @GetMapping(value = "/{applyId}")
    public AjaxResult getInfo(@PathVariable("applyId") Long applyId)
    {
        return AjaxResult.success(drApplyService.selectDrApplyByApplyId(applyId));
    }

    /**
     * 新增需求响应申报（申报容量 ≤ 可调容量由前端/调用方校验）
     */
    @RequiresPermissions("vpp:event:apply")
    @Log(title = "需求响应申报", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody DrApply apply)
    {
        return toAjax(drApplyService.insertDrApply(apply));
    }

    /**
     * 修改需求响应申报
     */
    @RequiresPermissions("vpp:event:apply")
    @Log(title = "需求响应申报", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody DrApply apply)
    {
        return toAjax(drApplyService.updateDrApply(apply));
    }

    /**
     * 确认申报（0待确认 → 1已确认）
     */
    @RequiresPermissions("vpp:event:apply")
    @Log(title = "需求响应申报", businessType = BusinessType.UPDATE)
    @PutMapping("/confirm/{applyIds}")
    public AjaxResult confirm(@PathVariable("applyIds") Long[] applyIds)
    {
        return toAjax(drApplyService.confirmApply(applyIds));
    }

    /**
     * 删除需求响应申报
     */
    @RequiresPermissions("vpp:event:apply")
    @Log(title = "需求响应申报", businessType = BusinessType.DELETE)
    @DeleteMapping("/{applyIds}")
    public AjaxResult remove(@PathVariable("applyIds") Long[] applyIds)
    {
        return toAjax(drApplyService.deleteDrApplyByApplyIds(applyIds));
    }
}
