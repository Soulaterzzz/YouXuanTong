package com.zs.ytbx.vo.anxinxuan;

import lombok.Data;

import java.util.List;

@Data
public class AdminStatsVO {
    private Long todayNewOrders;
    private Long pendingOrders;
    private Long monthOrders;
    private List<ProductSalesAnalysisVO> productSalesRanking;
    private List<OrderTrendAnalysisVO> orderTrendAnalysis;
}
