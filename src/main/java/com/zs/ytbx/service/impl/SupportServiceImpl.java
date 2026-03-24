package com.zs.ytbx.service.impl;

import com.zs.ytbx.common.api.PageResponse;
import com.zs.ytbx.common.enums.ResultCode;
import com.zs.ytbx.common.exception.BusinessException;
import com.zs.ytbx.dto.CreateClaimRequest;
import com.zs.ytbx.dto.CreateConsultOrderRequest;
import com.zs.ytbx.service.SupportService;
import com.zs.ytbx.vo.advisor.AdvisorDetailVO;
import com.zs.ytbx.vo.claim.ClaimCaseVO;
import com.zs.ytbx.vo.service.ConsultOrderVO;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@ConditionalOnProperty(name = "ytbx.mock.enabled", havingValue = "true", matchIfMissing = true)
public class SupportServiceImpl implements SupportService {

    private static final AdvisorDetailVO ADVISOR = AdvisorDetailVO.builder()
            .advisorId(501L)
            .advisorName("王若琳")
            .employeeNo("PA-8823910")
            .titleName("Senior Advisor")
            .avatarUrl("/mock/advisor-501.png")
            .profileSummary("根据家庭资产变化提供保险与信托组合建议。")
            .mobile("13800000000")
            .status("AVAILABLE")
            .build();

    private static final List<ClaimCaseVO> CLAIMS = List.of(
            ClaimCaseVO.builder().claimId(7001L).claimNo("PA20241020").policyId(1002L).policyNo("PA-CAR-991200344").productName("机动车辆综合商业保险").claimStatus("ASSESSING").incidentTime("2024-10-20 08:30:00").reportTime("2024-10-20 09:10:00").progressDesc("已进入损失核定阶段，预计2个工作日内完成。").build(),
            ClaimCaseVO.builder().claimId(7002L).claimNo("PA20241101").policyId(1001L).policyNo("PA-202308912234").productName("平安尊享e生百万医疗险").claimStatus("REVIEWING").incidentTime("2024-11-01 10:00:00").reportTime("2024-11-01 11:15:00").progressDesc("资料已提交，系统正在审核。").build()
    );

    @Override
    public AdvisorDetailVO getAdvisor(Long advisorId) {
        if (!ADVISOR.getAdvisorId().equals(advisorId)) {
            throw new BusinessException(ResultCode.ADVISOR_NOT_AVAILABLE);
        }
        return ADVISOR;
    }

    @Override
    public PageResponse<ClaimCaseVO> listClaims(Integer pageNo, Integer pageSize, String claimStatus) {
        List<ClaimCaseVO> filtered = CLAIMS.stream()
                .filter(item -> !StringUtils.hasText(claimStatus) || item.getClaimStatus().equals(claimStatus))
                .toList();
        return PageResponse.of(filtered, pageNo, pageSize);
    }

    @Override
    public ClaimCaseVO getClaim(Long claimId) {
        return CLAIMS.stream()
                .filter(item -> item.getClaimId().equals(claimId))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ResultCode.CLAIM_NOT_FOUND));
    }

    @Override
    public ClaimCaseVO createClaim(CreateClaimRequest request) {
        return ClaimCaseVO.builder()
                .claimId(7999L)
                .claimNo("PA202603190001")
                .policyId(request.getPolicyId())
                .policyNo("PA-MOCK-" + request.getPolicyId())
                .productName("新建报案单")
                .claimStatus("REPORTED")
                .incidentTime(request.getIncidentTime())
                .reportTime("2026-03-19 10:00:00")
                .progressDesc("报案成功，等待审核。")
                .build();
    }

    @Override
    public ConsultOrderVO createConsultOrder(CreateConsultOrderRequest request) {
        if (!ADVISOR.getAdvisorId().equals(request.getAdvisorId())) {
            throw new BusinessException(ResultCode.ADVISOR_NOT_AVAILABLE);
        }
        return ConsultOrderVO.builder()
                .consultOrderId(9001L)
                .consultOrderNo("CO202603190001")
                .status("CREATED")
                .build();
    }
}
