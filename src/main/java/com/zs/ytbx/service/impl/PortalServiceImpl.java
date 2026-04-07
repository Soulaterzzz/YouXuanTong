package com.zs.ytbx.service.impl;

import com.zs.ytbx.service.PortalService;
import com.zs.ytbx.vo.portal.PortalHomeVO;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@ConditionalOnProperty(name = "ytbx.mock.enabled", havingValue = "true", matchIfMissing = true)
public class PortalServiceImpl implements PortalService {

    @Override
    public PortalHomeVO getHome() {
        return PortalHomeVO.builder()
                .hero(PortalHomeVO.Hero.builder()
                        .title("为您的家庭保驾护航")
                        .subtitle("平安数字信托，基于数字化资产管理能力，为您提供专业、稳健、有温度的风险保障方案。")
                        .primaryAction("立即咨询")
                        .secondaryAction("了解更多")
                        .build())
                .categories(List.of(
                        PortalHomeVO.CategoryCard.builder().categoryId(1L).categoryName("健康保险").summary("提供重疾保障、百万医疗、门诊报销等全方位健康守护。").productCountText("50+ 热门产品").actionText("查看详情").build(),
                        PortalHomeVO.CategoryCard.builder().categoryId(2L).categoryName("人寿保险").summary("终身寿险、增额寿险，助力家庭财富传承与长远规划。").productCountText("12 款精选计划").actionText("立即测算").build(),
                        PortalHomeVO.CategoryCard.builder().categoryId(3L).categoryName("意外旅行").summary("境内外旅游意外、航延险，让每一次出发都充满安心。").productCountText("18 款旅行产品").actionText("浏览产品").build()
                ))
                .recommendedProducts(List.of(
                        PortalHomeVO.RecommendedProduct.builder().productId(1L).planId(101L).productName("平安e生保·长期医疗险").sellingPoint("最高400万医疗保障，支持院外特药直付").premiumText("¥160/年起").tags(List.of("高性价比")).build(),
                        PortalHomeVO.RecommendedProduct.builder().productId(2L).planId(102L).productName("平安守护·重疾险").sellingPoint("覆盖120种疾病，轻症/中症多次赔付").premiumText("¥245/月起").tags(List.of("明星爆款")).build(),
                        PortalHomeVO.RecommendedProduct.builder().productId(3L).planId(103L).productName("御享金越·增额寿").sellingPoint("现金价值稳健增长，支持减保取现").premiumText("¥10000/年起").tags(List.of("财富增值")).build()
                ))
                .claimSteps(List.of(
                        PortalHomeVO.ClaimStep.builder().sortNo(1).stepName("在线报案").summary("手机上传影像资料，3分钟自助报案").build(),
                        PortalHomeVO.ClaimStep.builder().sortNo(2).stepName("快速审核").summary("AI智能审核系统，最快秒级反馈").build(),
                        PortalHomeVO.ClaimStep.builder().sortNo(3).stepName("理赔结案").summary("审核通过即刻结案，确认支付信息").build(),
                        PortalHomeVO.ClaimStep.builder().sortNo(4).stepName("赔款到账").summary("资金实时拨付，最快当天到账").build()
                ))
                .build();
    }
}
