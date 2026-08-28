package com.witos.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.witos.common.core.web.domain.AjaxResult;
import com.witos.system.domain.SysPlatform;

/**
 * 平台用户管理Service接口
 */
public interface ISysPlatformService
{
    IPage<SysPlatform> selectPlatformPage(SysPlatform query);

    SysPlatform selectPlatformById(Long id);

    boolean checkUserNameUnique(String userName);

    AjaxResult insertPlatform(SysPlatform platform);

    int updatePlatform(SysPlatform platform);

    int deletePlatformByIds(Long[] ids);
}
