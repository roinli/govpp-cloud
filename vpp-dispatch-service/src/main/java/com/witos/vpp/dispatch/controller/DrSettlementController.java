package com.witos.vpp.dispatch.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletResponse;

import com.witos.vpp.dispatch.domain.DrSettlement;
import com.witos.vpp.dispatch.service.IDrSettlementService;
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
 * VPP结算分账Controller
 *
 * @author witos
 * @date 2026-08-02
 */
@RestController
@RequestMapping("/settlement")
public class DrSettlementController extends BaseController
{
    @Autowired
    private IDrSettlementService drSettlementService;

    /**
     * 查询VPP结算分账列表
     */
    @RequiresPermissions("vpp:settlement:list")
    @GetMapping("/list")
    public AjaxResult list(DrSettlement settlement)
    {
        IPage<DrSettlement> list = drSettlementService.selectDrSettlementPage(settlement);
        return AjaxResult.success(list);
    }

    /**
     * 查询VPP结算分账列表（不分页）
     */
//    @RequiresPermissions("vpp:settlement:list")
    @GetMapping("/listAll")
    public AjaxResult listAll(DrSettlement settlement)
    {
        List<DrSettlement> list = drSettlementService.selectDrSettlementList(settlement);
        return AjaxResult.success(list);
    }

    /**
     * 获取VPP结算分账详细信息
     */
    @RequiresPermissions("vpp:settlement:list")
    @GetMapping(value = "/{settlementId}")
    public AjaxResult getInfo(@PathVariable("settlementId") Long settlementId)
    {
        return AjaxResult.success(drSettlementService.selectDrSettlementBySettlementId(settlementId));
    }

    /**
     * 导出结算单
     */
    @RequiresPermissions("vpp:settlement:list")
    @PostMapping("/export")
    public void export(HttpServletResponse response, DrSettlement settlement)
    {
        List<DrSettlement> list = drSettlementService.selectDrSettlementList(settlement);
        com.witos.common.core.utils.poi.ExcelUtil<DrSettlement> util = new com.witos.common.core.utils.poi.ExcelUtil<>(DrSettlement.class);
        util.exportExcel(response, list, "结算单");
    }

    /**
     * 新增VPP结算分账
     */
    @RequiresPermissions("vpp:settlement:add")
    @Log(title = "VPP结算分账", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody DrSettlement settlement)
    {
        return toAjax(drSettlementService.insertDrSettlement(settlement));
    }

    /**
     * 修改VPP结算分账
     */
    @RequiresPermissions("vpp:settlement:edit")
    @Log(title = "VPP结算分账", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody DrSettlement settlement)
    {
        return toAjax(drSettlementService.updateDrSettlement(settlement));
    }

    /**
     * 删除VPP结算分账
     */
    @RequiresPermissions("vpp:settlement:remove")
    @Log(title = "VPP结算分账", businessType = BusinessType.DELETE)
    @DeleteMapping("/{settlementIds}")
    public AjaxResult remove(@PathVariable("settlementIds") Long[] settlementIds)
    {
        return toAjax(drSettlementService.deleteDrSettlementBySettlementIds(settlementIds));
    }
}
