package com.witos.system.service.impl;

import java.util.*;
import java.util.stream.Collectors;

import com.witos.common.core.constant.CacheConstants;
import com.witos.common.core.exception.ServiceException;
import com.witos.common.core.utils.StringUtils;
import com.witos.common.core.web.domain.AjaxResult;
import com.witos.common.message.mail.EmailUtil;
import com.witos.common.message.sms.SmsUtil;
import com.witos.common.mybatisplus.util.TenantUtils;
import com.witos.common.redis.service.RedisService;
import com.witos.common.security.utils.SecurityUtils;
import com.witos.system.api.domain.*;
import com.witos.system.api.model.LoginUser;
import com.witos.system.domain.*;
import com.witos.system.mapper.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.witos.system.service.ISysTenantService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.witos.common.core.text.Convert;
import com.witos.common.core.utils.ServletUtils;
import com.witos.common.mybatisplus.constant.MybatisPageConstants;
import org.springframework.transaction.annotation.Transactional;


/**
 * 租户管理Service业务层处理
 *
 * @author witos
 * @date 2022-04-11
 */
@Slf4j
@Service
public class SysTenantServiceImpl implements ISysTenantService
{
    @Autowired
    private SysTenantMapper sysTenantMapper;

    @Autowired
    private SysTenantPackageMapper sysTenantPackageMapper;

    @Autowired
    private SysDeptMapper deptMapper;

    @Autowired
    private SysPostMapper sysPostMapper;

    @Autowired
    private SysRoleMapper sysRoleMapper;

    @Autowired
    private SysRoleMenuMapper sysRoleMenuMapper;

    @Autowired
    private SysRoleDeptMapper sysRoleDeptMapper;

    @Autowired
    private SysUserMapper userMapper;

    @Autowired
    private SysUserRoleMapper userRoleMapper;

    @Autowired
    private SysUserPostMapper userPostMapper;

    @Autowired
    EmailUtil emailUtil;

    @Autowired
    SmsUtil smsUtil;

    @Autowired
    private RedisService redisService;


    /**
     * 查询租户管理
     *
     * @param id 租户管理主键
     * @return 租户管理
     */
    @Override
    public SysTenant selectSysTenantById(Long id)
    {
        return sysTenantMapper.selectById(id);
    }

    /**
     * 查询租户管理列表-分页
     *
     * @param sysTenant 租户管理
     * @return 租户管理
     */
    @Override
    public IPage<SysTenant> selectSysTenantPage(SysTenant sysTenant)
    {
        Page mpPage =new Page(Convert.toLong(ServletUtils.getParameterToInt(MybatisPageConstants.PAGE_NUM),1L)
                ,Convert.toLong(ServletUtils.getParameterToInt(MybatisPageConstants.PAGE_SIZE),10L));
        return sysTenantMapper.selectSysTenantList(mpPage,sysTenant);
    }
    /**
     * 查询租户管理列表
     *
     * @param sysTenant 租户管理
     * @return 租户管理
     */
    @Override
    public List<SysTenant> selectSysTenantList(SysTenant sysTenant) {return sysTenantMapper.selectSysTenantList(sysTenant);}

    /**
     * 新增租户管理
     *
     * @param sysTenant 租户管理
     * @return 结果
     */

    @Override
    @Transactional(rollbackFor = Exception.class)

    public AjaxResult insertSysTenant(SysTenant sysTenant)
    {
        AjaxResult res = new AjaxResult();
        if (StringUtils.isEmpty(sysTenant.getUserName())){
            return res.error("管理员账号为空,请重新设置!");
        }
        //先判断租户管理员设置的账号是否存在
        SysUser usercount = userMapper.checkUserNameUnique(sysTenant.getUserName());
        if (!(usercount == null))
        {
            return res.error("用户名已存在,请重新设置!");
        }
        //创建租户
        sysTenantMapper.insert(sysTenant);
        //租户创建完成后 开始创建相关基础数据
        TenantUtils.execute(sysTenant.getId(), () -> {
            //创建默认部门--部门默认名称以租户名称
            Long deptid = createDept(sysTenant);
            //创建默认岗位--岗位默认为董事长
            Long postid = createPost(sysTenant.getUserName());
            //创建默认角色--角色默认为租户名称+管理员
            Long roleid = createRole(sysTenant);
            //创建场站管理员角色
            createStationAdminRole(sysTenant);
            //创建默认账号
            createUser(sysTenant,deptid,postid,roleid);
        });
        return res.success("租户创建成功!");
    }

