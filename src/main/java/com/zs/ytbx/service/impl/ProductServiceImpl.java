package com.zs.ytbx.service.impl;

import com.zs.ytbx.common.api.PageResponse;
import com.zs.ytbx.common.enums.ResultCode;
import com.zs.ytbx.common.exception.BusinessException;
import com.zs.ytbx.query.ProductQuery;
import com.zs.ytbx.service.ProductService;
import com.zs.ytbx.vo.product.ProductCategoryVO;
import com.zs.ytbx.vo.product.ProductCompanyVO;
import com.zs.ytbx.vo.product.ProductCompareResultVO;
import com.zs.ytbx.vo.product.ProductDetailVO;
import com.zs.ytbx.vo.product.ProductListItemVO;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@ConditionalOnProperty(name = "ytbx.mock.enabled", havingValue = "true", matchIfMissing = true)
public class ProductServiceImpl implements ProductService {

    private static final List<ProductCategoryVO> CATEGORIES = List.of(
            ProductCategoryVO.builder().categoryId(1L).categoryCode("medical").categoryName("医疗险").build(),
            ProductCategoryVO.builder().categoryId(2L).categoryCode("critical").categoryName("重疾险").build(),
            ProductCategoryVO.builder().categoryId(3L).categoryCode("accident").categoryName("意外险").build(),
            ProductCategoryVO.builder().categoryId(4L).categoryCode("life").categoryName("人寿险").build(),
            ProductCategoryVO.builder().categoryId(5L).categoryCode("car").categoryName("车险").build()
    );

    private static final List<ProductCompanyVO> COMPANIES = List.of(
            ProductCompanyVO.builder().companyId(1L).companyCode("pingan-health").companyName("平安健康").companyShortName("平安健康").build(),
            ProductCompanyVO.builder().companyId(2L).companyCode("pingan-life").companyName("平安人寿").companyShortName("平安人寿").build(),
            ProductCompanyVO.builder().companyId(3L).companyCode("pingan-pc").companyName("平安产险").companyShortName("平安产险").build()
    );

    private static final List<ProductListItemVO> PRODUCTS = List.of(
            ProductListItemVO.builder().productId(1L).planId(101L).productName("平安e生保·长期医疗险").companyName("平安健康").categoryName("医疗险").summary("20年保证续保，保额高达400万，覆盖重疾医疗和院外特药。").premiumText("¥16/月起").coverageText("最高保障额度 ¥4,000,000").tags(List.of("平安自营", "热销爆款")).supportCompare(true).build(),
            ProductListItemVO.builder().productId(2L).planId(102L).productName("少儿平安福·尊享版").companyName("平安人寿").categoryName("重疾险").summary("专为孩子设计的终身重疾保障，覆盖重疾、中症和轻症。").premiumText("¥2,380/年起").coverageText("身故/重疾保障 ¥1,000,000").tags(List.of("平安人寿", "少儿专属")).supportCompare(true).build(),
            ProductListItemVO.builder().productId(3L).planId(103L).productName("平安车险·商业险自选").companyName("平安产险").categoryName("车险").summary("支持线上自助投保，理赔流程极简，买车险更省心。").premiumText("¥980/年起").coverageText("三者责任保障 ¥3,000,000").tags(List.of("平安产险", "限时特惠")).supportCompare(false).build(),
            ProductListItemVO.builder().productId(4L).planId(104L).productName("平安防癌医疗险·关爱版").companyName("平安健康").categoryName("医疗险").summary("三高人群可投，50-80岁中老年专属关爱保障。").premiumText("¥588/年起").coverageText("恶性肿瘤保额 ¥2,000,000").tags(List.of("平安健康", "中老年适用")).supportCompare(true).build()
    );

    @Override
    public List<ProductCategoryVO> listCategories() {
        return CATEGORIES;
    }

    @Override
    public List<ProductCompanyVO> listCompanies() {
        return COMPANIES;
    }

    @Override
    public PageResponse<ProductListItemVO> listProducts(ProductQuery query) {
        List<ProductListItemVO> filtered = PRODUCTS.stream()
                .filter(item -> !StringUtils.hasText(query.getKeyword())
                        || item.getProductName().contains(query.getKeyword())
                        || item.getSummary().contains(query.getKeyword()))
                .filter(item -> query.getCategoryId() == null || categoryMatch(item, query.getCategoryId()))
                .filter(item -> query.getCompanyIds() == null || query.getCompanyIds().isEmpty()
                        || companyMatch(item, query.getCompanyIds()))
                .filter(item -> query.getInsuranceTypes() == null || query.getInsuranceTypes().isEmpty()
                        || query.getInsuranceTypes().contains(item.getCategoryName()))
                .filter(item -> targetPeopleMatch(item, query.getTargetPeople()))
                .toList();
        filtered = sortProducts(filtered, query.getSortBy());
        return PageResponse.of(filtered, query.getPageNo(), query.getPageSize());
    }

    @Override
    public ProductDetailVO getProductDetail(Long productId) {
        ProductListItemVO product = PRODUCTS.stream()
                .filter(item -> item.getProductId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ResultCode.PRODUCT_NOT_FOUND));

