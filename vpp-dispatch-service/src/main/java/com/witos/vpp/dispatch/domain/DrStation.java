package com.witos.vpp.dispatch.domain;

import java.math.BigDecimal;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.witos.common.core.web.domain.TenantEntity;
import lombok.Data;

@Data
@TableName("dr_station")
public class DrStation extends TenantEntity {
    private static final long serialVersionUID = 1L;

    @TableId
    private Long stationId;
    private String stationName;
    private String address;
    private BigDecimal capacity;
    private Long userId;
    private String contact;
    private String phone;
    private String status;

    /** 运营商分成比例（运营商在运营商+场站里的占比，NULL=默认0.01，即运营商:场站=1:99） */
    private BigDecimal operatorRate;
}
