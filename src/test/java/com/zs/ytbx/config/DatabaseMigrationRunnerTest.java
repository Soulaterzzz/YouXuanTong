package com.zs.ytbx.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.env.MockEnvironment;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class DatabaseMigrationRunnerTest {

    @Test
    void shouldSkipMigrationWhenDatabaseConnectionIsUnavailableInDefaultMode() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        doThrow(new CannotGetJdbcConnectionException("connection failed", new SQLException("Access denied")))
                .when(jdbcTemplate)
                .execute(anyString());

        DatabaseMigrationRunner runner = new DatabaseMigrationRunner(jdbcTemplate, new MockEnvironment());

        assertThatCode(() -> runner.run(new DefaultApplicationArguments(new String[0]))).doesNotThrowAnyException();
        verify(jdbcTemplate).execute(anyString());
    }

    @Test
    void shouldStillFailWhenDatabaseConnectionIsUnavailableInProdMode() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        doThrow(new CannotGetJdbcConnectionException("connection failed", new SQLException("Access denied")))
                .when(jdbcTemplate)
                .execute(anyString());

        MockEnvironment environment = new MockEnvironment().withProperty("spring.profiles.active", "prod");
        DatabaseMigrationRunner runner = new DatabaseMigrationRunner(jdbcTemplate, environment);

        assertThatThrownBy(() -> runner.run(new DefaultApplicationArguments(new String[0])))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("数据库迁移失败");
    }
}
