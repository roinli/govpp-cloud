package com.witos.system.service.impl;

import java.util.ArrayList;
import java.util.List;

import com.witos.common.core.utils.StringUtils;
import com.witos.common.core.web.domain.AjaxResult;
import com.witos.common.mybatisplus.util.TenantUtils;
import com.witos.common.security.utils.SecurityUtils;
import com.witos.system.api.domain.SysRole;
import com.witos.system.api.domain.SysUser;
import com.witos.system.domain.SysPlatform;
import com.witos.system.domain.SysPlatformPackage;
import com.witos.system.domain.SysRoleMenu;
import com.witos.system.domain.SysUserPost;
import com.witos.system.domain.SysUserRole;
import com.witos.system.mapper.SysPlatformMapper;
import com.witos.system.mapper.SysRoleMapper;
import com.witos.system.mapper.SysRoleMenuMapper;
import com.witos.system.mapper.SysUserMapper;
import com.witos.system.mapper.SysUserRoleMapper;
import com.witos.system.service.ISysPlatformPackageService;
import com.witos.system.service.ISysPlatformService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.witos.common.core.text.Convert;
import com.witos.common.core.utils.ServletUtils;
import com.witos.common.mybatisplus.constant.MybatisPageConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 平台用户管理Service实现
 */
@Service
public class SysPlatformServiceImpl implements ISysPlatformService
{
    @Autowired
    private SysPlatformMapper platformMapper;

    @Autowired
    private SysUserMapper userMapper;

    @Autowired
    private SysRoleMapper roleMapper;

    @Autowired
    private SysRoleMenuMapper roleMenuMapper;

    @Autowired
    private SysUserRoleMapper userRoleMapper;

    @Autowired
    private ISysPlatformPackageService platformPackageService;

    /**
     * 分页查询平台用户列表
     */
    @Override
    public IPage<SysPlatform> selectPlatformPage(SysPlatform query)
    {
        Page mpPage = new Page(
                Convert.toLong(ServletUtils.getParameterToInt(MybatisPageConstants.PAGE_NUM), 1L),
                Convert.toLong(ServletUtils.getParameterToInt(MybatisPageConstants.PAGE_SIZE), 10L));
        return platformMapper.selectPlatformList(mpPage, query);
    }

    @Override
    public SysPlatform selectPlatformById(Long id)
    {
        return platformMapper.selectPlatformById(id);
    }

    @Override
    public boolean checkUserNameUnique(String userName)
    {
        SysPlatform info = platformMapper.checkUserNameUnique(userName);
        return info == null;
    }