    private void createUser(SysTenant sysTenant,Long deptId,Long postid,Long roleid) {
        SysUser user = new SysUser();
        user.setDeptId(deptId).setUserName(sysTenant.getUserName()).setNickName(sysTenant.getTenantName())
                .setUserType("00")//用户类型 00 表示各管理员账号，不允许租户修改删除 其他账号为10
                .setEmail(sysTenant.getUserEmail()).setPhonenumber(sysTenant.getUserPhone()).setRemark("租户管理员");
        //默认密码 admin123
        String password = SecurityUtils.encryptPassword("admin123");
        user.setPassword(password);
        userMapper.insert(user);
        userPostMapper.insert(new SysUserPost().setUserId(user.getUserId()).setPostId(postid));
        userRoleMapper.insert(new SysUserRole().setRoleId(roleid).setUserId(user.getUserId()));
        String configValue = Convert.toStr(redisService.getCacheObject( CacheConstants.SYS_CONFIG_KEY + "sys.message.type"));
        if ("false".equals(configValue)){
            emailUtil.sendSimpleMail("租户管理员账号注册成功","初始密码: admin123，请登录后尽快修改",sysTenant.getUserEmail());
        }else {
            try {
                smsUtil.send(sysTenant.getUserPhone(),"初始密码: admin123，请登录后尽快修改");
            }catch (Exception e){
                log.info("短信调用失败:"+e.getMessage());
                emailUtil.sendSimpleMail("租户管理员账号注册成功","初始密码: admin123，请登录后尽快修改",sysTenant.getUserEmail());
            }
        }
    }

    private Long createRole(SysTenant sysTenant) {
        // 创建角色
        SysRole role = new SysRole();
        role.setRoleName(sysTenant.getTenantName()+"管理员").setRoleKey("admin")
                .setRoleSort("1").setDataScope("1").setMenuCheckStrictly(true).setDeptCheckStrictly(true);
        role.setCreateBy(sysTenant.getUserName());
        role.setRemark("租户管理员");
        role.setAdminRole(true);
        sysRoleMapper.insert(role);
        //根据租户套餐ids查出套餐编码塞入角色-菜单表
        createRoleMenu(sysTenant,role);
        return role.getRoleId();
    }

    //目前为单套餐,跟租户绑定,解耦防止套餐变动影响多个租户
    private void createRoleMenu(SysTenant sysTenant,SysRole role)
    {
        SysTenantPackage sysTenantPackage = sysTenantPackageMapper.selectById(sysTenant.getTenantPackage());
        List<String> subMeuns = Arrays.asList(sysTenantPackage.getMenuIds().split(","));

        List<SysRoleMenu> roleMenuList = subMeuns.stream().map(menuid -> {
            SysRoleMenu entity = new SysRoleMenu();
            entity.setRoleId(role.getRoleId());
            entity.setMenuId(Convert.toLong(menuid));
            entity.setTenantId(Convert.toLong(sysTenant.getId()));
            return entity;
        }).collect(Collectors.toList());
        sysRoleMenuMapper.batchRoleMenu(roleMenuList);
    }

    // 创建场站管理员角色并分配菜单
    private Long createStationAdminRole(SysTenant sysTenant) {
        SysRole role = new SysRole();
        role.setRoleName("场站管理员").setRoleKey("station_admin")
                .setRoleSort("2").setDataScope("5").setMenuCheckStrictly(true).setDeptCheckStrictly(true);
        role.setCreateBy(sysTenant.getUserName());
        role.setRemark("运营商场站管理员");
        role.setAdminRole(false);
        sysRoleMapper.insert(role);
        createStationAdminRoleMenu(sysTenant, role);
        return role.getRoleId();
    }

