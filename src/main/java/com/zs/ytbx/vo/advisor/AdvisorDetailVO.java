package com.zs.ytbx.vo.advisor;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdvisorDetailVO {

    private Long advisorId;

    private String advisorName;

    private String employeeNo;

    private String titleName;

    private String avatarUrl;

    private String profileSummary;

    private String mobile;

    private String status;
}