        return ProductDetailVO.builder()
                .productId(product.getProductId())
                .productName(product.getProductName())
                .companyName(product.getCompanyName())
                .categoryName(product.getCategoryName())
                .summary(product.getSummary())
                .tags(product.getTags())
                .plans(List.of(ProductDetailVO.PlanOption.builder()
                                .planId(product.getPlanId())
                                .planName(product.getProductName() + " 标准计划")
                                .premiumText(product.getPremiumText())
                                .coverageText(product.getCoverageText())
                                .periodText("1年保障 / 支持续保")
                                .renewalText("可连续续保，具体以条款为准")
                                .build()))
                .guaranteeItems(List.of(
                        ProductDetailVO.GuaranteeItem.builder().itemName("核心保障").itemValue("住院医疗 / 重疾医疗").itemDesc("覆盖住院、重疾、特定门诊等场景").build(),
                        ProductDetailVO.GuaranteeItem.builder().itemName("理赔服务").itemValue("在线报案").itemDesc("支持线上报案、理赔材料上传和进度追踪").build(),
                        ProductDetailVO.GuaranteeItem.builder().itemName("增值服务").itemValue("专家预约").itemDesc("部分计划支持绿通、专家门诊和术后随访").build()
                ))
                .build();
    }

    @Override
    public ProductCompareResultVO compare(List<Long> productPlanIds) {
        Map<Long, ProductListItemVO> matched = PRODUCTS.stream()
                .filter(item -> productPlanIds.contains(item.getPlanId()))
                .collect(Collectors.toMap(ProductListItemVO::getPlanId, item -> item));
        if (matched.size() != productPlanIds.size()) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_FOUND);
        }
        if (matched.values().stream().anyMatch(item -> !Boolean.TRUE.equals(item.getSupportCompare()))) {
            throw new BusinessException(ResultCode.PRODUCT_COMPARE_UNSUPPORTED);
        }

        List<ProductCompareResultVO.ComparedPlan> items = productPlanIds.stream()
                .map(matched::get)
                .map(item -> ProductCompareResultVO.ComparedPlan.builder()
                        .planId(item.getPlanId())
                        .productName(item.getProductName())
                        .companyName(item.getCompanyName())
                        .premiumText(item.getPremiumText())
                        .recommendationTag(item.getPlanId().equals(productPlanIds.get(0)) ? "推荐方案 A" : "推荐方案 B")
                        .build())
                .toList();

        return ProductCompareResultVO.builder()
                .selectedCount(items.size())
                .recommendedPlanId(productPlanIds.get(0))
                .items(items)
                .compareSections(List.of(
                        ProductCompareResultVO.CompareSection.builder()
                                .sectionName("基本投保信息")
                                .fields(List.of(
                                        ProductCompareResultVO.CompareField.builder().fieldName("投保年龄").fieldValues(List.of("出生满30天-70周岁", "出生满30天-60周岁")).build(),
                                        ProductCompareResultVO.CompareField.builder().fieldName("保障期限").fieldValues(List.of("1年（可续保）", "1年（保证续保6年）")).build(),
                                        ProductCompareResultVO.CompareField.builder().fieldName("最高保额").fieldValues(List.of("600万", "400万")).build()
                                ))
                                .build(),
                        ProductCompareResultVO.CompareSection.builder()
                                .sectionName("保障范围明细")
                                .fields(List.of(
                                        ProductCompareResultVO.CompareField.builder().fieldName("核心保障").fieldValues(List.of("一般住院医疗300万；重疾住院医疗600万", "一般住院医疗200万；108种重疾医疗400万")).build(),
                                        ProductCompareResultVO.CompareField.builder().fieldName("理赔流程").fieldValues(List.of("在线提交，资料齐全最快24小时赔付", "人工审核，3-5个工作日")).build()
                                ))
                                .build()
                ))
                .build();
    }

    private boolean categoryMatch(ProductListItemVO item, Long categoryId) {
        return CATEGORIES.stream()
                .filter(category -> category.getCategoryId().equals(categoryId))
                .findFirst()
                .map(ProductCategoryVO::getCategoryName)
                .map(item.getCategoryName()::equals)
                .orElse(false);
    }

    private boolean companyMatch(ProductListItemVO item, List<Long> companyIds) {
        Map<Long, String> companyMapping = COMPANIES.stream()
                .collect(Collectors.toMap(ProductCompanyVO::getCompanyId, ProductCompanyVO::getCompanyName));
        return companyIds.stream()
                .map(companyMapping::get)
                .filter(StringUtils::hasText)
                .anyMatch(item.getCompanyName()::equals);
    }

    private boolean targetPeopleMatch(ProductListItemVO item, String targetPeople) {
        if (!StringUtils.hasText(targetPeople) || Objects.equals(targetPeople, "全部")) {
            return true;
        }
        return switch (targetPeople) {
            case "婴幼儿", "少儿" -> item.getProductName().contains("少儿") || item.getTags().contains("少儿专属");
            case "中老年" -> item.getProductName().contains("防癌") || item.getTags().contains("中老年适用");
            case "成年人" -> !item.getProductName().contains("少儿");
            default -> true;
        };
    }

    private List<ProductListItemVO> sortProducts(List<ProductListItemVO> source, String sortBy) {
        if (!StringUtils.hasText(sortBy)) {
            return source;
        }
        return switch (sortBy) {
            case "premiumAsc" -> source.stream()
                    .sorted((left, right) -> Integer.compare(extractPrice(left.getPremiumText()), extractPrice(right.getPremiumText())))
                    .toList();
            case "premiumDesc" -> source.stream()
                    .sorted((left, right) -> Integer.compare(extractPrice(right.getPremiumText()), extractPrice(left.getPremiumText())))
                    .toList();
            default -> source;
        };
    }

    private int extractPrice(String premiumText) {
        if (!StringUtils.hasText(premiumText)) {
            return Integer.MAX_VALUE;
        }
        String digits = premiumText.replaceAll("[^0-9]", "");
        if (!StringUtils.hasText(digits)) {
            return Integer.MAX_VALUE;
        }
        return Integer.parseInt(digits);
    }
}
