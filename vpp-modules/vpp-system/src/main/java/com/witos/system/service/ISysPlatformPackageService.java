package com.witos.system.service;

import java.util.List;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.witos.system.domain.SysPlatformPackage;

/**
 * 平台套餐Service接口
 *
 * @author witos
 */
public interface ISysPlatformPackageService
{
    /**
     * 查询平台套餐
     *
     * @param id 套餐主键
     * @return 平台套餐
     */
    SysPlatformPackage selectPackageById(Long id);

    /**
     * 查询平台套餐列表
     *
     * @param query 查询条件
     * @return 套餐集合
     */
    IPage<SysPlatformPackage> selectPackageList(SysPlatformPackage query);

    /**
     * 查询平台套餐精简列表（下拉选择用）
     *
     * @return 套餐精简列表
     */
    List<SysPlatformPackage> selectSimpleList();

    /**
     * 新增平台套餐
     *
     * @param pkg 套餐信息
     * @return 结果
     */
    int insertPackage(SysPlatformPackage pkg);

    /**
     * 修改平台套餐
     *
     * @param pkg 套餐信息
     * @return 结果
     */
    int updatePackage(SysPlatformPackage pkg);

    /**
     * 批量删除平台套餐
     *
     * @param ids 需要删除的套餐主键集合
     * @return 结果
     */
    int deletePackageByIds(Long[] ids);
}
