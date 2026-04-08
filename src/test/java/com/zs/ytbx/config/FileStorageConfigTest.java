package com.zs.ytbx.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FileStorageConfigTest {

    @Test
    void shouldUsePortableRelativeDefaults() {
        FileStorageConfig config = new FileStorageConfig();

        assertThat(config.getUploadPath()).isEqualTo("./uploads");
        assertThat(config.getTemplateFilePath()).isEqualTo("./uploads/templates");
        assertThat(config.getUploadPath()).doesNotContain("/Users/");
        assertThat(config.getTemplateFilePath()).doesNotContain("/Users/");
    }
}
