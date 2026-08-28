package com.witos.system.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.witos.common.mybatisplus.mapper.BaseMapperX;
import com.witos.common.mybatisplus.query.LambdaQueryWrapperX;
import com.witos.system.domain.SysPlatformPackage;

/**
 * 平台套餐Mapper接口
 *
 * @author witos
 */
public interface SysPlatformPackageMapper extends BaseMapperX<SysPlatformPackage>
{
    /**
     * 查询平台套餐列表
     */
    default IPage<SysPlatformPackage> selectPackageList(SysPlatformPackage query) {
        return selectPage(new LambdaQueryWrapperX<SysPlatformPackage>()
                .likeIfPresent(SysPlatformPackage::getName, query.getName())
                .eqIfPresent(SysPlatformPackage::getStatus, query.getStatus())
        );
    }

    /**
     * 查询平台套餐精简列表（仅 id + name，用于下拉选择）
     */
    default List<SysPlatformPackage> selectSimpleList() {
        return selectList(new LambdaQueryWrapperX<SysPlatformPackage>()
                .select(SysPlatformPackage::getId, SysPlatformPackage::getName)
                .eq(SysPlatformPackage::getStatus, 0)
                .eq(SysPlatformPackage::getDelFlag, "0")
        );
    }
}
