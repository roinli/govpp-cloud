package com.witos.system.controller;

import java.util.List;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.witos.common.log.annotation.Log;
import com.witos.common.log.enums.BusinessType;
import com.witos.common.security.annotation.RequiresPermissions;
import com.witos.system.domain.SysPlatformPackage;
import com.witos.system.service.ISysPlatformPackageService;
import com.witos.common.core.web.controller.BaseController;
import com.witos.common.core.web.domain.AjaxResult;

/**
 * 平台套餐Controller
 *
 * @author witos
 */
@RestController
@RequestMapping("/platformpackage")
public class SysPlatformPackageController extends BaseController
{
    @Autowired
    private ISysPlatformPackageService packageService;

    /**
     * 查询平台套餐列表
     */
    @RequiresPermissions("system:platformpackage:list")
    @GetMapping("/list")
    public AjaxResult list(SysPlatformPackage query)
    {
        IPage<SysPlatformPackage> list = packageService.selectPackageList(query);
        return AjaxResult.success(list);
    }

    /**
     * 查询平台套餐精简列表（下拉选择用）
     */
    @GetMapping("/get-simple-list")
    public AjaxResult getSimpleList()
    {
        List<SysPlatformPackage> list = packageService.selectSimpleList();
        return AjaxResult.success(list);
    }

    /**
     * 获取平台套餐详细信息
     */
    @RequiresPermissions("system:platformpackage:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return AjaxResult.success(packageService.selectPackageById(id));
    }

    /**
     * 新增平台套餐
     */
    @RequiresPermissions("system:platformpackage:add")
    @Log(title = "平台套餐", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody SysPlatformPackage pkg)
    {
        return toAjax(packageService.insertPackage(pkg));
    }

    /**
     * 修改平台套餐
     */
    @RequiresPermissions("system:platformpackage:edit")
    @Log(title = "平台套餐", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody SysPlatformPackage pkg)
    {
        return toAjax(packageService.updatePackage(pkg));
    }

    /**
     * 删除平台套餐
     */
    @RequiresPermissions("system:platformpackage:remove")
    @Log(title = "平台套餐", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable("ids") Long[] ids)
    {
        return toAjax(packageService.deletePackageByIds(ids));
    }
}
