package com.zs.ytbx.service.impl;

import com.zs.ytbx.common.api.PageResponse;
import com.zs.ytbx.common.enums.ResultCode;
import com.zs.ytbx.common.exception.BusinessException;
import com.zs.ytbx.query.PolicyQuery;
import com.zs.ytbx.service.PolicyService;
import com.zs.ytbx.vo.policy.PolicyDetailVO;
import com.zs.ytbx.vo.policy.PolicyDownloadVO;
import com.zs.ytbx.vo.policy.PolicyEventVO;
import com.zs.ytbx.vo.policy.PolicyListItemVO;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@ConditionalOnProperty(name = "ytbx.mock.enabled", havingValue = "true", matchIfMissing = true)
public class PolicyServiceImpl implements PolicyService {

    private static final List<PolicyListItemVO> POLICIES = List.of(
            PolicyListItemVO.builder().policyId(1001L).policyNo("PA-202308912234").productName("平安尊享e生百万医疗险").categoryName("健康医疗").effectiveDate("2023-08-15").policyStatus("有效").build(),
            PolicyListItemVO.builder().policyId(1002L).policyNo("PA-CAR-991200344").productName("家庭小轿车交强险及三责").categoryName("车辆保险").effectiveDate("2023-01-10").policyStatus("有效").build(),
            PolicyListItemVO.builder().policyId(1003L).policyNo("PA-TRV-778129331").productName("环球旅游意外险(欧洲线)").categoryName("意外险").effectiveDate("2022-12-01").policyStatus("失效").build()
    );

    @Override
    public PageResponse<PolicyListItemVO> listPolicies(PolicyQuery query) {
        List<PolicyListItemVO> filtered = POLICIES.stream()
                .filter(item -> !StringUtils.hasText(query.getKeyword())
                        || item.getPolicyNo().contains(query.getKeyword())
                        || item.getProductName().contains(query.getKeyword()))
                .filter(item -> !StringUtils.hasText(query.getPolicyStatus())
                        || item.getPolicyStatus().equals(query.getPolicyStatus()))
                .toList();
        return PageResponse.of(filtered, query.getPageNo(), query.getPageSize());
    }

    @Override
    public PolicyDetailVO getPolicyDetail(Long policyId) {
        if (!policyId.equals(1001L)) {
            throw new BusinessException(ResultCode.POLICY_NOT_FOUND);
        }
        return PolicyDetailVO.builder()
                .policyId(1001L)
                .policyNo("PA-TRUST-20240822-0941")
                .policyStatus("保障中")
                .productName("平安「尊享一生」终身寿险")
                .categoryName("分红型寿险（数字信托增强版）")
                .coverageAmountText("¥ 5,000,000.00")
                .insurancePeriodDesc("终身")
                .paymentPeriodDesc("20年交（已交3年）")
                .effectiveDate("2021-08-22")
                .parties(List.of(
                        PolicyDetailVO.PartyInfo.builder().partyRole("投保人").partyName("张晓明").relationDesc("本人").build(),
                        PolicyDetailVO.PartyInfo.builder().partyRole("被保险人").partyName("张晓明").relationDesc("本人").build()
                ))
                .beneficiaries(List.of(
                        PolicyDetailVO.BeneficiaryInfo.builder().beneficiaryName("李美芳").beneficiaryType("法定受益人").ratioText("100%").build()
                ))
                .guarantees(List.of(
                        PolicyDetailVO.GuaranteeInfo.builder().itemName("身故保障").itemDesc("给付基本保额的120%-160%或现金价值").build(),
                        PolicyDetailVO.GuaranteeInfo.builder().itemName("红利分配").itemDesc("根据公司经营状况，每年分配保单红利").build(),
                        PolicyDetailVO.GuaranteeInfo.builder().itemName("保单贷款").itemDesc("最高可申请保单现金价值80%的贷款").build()
                ))
                .trustInfo(PolicyDetailVO.TrustInfo.builder()
                        .trustEnabled(true)
                        .summary("该保单已接入平安数字信托协议，权益受区块链技术保障，理赔金将根据信托合同定向拨付。")
                        .agreementUrl("/agreements/trust/PA-TRUST-20240822-0941")
                        .build())
                .build();
    }

    @Override
    public PolicyDownloadVO getEPolicy(Long policyId) {
        if (!policyId.equals(1001L)) {
            throw new BusinessException(ResultCode.POLICY_ELECTRONIC_FILE_NOT_FOUND);
        }
        return PolicyDownloadVO.builder()
                .fileName("PA-TRUST-20240822-0941.pdf")
                .downloadUrl("/files/mock/PA-TRUST-20240822-0941.pdf")
                .expireAt("2026-03-20 23:59:59")
                .build();
    }

    @Override
    public List<PolicyEventVO> getEvents(Long policyId) {
        if (!policyId.equals(1001L)) {
            throw new BusinessException(ResultCode.POLICY_NOT_FOUND);
        }
        return List.of(
                PolicyEventVO.builder().eventType("RENEWAL_PAYMENT").eventTitle("续期缴费成功").eventTime("2023-08-20 10:15:30").eventContent("保单年度第3年保费已成功扣除，共计 ¥12,800.00").build(),
                PolicyEventVO.builder().eventType("TRUST_UPDATE").eventTitle("数字信托协议更新").eventTime("2022-12-05 14:22:11").eventContent("受益人信托分配比例调整申请已生效").build(),
                PolicyEventVO.builder().eventType("UNDERWRITING_SUCCESS").eventTitle("保单承保成功").eventTime("2021-08-22 09:00:00").eventContent("合同签署完成，正式进入犹豫期").build()
        );
    }
}
