package com.zs.ytbx.service;

import com.zs.ytbx.common.api.PageResponse;
import com.zs.ytbx.dto.*;
import com.zs.ytbx.vo.anxinxuan.*;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface AdminService {
    PageResponse<UserVO> listUsers(UserQuery query);
    UserVO getUser(Long userId);
    void createUser(RegisterRequest request);
    void updateUser(UpdateUserRequest request);
    void deleteUser(Long userId);
    void enableUser(Long userId);
    void disableUser(Long userId);

    PageResponse<ProductVO> listProducts(ProductQuery query);
    ProductDetailVO getProduct(Long productId);
    void createProduct(ProductRequest request);
    void updateProduct(ProductRequest request);
    void deleteProduct(Long productId);
    void onSale(Long productId);
    void offSale(Long productId);

    PageResponse<ExpenseVO> listAllExpenses(ExpenseQuery query);
    PageResponse<InsuranceVO> listAllInsurances(InsuranceQuery query);
    void approveInsurance(Long insuranceId, InsuranceApproveRequest request, Long reviewerId, String reviewerName);
    void rejectInsurance(Long insuranceId, InsuranceRejectRequest request, Long reviewerId, String reviewerName);
    void startUnderwriting(Long insuranceId);
    void activateInsurance(Long insuranceId, ActivateInsuranceRequest request);
    void activateInsurances(List<BatchActivateInsuranceRequest.Item> items);
    PageResponse<RechargeVO> listAllRecharges(RechargeQuery query);
    void rechargeUser(Long userId, java.math.BigDecimal amount, String method, String remark);

    Long getTodayNewOrders();
    Long getPendingOrders();
    Long getMonthOrders();
    List<ProductSalesAnalysisVO> getProductSalesRanking(java.time.LocalDate startDate, java.time.LocalDate endDate);
    List<OrderTrendAnalysisVO> getOrderTrendAnalysis(java.time.LocalDate startDate, java.time.LocalDate endDate, String periodType);

    byte[] downloadProductImportTemplate();
    int importProducts(MultipartFile file);
}
