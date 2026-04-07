package com.zs.ytbx.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("biz_advisor")
public class AdvisorEntity extends BaseEntity {

    private String advisorCode;

    private String advisorName;

    private String mobile;

    private String employeeNo;

    private String titleName;

    private String profileSummary;

    private String avatarUrl;

    private String status;
}
