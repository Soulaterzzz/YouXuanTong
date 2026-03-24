package com.zs.ytbx.controller;

import com.zs.ytbx.common.api.ApiResponse;
import com.zs.ytbx.common.api.PageResponse;
import com.zs.ytbx.common.auth.SessionConstants;
import com.zs.ytbx.common.enums.ResultCode;
import com.zs.ytbx.common.exception.BusinessException;
import com.zs.ytbx.dto.*;
import com.zs.ytbx.service.AdminService;
import com.zs.ytbx.vo.anxinxuan.*;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    private void requireAdmin(HttpSession session) {
        Long userId = (Long) session.getAttribute(SessionConstants.SESSION_USER_ID);
        String userType = (String) session.getAttribute(SessionConstants.SESSION_USER_TYPE);
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        if (!"ADMIN".equals(userType)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
    }

    @GetMapping("/check")
    public ApiResponse<Boolean> checkAdmin(HttpSession session) {
        String userType = (String) session.getAttribute(SessionConstants.SESSION_USER_TYPE);
        boolean isAdmin = "ADMIN".equals(userType);
        return ApiResponse.success(isAdmin);
    }

    @GetMapping("/users")
    public ApiResponse<PageResponse<UserVO>> listUsers(@Valid @ModelAttribute UserQuery query, HttpSession session) {
        requireAdmin(session);
        return ApiResponse.success(adminService.listUsers(query));
    }

    @GetMapping("/users/{userId}")
    public ApiResponse<UserVO> getUser(@PathVariable Long userId, HttpSession session) {
        requireAdmin(session);
        return ApiResponse.success(adminService.getUser(userId));
    }

    @PostMapping("/users")
    public ApiResponse<Void> createUser(@Valid @RequestBody RegisterRequest request, HttpSession session) {
        requireAdmin(session);
        adminService.createUser(request);
        return ApiResponse.success(null);
    }

    @PutMapping("/users")
    public ApiResponse<Void> updateUser(@Valid @RequestBody UpdateUserRequest request, HttpSession session) {
        requireAdmin(session);
        adminService.updateUser(request);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/users/{userId}")
    public ApiResponse<Void> deleteUser(@PathVariable Long userId, HttpSession session) {
        requireAdmin(session);
        adminService.deleteUser(userId);
        return ApiResponse.success(null);
    }

    @PutMapping("/users/{userId}/enable")
    public ApiResponse<Void> enableUser(@PathVariable Long userId, HttpSession session) {
        requireAdmin(session);
        adminService.enableUser(userId);
        return ApiResponse.success(null);
    }

    @PutMapping("/users/{userId}/disable")
    public ApiResponse<Void> disableUser(@PathVariable Long userId, HttpSession session) {
        requireAdmin(session);
        adminService.disableUser(userId);
        return ApiResponse.success(null);
    }

    @GetMapping("/products")
    public ApiResponse<PageResponse<ProductVO>> listProducts(@Valid @ModelAttribute ProductQuery query, HttpSession session) {
        requireAdmin(session);
        return ApiResponse.success(adminService.listProducts(query));
    }

    @GetMapping("/products/{productId}")
    public ApiResponse<ProductDetailVO> getProduct(@PathVariable Long productId, HttpSession session) {
        requireAdmin(session);
        return ApiResponse.success(adminService.getProduct(productId));
    }

    @PostMapping("/products")
    public ApiResponse<Void> createProduct(@Valid @RequestBody ProductRequest request, HttpSession session) {
        requireAdmin(session);
        adminService.createProduct(request);
        return ApiResponse.success(null);
    }

    @PutMapping("/products")
    public ApiResponse<Void> updateProduct(@Valid @RequestBody ProductRequest request, HttpSession session) {
        requireAdmin(session);
        adminService.updateProduct(request);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/products/{productId}")
    public ApiResponse<Void> deleteProduct(@PathVariable Long productId, HttpSession session) {
        requireAdmin(session);
        adminService.deleteProduct(productId);
        return ApiResponse.success(null);
    }

    @PutMapping("/products/{productId}/on-sale")
    public ApiResponse<Void> onSale(@PathVariable Long productId, HttpSession session) {
        requireAdmin(session);
        adminService.onSale(productId);
        return ApiResponse.success(null);
    }

    @PutMapping("/products/{productId}/off-sale")
    public ApiResponse<Void> offSale(@PathVariable Long productId, HttpSession session) {
        requireAdmin(session);
        adminService.offSale(productId);
        return ApiResponse.success(null);
    }

    @GetMapping("/expenses")
    public ApiResponse<PageResponse<ExpenseVO>> listAllExpenses(@Valid @ModelAttribute ExpenseQuery query, HttpSession session) {
        requireAdmin(session);
        return ApiResponse.success(adminService.listAllExpenses(query));
    }

    @GetMapping("/insurances")
    public ApiResponse<PageResponse<InsuranceVO>> listAllInsurances(@Valid @ModelAttribute InsuranceQuery query, HttpSession session) {
        requireAdmin(session);
        return ApiResponse.success(adminService.listAllInsurances(query));
    }

    @GetMapping("/recharges")
    public ApiResponse<PageResponse<RechargeVO>> listAllRecharges(@Valid @ModelAttribute RechargeQuery query, HttpSession session) {
        requireAdmin(session);
        return ApiResponse.success(adminService.listAllRecharges(query));
    }

    @GetMapping("/stats")
    public ApiResponse<AdminStatsVO> getAdminStats(HttpSession session) {
        requireAdmin(session);
        AdminStatsVO stats = new AdminStatsVO();
        stats.setTodayNewOrders(adminService.getTodayNewOrders());
        stats.setPendingOrders(adminService.getPendingOrders());
        stats.setMonthOrders(adminService.getMonthOrders());
        return ApiResponse.success(stats);
    }
}
