package com.witos.vpp.dispatch.service;

import java.util.List;
import com.witos.vpp.dispatch.domain.DrAssess;

/**
 * VPP效果评估Service接口
 *
 * @author witos
 * @date 2026-08-02
 */
public interface IDrAssessService
{
    List<DrAssess> selectDrAssessList(DrAssess assess);
}
