package com.zs.ytbx.service;

import com.zs.ytbx.common.api.PageResponse;
import com.zs.ytbx.dto.*;
import com.zs.ytbx.vo.anxinxuan.*;

public interface AnXinXuanService {

    PageResponse<ProductVO> listProducts(ProductQuery query);
    ProductDetailVO getProductDetail(Long productId);
    void activateProduct(ActivateRequest request, Long userId);

    PageResponse<ExpenseVO> listExpenses(ExpenseQuery query, Long userId);

    PageResponse<InsuranceVO> listInsurances(InsuranceQuery query, Long userId);
    void exportInsurances(ExportRequest request);

    PageResponse<RechargeVO> listRecharges(RechargeQuery query, Long userId);
    void createRecharge(CreateRechargeRequest request, Long userId);
}
