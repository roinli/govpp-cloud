package com.witos.system.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.witos.system.domain.SysPlatform;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 平台用户管理Mapper接口
 */
public interface SysPlatformMapper
{
    IPage<SysPlatform> selectPlatformList(Page page, @Param("query") SysPlatform query);

    List<SysPlatform> selectPlatformList(@Param("query") SysPlatform query);

    SysPlatform selectPlatformById(Long id);

    SysPlatform checkUserNameUnique(String userName);

    int insertPlatform(SysPlatform platform);

    int updatePlatform(SysPlatform platform);

    int deletePlatformByIds(Long[] ids);
}
