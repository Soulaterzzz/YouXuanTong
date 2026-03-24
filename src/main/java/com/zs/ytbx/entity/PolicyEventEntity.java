package com.zs.ytbx.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("biz_policy_event")
public class PolicyEventEntity extends BaseEntity {

    private Long policyId;

    private String eventType;

    private String eventTitle;

    private String eventContent;

    private LocalDateTime eventTime;
}
