package com.witos.vpp.event.service;

import java.util.List;
import com.witos.vpp.event.domain.DrApply;
import com.baomidou.mybatisplus.core.metadata.IPage;

/**
 * 需求响应申报Service接口
 *
 * @author witos
 * @date 2026-08-02
 */
public interface IDrApplyService
{
    DrApply selectDrApplyByApplyId(Long applyId);

    IPage<DrApply> selectDrApplyPage(DrApply apply);

    List<DrApply> selectDrApplyList(DrApply apply);

    int insertDrApply(DrApply apply);

    int updateDrApply(DrApply apply);

    int deleteDrApplyByApplyIds(Long[] applyIds);

    /** 确认申报（0→1） */
    int confirmApply(Long[] applyIds);
}
