package com.zs.ytbx.common.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class AuthTokenServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final AuthTokenService authTokenService = new AuthTokenService(objectMapper, "test-secret-key");

    @Test
    void shouldIssueUniqueTokensConcurrently() throws Exception {
        ExecutorService executorService = Executors.newFixedThreadPool(8);
        CountDownLatch startLatch = new CountDownLatch(1);
        List<Future<String>> futures = new ArrayList<>();

        for (int i = 0; i < 50; i++) {
            final int index = i;
            Callable<String> task = () -> {
                startLatch.await(3, TimeUnit.SECONDS);
                return authTokenService.issueToken((long) index, "user" + index, "USER");
            };
            futures.add(executorService.submit(task));
        }

        startLatch.countDown();

        Set<String> tokens = new HashSet<>();
        for (Future<String> future : futures) {
            tokens.add(future.get(5, TimeUnit.SECONDS));
        }

        executorService.shutdown();

        assertThat(tokens).hasSize(50);
    }

    @Test
    void shouldReturnUserAndRejectRevokedToken() {
        String token = authTokenService.issueToken(1L, "admin", "ADMIN");

        SessionUser user = authTokenService.getUser(token);
        assertThat(user).isNotNull();
        assertThat(user.getUserId()).isEqualTo(1L);
        assertThat(user.getUsername()).isEqualTo("admin");
        assertThat(user.getUserType()).isEqualTo("ADMIN");

        authTokenService.revoke(token);

        assertThat(authTokenService.getUser(token)).isNull();
    }

    @Test
    void shouldRejectTamperedToken() {
        String token = authTokenService.issueToken(7L, "user7", "USER");
        String tamperedToken = token.substring(0, token.length() - 1) + "A";

        assertThat(authTokenService.getUser(tamperedToken)).isNull();
    }
}
