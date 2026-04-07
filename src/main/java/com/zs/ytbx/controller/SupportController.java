package com.zs.ytbx.controller;

import com.zs.ytbx.common.api.ApiResponse;
import com.zs.ytbx.common.api.PageResponse;
import com.zs.ytbx.dto.CreateClaimRequest;
import com.zs.ytbx.dto.CreateConsultOrderRequest;
import com.zs.ytbx.service.SupportService;
import com.zs.ytbx.vo.advisor.AdvisorDetailVO;
import com.zs.ytbx.vo.claim.ClaimCaseVO;
import com.zs.ytbx.vo.service.ConsultOrderVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class SupportController {

    private final SupportService supportService;

    @GetMapping("/advisors/{advisorId}")
    public ApiResponse<AdvisorDetailVO> getAdvisor(@PathVariable Long advisorId) {
        return ApiResponse.success(supportService.getAdvisor(advisorId));
    }

    @GetMapping("/claims")
    public ApiResponse<PageResponse<ClaimCaseVO>> listClaims(
            @RequestParam(defaultValue = "1") @Min(1) Integer pageNo,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) Integer pageSize,
            @RequestParam(required = false) String claimStatus) {
        return ApiResponse.success(supportService.listClaims(pageNo, pageSize, claimStatus));
    }

    @GetMapping("/claims/{claimId}")
    public ApiResponse<ClaimCaseVO> getClaim(@PathVariable Long claimId) {
        return ApiResponse.success(supportService.getClaim(claimId));
    }

    @PostMapping("/claims")
    public ApiResponse<ClaimCaseVO> createClaim(@Valid @RequestBody CreateClaimRequest request) {
        return ApiResponse.success(supportService.createClaim(request));
    }

    @PostMapping("/consult-orders")
    public ApiResponse<ConsultOrderVO> createConsultOrder(@Valid @RequestBody CreateConsultOrderRequest request) {
        return ApiResponse.success(supportService.createConsultOrder(request));
    }
}
