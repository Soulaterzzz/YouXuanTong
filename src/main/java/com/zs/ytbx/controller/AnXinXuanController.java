package com.zs.ytbx.controller;

import com.zs.ytbx.common.api.ApiResponse;
import com.zs.ytbx.common.api.PageResponse;
import com.zs.ytbx.common.auth.AuthContext;
import com.zs.ytbx.dto.*;
import com.zs.ytbx.service.AnXinXuanService;
import com.zs.ytbx.service.impl.AnXinXuanServiceImpl;
import com.zs.ytbx.vo.anxinxuan.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/anxinxuan")
public class AnXinXuanController {

    private final AnXinXuanService anxinXuanService;
    private final AnXinXuanServiceImpl anXinXuanServiceImpl;
    private final AuthContext authContext;

    private Long getCurrentUserId() {
        return authContext.requireCurrentUser().getUserId();
    }

    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> getStats() {
        Long userId = getCurrentUserId();
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalProducts", anXinXuanServiceImpl.getTotalProducts());
        stats.put("totalPolicies", anXinXuanServiceImpl.getTotalPolicies(userId));
        stats.put("totalExpenses", anXinXuanServiceImpl.getTotalExpenses(userId));
        stats.put("balance", anXinXuanServiceImpl.getBalance(userId));
        return ApiResponse.success(stats);
    }

    @GetMapping("/balance")
    public ApiResponse<BigDecimal> getBalance() {
        Long userId = getCurrentUserId();
        return ApiResponse.success(anXinXuanServiceImpl.getBalance(userId));
    }

    @GetMapping("/products")
    public ApiResponse<PageResponse<ProductVO>> listProducts(@Valid @ModelAttribute ProductQuery query) {
        return ApiResponse.success(anxinXuanService.listProducts(query));
    }

    @GetMapping("/products/{productId}")
    public ApiResponse<ProductDetailVO> getProductDetail(@PathVariable("productId") Long productId) {
        return ApiResponse.success(anxinXuanService.getProductDetail(productId));
    }

    @PostMapping("/products/activate")
    public ApiResponse<Void> activateProduct(@Valid @RequestBody ActivateRequest request) {
        Long userId = getCurrentUserId();
        anxinXuanService.activateProduct(request, userId);
        return ApiResponse.success(null);
    }

    @PostMapping("/products/save-draft")
    public ApiResponse<Void> saveDraft(@Valid @RequestBody ActivateRequest request) {
        Long userId = getCurrentUserId();
        anxinXuanService.saveDraft(request, userId);
        return ApiResponse.success(null);
    }

    @PutMapping("/insurances/{insuranceId}/submit")
    public ApiResponse<Void> submitDraft(@PathVariable("insuranceId") Long insuranceId) {
        Long userId = getCurrentUserId();
        anxinXuanService.submitDraft(insuranceId, userId);
        return ApiResponse.success(null);
    }

    @GetMapping("/expenses")
    public ApiResponse<PageResponse<ExpenseVO>> listExpenses(@Valid @ModelAttribute ExpenseQuery query) {
        Long userId = getCurrentUserId();
        return ApiResponse.success(anxinXuanService.listExpenses(query, userId));
    }

    @GetMapping("/insurances")
    public ApiResponse<PageResponse<InsuranceVO>> listInsurances(@Valid @ModelAttribute InsuranceQuery query) {
        Long userId = getCurrentUserId();
        return ApiResponse.success(anxinXuanService.listInsurances(query, userId));
    }

    @PostMapping("/insurances/export")
    public ApiResponse<Void> exportInsurances(@Valid @RequestBody ExportRequest request) {
        anxinXuanService.exportInsurances(request);
        return ApiResponse.success(null);
    }

    @GetMapping("/recharges")
    public ApiResponse<PageResponse<RechargeVO>> listRecharges(@Valid @ModelAttribute RechargeQuery query) {
        Long userId = getCurrentUserId();
        return ApiResponse.success(anxinXuanService.listRecharges(query, userId));
    }

    @PostMapping("/recharges")
    public ApiResponse<Void> createRecharge(@Valid @RequestBody CreateRechargeRequest request) {
        Long userId = getCurrentUserId();
        anxinXuanService.createRecharge(request, userId);
        return ApiResponse.success(null);
    }
}
