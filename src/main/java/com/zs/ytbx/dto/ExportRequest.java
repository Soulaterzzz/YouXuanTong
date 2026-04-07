package com.zs.ytbx.dto;

import lombok.Data;
import java.util.List;

@Data
public class ExportRequest {
    private List<Long> ids;
    private String type;
}
