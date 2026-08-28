package com.witos.system.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.witos.common.core.utils.StringUtils;
import com.witos.common.core.web.controller.BaseController;
import com.witos.common.core.web.domain.AjaxResult;
import com.witos.common.log.annotation.Log;
import com.witos.common.log.enums.BusinessType;
import com.witos.common.security.annotation.RequiresPermissions;
import com.witos.common.security.utils.SecurityUtils;
import com.witos.system.domain.SysPlatform;
import com.witos.system.mapper.SysUserMapper;
import com.witos.system.service.ISysPlatformService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 平台用户管理Controller
 */
@RestController
@RequestMapping("/platform")
public class SysPlatformController extends BaseController
{
    @Autowired
    private ISysPlatformService platformService;

    @Autowired
    private SysUserMapper userMapper;

    /**
     * 查询平台用户列表
     */
    @RequiresPermissions("system:user:list")
    @GetMapping("/list")
    public AjaxResult list(SysPlatform query)
    {
        IPage<SysPlatform> list = platformService.selectPlatformPage(query);
        return AjaxResult.success(list);
    }

    /**
     * 获取平台用户详细信息
     */
    @RequiresPermissions("system:user:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return AjaxResult.success(platformService.selectPlatformById(id));
    }

    /**
     * 新增平台用户
     */
    @RequiresPermissions("system:user:add")
    @Log(title = "平台用户", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody SysPlatform platform)
    {
        if (!platformService.checkUserNameUnique(platform.getUserName()))
        {
            return error("新增用户'" + platform.getUserName() + "'失败，登录账号已存在");
        }
        platform.setCreateBy(SecurityUtils.getUsername());
        return platformService.insertPlatform(platform);
    }

    /**
     * 修改平台用户
     */
    @RequiresPermissions("system:user:edit")
    @Log(title = "平台用户", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody SysPlatform platform)
    {
        platform.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(platformService.updatePlatform(platform));
    }

    /**
     * 删除平台用户
     */
    @RequiresPermissions("system:user:remove")
    @Log(title = "平台用户", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable("ids") Long[] ids)
    {
        return toAjax(platformService.deletePlatformByIds(ids));
    }

    /**
     * 重置密码
     */
    @RequiresPermissions("system:user:resetPwd")
    @Log(title = "平台用户", businessType = BusinessType.UPDATE)
    @PutMapping("/resetPwd")
    public AjaxResult resetPwd(@RequestBody SysPlatform platform)
    {
        if (platform.getUserId() == null)
        {
            return error("用户不存在");
        }
        platform.setUpdateBy(SecurityUtils.getUsername());
        int rows = userMapper.resetPwdByUserId(platform.getUserId(), SecurityUtils.encryptPassword(platform.getPassword()));
        return toAjax(rows);
    }
}
