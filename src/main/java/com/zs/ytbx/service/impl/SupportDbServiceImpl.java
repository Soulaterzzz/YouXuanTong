package com.zs.ytbx.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zs.ytbx.common.auth.AuthContext;
import com.zs.ytbx.common.api.PageResponse;
import com.zs.ytbx.common.enums.ResultCode;
import com.zs.ytbx.common.exception.BusinessException;
import com.zs.ytbx.dto.CreateClaimRequest;
import com.zs.ytbx.dto.CreateConsultOrderRequest;
import com.zs.ytbx.entity.AdvisorEntity;
import com.zs.ytbx.entity.ClaimCaseEntity;
import com.zs.ytbx.entity.ConsultOrderEntity;
import com.zs.ytbx.entity.InsuranceProductEntity;
import com.zs.ytbx.entity.PolicyEntity;
import com.zs.ytbx.mapper.AdvisorMapper;
import com.zs.ytbx.mapper.ClaimCaseMapper;
import com.zs.ytbx.mapper.ConsultOrderMapper;
import com.zs.ytbx.mapper.InsuranceProductMapper;
import com.zs.ytbx.mapper.PolicyMapper;
import com.zs.ytbx.service.SupportService;
import com.zs.ytbx.vo.advisor.AdvisorDetailVO;
import com.zs.ytbx.vo.claim.ClaimCaseVO;
import com.zs.ytbx.vo.service.ConsultOrderVO;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ytbx.mock.enabled", havingValue = "false")
public class SupportDbServiceImpl implements SupportService {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_TIME_MINUTE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final AuthContext authContext;
    private final AdvisorMapper advisorMapper;
    private final ClaimCaseMapper claimCaseMapper;
    private final ConsultOrderMapper consultOrderMapper;
    private final PolicyMapper policyMapper;
    private final InsuranceProductMapper insuranceProductMapper;

    @Override
    public AdvisorDetailVO getAdvisor(Long advisorId) {
        AdvisorEntity advisor = advisorMapper.selectOne(Wrappers.<AdvisorEntity>lambdaQuery()
                .eq(AdvisorEntity::getId, advisorId)
                .eq(AdvisorEntity::getStatus, "AVAILABLE")
                .last("limit 1"));
        if (advisor == null) {
            throw new BusinessException(ResultCode.ADVISOR_NOT_AVAILABLE);
        }
        return AdvisorDetailVO.builder()
                .advisorId(advisor.getId())
                .advisorName(advisor.getAdvisorName())
                .employeeNo(advisor.getEmployeeNo())
                .titleName(advisor.getTitleName())
                .avatarUrl(advisor.getAvatarUrl())
                .profileSummary(advisor.getProfileSummary())
                .mobile(advisor.getMobile())
                .status(advisor.getStatus())
                .build();
    }

    @Override
    public PageResponse<ClaimCaseVO> listClaims(Integer pageNo, Integer pageSize, String claimStatus) {
        Long customerId = authContext.requireCurrentUser().getCustomerId();
        List<ClaimCaseEntity> claims = claimCaseMapper.selectList(Wrappers.<ClaimCaseEntity>lambdaQuery()
                .eq(ClaimCaseEntity::getCustomerId, customerId)
                .eq(StringUtils.hasText(claimStatus), ClaimCaseEntity::getClaimStatus, claimStatus)
                .orderByDesc(ClaimCaseEntity::getReportTime));
        return PageResponse.of(toClaimVos(claims), pageNo, pageSize);
    }

    @Override
    public ClaimCaseVO getClaim(Long claimId) {
        Long customerId = authContext.requireCurrentUser().getCustomerId();
        ClaimCaseEntity claim = claimCaseMapper.selectOne(Wrappers.<ClaimCaseEntity>lambdaQuery()
                .eq(ClaimCaseEntity::getId, claimId)
                .eq(ClaimCaseEntity::getCustomerId, customerId)
                .last("limit 1"));
        if (claim == null) {
            throw new BusinessException(ResultCode.CLAIM_NOT_FOUND);
        }
        return toClaimVos(List.of(claim)).get(0);
    }

