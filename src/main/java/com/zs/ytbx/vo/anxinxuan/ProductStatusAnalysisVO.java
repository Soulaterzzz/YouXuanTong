package com.zs.ytbx.vo.anxinxuan;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ProductStatusAnalysisVO {
    private String productName;
    private Long totalOrderCount;
    private Long pendingReviewCount;
    private Long approvedCount;
    private Long underwritingCount;
    private Long activeCount;
    private Long rejectedCount;
    private BigDecimal successRate;
}
