package com.witos.vpp.dispatch.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import com.witos.vpp.dispatch.domain.DrSettlement;
import com.baomidou.mybatisplus.core.metadata.IPage;

/**
 * VPP结算分账Service接口
 *
 * @author witos
 * @date 2026-08-02
 */
public interface IDrSettlementService
{
    DrSettlement selectDrSettlementBySettlementId(Long settlementId);

    IPage<DrSettlement> selectDrSettlementPage(DrSettlement settlement);

    List<DrSettlement> selectDrSettlementList(DrSettlement settlement);

    int insertDrSettlement(DrSettlement settlement);

    int updateDrSettlement(DrSettlement settlement);

    int deleteDrSettlementBySettlementIds(Long[] settlementIds);
}
