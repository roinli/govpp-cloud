package com.witos.vpp.event.controller;

import java.util.List;
import java.util.Map;

import com.witos.common.core.web.controller.BaseController;
import com.witos.common.core.web.domain.AjaxResult;
import com.witos.common.log.annotation.Log;
import com.witos.common.log.enums.BusinessType;
import com.witos.common.security.annotation.RequiresPermissions;
import com.witos.common.security.utils.SecurityUtils;
import com.witos.system.api.domain.SysUser;
import com.witos.vpp.event.domain.DrStation;
import com.witos.vpp.event.service.IDrStationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.baomidou.mybatisplus.core.metadata.IPage;

/**
 * VPP 场站管理操作处理
 *
 * @author witos
 */
@RestController
@RequestMapping("/station")
public class DrStationController extends BaseController
{
    @Autowired
    private IDrStationService drStationService;

    /**
     * 查询场站列表（分页）
     */
    @RequiresPermissions("vpp:station:list")
    @GetMapping("/list")
    public AjaxResult list(DrStation station)
    {
        IPage<DrStation> list = drStationService.selectDrStationPage(station);
        return AjaxResult.success(list);
    }

    /**
     * 查询全部场站（下拉框用，当前租户的）
     */
    @RequiresPermissions("vpp:station:list")
    @GetMapping("/all")
    public AjaxResult all(DrStation station)
    {
        List<DrStation> list = drStationService.selectDrStationList(station);
        return AjaxResult.success(list);
    }

    /**
     * 场站统计
     */
    @RequiresPermissions("vpp:station:list")
    @GetMapping("/stats")
    public AjaxResult stats()
    {
        Map<String, Object> stats = drStationService.getStationStats(drStationService.getMyStationIds());
        return AjaxResult.success(stats);
    }

    /**
     * 获取场站详细信息
     */
    @RequiresPermissions("vpp:station:list")
    @GetMapping(value = "/{stationId}")
    public AjaxResult getInfo(@PathVariable("stationId") Long stationId)
    {
        return AjaxResult.success(drStationService.selectDrStationByStationId(stationId));
    }

    /**
     * 新增场站
     */
    @RequiresPermissions("vpp:station:add")
    @Log(title = "场站管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody DrStation station)
    {
        station.setCreateBy(SecurityUtils.getUsername());
        // 场站方用户自动绑定自己的 userId
        if (station.getUserId() == null) {
            try {
                SysUser sysUser = SecurityUtils.getLoginUser().getSysUser();
                if (sysUser != null && sysUser.getIsStation() != null && sysUser.getIsStation() == 1) {
                    station.setUserId(SecurityUtils.getUserId());
                }
            } catch (Exception ignored) {}
        }
        return toAjax(drStationService.insertDrStation(station));
    }

    /**
     * 修改场站
     */
    @RequiresPermissions("vpp:station:edit")
    @Log(title = "场站管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody DrStation station)
    {
        station.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(drStationService.updateDrStation(station));
    }

    /**
     * 删除场站
     */
    @RequiresPermissions("vpp:station:remove")
    @Log(title = "场站管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{stationIds}")
    public AjaxResult remove(@PathVariable("stationIds") Long[] stationIds)
    {
        return toAjax(drStationService.deleteDrStationByIds(stationIds));
    }
}
