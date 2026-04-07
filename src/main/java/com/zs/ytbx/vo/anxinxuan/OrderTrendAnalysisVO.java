package com.zs.ytbx.vo.anxinxuan;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrderTrendAnalysisVO {
    private String dateLabel;
    private Long orderCount;
}
