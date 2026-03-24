package com.zs.ytbx.controller;

import com.zs.ytbx.common.api.ApiResponse;
import com.zs.ytbx.common.api.PageResponse;
import com.zs.ytbx.query.PolicyQuery;
import com.zs.ytbx.service.PolicyService;
import com.zs.ytbx.vo.policy.PolicyDetailVO;
import com.zs.ytbx.vo.policy.PolicyDownloadVO;
import com.zs.ytbx.vo.policy.PolicyEventVO;
import com.zs.ytbx.vo.policy.PolicyListItemVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/policies")
public class PolicyController {

    private final PolicyService policyService;

    @GetMapping
    public ApiResponse<PageResponse<PolicyListItemVO>> listPolicies(@Valid @ModelAttribute PolicyQuery query) {
        return ApiResponse.success(policyService.listPolicies(query));
    }

    @GetMapping("/{policyId}")
    public ApiResponse<PolicyDetailVO> getPolicyDetail(@PathVariable Long policyId) {
        return ApiResponse.success(policyService.getPolicyDetail(policyId));
    }

    @GetMapping("/{policyId}/e-policy")
    public ApiResponse<PolicyDownloadVO> getEPolicy(@PathVariable Long policyId) {
        return ApiResponse.success(policyService.getEPolicy(policyId));
    }

    @GetMapping("/{policyId}/events")
    public ApiResponse<List<PolicyEventVO>> getEvents(@PathVariable Long policyId) {
        return ApiResponse.success(policyService.getEvents(policyId));
    }
}
