package com.zs.ytbx.vo.anxinxuan;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ProductSalesAnalysisVO {
    private Integer rankNo;
    private String productName;
    private Long orderCount;
    private Integer salesQuantity;
    private BigDecimal salesAmount;
    private Long activePolicyCount;
}
