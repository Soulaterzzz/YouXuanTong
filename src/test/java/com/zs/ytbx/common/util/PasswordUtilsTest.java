package com.zs.ytbx.common.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordUtilsTest {

    @Test
    void shouldEncodeAndMatchPassword() {
        String encoded = PasswordUtils.encode("secret-123");

        assertThat(encoded).startsWith("pbkdf2$");
        assertThat(PasswordUtils.matches("secret-123", encoded)).isTrue();
        assertThat(PasswordUtils.matches("wrong-password", encoded)).isFalse();
    }

    @Test
    void shouldSupportLegacyPlaintextValuesDuringMigration() {
        assertThat(PasswordUtils.matches("legacy-pass", "legacy-pass")).isTrue();
        assertThat(PasswordUtils.matches("legacy-pass", "legacy-pass-2")).isFalse();
    }
}