    @Override
    public ClaimCaseVO createClaim(CreateClaimRequest request) {
        Long customerId = authContext.requireCurrentUser().getCustomerId();
        PolicyEntity policy = policyMapper.selectOne(Wrappers.<PolicyEntity>lambdaQuery()
                .eq(PolicyEntity::getId, request.getPolicyId())
                .eq(PolicyEntity::getCustomerId, customerId)
                .last("limit 1"));
        if (policy == null) {
            throw new BusinessException(ResultCode.POLICY_NOT_FOUND);
        }
        ClaimCaseEntity entity = new ClaimCaseEntity();
        entity.setClaimNo("PA" + System.currentTimeMillis());
        entity.setPolicyId(request.getPolicyId());
        entity.setCustomerId(customerId);
        entity.setClaimStatus("REPORTED");
        entity.setIncidentTime(parseIncidentTime(request.getIncidentTime()));
        entity.setReportTime(LocalDateTime.now());
        entity.setProgressDesc("报案成功，平台已接收并等待审核。");
        claimCaseMapper.insert(entity);
        return getClaim(entity.getId());
    }

    @Override
    public ConsultOrderVO createConsultOrder(CreateConsultOrderRequest request) {
        Long customerId = authContext.requireCurrentUser().getCustomerId();
        if (advisorMapper.selectById(request.getAdvisorId()) == null) {
            throw new BusinessException(ResultCode.ADVISOR_NOT_AVAILABLE);
        }
        ConsultOrderEntity entity = new ConsultOrderEntity();
        entity.setConsultOrderNo("CO" + System.currentTimeMillis());
        entity.setCustomerId(customerId);
        entity.setAdvisorId(request.getAdvisorId());
        entity.setContactMobile(request.getContactMobile());
        entity.setExpectContactTime(LocalDateTime.parse(request.getExpectContactTime(), DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")));
        entity.setRemark(request.getRemark());
        entity.setConsultStatus("CREATED");
        consultOrderMapper.insert(entity);
        return ConsultOrderVO.builder()
                .consultOrderId(entity.getId())
                .consultOrderNo(entity.getConsultOrderNo())
                .status(entity.getConsultStatus())
                .build();
    }

    private List<ClaimCaseVO> toClaimVos(List<ClaimCaseEntity> claims) {
        if (claims.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, PolicyEntity> policyMap = policyMapper.selectBatchIds(claims.stream().map(ClaimCaseEntity::getPolicyId).toList()).stream()
                .collect(Collectors.toMap(PolicyEntity::getId, Function.identity()));
        Map<Long, InsuranceProductEntity> productMap = insuranceProductMapper.selectBatchIds(policyMap.values().stream().map(PolicyEntity::getProductId).toList()).stream()
                .collect(Collectors.toMap(InsuranceProductEntity::getId, Function.identity()));
        return claims.stream()
                .map(item -> {
                    PolicyEntity policy = policyMap.get(item.getPolicyId());
                    InsuranceProductEntity product = policy == null ? null : productMap.get(policy.getProductId());
                    return ClaimCaseVO.builder()
                            .claimId(item.getId())
                            .claimNo(item.getClaimNo())
                            .policyId(item.getPolicyId())
                            .policyNo(policy == null ? null : policy.getPolicyNo())
                            .productName(product == null ? null : product.getProductName())
                            .claimStatus(item.getClaimStatus())
                            .incidentTime(format(item.getIncidentTime()))
                            .reportTime(format(item.getReportTime()))
                            .progressDesc(item.getProgressDesc())
                            .build();
                })
                .toList();
    }

    private String format(LocalDateTime value) {
        return value == null ? null : DATE_TIME.format(value);
    }

    private LocalDateTime parseIncidentTime(String incidentTime) {
        try {
            return LocalDateTime.parse(incidentTime, DATE_TIME);
        } catch (Exception ignore) {
            return LocalDateTime.parse(incidentTime, DATE_TIME_MINUTE);
        }
    }
}
