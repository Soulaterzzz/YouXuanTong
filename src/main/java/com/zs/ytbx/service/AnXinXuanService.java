package com.zs.ytbx.service;

import com.zs.ytbx.common.api.PageResponse;
import com.zs.ytbx.dto.*;
import com.zs.ytbx.vo.anxinxuan.*;
import org.springframework.web.multipart.MultipartFile;

public interface AnXinXuanService {

    PageResponse<ProductVO> listProducts(ProductQuery query);
    ProductDetailVO getProductDetail(Long productId);
    void activateProduct(ActivateRequest request, Long userId);
    void saveDraft(ActivateRequest request, Long userId);
    void submitDraft(Long insuranceId, Long userId);

    PageResponse<ExpenseVO> listExpenses(ExpenseQuery query, Long userId);

    PageResponse<InsuranceVO> listInsurances(InsuranceQuery query, Long userId);
    void exportInsurances(ExportRequest request);

    PageResponse<RechargeVO> listRecharges(RechargeQuery query, Long userId);
    void createRecharge(CreateRechargeRequest request, Long userId);
    void updateProductPrice(UpdateProductPriceRequest request, Long userId);
    void batchImportProducts(Long productId, MultipartFile file, Long userId);
}
