package com.zs.ytbx.vo.product;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProductCompanyVO {

    private Long companyId;

    private String companyCode;

    private String companyName;

    private String companyShortName;
}
