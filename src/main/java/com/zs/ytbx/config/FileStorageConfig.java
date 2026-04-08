package com.zs.ytbx.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "file.storage")
public class FileStorageConfig {
    private String uploadPath = "./uploads";
    private String productImagePath = "products";
    private String templateFilePath = "./uploads/templates";
    private long maxFileSize = 10 * 1024 * 1024;
    private String[] allowedExtensions = {"jpg", "jpeg", "png", "gif", "webp"};
    private String[] allowedTemplateExtensions = {"pdf", "doc", "docx", "xls", "xlsx", "txt"};
}
