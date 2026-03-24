package com.zs.ytbx.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;

@Data
@TableName("biz_customer_profile")
public class CustomerProfileEntity extends BaseEntity {

    private Long userId;

    private String customerName;

    private String gender;

    private String idNo;

    private LocalDate birthday;

    private String memberLevel;

    private Long advisorId;
}
