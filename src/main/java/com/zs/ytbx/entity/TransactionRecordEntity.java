package com.zs.ytbx.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("axx_transaction_record")
public class TransactionRecordEntity extends BaseEntity {

    private String serialNo;

    private Long userId;

    private String transType;

    private BigDecimal amount;

    private BigDecimal balanceBefore;

    private BigDecimal balanceAfter;

    private String description;

    private String refType;

    private Long refId;

    private String paymentMethod;

    private String paymentStatus;
}