    private void createStationAdminRoleMenu(SysTenant sysTenant, SysRole role) {
        // 从系统配置读取场站管理员角色菜单ID，逗号分隔；取不到时用默认值
        String menuIds = Convert.toStr(redisService.getCacheObject(CacheConstants.SYS_CONFIG_KEY + "vpp.station.admin.menuIds"));
        if (StringUtils.isEmpty(menuIds)) {
            menuIds = "4000,4001,4002,4009,4011,4012,4013,4014,4091,4092,4093";
        }
        List<SysRoleMenu> roleMenuList = Arrays.stream(menuIds.split(","))
                .filter(StringUtils::isNotEmpty)
                .map(id -> {
                    SysRoleMenu entity = new SysRoleMenu();
                    entity.setRoleId(role.getRoleId());
                    entity.setMenuId(Convert.toLong(id.trim()));
                    entity.setTenantId(sysTenant.getId());
                    return entity;
                }).collect(Collectors.toList());
        sysRoleMenuMapper.batchRoleMenu(roleMenuList);
    }

    private Long createPost(String username) {
        SysPost post = new SysPost();
        post.setPostCode("yys").setPostName("运营商").setPostSort("1");
        post.setCreateBy(username);
        sysPostMapper.insert(post);
        return post.getPostId();
    }

    private Long createDept(SysTenant sysTenant) {
        // 创建部门
        SysDept dept = new SysDept();
        dept.setParentId(0L).setAncestors("0").setDeptName(sysTenant.getTenantName()).setOrderNum(0)
                .setLeader(sysTenant.getTenantName()+"管理员").setPhone(sysTenant.getUserPhone()).setEmail(sysTenant.getUserEmail());
        deptMapper.insert(dept);
        return dept.getDeptId();
    }


    /**
     * 修改租户管理
     *
     * @param sysTenant 租户管理
     * @return 结果
     */
    @Override
    public int updateSysTenant(SysTenant sysTenant)
    {
        //判断最新的租户套餐是否改变 重新授权 租户二级管理员账号需重新分配三级账号权限
        SysTenant t_sysTenant = sysTenantMapper.selectById(sysTenant.getId());
        if(sysTenant.getTenantPackage() != null && !sysTenant.getTenantPackage().equals(t_sysTenant.getTenantPackage()))
        {
            List<SysRole> roleList = sysRoleMapper.queryAdminRole(sysTenant.getId());
            SysRole t_role = roleList.get(0);//正常逻辑下每个租户只有一个二级管理员账号
            if(t_role != null)
            {
                //删除原租户下所有的角色-菜单信息
                sysRoleMenuMapper.deleteRoleMenuByTenantId(sysTenant.getId());
                //新增默认角色-菜单信息
                TenantUtils.execute(sysTenant.getId(), () -> {
                    createRoleMenu(sysTenant,t_role);
                });
                // TODO  此处需要优化,如果当前同时在线人数较多,会出现卡顿
                //原登租户录账号退出重登 租户二级管理员账号需重新分配三级账号权限
                Collection<String> keys = redisService.keys(CacheConstants.LOGIN_TOKEN_KEY + "*");
                for (String key : keys)
                {
                    LoginUser onlineUser = redisService.getCacheObject(key);
                    if(onlineUser.getSysUser().getTenantId() != null && onlineUser.getSysUser().getTenantId().equals(sysTenant.getId()))
                    {
                        redisService.deleteObject(key);
                    }
                }
            }
        }
        return sysTenantMapper.updateById(sysTenant);
    }

    /**
     * 批量删除租户管理
     *
     * @param ids 需要删除的租户管理主键
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteSysTenantByIds(Long[] ids)
    {
        //优化删除逻辑
        //1.先删租户
        int tenantres = sysTenantMapper.deleteSysTenantByIds(ids);
        if(tenantres>0){
            //下面才会进行子模块数据的删除
            //部门模块
            deptMapper.deleteDeptByTenantId(ids);
            //职位模块
            sysPostMapper.deletePostByTenantId(ids);
            //权限
            sysRoleMapper.deleteRoleByTenantId(ids);
            sysRoleMenuMapper.deleteRoleMenuByTenantIds(ids);
            sysRoleDeptMapper.deleteRoleDeptByTenantId(ids);
            //账号
            userMapper.deleteUserByTenantId(ids);
            userRoleMapper.deleteUserRoleByTenantId(ids);
            userPostMapper.deleteUserPostByTenantId(ids);
            return 1;
        }else {
            throw new ServiceException("当前租户已被删除不存在！");
        }
    }

    /**
     * 删除租户管理信息
     *
     * @param id 租户管理主键
     * @return 结果
     */
    @Override
    public int deleteSysTenantById(Long id)
    {
        return sysTenantMapper.deleteById(id);
    }
}
