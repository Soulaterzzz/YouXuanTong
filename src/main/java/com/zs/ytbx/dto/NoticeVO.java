package com.zs.ytbx.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoticeVO {
    private Long id;
    private String title;
    private String content;
    private Integer sortNo;
    private LocalDateTime publishedAt;
    private LocalDateTime createTime;
}
