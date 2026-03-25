package com.zs.ytbx.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TemplateFileVO {
    private byte[] data;
    private String fileName;
}