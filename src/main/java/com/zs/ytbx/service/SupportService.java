package com.zs.ytbx.service;

import com.zs.ytbx.common.api.PageResponse;
import com.zs.ytbx.dto.CreateClaimRequest;
import com.zs.ytbx.dto.CreateConsultOrderRequest;
import com.zs.ytbx.vo.advisor.AdvisorDetailVO;
import com.zs.ytbx.vo.claim.ClaimCaseVO;
import com.zs.ytbx.vo.service.ConsultOrderVO;

public interface SupportService {

    AdvisorDetailVO getAdvisor(Long advisorId);

    PageResponse<ClaimCaseVO> listClaims(Integer pageNo, Integer pageSize, String claimStatus);

    ClaimCaseVO getClaim(Long claimId);

    ClaimCaseVO createClaim(CreateClaimRequest request);

    ConsultOrderVO createConsultOrder(CreateConsultOrderRequest request);
}
