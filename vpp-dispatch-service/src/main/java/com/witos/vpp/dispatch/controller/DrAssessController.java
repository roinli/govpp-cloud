package com.witos.vpp.dispatch.controller;

import java.util.List;

import com.witos.vpp.dispatch.domain.DrAssess;
import com.witos.vpp.dispatch.service.IDrAssessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.witos.common.security.annotation.RequiresPermissions;
import com.witos.common.core.web.controller.BaseController;
import com.witos.common.core.web.domain.AjaxResult;

/**
 * VPP效果评估Controller
 *
 * @author witos
 * @date 2026-08-02
 */
@RestController
@RequestMapping("/assess")
public class DrAssessController extends BaseController
{
    @Autowired
    private IDrAssessService drAssessService;

    /**
     * 查询VPP效果评估列表（不分页）
     */
//    @RequiresPermissions("vpp:assess:list")
    @GetMapping("/listAll")
    public AjaxResult listAll(DrAssess assess)
    {
        List<DrAssess> list = drAssessService.selectDrAssessList(assess);
        return AjaxResult.success(list);
    }
}