    /**
     * 新增平台用户（和新增租户一样：存主表 + 创建 sys_user 账号 + 创建角色菜单）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult insertPlatform(SysPlatform platform)
    {
        if (StringUtils.isEmpty(platform.getUserName()))
        {
            return AjaxResult.error("用户名称为空，请重新设置！");
        }
        SysPlatform check = platformMapper.checkUserNameUnique(platform.getUserName());
        if (check != null)
        {
            return AjaxResult.error("用户名已存在，请重新设置！");
        }
        // 默认密码
        if (StringUtils.isEmpty(platform.getPassword()))
        {
            platform.setPassword(SecurityUtils.encryptPassword("admin123"));
        }
        else
        {
            platform.setPassword(SecurityUtils.encryptPassword(platform.getPassword()));
        }
        // 1. 插入 sys_platform
        platformMapper.insertPlatform(platform);

        // 2. 在平台租户上下文下创建 sys_user 账号 + 角色权限
        TenantUtils.execute(9999L, () -> {
            // 创建 sys_user
            SysUser user = new SysUser();
            user.setUserName(platform.getUserName());
            user.setNickName(platform.getNickName());
            user.setPhonenumber(platform.getUserPhone());
            user.setPassword(platform.getPassword());
            user.setUserType("00");
            user.setTenantId(9999L);
            user.setStatus(platform.getStatus() != null ? platform.getStatus() : "0");
            user.setCreateBy(platform.getCreateBy());
            userMapper.insertUser(user);

            // 创建套餐角色（菜单权限）
            if (platform.getPackageId() != null)
            {
                createPackageRole(platform.getPackageId(), user.getUserId(), platform.getUserName());
            }
        });

        return AjaxResult.success("平台用户创建成功！");
    }

    /**
     * 修改平台用户
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updatePlatform(SysPlatform platform)
    {
        SysPlatform old = platformMapper.selectPlatformById(platform.getId());
        // 如果套餐变了，需要重新授权角色菜单
        if (platform.getPackageId() != null && !platform.getPackageId().equals(old.getPackageId()))
        {
            TenantUtils.execute(9999L, () -> {
                // 查找旧的套餐角色
                SysRole oldPkgRole = roleMapper.checkRoleKeyUnique("_pkg_" + old.getPackageId(), 9999L);
                if (oldPkgRole != null)
                {
                    // 删除旧角色的菜单关联
                    roleMenuMapper.deleteRoleMenuByRoleId(oldPkgRole.getRoleId());
                }
                // 查找对应的 sys_user
                SysUser user = userMapper.selectUserByUserName(old.getUserName());
                if (user != null)
                {
                    // 创建新套餐角色并绑定
                    createPackageRole(platform.getPackageId(), user.getUserId(), old.getUserName());
                }
            });
        }
        return platformMapper.updatePlatform(platform);
    }

    /**
     * 批量删除平台用户
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deletePlatformByIds(Long[] ids)
    {
        for (Long id : ids)
        {
            SysPlatform platform = platformMapper.selectPlatformById(id);
            if (platform != null)
            {
                // 删除对应的 sys_user
                SysUser user = userMapper.selectUserByUserName(platform.getUserName());
                if (user != null)
                {
                    userRoleMapper.deleteUserRoleByUserId(user.getUserId());
                    userMapper.deleteUserById(user.getUserId());
                }
            }
        }
        return platformMapper.deletePlatformByIds(ids);
    }

    /**
     * 创建平台用户账号（和租户 createUser 对应）
     */
    private void createPlatformUser(SysPlatform platform)
    {
        SysUser user = new SysUser();
        user.setUserName(platform.getUserName());
        user.setNickName(platform.getNickName());
        user.setPhonenumber(platform.getUserPhone());
        user.setPassword(platform.getPassword());
        user.setUserType("00"); // 平台管理员，不允许修改删除
        user.setTenantId(9999L);
        user.setStatus(platform.getStatus() != null ? platform.getStatus() : "0");
        user.setCreateBy(platform.getCreateBy());
        userMapper.insertUser(user);

        if (platform.getPackageId() != null)
        {
            createPackageRole(platform.getPackageId(), user.getUserId(), platform.getUserName());
        }
    }

    /**
     * 创建套餐角色并绑定用户（和租户 createRoleMenu 对应）
     */
    private void createPackageRole(Long packageId, Long userId, String userName)
    {
        SysPlatformPackage pkg = platformPackageService.selectPackageById(packageId);
        if (pkg == null || StringUtils.isEmpty(pkg.getMenuIds()))
        {
            return;
        }
        String roleKey = "_pkg_" + packageId;
        // 查找或创建角色
        SysRole pkgRole = roleMapper.checkRoleKeyUnique(roleKey, 9999L);
        if (pkgRole == null)
        {
            pkgRole = new SysRole();
            pkgRole.setRoleName(pkg.getName());
            pkgRole.setRoleKey(roleKey);
            pkgRole.setRoleSort("100");
            pkgRole.setDataScope("1");
            pkgRole.setStatus("0");
            pkgRole.setTenantId(9999L);
            pkgRole.setCreateBy(userName);
            roleMapper.insertRole(pkgRole);
        }
        // 更新角色菜单
        roleMenuMapper.deleteRoleMenuByRoleId(pkgRole.getRoleId());
        String[] menuIdArr = pkg.getMenuIds().split(",");
        List<SysRoleMenu> roleMenuList = new ArrayList<>();
        for (String menuIdStr : menuIdArr)
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
        // 绑定用户角色
        List<SysRole> userRoles = roleMapper.selectRolesByUserName(
                userMapper.selectUserById(userId).getUserName());
        boolean alreadyBound = false;
        for (SysRole r : userRoles)
        {
            if (r.getRoleId().equals(pkgRole.getRoleId()))
            {
                alreadyBound = true;
                break;
            }
        }
        if (!alreadyBound)
        {
            SysUserRole userRole = new SysUserRole();
            userRole.setUserId(userId);
            userRole.setRoleId(pkgRole.getRoleId());
            userRole.setTenantId(9999L);
            userRoleMapper.batchUserRole(java.util.Collections.singletonList(userRole));
        }
    }
}
