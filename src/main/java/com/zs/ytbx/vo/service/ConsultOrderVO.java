package com.zs.ytbx.vo.service;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ConsultOrderVO {

    private Long consultOrderId;

    private String consultOrderNo;

    private String status;
}
