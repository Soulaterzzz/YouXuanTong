package com.zs.ytbx.vo.policy;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PolicyEventVO {

    private String eventType;

    private String eventTitle;

    private String eventTime;

    private String eventContent;
}
