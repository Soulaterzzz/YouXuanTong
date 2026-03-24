package com.zs.ytbx.vo.policy;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PolicyDownloadVO {

    private String fileName;

    private String downloadUrl;

    private String expireAt;
}
