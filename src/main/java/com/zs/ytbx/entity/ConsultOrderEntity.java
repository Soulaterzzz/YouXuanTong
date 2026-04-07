package com.zs.ytbx.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("biz_consult_order")
public class ConsultOrderEntity extends BaseEntity {

    private String consultOrderNo;

    private Long customerId;

    private Long advisorId;

    private String contactMobile;

    private LocalDateTime expectContactTime;

    private String remark;

    private String consultStatus;
}
