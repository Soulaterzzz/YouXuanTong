package com.zs.ytbx.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zs.ytbx.common.auth.AuthContext;
import com.zs.ytbx.common.api.PageResponse;
import com.zs.ytbx.common.enums.ResultCode;
import com.zs.ytbx.common.exception.BusinessException;
import com.zs.ytbx.entity.InsuranceProductEntity;
import com.zs.ytbx.entity.PolicyBeneficiaryEntity;
import com.zs.ytbx.entity.PolicyEntity;
import com.zs.ytbx.entity.PolicyEventEntity;
import com.zs.ytbx.entity.PolicyPartyEntity;
import com.zs.ytbx.entity.ProductCategoryEntity;
import com.zs.ytbx.entity.ProductGuaranteeItemEntity;
import com.zs.ytbx.entity.ProductPlanEntity;
import com.zs.ytbx.mapper.InsuranceProductMapper;
import com.zs.ytbx.mapper.PolicyBeneficiaryMapper;
import com.zs.ytbx.mapper.PolicyEventMapper;
import com.zs.ytbx.mapper.PolicyMapper;
import com.zs.ytbx.mapper.PolicyPartyMapper;
import com.zs.ytbx.mapper.ProductCategoryMapper;
import com.zs.ytbx.mapper.ProductGuaranteeItemMapper;
import com.zs.ytbx.mapper.ProductPlanMapper;
import com.zs.ytbx.query.PolicyQuery;
import com.zs.ytbx.service.PolicyService;
import com.zs.ytbx.vo.policy.PolicyDetailVO;
import com.zs.ytbx.vo.policy.PolicyDownloadVO;
import com.zs.ytbx.vo.policy.PolicyEventVO;
import com.zs.ytbx.vo.policy.PolicyListItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ytbx.mock.enabled", havingValue = "false")
public class PolicyDbServiceImpl implements PolicyService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final PolicyMapper policyMapper;
    private final InsuranceProductMapper insuranceProductMapper;
    private final ProductCategoryMapper productCategoryMapper;
    private final ProductPlanMapper productPlanMapper;
    private final ProductGuaranteeItemMapper productGuaranteeItemMapper;
    private final PolicyPartyMapper policyPartyMapper;
    private final PolicyBeneficiaryMapper policyBeneficiaryMapper;
    private final PolicyEventMapper policyEventMapper;
    private final AuthContext authContext;

    @Override
    public PageResponse<PolicyListItemVO> listPolicies(PolicyQuery query) {
        Long customerId = authContext.requireCurrentUser().getCustomerId();
        List<PolicyEntity> policies = policyMapper.selectList(Wrappers.<PolicyEntity>lambdaQuery()
                .eq(PolicyEntity::getCustomerId, customerId)
                .like(StringUtils.hasText(query.getKeyword()), PolicyEntity::getPolicyNo, query.getKeyword())
                .eq(StringUtils.hasText(query.getPolicyStatus()), PolicyEntity::getPolicyStatus, query.getPolicyStatus())
                .orderByDesc(PolicyEntity::getEffectiveDate));
        if (policies.isEmpty()) {
            return PageResponse.of(Collections.emptyList(), query.getPageNo(), query.getPageSize());
        }

        Set<Long> productIds = policies.stream().map(PolicyEntity::getProductId).collect(Collectors.toSet());
        Map<Long, InsuranceProductEntity> productMap = insuranceProductMapper.selectBatchIds(productIds).stream()
                .collect(Collectors.toMap(InsuranceProductEntity::getId, Function.identity()));
        Map<Long, String> categoryNameMap = productCategoryMapper.selectBatchIds(productMap.values().stream()
                        .map(InsuranceProductEntity::getCategoryId).collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(ProductCategoryEntity::getId, ProductCategoryEntity::getCategoryName));

        List<PolicyListItemVO> records = policies.stream()
                .map(policy -> {
                    InsuranceProductEntity product = productMap.get(policy.getProductId());
                    return PolicyListItemVO.builder()
                            .policyId(policy.getId())
                            .policyNo(policy.getPolicyNo())
                            .productName(product == null ? null : product.getProductName())
                            .categoryName(product == null ? null : categoryNameMap.get(product.getCategoryId()))
                            .effectiveDate(formatDate(policy.getEffectiveDate()))
                            .policyStatus(policy.getPolicyStatus())
                            .build();
                })
                .toList();
        return PageResponse.of(records, query.getPageNo(), query.getPageSize());
    }

    @Override
    public PolicyDetailVO getPolicyDetail(Long policyId) {
        PolicyEntity policy = getOwnedPolicy(policyId);
        if (policy == null) {
            throw new BusinessException(ResultCode.POLICY_NOT_FOUND);
        }
        InsuranceProductEntity product = insuranceProductMapper.selectById(policy.getProductId());
        ProductCategoryEntity category = product == null ? null : productCategoryMapper.selectById(product.getCategoryId());
        ProductPlanEntity plan = productPlanMapper.selectById(policy.getPlanId());

        List<PolicyPartyEntity> parties = policyPartyMapper.selectList(Wrappers.<PolicyPartyEntity>lambdaQuery()
                .eq(PolicyPartyEntity::getPolicyId, policyId));
        List<PolicyBeneficiaryEntity> beneficiaries = policyBeneficiaryMapper.selectList(Wrappers.<PolicyBeneficiaryEntity>lambdaQuery()
                .eq(PolicyBeneficiaryEntity::getPolicyId, policyId));
        List<ProductGuaranteeItemEntity> guarantees = plan == null ? Collections.emptyList() :
                productGuaranteeItemMapper.selectList(Wrappers.<ProductGuaranteeItemEntity>lambdaQuery()
                        .eq(ProductGuaranteeItemEntity::getPlanId, plan.getId())
                        .orderByAsc(ProductGuaranteeItemEntity::getSortNo));

        return PolicyDetailVO.builder()
                .policyId(policy.getId())
                .policyNo(policy.getPolicyNo())
                .policyStatus(policy.getPolicyStatus())
                .productName(product == null ? null : product.getProductName())
                .categoryName(category == null ? null : category.getCategoryName())
                .coverageAmountText(formatAmount(policy.getCoverageAmount()))
                .insurancePeriodDesc(policy.getInsurancePeriodDesc())
                .paymentPeriodDesc(policy.getPaymentPeriodDesc())
                .effectiveDate(formatDate(policy.getEffectiveDate()))
                .parties(parties.stream()
                        .map(item -> PolicyDetailVO.PartyInfo.builder()
                                .partyRole(item.getPartyRole())
                                .partyName(item.getPartyName())
                                .relationDesc(item.getRelationDesc())
                                .build())
                        .toList())
                .beneficiaries(beneficiaries.stream()
                        .map(item -> PolicyDetailVO.BeneficiaryInfo.builder()
                                .beneficiaryName(item.getBeneficiaryName())
                                .beneficiaryType(item.getBeneficiaryType())
                                .ratioText(item.getBeneficiaryRatio() == null ? null : item.getBeneficiaryRatio().stripTrailingZeros().toPlainString() + "%")
                                .build())
                        .toList())
                .guarantees(guarantees.stream()
                        .map(item -> PolicyDetailVO.GuaranteeInfo.builder()
                                .itemName(item.getItemName())
                                .itemDesc(item.getItemDesc())
                                .build())
                        .toList())
                .trustInfo(PolicyDetailVO.TrustInfo.builder()
                        .trustEnabled(Objects.equals(policy.getTrustFlag(), 1))
                        .summary(Objects.equals(policy.getTrustFlag(), 1) ? "该保单已接入数字信托托管流程。" : "当前保单未启用数字信托托管。")
                        .agreementUrl(Objects.equals(policy.getTrustFlag(), 1) ? "/agreements/trust/" + policy.getPolicyNo() : null)
                        .build())
                .build();
    }

    @Override
    public PolicyDownloadVO getEPolicy(Long policyId) {
        PolicyEntity policy = getOwnedPolicy(policyId);
        if (policy == null || !StringUtils.hasText(policy.getEPolicyFileId())) {
            throw new BusinessException(ResultCode.POLICY_ELECTRONIC_FILE_NOT_FOUND);
        }
        return PolicyDownloadVO.builder()
                .fileName(policy.getPolicyNo() + ".pdf")
                .downloadUrl("/files/" + policy.getEPolicyFileId())
                .expireAt(formatDateTime(LocalDateTime.now().plusHours(24)))
                .build();
    }

    @Override
    public List<PolicyEventVO> getEvents(Long policyId) {
        if (getOwnedPolicy(policyId) == null) {
            throw new BusinessException(ResultCode.POLICY_NOT_FOUND);
        }
        return policyEventMapper.selectList(Wrappers.<PolicyEventEntity>lambdaQuery()
                        .eq(PolicyEventEntity::getPolicyId, policyId)
                        .orderByDesc(PolicyEventEntity::getEventTime))
                .stream()
                .map(item -> PolicyEventVO.builder()
                        .eventType(item.getEventType())
                        .eventTitle(item.getEventTitle())
                        .eventTime(formatDateTime(item.getEventTime()))
                        .eventContent(item.getEventContent())
                        .build())
                .toList();
    }

    private String formatDate(LocalDateTime time) {
        return time == null ? null : DATE_FORMATTER.format(time);
    }

    private String formatDateTime(LocalDateTime time) {
        return time == null ? null : DATE_TIME_FORMATTER.format(time);
    }

    private String formatAmount(BigDecimal amount) {
        return amount == null ? null : "¥ " + amount.stripTrailingZeros().toPlainString();
    }

    private PolicyEntity getOwnedPolicy(Long policyId) {
        Long customerId = authContext.requireCurrentUser().getCustomerId();
        return policyMapper.selectOne(Wrappers.<PolicyEntity>lambdaQuery()
                .eq(PolicyEntity::getId, policyId)
                .eq(PolicyEntity::getCustomerId, customerId)
                .last("limit 1"));
    }
}
