package com.zs.ytbx.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("axx_notice")
public class AxxNoticeEntity extends BaseEntity {

    private String title;

    private String content;

    private Integer sortNo;

    private LocalDateTime publishedAt;
}
