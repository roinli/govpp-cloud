package com.witos.system.service.impl;

import java.util.ArrayList;
import java.util.List;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.witos.common.core.utils.StringUtils;
import com.witos.system.api.domain.SysRole;
import com.witos.system.domain.SysPlatformPackage;
import com.witos.system.domain.SysRoleMenu;
import com.witos.system.mapper.SysPlatformPackageMapper;
import com.witos.system.mapper.SysRoleMapper;
import com.witos.system.mapper.SysRoleMenuMapper;
import com.witos.system.service.ISysPlatformPackageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 平台套餐Service业务层处理
 *
 * @author witos
 */
@Service
public class SysPlatformPackageServiceImpl implements ISysPlatformPackageService
{
    @Autowired
    private SysPlatformPackageMapper packageMapper;

    @Autowired
    private SysRoleMapper roleMapper;

    @Autowired
    private SysRoleMenuMapper roleMenuMapper;

    @Override
    public SysPlatformPackage selectPackageById(Long id)
    {
        return packageMapper.selectById(id);
    }

    @Override
    public IPage<SysPlatformPackage> selectPackageList(SysPlatformPackage query)
    {
        return packageMapper.selectPackageList(query);
    }

    @Override
    public List<SysPlatformPackage> selectSimpleList()
    {
        return packageMapper.selectSimpleList();
    }

    @Override
    public int insertPackage(SysPlatformPackage pkg)
    {
        return packageMapper.insert(pkg);
    }

    /**
     * 修改平台套餐（同步更新角色菜单关联）
     */
    @Override
    @Transactional
    public int updatePackage(SysPlatformPackage pkg)
    {
        // 获取旧套餐，比较菜单变化
        SysPlatformPackage oldPkg = packageMapper.selectById(pkg.getId());
        if (oldPkg != null && StringUtils.isNotEmpty(oldPkg.getMenuIds()) && StringUtils.isNotEmpty(pkg.getMenuIds()))
        {
            String[] oldMenuIds = oldPkg.getMenuIds().split(",");
            String[] newMenuIds = pkg.getMenuIds().split(",");
            // 查找该套餐对应的角色（roleKey = _pkg_ + packageId）
            String roleKey = "_pkg_" + pkg.getId();
            SysRole pkgRole = roleMapper.checkRoleKeyUnique(roleKey, 9999L);
            if (pkgRole != null)
            {
                // 删除该角色所有菜单关联，重新插入新菜单
                roleMenuMapper.deleteRoleMenuByRoleId(pkgRole.getRoleId());
                List<SysRoleMenu> roleMenuList = new ArrayList<>();
                for (String menuIdStr : newMenuIds)
                {
                    if (StringUtils.isNotEmpty(menuIdStr))
                    {
                        SysRoleMenu rm = new SysRoleMenu();
                        rm.setRoleId(pkgRole.getRoleId());
                        rm.setMenuId(Long.parseLong(menuIdStr.trim()));
                        rm.setTenantId(9999L);
                        roleMenuList.add(rm);
                    }
                }
                if (!roleMenuList.isEmpty())
                {
                    roleMenuMapper.batchRoleMenu(roleMenuList);
                }
            }
        }
        return packageMapper.updateById(pkg);
    }

    @Override
    public int deletePackageByIds(Long[] ids)
    {
        return packageMapper.deleteBatchIds(java.util.Arrays.asList(ids));
    }
}
