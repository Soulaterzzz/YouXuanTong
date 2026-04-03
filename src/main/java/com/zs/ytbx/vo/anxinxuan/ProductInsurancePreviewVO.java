package com.zs.ytbx.vo.anxinxuan;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProductInsurancePreviewVO {
    private Integer rowNumber;
    private String planName;
    private String beneficiaryName;
    private String beneficiaryId;
    private String beneficiaryJob;
    private Integer count;
    private String address;
    private String agent;
}
