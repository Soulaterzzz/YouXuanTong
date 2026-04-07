package com.zs.ytbx.service;

import com.zs.ytbx.common.api.PageResponse;
import com.zs.ytbx.query.ProductQuery;
import com.zs.ytbx.vo.product.ProductCategoryVO;
import com.zs.ytbx.vo.product.ProductCompanyVO;
import com.zs.ytbx.vo.product.ProductCompareResultVO;
import com.zs.ytbx.vo.product.ProductDetailVO;
import com.zs.ytbx.vo.product.ProductListItemVO;

import java.util.List;

public interface ProductService {

    List<ProductCategoryVO> listCategories();

    List<ProductCompanyVO> listCompanies();

    PageResponse<ProductListItemVO> listProducts(ProductQuery query);

    ProductDetailVO getProductDetail(Long productId);

    ProductCompareResultVO compare(List<Long> productPlanIds);
}
