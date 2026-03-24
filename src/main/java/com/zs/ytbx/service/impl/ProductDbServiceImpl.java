package com.zs.ytbx.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zs.ytbx.common.api.PageResponse;
import com.zs.ytbx.common.enums.ResultCode;
import com.zs.ytbx.common.exception.BusinessException;
import com.zs.ytbx.entity.InsuranceProductEntity;
import com.zs.ytbx.entity.InsurerCompanyEntity;
import com.zs.ytbx.entity.ProductCategoryEntity;
import com.zs.ytbx.entity.ProductGuaranteeItemEntity;
import com.zs.ytbx.entity.ProductPlanEntity;
import com.zs.ytbx.mapper.InsuranceProductMapper;
import com.zs.ytbx.mapper.InsurerCompanyMapper;
import com.zs.ytbx.mapper.ProductCategoryMapper;
import com.zs.ytbx.mapper.ProductGuaranteeItemMapper;
import com.zs.ytbx.mapper.ProductPlanMapper;
import com.zs.ytbx.query.ProductQuery;
import com.zs.ytbx.service.ProductService;
import com.zs.ytbx.vo.product.ProductCategoryVO;
import com.zs.ytbx.vo.product.ProductCompanyVO;
import com.zs.ytbx.vo.product.ProductCompareResultVO;
import com.zs.ytbx.vo.product.ProductDetailVO;
import com.zs.ytbx.vo.product.ProductListItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
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
public class ProductDbServiceImpl implements ProductService {

    private final ProductCategoryMapper productCategoryMapper;
    private final InsuranceProductMapper insuranceProductMapper;
    private final ProductPlanMapper productPlanMapper;
    private final ProductGuaranteeItemMapper productGuaranteeItemMapper;
    private final InsurerCompanyMapper insurerCompanyMapper;

    @Override
    public List<ProductCategoryVO> listCategories() {
        return productCategoryMapper.selectList(Wrappers.<ProductCategoryEntity>lambdaQuery()
                        .eq(ProductCategoryEntity::getCategoryStatus, "ENABLED")
                        .orderByAsc(ProductCategoryEntity::getSortNo))
                .stream()
                .map(item -> ProductCategoryVO.builder()
                        .categoryId(item.getId())
                        .categoryCode(item.getCategoryCode())
                        .categoryName(item.getCategoryName())
                        .build())
                .toList();
    }

    @Override
    public List<ProductCompanyVO> listCompanies() {
        return insurerCompanyMapper.selectList(Wrappers.<InsurerCompanyEntity>lambdaQuery()
                        .eq(InsurerCompanyEntity::getCompanyStatus, "ENABLED")
                        .orderByAsc(InsurerCompanyEntity::getId))
                .stream()
                .map(item -> ProductCompanyVO.builder()
                        .companyId(item.getId())
                        .companyCode(item.getCompanyCode())
                        .companyName(item.getCompanyName())
                        .companyShortName(item.getCompanyShortName())
                        .build())
                .toList();
    }

    @Override
    public PageResponse<ProductListItemVO> listProducts(ProductQuery query) {
        List<InsuranceProductEntity> products = insuranceProductMapper.selectList(Wrappers.<InsuranceProductEntity>lambdaQuery()
                .eq(InsuranceProductEntity::getSaleStatus, "ON_SHELF")
                .like(StringUtils.hasText(query.getKeyword()), InsuranceProductEntity::getProductName, query.getKeyword())
                .eq(query.getCategoryId() != null, InsuranceProductEntity::getCategoryId, query.getCategoryId())
                .in(!CollectionUtils.isEmpty(query.getCompanyIds()), InsuranceProductEntity::getCompanyId, query.getCompanyIds())
                .in(!CollectionUtils.isEmpty(query.getInsuranceTypes()), InsuranceProductEntity::getProductType, query.getInsuranceTypes())
                .like(StringUtils.hasText(query.getTargetPeople()), InsuranceProductEntity::getTargetPeople, query.getTargetPeople())
                .orderByDesc(InsuranceProductEntity::getRecommendFlag)
                .orderByAsc(InsuranceProductEntity::getSortNo));

        if (products.isEmpty()) {
            return PageResponse.of(Collections.emptyList(), query.getPageNo(), query.getPageSize());
        }

        Set<Long> productIds = products.stream().map(InsuranceProductEntity::getId).collect(Collectors.toSet());
        Set<Long> companyIds = products.stream().map(InsuranceProductEntity::getCompanyId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> categoryIds = products.stream().map(InsuranceProductEntity::getCategoryId).filter(Objects::nonNull).collect(Collectors.toSet());

        Map<Long, ProductPlanEntity> firstPlanMap = productPlanMapper.selectList(Wrappers.<ProductPlanEntity>lambdaQuery()
                        .in(ProductPlanEntity::getProductId, productIds)
                        .eq(ProductPlanEntity::getPlanStatus, "ENABLED")
                        .orderByAsc(ProductPlanEntity::getSortNo))
                .stream()
                .collect(Collectors.toMap(ProductPlanEntity::getProductId, Function.identity(), (left, right) -> left));

        Map<Long, String> companyNameMap = insurerCompanyMapper.selectList(Wrappers.<InsurerCompanyEntity>lambdaQuery()
                        .in(InsurerCompanyEntity::getId, companyIds))
                .stream()
                .collect(Collectors.toMap(InsurerCompanyEntity::getId, InsurerCompanyEntity::getCompanyName));

        Map<Long, String> categoryNameMap = productCategoryMapper.selectList(Wrappers.<ProductCategoryEntity>lambdaQuery()
                        .in(ProductCategoryEntity::getId, categoryIds))
                .stream()
                .collect(Collectors.toMap(ProductCategoryEntity::getId, ProductCategoryEntity::getCategoryName));

        List<ProductListItemVO> result = products.stream()
                .map(item -> toProductListItem(item, firstPlanMap.get(item.getId()), companyNameMap, categoryNameMap))
                .toList();
        result = sortProducts(result, query.getSortBy());

        return PageResponse.of(result, query.getPageNo(), query.getPageSize());
    }

    @Override
    public ProductDetailVO getProductDetail(Long productId) {
        InsuranceProductEntity product = insuranceProductMapper.selectById(productId);
        if (product == null || !"ON_SHELF".equals(product.getSaleStatus())) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_FOUND);
        }

