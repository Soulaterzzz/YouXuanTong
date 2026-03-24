package com.zs.ytbx.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("biz_insurer_company")
public class InsurerCompanyEntity extends BaseEntity {

    private String companyCode;

    private String companyName;

    private String companyShortName;

    private String companyType;

    private String companyStatus;

    private String logoUrl;
}
