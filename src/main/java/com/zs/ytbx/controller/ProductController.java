package com.zs.ytbx.controller;

import com.zs.ytbx.common.api.ApiResponse;
import com.zs.ytbx.common.api.PageResponse;
import com.zs.ytbx.dto.CompareProductsRequest;
import com.zs.ytbx.query.ProductQuery;
import com.zs.ytbx.service.ProductService;
import com.zs.ytbx.vo.product.ProductCategoryVO;
import com.zs.ytbx.vo.product.ProductCompanyVO;
import com.zs.ytbx.vo.product.ProductCompareResultVO;
import com.zs.ytbx.vo.product.ProductDetailVO;
import com.zs.ytbx.vo.product.ProductListItemVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ProductController {

    private final ProductService productService;

    @GetMapping("/product/categories")
    public ApiResponse<List<ProductCategoryVO>> listCategories() {
        return ApiResponse.success(productService.listCategories());
    }

    @GetMapping("/product/companies")
    public ApiResponse<List<ProductCompanyVO>> listCompanies() {
        return ApiResponse.success(productService.listCompanies());
    }

    @GetMapping("/products")
    public ApiResponse<PageResponse<ProductListItemVO>> listProducts(@Valid @ModelAttribute ProductQuery query) {
        return ApiResponse.success(productService.listProducts(query));
    }

    @GetMapping("/products/{productId}")
    public ApiResponse<ProductDetailVO> getProductDetail(@PathVariable Long productId) {
        return ApiResponse.success(productService.getProductDetail(productId));
    }

    @PostMapping("/products/compare")
    public ApiResponse<ProductCompareResultVO> compare(@Valid @RequestBody CompareProductsRequest request) {
        return ApiResponse.success(productService.compare(request.getProductPlanIds()));
    }
}
