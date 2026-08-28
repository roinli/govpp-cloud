package com.witos.vpp.dispatch.controller;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import com.witos.vpp.dispatch.domain.DrExecute;
import com.witos.vpp.dispatch.service.IDrExecuteService;
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
import com.witos.common.log.annotation.Log;
import com.witos.common.log.enums.BusinessType;
import com.witos.common.security.annotation.RequiresPermissions;
import com.witos.common.core.web.controller.BaseController;
import com.witos.common.core.web.domain.AjaxResult;
import com.baomidou.mybatisplus.core.metadata.IPage;

/**
 * VPP执行监测Controller
 *
 * @author witos
 * @date 2026-08-02
 */
@RestController
@RequestMapping("/execute")
public class DrExecuteController extends BaseController
{
    @Autowired
    private IDrExecuteService drExecuteService;

    /**
     * 查询VPP执行监测列表
     */
    @RequiresPermissions("vpp:execute:list")
    @GetMapping("/list")
    public AjaxResult list(DrExecute execute)
    {
        IPage<DrExecute> list = drExecuteService.selectDrExecutePage(execute);
        return AjaxResult.success(list);
    }

    /**
     * 偏差监测：目标 vs 实际曲线，偏差超阈值告警
     * 可选按场站/桩过滤，前端选桩后按需请求，避免全量传输
     */
    @RequiresPermissions("vpp:execute:list")
    @GetMapping("/deviation")
    public AjaxResult deviation(@RequestParam("eventId") Long eventId,
            @RequestParam(value = "thresholdPercent", required = false) BigDecimal thresholdPercent,
            @RequestParam(value = "stationId", required = false) Long stationId,
            @RequestParam(value = "pileNo", required = false) String pileNo)
    {
        List<Map<String, Object>> list = drExecuteService.selectDeviationList(eventId, thresholdPercent, stationId, pileNo);
        return AjaxResult.success(list);
    }

    /**
     * 事件的桩清单（轻量，供前端选桩后再按需拉偏差数据）
     */
    @RequiresPermissions("vpp:execute:list")
    @GetMapping("/piles/{eventId}")
    public AjaxResult piles(@PathVariable("eventId") Long eventId)
    {
        return AjaxResult.success(drExecuteService.selectPileOptions(eventId));
    }

    /**
     * 获取VPP执行监测详细信息
     */
    @RequiresPermissions("vpp:execute:list")
    @GetMapping(value = "/{executeId}")
    public AjaxResult getInfo(@PathVariable("executeId") Long executeId)
    {
        return AjaxResult.success(drExecuteService.selectDrExecuteByExecuteId(executeId));
    }

    /**
     * 手动触发：为指定事件模拟生成一次遥测数据
     */
    @RequiresPermissions("vpp:execute:list")
    @PostMapping("/simulate/{eventId}")
    public AjaxResult simulate(@PathVariable("eventId") Long eventId)
    {
        // 手动模拟上报：若当前正好是整秒(:00)，睡眠1秒再生成，避免与定时任务整分钟产生的数据落在同一秒被累加
        if (LocalTime.now().getSecond() == 0)
        {
            try { Thread.sleep(1000); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
        }
        int count = drExecuteService.generateSimulatedData(eventId);
        return count > 0 ? AjaxResult.success("已生成 " + count + " 条模拟遥测数据") : AjaxResult.warn("该事件暂无已下发的分配记录或不在响应中");
    }

    /**
     * 记录执行反馈（桩上报实际功率）
     */
    @RequiresPermissions("vpp:execute:add")
    @Log(title = "VPP执行监测", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody DrExecute execute)
    {
        return toAjax(drExecuteService.insertDrExecute(execute));
    }

    /**
     * 修改VPP执行监测
     */
    @RequiresPermissions("vpp:execute:edit")
    @Log(title = "VPP执行监测", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody DrExecute execute)
    {
        return toAjax(drExecuteService.updateDrExecute(execute));
    }

    /**
     * 删除VPP执行监测
     */
    @RequiresPermissions("vpp:execute:remove")
    @Log(title = "VPP执行监测", businessType = BusinessType.DELETE)
    @DeleteMapping("/{executeIds}")
    public AjaxResult remove(@PathVariable("executeIds") Long[] executeIds)
    {
        return toAjax(drExecuteService.deleteDrExecuteByExecuteIds(executeIds));
    }
}
