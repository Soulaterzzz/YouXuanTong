package com.zs.ytbx.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zs.ytbx.common.auth.AuthContext;
import com.zs.ytbx.entity.AdvisorEntity;
import com.zs.ytbx.entity.ClaimCaseEntity;
import com.zs.ytbx.entity.CustomerProfileEntity;
import com.zs.ytbx.entity.InsuranceProductEntity;
import com.zs.ytbx.entity.PolicyEntity;
import com.zs.ytbx.entity.PolicyEventEntity;
import com.zs.ytbx.mapper.AdvisorMapper;
import com.zs.ytbx.mapper.ClaimCaseMapper;
import com.zs.ytbx.mapper.CustomerProfileMapper;
import com.zs.ytbx.mapper.InsuranceProductMapper;
import com.zs.ytbx.mapper.PolicyEventMapper;
import com.zs.ytbx.mapper.PolicyMapper;
import com.zs.ytbx.service.UserDashboardService;
import com.zs.ytbx.vo.user.UserDashboardVO;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ytbx.mock.enabled", havingValue = "false")
public class UserDashboardDbServiceImpl implements UserDashboardService {

    private static final DateTimeFormatter MONTH_DAY = DateTimeFormatter.ofPattern("MM月dd日");

    private final AuthContext authContext;
    private final CustomerProfileMapper customerProfileMapper;
    private final AdvisorMapper advisorMapper;
    private final PolicyMapper policyMapper;
    private final PolicyEventMapper policyEventMapper;
    private final InsuranceProductMapper insuranceProductMapper;
    private final ClaimCaseMapper claimCaseMapper;

    @Override
    public UserDashboardVO getDashboard() {
        Long customerId = authContext.requireCurrentUser().getCustomerId();
        CustomerProfileEntity profile = customerProfileMapper.selectById(customerId);

        List<PolicyEntity> policies = policyMapper.selectList(Wrappers.<PolicyEntity>lambdaQuery()
                .eq(PolicyEntity::getCustomerId, customerId)
                .orderByDesc(PolicyEntity::getEffectiveDate));
        Set<Long> productIds = policies.stream().map(PolicyEntity::getProductId).collect(Collectors.toSet());
        Map<Long, InsuranceProductEntity> productMap = productIds.isEmpty() ? Collections.emptyMap() :
                insuranceProductMapper.selectBatchIds(productIds).stream()
                        .collect(Collectors.toMap(InsuranceProductEntity::getId, Function.identity()));

        List<PolicyEventEntity> events = policies.isEmpty() ? Collections.emptyList() :
                policyEventMapper.selectList(Wrappers.<PolicyEventEntity>lambdaQuery()
                        .in(PolicyEventEntity::getPolicyId, policies.stream().map(PolicyEntity::getId).toList())
                        .orderByDesc(PolicyEventEntity::getEventTime));

        Map<Long, PolicyEntity> policyMap = policies.stream()
                .collect(Collectors.toMap(PolicyEntity::getId, Function.identity()));

        List<ClaimCaseEntity> claims = claimCaseMapper.selectList(Wrappers.<ClaimCaseEntity>lambdaQuery()
                .eq(ClaimCaseEntity::getCustomerId, customerId)
                .orderByDesc(ClaimCaseEntity::getReportTime)
                .last("limit 5"));

        AdvisorEntity advisor = profile != null && profile.getAdvisorId() != null ? advisorMapper.selectById(profile.getAdvisorId()) : null;

        return UserDashboardVO.builder()
                .profile(UserDashboardVO.Profile.builder()
                        .customerName(profile == null ? "客户" : profile.getCustomerName())
                        .welcomeText(buildWelcomeText(policies, claims))
                        .memberLevel(profile == null ? "标准会员" : profile.getMemberLevel())
                        .build())
                .metrics(List.of(
                        UserDashboardVO.MetricCard.builder().metricCode("policy_count").metricName("已投保险数").metricValue(countPolicies(policies) + "份有效保单").metricTag("运行中").build(),
                        UserDashboardVO.MetricCard.builder().metricCode("monthly_cost").metricName("本月保障支出").metricValue(formatAmount(sumPremium(policies))).metricTag("本月").build(),
                        UserDashboardVO.MetricCard.builder().metricCode("claim_count").metricName("理赔进度数量").metricValue(String.format("%02d笔案件处理中", claims.size())).metricTag("理赔中").build()
                ))
                .recentPolicyUpdates(events.stream()
                        .limit(2)
                        .map(event -> {
                            PolicyEntity policy = policyMap.get(event.getPolicyId());
                            InsuranceProductEntity product = policy == null ? null : productMap.get(policy.getProductId());
                            return UserDashboardVO.PolicyUpdate.builder()
                                    .productName(product == null ? event.getEventTitle() : product.getProductName())
                                    .updateDate(event.getEventTime() == null ? "-" : MONTH_DAY.format(event.getEventTime()))
                                    .content(event.getEventContent())
                                    .tags(buildEventTags(event.getEventType(), policy == null ? null : policy.getPolicyStatus()))
                                    .build();
                        })
                        .toList())
                .advisorInfo(UserDashboardVO.AdvisorSnapshot.builder()
                        .advisorId(advisor == null ? null : advisor.getId())
                        .advisorName(advisor == null ? "暂无顾问" : advisor.getAdvisorName())
                        .employeeNo(advisor == null ? "-" : advisor.getEmployeeNo())
                        .summary(advisor == null ? "当前暂无专属顾问，建议联系平台客服。"
                                : (advisor.getProfileSummary() == null ? "专属顾问可为您提供保障方案解读。" : advisor.getProfileSummary()))
                        .build())
                .quickServices(List.of(
                        UserDashboardVO.QuickService.builder().serviceCode("claim_report").serviceName("报案理赔").servicePath("/claims/report").build(),
                        UserDashboardVO.QuickService.builder().serviceCode("e_policy").serviceName("电子保单").servicePath("/policy/list").build()
                ))
                .build();
    }

    private String buildWelcomeText(List<PolicyEntity> policies, List<ClaimCaseEntity> claims) {
        return "当前共管理 " + policies.size() + " 份保单，"
                + (claims.isEmpty() ? "暂无理赔案件在处理。" : "有 " + claims.size() + " 笔理赔案件在处理中。")
                + "平台将持续跟踪保单动态与顾问服务。";
    }

    private long countPolicies(List<PolicyEntity> policies) {
        return policies.stream()
                .filter(item -> "保障中".equals(item.getPolicyStatus()) || "有效".equals(item.getPolicyStatus()))
                .count();
    }

    private BigDecimal sumPremium(List<PolicyEntity> policies) {
        return policies.stream()
                .map(PolicyEntity::getPremiumAmount)
                .filter(item -> item != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String formatAmount(BigDecimal amount) {
        return "¥" + amount.stripTrailingZeros().toPlainString() + ".00";
    }

    private List<String> buildEventTags(String eventType, String policyStatus) {
        if ("RENEWAL_PAYMENT".equals(eventType)) {
            return List.of("自动续费", policyStatus == null ? "保障中" : policyStatus);
        }
        if ("TRUST_UPDATE".equals(eventType)) {
            return List.of("信托更新");
        }
        return List.of("保单动态");
    }
}
