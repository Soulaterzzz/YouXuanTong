package com.zs.ytbx.service;

import com.zs.ytbx.common.api.PageResponse;
import com.zs.ytbx.dto.*;
import com.zs.ytbx.vo.anxinxuan.*;
import org.springframework.web.multipart.MultipartFile;
import java.math.BigDecimal;
import java.util.List;

public interface AnXinXuanService {

    PageResponse<ProductVO> listProducts(ProductQuery query);
    ProductDetailVO getProductDetail(Long productId);
    void activateProduct(ActivateRequest request, Long userId);
    void saveDraft(ActivateRequest request, Long userId);
    void submitDraft(Long insuranceId, Long userId);

    PageResponse<ExpenseVO> listExpenses(ExpenseQuery query, Long userId);
    byte[] exportExpenses(ExpenseQuery query);

    PageResponse<InsuranceVO> listInsurances(InsuranceQuery query, Long userId);
    byte[] exportInsurances(InsuranceQuery query);
    byte[] exportInsurancePdf(Long insuranceId);
    List<ProductInsurancePreviewVO> previewProductInsurances(Long productId, MultipartFile file);
    int importProductInsurances(Long productId, MultipartFile file);

    PageResponse<RechargeVO> listRecharges(RechargeQuery query, Long userId);
    void createRecharge(CreateRechargeRequest request, Long userId);

    void updateDisplayPrice(Long insuranceId, BigDecimal displayPrice, Long userId);
}
