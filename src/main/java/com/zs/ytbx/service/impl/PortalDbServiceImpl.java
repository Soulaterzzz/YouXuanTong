package com.zs.ytbx.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zs.ytbx.entity.InsuranceProductEntity;
import com.zs.ytbx.entity.ProductCategoryEntity;
import com.zs.ytbx.entity.ProductPlanEntity;
import com.zs.ytbx.mapper.InsuranceProductMapper;
import com.zs.ytbx.mapper.ProductCategoryMapper;
import com.zs.ytbx.mapper.ProductPlanMapper;
import com.zs.ytbx.service.PortalService;
import com.zs.ytbx.vo.portal.PortalHomeVO;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ytbx.mock.enabled", havingValue = "false")
public class PortalDbServiceImpl implements PortalService {

    private final ProductCategoryMapper productCategoryMapper;
    private final InsuranceProductMapper insuranceProductMapper;
    private final ProductPlanMapper productPlanMapper;

    @Override
    public PortalHomeVO getHome() {
        List<ProductCategoryEntity> categories = productCategoryMapper.selectList(Wrappers.<ProductCategoryEntity>lambdaQuery()
                .eq(ProductCategoryEntity::getCategoryStatus, "ENABLED")
                .orderByAsc(ProductCategoryEntity::getSortNo)
                .last("limit 4"));

        List<InsuranceProductEntity> recommended = insuranceProductMapper.selectList(Wrappers.<InsuranceProductEntity>lambdaQuery()
                .eq(InsuranceProductEntity::getSaleStatus, "ON_SHELF")
                .eq(InsuranceProductEntity::getRecommendFlag, 1)
                .orderByAsc(InsuranceProductEntity::getSortNo)
                .last("limit 3"));

        Map<Long, ProductPlanEntity> planMap = recommended.isEmpty() ? Collections.emptyMap() :
                productPlanMapper.selectList(Wrappers.<ProductPlanEntity>lambdaQuery()
                                .in(ProductPlanEntity::getProductId, recommended.stream().map(InsuranceProductEntity::getId).toList())
                                .eq(ProductPlanEntity::getPlanStatus, "ENABLED")
                                .orderByAsc(ProductPlanEntity::getSortNo))
                        .stream()
                        .collect(Collectors.toMap(ProductPlanEntity::getProductId, Function.identity(), (left, right) -> left));

        return PortalHomeVO.builder()
                .hero(PortalHomeVO.Hero.builder()
                        .title("为您的家庭保驾护航")
                        .subtitle("平安数字信托，基于数字化资产管理能力，为您提供专业、稳健、有温度的风险保障方案。")
                        .primaryAction("立即咨询")
                        .secondaryAction("了解更多")
                        .build())
                .categories(categories.stream()
                        .map(item -> PortalHomeVO.CategoryCard.builder()
                                .categoryId(item.getId())
                                .categoryName(item.getCategoryName())
                                .summary("精选" + item.getCategoryName() + "方案，为不同人生阶段提供稳健保障。")
                                .productCountText(productCountText(item.getId()))
                                .actionText("查看详情")
                                .build())
                        .toList())
                .recommendedProducts(recommended.stream()
                        .map(item -> {
                            ProductPlanEntity plan = planMap.get(item.getId());
                            return PortalHomeVO.RecommendedProduct.builder()
                                    .productId(item.getId())
                                    .planId(plan == null ? null : plan.getId())
                                    .productName(item.getProductName())
                                    .sellingPoint(item.getSummary())
                                    .premiumText(plan == null ? null : "¥" + plan.getPremiumStartAmount().stripTrailingZeros().toPlainString() + "/年起")
                                    .tags(parseTags(plan == null ? null : plan.getHighlightTags()))
                                    .build();
                        })
                        .toList())
                .claimSteps(List.of(
                        PortalHomeVO.ClaimStep.builder().sortNo(1).stepName("在线报案").summary("手机提交出险信息和基础材料").build(),
                        PortalHomeVO.ClaimStep.builder().sortNo(2).stepName("快速审核").summary("系统自动分发并进入人工审核").build(),
                        PortalHomeVO.ClaimStep.builder().sortNo(3).stepName("理赔结案").summary("审核通过后确认赔付方案").build(),
                        PortalHomeVO.ClaimStep.builder().sortNo(4).stepName("赔款到账").summary("赔付完成后同步理赔进度与到账结果").build()
                ))
                .build();
    }

    private String productCountText(Long categoryId) {
        long count = insuranceProductMapper.selectCount(Wrappers.<InsuranceProductEntity>lambdaQuery()
                .eq(InsuranceProductEntity::getCategoryId, categoryId)
                .eq(InsuranceProductEntity::getSaleStatus, "ON_SHELF"));
        return count + " 款在售产品";
    }

    private List<String> parseTags(String tags) {
        if (tags == null || tags.isBlank()) {
            return Collections.emptyList();
        }
        return List.of(tags.split("[,，]"));
    }
}