        String companyName = getCompanyName(product.getCompanyId());
        String categoryName = getCategoryName(product.getCategoryId());
        List<ProductPlanEntity> plans = productPlanMapper.selectList(Wrappers.<ProductPlanEntity>lambdaQuery()
                .eq(ProductPlanEntity::getProductId, productId)
                .eq(ProductPlanEntity::getPlanStatus, "ENABLED")
                .orderByAsc(ProductPlanEntity::getSortNo));
        List<Long> planIds = plans.stream().map(ProductPlanEntity::getId).toList();
        List<ProductGuaranteeItemEntity> guarantees = planIds.isEmpty() ? Collections.emptyList() :
                productGuaranteeItemMapper.selectList(Wrappers.<ProductGuaranteeItemEntity>lambdaQuery()
                        .in(ProductGuaranteeItemEntity::getPlanId, planIds)
                        .orderByAsc(ProductGuaranteeItemEntity::getSortNo));

        return ProductDetailVO.builder()
                .productId(product.getId())
                .productName(product.getProductName())
                .companyName(companyName)
                .categoryName(categoryName)
                .summary(product.getSummary())
                .tags(parseTags(plans.isEmpty() ? null : plans.get(0).getHighlightTags()))
                .plans(plans.stream().map(this::toPlanOption).toList())
                .guaranteeItems(guarantees.stream()
                        .map(item -> ProductDetailVO.GuaranteeItem.builder()
                                .itemName(item.getItemName())
                                .itemValue(item.getItemValue())
                                .itemDesc(item.getItemDesc())
                                .build())
                        .toList())
                .build();
    }

    @Override
    public ProductCompareResultVO compare(List<Long> productPlanIds) {
        List<ProductPlanEntity> plans = productPlanMapper.selectBatchIds(productPlanIds);
        if (plans.size() != productPlanIds.size()) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_FOUND);
        }

        Map<Long, InsuranceProductEntity> productMap = insuranceProductMapper.selectBatchIds(
                        plans.stream().map(ProductPlanEntity::getProductId).toList())
                .stream()
                .collect(Collectors.toMap(InsuranceProductEntity::getId, Function.identity()));

        if (plans.stream().anyMatch(item -> {
            InsuranceProductEntity product = productMap.get(item.getProductId());
            return product == null || !Objects.equals(product.getSupportCompare(), 1);
        })) {
            throw new BusinessException(ResultCode.PRODUCT_COMPARE_UNSUPPORTED);
        }

        Map<Long, String> companyNameMap = insurerCompanyMapper.selectBatchIds(productMap.values().stream()
                        .map(InsuranceProductEntity::getCompanyId)
                        .toList())
                .stream()
                .collect(Collectors.toMap(InsurerCompanyEntity::getId, InsurerCompanyEntity::getCompanyName));

        List<ProductCompareResultVO.ComparedPlan> items = plans.stream()
                .map(plan -> {
                    InsuranceProductEntity product = productMap.get(plan.getProductId());
                    return ProductCompareResultVO.ComparedPlan.builder()
                            .planId(plan.getId())
                            .productName(product.getProductName())
                            .companyName(companyNameMap.get(product.getCompanyId()))
                            .premiumText(formatAmount(plan.getPremiumStartAmount(), "/年起"))
                            .recommendationTag(Objects.equals(plan.getId(), productPlanIds.get(0)) ? "推荐方案 A" : "推荐方案 B")
                            .build();
                })
                .toList();

        return ProductCompareResultVO.builder()
                .selectedCount(items.size())
                .recommendedPlanId(productPlanIds.get(0))
                .items(items)
                .compareSections(List.of(
                        ProductCompareResultVO.CompareSection.builder()
                                .sectionName("基本投保信息")
                                .fields(List.of(
                                        ProductCompareResultVO.CompareField.builder()
                                                .fieldName("保障期限")
                                                .fieldValues(plans.stream().map(ProductPlanEntity::getCoveragePeriodDesc).toList())
                                                .build(),
                                        ProductCompareResultVO.CompareField.builder()
                                                .fieldName("最高保额")
                                                .fieldValues(plans.stream().map(item -> formatAmount(item.getCoverageAmount(), "")).toList())
                                                .build(),
                                        ProductCompareResultVO.CompareField.builder()
                                                .fieldName("续保说明")
                                                .fieldValues(plans.stream().map(ProductPlanEntity::getRenewalDesc).toList())
                                                .build()
                                ))
                                .build()
                ))
                .build();
    }

    private ProductListItemVO toProductListItem(InsuranceProductEntity item,
                                                ProductPlanEntity plan,
                                                Map<Long, String> companyNameMap,
                                                Map<Long, String> categoryNameMap) {
        return ProductListItemVO.builder()
                .productId(item.getId())
                .planId(plan == null ? null : plan.getId())
                .productName(item.getProductName())
                .companyName(companyNameMap.get(item.getCompanyId()))
                .categoryName(categoryNameMap.get(item.getCategoryId()))
                .summary(item.getSummary())
                .premiumText(plan == null ? null : formatAmount(plan.getPremiumStartAmount(), "起"))
                .coverageText(plan == null ? null : formatAmount(plan.getCoverageAmount(), ""))
                .tags(parseTags(plan == null ? null : plan.getHighlightTags()))
                .supportCompare(Objects.equals(item.getSupportCompare(), 1))
                .build();
    }

    private ProductDetailVO.PlanOption toPlanOption(ProductPlanEntity plan) {
        return ProductDetailVO.PlanOption.builder()
                .planId(plan.getId())
                .planName(plan.getPlanName())
                .premiumText(formatAmount(plan.getPremiumStartAmount(), "起"))
                .coverageText(formatAmount(plan.getCoverageAmount(), ""))
                .periodText(plan.getCoveragePeriodDesc())
                .renewalText(plan.getRenewalDesc())
                .build();
    }

    private String getCompanyName(Long companyId) {
        InsurerCompanyEntity company = insurerCompanyMapper.selectById(companyId);
        return company == null ? null : company.getCompanyName();
    }

    private String getCategoryName(Long categoryId) {
        ProductCategoryEntity category = productCategoryMapper.selectById(categoryId);
        return category == null ? null : category.getCategoryName();
    }

    private List<String> parseTags(String highlightTags) {
        if (!StringUtils.hasText(highlightTags)) {
            return Collections.emptyList();
        }
        return List.of(highlightTags.split("[,，]"));
    }

    private String formatAmount(BigDecimal amount, String suffix) {
        if (amount == null) {
            return null;
        }
        return "¥" + amount.stripTrailingZeros().toPlainString() + suffix;
    }

    private List<ProductListItemVO> sortProducts(List<ProductListItemVO> source, String sortBy) {
        if (!StringUtils.hasText(sortBy)) {
            return source;
        }
        return switch (sortBy) {
            case "premiumAsc" -> source.stream()
                    .sorted((left, right) -> comparePremium(left.getPremiumText(), right.getPremiumText()))
                    .toList();
            case "premiumDesc" -> source.stream()
                    .sorted((left, right) -> comparePremium(right.getPremiumText(), left.getPremiumText()))
                    .toList();
            default -> source;
        };
    }

    private int comparePremium(String left, String right) {
        return Integer.compare(extractDigits(left), extractDigits(right));
    }

    private int extractDigits(String value) {
        if (!StringUtils.hasText(value)) {
            return Integer.MAX_VALUE;
        }
        String digits = value.replaceAll("[^0-9]", "");
        if (!StringUtils.hasText(digits)) {
            return Integer.MAX_VALUE;
        }
        return Integer.parseInt(digits);
    }
}
