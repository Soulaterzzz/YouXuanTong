package com.zs.ytbx.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("axx_account_balance")
public class AccountBalanceEntity extends BaseEntity {

    private Long userId;

    private BigDecimal balance;

    private BigDecimal frozenBalance;
}
