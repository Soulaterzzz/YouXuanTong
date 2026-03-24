package com.zs.ytbx.service;

import com.zs.ytbx.common.api.PageResponse;
import com.zs.ytbx.dto.*;
import com.zs.ytbx.vo.anxinxuan.*;

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
    PageResponse<RechargeVO> listAllRecharges(RechargeQuery query);
    
    // 统计方法
    Long getTodayNewOrders();
    Long getPendingOrders();
    Long getMonthOrders();
}
