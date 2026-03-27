package com.zs.ytbx.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class NoticePublishRequest {

    @Valid
    @NotEmpty(message = "通知列表不能为空")
    private List<NoticeItem> notices;

    @Data
    public static class NoticeItem {
        @NotNull(message = "排序号不能为空")
        private Integer sortNo;

        @Size(max = 30, message = "通知标题长度不能超过30个字符")
        private String title;

        @Size(max = 500, message = "通知内容长度不能超过500个字符")
        private String content;
    }
}
