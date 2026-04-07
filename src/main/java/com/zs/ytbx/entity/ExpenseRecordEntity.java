package com.zs.ytbx.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("axx_expense_record")
public class ExpenseRecordEntity extends BaseEntity {

    private String serialNo;

    private Long userId;

    private Long productId;

    private String productName;

    private String contactName;

    private String contactMobile;

    private String expenseStatus;

    private String policyNo;

    private BigDecimal premiumAmount;

    private Integer quantity;

    private BigDecimal totalAmount;

    private LocalDate effectiveDate;

    private LocalDate expiryDate;

    private LocalDateTime exportTime;
}
