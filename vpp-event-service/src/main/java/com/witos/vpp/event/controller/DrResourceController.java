package com.witos.vpp.event.controller;

import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletResponse;

import com.witos.common.core.utils.poi.ExcelUtil;
import com.witos.common.core.web.controller.BaseController;
import com.witos.common.core.web.domain.AjaxResult;
import com.witos.common.log.annotation.Log;
import com.witos.common.log.enums.BusinessType;
import com.witos.common.security.annotation.RequiresPermissions;
import com.witos.common.security.utils.SecurityUtils;
import com.witos.vpp.event.domain.DrResource;
import com.witos.vpp.event.service.IDrResourceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.baomidou.mybatisplus.core.metadata.IPage;

/**
 * VPP 资源台账操作处理
 *
 * @author witos
 */
@RestController
@RequestMapping("/resource")
public class DrResourceController extends BaseController
{
    @Autowired
    private IDrResourceService drResourceService;

    /**
     * 查询资源列表
     */
    @RequiresPermissions("vpp:resource:list")
    @GetMapping("/list")
    public AjaxResult list(DrResource resource)
    {
        IPage<DrResource> list = drResourceService.selectDrResourcePage(resource);
        return AjaxResult.success(list);
    }
    /**
     * 场站列表（含可调容量汇总，用于事件申报选择场站）
     */
    @RequiresPermissions("vpp:resource:list")
    @GetMapping("/station-list")
    public AjaxResult stationList()
    {
        List<Map<String, Object>> list = drResourceService.getStationCapacityList();
        return AjaxResult.success(list);
    }

    /**
     * 资源统计
     */
    @RequiresPermissions("vpp:resource:list")
    @GetMapping("/stats")
    public AjaxResult stats()
    {
        Map<String, Object> stats = drResourceService.getResourceStats(null);
        return AjaxResult.success(stats);
    }

    /**
     * 场站可调TOP排行（type：peak=削峰 valley=填谷，limit 默认5）
     */
    @RequiresPermissions("vpp:resource:list")
    @GetMapping("/station-rank")
    public AjaxResult stationRank(@RequestParam(value = "type", defaultValue = "peak") String type,
            @RequestParam(value = "limit", defaultValue = "5") int limit)
    {
        return AjaxResult.success(drResourceService.getStationRank(type, limit));
    }

    /**
     * 事件参与占比饼图（平台看运营商 / 运营商看场站 / 场站看自己）
     */
    @RequiresPermissions("vpp:resource:list")
    @GetMapping("/operator-event-pie")
    public AjaxResult operatorEventPie()
    {
        return AjaxResult.success(drResourceService.getOperatorEventPie());
    }

    /**
     * 资源7日功率趋势（卡片折线图，充电负荷每日均值口径；数据存放在 dr_resource_power_daily，
     * 由定时任务模拟写入，同一资源同一天的值固定）
     */
    @RequiresPermissions("vpp:resource:list")
    @GetMapping("/power7d")
    public AjaxResult power7d(@RequestParam("resourceIds") Long[] resourceIds)
    {
        return AjaxResult.success(drResourceService.getPower7dTrend(resourceIds));
    }

    /**
     * 总负荷曲线（资源统计页；充电负荷实时口径，分钟级数据按15分钟聚合，
     * 由分钟级模拟任务 dr_resource_power_minute 写入；ownerType：operator=运营商桩 private=私有桩）
     */
    @RequiresPermissions("vpp:resource:list")
    @GetMapping("/load-curve")
    public AjaxResult loadCurve(@RequestParam(value = "date", required = false) String date,
            @RequestParam(value = "ownerType", required = false) String ownerType)
    {
        return AjaxResult.success(drResourceService.getLoadCurve(date, ownerType));
    }


    /**
     * 导出资源列表
     */
    @RequiresPermissions("vpp:resource:list")
    @Log(title = "资源台账", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, DrResource resource)
    {
        List<DrResource> list = drResourceService.selectDrResourceList(resource);
        ExcelUtil<DrResource> util = new ExcelUtil<DrResource>(DrResource.class);
        util.exportExcel(response, list, "资源台账");
    }

    /**
     * 获取资源详细信息
     */
    @RequiresPermissions("vpp:resource:list")
    @GetMapping(value = "/{resourceId}")
    public AjaxResult getInfo(@PathVariable("resourceId") Long resourceId)
    {
        return AjaxResult.success(drResourceService.selectDrResourceByResourceId(resourceId));
    }

    /**
     * 新增资源
     */
    @RequiresPermissions("vpp:resource:add")
    @Log(title = "资源台账", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody DrResource resource)
    {
        resource.setCreateBy(SecurityUtils.getUsername());
        return toAjax(drResourceService.insertDrResource(resource));
    }

    /**
     * 修改资源
     */
    @RequiresPermissions("vpp:resource:edit")
    @Log(title = "资源台账", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody DrResource resource)
    {
        resource.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(drResourceService.updateDrResource(resource));
    }

    /**
     * 批量设置参与状态（参与/停用）
     */
    @RequiresPermissions("vpp:resource:edit")
    @Log(title = "资源台账", businessType = BusinessType.UPDATE)
    @PutMapping("/batchStatus")
    public AjaxResult batchStatus(@RequestBody Map<String, Object> params)
    {
        @SuppressWarnings("unchecked")
        List<Object> ids = (List<Object>) params.get("resourceIds");
        String participateStatus = String.valueOf(params.get("participateStatus"));
        if (ids == null || ids.isEmpty())
        {
            return error("请选择要操作的资源");
        }
        Long[] resourceIds = ids.stream().map(id -> Long.valueOf(String.valueOf(id))).toArray(Long[]::new);
        return toAjax(drResourceService.updateParticipateStatus(resourceIds, participateStatus));
    }

    /**
     * 删除资源
     */
    @RequiresPermissions("vpp:resource:remove")
    @Log(title = "资源台账", businessType = BusinessType.DELETE)
    @DeleteMapping("/{resourceIds}")
    public AjaxResult remove(@PathVariable("resourceIds") Long[] resourceIds)
    {
        return toAjax(drResourceService.deleteDrResourceByIds(resourceIds));
    }
}
