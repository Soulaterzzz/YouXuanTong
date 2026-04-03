package com.zs.ytbx.controller;

import com.zs.ytbx.common.api.ApiResponse;
import com.zs.ytbx.common.api.PageResponse;
import com.zs.ytbx.common.auth.AuthContext;
import com.zs.ytbx.common.auth.SessionUser;
import com.zs.ytbx.common.enums.ResultCode;
import com.zs.ytbx.common.exception.BusinessException;
import com.zs.ytbx.dto.*;
import com.zs.ytbx.service.AdminService;
import com.zs.ytbx.service.NoticeService;
import com.zs.ytbx.vo.anxinxuan.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;
    private final NoticeService noticeService;
    private final AuthContext authContext;

    private SessionUser requireAdmin() {
        SessionUser user = authContext.requireCurrentUser();
        if (!"ADMIN".equals(user.getUserType())) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        return user;
    }

    @GetMapping("/check")
    public ApiResponse<Boolean> checkAdmin() {
        SessionUser user = authContext.getCurrentUser();
        boolean isAdmin = user != null && "ADMIN".equals(user.getUserType());
        return ApiResponse.success(isAdmin);
    }

    @GetMapping("/users")
    public ApiResponse<PageResponse<UserVO>> listUsers(@Valid @ModelAttribute UserQuery query) {
        requireAdmin();
        return ApiResponse.success(adminService.listUsers(query));
    }

    @GetMapping("/users/{userId}")
    public ApiResponse<UserVO> getUser(@PathVariable("userId") Long userId) {
        requireAdmin();
        return ApiResponse.success(adminService.getUser(userId));
    }

    @PostMapping("/users")
    public ApiResponse<Void> createUser(@Valid @RequestBody RegisterRequest request) {
        requireAdmin();
        adminService.createUser(request);
        return ApiResponse.success(null);
    }

    @PutMapping("/users")
    public ApiResponse<Void> updateUser(@Valid @RequestBody UpdateUserRequest request) {
        requireAdmin();
        adminService.updateUser(request);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/users/{userId}")
    public ApiResponse<Void> deleteUser(@PathVariable("userId") Long userId) {
        requireAdmin();
        adminService.deleteUser(userId);
        return ApiResponse.success(null);
    }

    @PutMapping("/users/{userId}/enable")
    public ApiResponse<Void> enableUser(@PathVariable("userId") Long userId) {
        requireAdmin();
        adminService.enableUser(userId);
        return ApiResponse.success(null);
    }

    @PutMapping("/users/{userId}/disable")
    public ApiResponse<Void> disableUser(@PathVariable("userId") Long userId) {
        requireAdmin();
        adminService.disableUser(userId);
        return ApiResponse.success(null);
    }

    @GetMapping("/products")
    public ApiResponse<PageResponse<ProductVO>> listProducts(@Valid @ModelAttribute ProductQuery query) {
        requireAdmin();
        return ApiResponse.success(adminService.listProducts(query));
    }

    @GetMapping("/products/{productId}")
    public ApiResponse<ProductDetailVO> getProduct(@PathVariable("productId") Long productId) {
        requireAdmin();
        return ApiResponse.success(adminService.getProduct(productId));
    }

    @PostMapping("/products")
    public ApiResponse<Void> createProduct(@Valid @RequestBody ProductRequest request) {
        requireAdmin();
        adminService.createProduct(request);
        return ApiResponse.success(null);
    }

    @PutMapping("/products")
    public ApiResponse<Void> updateProduct(@Valid @RequestBody ProductRequest request) {
        requireAdmin();
        adminService.updateProduct(request);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/products/{productId}")
    public ApiResponse<Void> deleteProduct(@PathVariable("productId") Long productId) {
        requireAdmin();
        adminService.deleteProduct(productId);
        return ApiResponse.success(null);
    }

    @PutMapping("/products/{productId}/on-sale")
    public ApiResponse<Void> onSale(@PathVariable("productId") Long productId) {
        requireAdmin();
        adminService.onSale(productId);
        return ApiResponse.success(null);
    }

    @PutMapping("/products/{productId}/off-sale")
    public ApiResponse<Void> offSale(@PathVariable("productId") Long productId) {
        requireAdmin();
        adminService.offSale(productId);
        return ApiResponse.success(null);
    }

    @GetMapping("/products/import-template")
    public ResponseEntity<byte[]> downloadProductImportTemplate() {
        requireAdmin();
        byte[] data = adminService.downloadProductImportTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentLength(data.length);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("product-import-template.xlsx", StandardCharsets.UTF_8)
                .build());
        return ResponseEntity.ok().headers(headers).body(data);
    }

    @PostMapping("/products/import")
    public ApiResponse<Map<String, Object>> importProducts(@RequestParam("file") MultipartFile file) {
        requireAdmin();
        int importedCount = adminService.importProducts(file);
        return ApiResponse.success(Map.of(
                "importedCount", importedCount,
                "message", "已导入 " + importedCount + " 条产品数据"
        ));
    }

    @GetMapping("/expenses")
    public ApiResponse<PageResponse<ExpenseVO>> listAllExpenses(@Valid @ModelAttribute ExpenseQuery query) {
        requireAdmin();
        return ApiResponse.success(adminService.listAllExpenses(query));
    }

    @GetMapping("/insurances")
    public ApiResponse<PageResponse<InsuranceVO>> listAllInsurances(@Valid @ModelAttribute InsuranceQuery query) {
        requireAdmin();
        return ApiResponse.success(adminService.listAllInsurances(query));
    }

    @PutMapping("/insurances/{insuranceId}/approve")
    public ApiResponse<Void> approveInsurance(@PathVariable("insuranceId") Long insuranceId,
                                              @Valid @RequestBody InsuranceApproveRequest request) {
        SessionUser admin = requireAdmin();
        adminService.approveInsurance(insuranceId, request, admin.getUserId(), admin.getUsername());
        return ApiResponse.success(null);
    }

    @PutMapping("/insurances/{insuranceId}/reject")
    public ApiResponse<Void> rejectInsurance(@PathVariable("insuranceId") Long insuranceId,
                                             @Valid @RequestBody InsuranceRejectRequest request) {
        SessionUser admin = requireAdmin();
        adminService.rejectInsurance(insuranceId, request, admin.getUserId(), admin.getUsername());
        return ApiResponse.success(null);
    }

    @PutMapping("/insurances/{insuranceId}/underwriting")
    public ApiResponse<Void> startUnderwriting(@PathVariable("insuranceId") Long insuranceId) {
        requireAdmin();
        adminService.startUnderwriting(insuranceId);
        return ApiResponse.success(null);
    }

    @PutMapping("/insurances/{insuranceId}/activate")
    public ApiResponse<Void> activateInsurance(@PathVariable("insuranceId") Long insuranceId,
                                               @Valid @RequestBody ActivateInsuranceRequest request) {
        requireAdmin();
        adminService.activateInsurance(insuranceId, request);
        return ApiResponse.success(null);
    }

    @PostMapping("/insurances/activate-batch")
    public ApiResponse<Void> activateInsurances(@Valid @RequestBody BatchActivateInsuranceRequest request) {
        requireAdmin();
        adminService.activateInsurances(request.getItems());
        return ApiResponse.success(null);
    }

    @GetMapping("/recharges")
    public ApiResponse<PageResponse<RechargeVO>> listAllRecharges(@Valid @ModelAttribute RechargeQuery query) {
        requireAdmin();
        return ApiResponse.success(adminService.listAllRecharges(query));
    }

    @PostMapping("/recharges")
    public ApiResponse<Void> rechargeUser(@Valid @RequestBody CreateRechargeRequest request) {
        SessionUser admin = requireAdmin();
        adminService.rechargeUser(request.getUserId(), request.getAmount(), request.getMethod(), request.getRemark());
        return ApiResponse.success(null);
    }

    @GetMapping("/stats")
    public ApiResponse<AdminStatsVO> getAdminStats(@ModelAttribute AdminStatsQuery query) {
        requireAdmin();
        AdminStatsVO stats = new AdminStatsVO();
        stats.setTodayNewOrders(adminService.getTodayNewOrders());
        stats.setPendingOrders(adminService.getPendingOrders());
        stats.setMonthOrders(adminService.getMonthOrders());
        stats.setProductSalesRanking(adminService.getProductSalesRanking(query.getStartDate(), query.getEndDate()));
        stats.setOrderTrendAnalysis(adminService.getOrderTrendAnalysis(query.getStartDate(), query.getEndDate(), query.getPeriodType()));
        return ApiResponse.success(stats);
    }

    @PostMapping("/notices/publish")
    public ApiResponse<Void> publishNotices(@Valid @RequestBody NoticePublishRequest request) {
        requireAdmin();
        noticeService.publishNotices(request);
        return ApiResponse.success(null);
    }
}
