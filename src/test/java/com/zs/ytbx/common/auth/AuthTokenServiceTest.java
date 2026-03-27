package com.zs.ytbx.common.auth;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

class AuthTokenServiceTest {

    @Test
    void shouldIssueUniqueTokensConcurrently() throws Exception {
        AuthTokenService authTokenService = new AuthTokenService();
        ExecutorService executorService = Executors.newFixedThreadPool(8);
        CountDownLatch startLatch = new CountDownLatch(1);
        List<Future<String>> futures = new ArrayList<>();

        for (int i = 0; i < 50; i++) {
            final int index = i;
            futures.add(executorService.submit(() -> {
                startLatch.await(3, TimeUnit.SECONDS);
                return authTokenService.issueToken((long) index, "user" + index, "USER");
            }));
        }

        startLatch.countDown();

        Set<String> tokens = new HashSet<>();
        for (Future<String> future : futures) {
            tokens.add(future.get(5, TimeUnit.SECONDS));
        }

        executorService.shutdown();

        assertThat(tokens).hasSize(50);
        for (String token : tokens) {
            assertThat(authTokenService.getUser(token)).isNotNull();
        }
    }

    @Test
    void shouldReturnNullAfterTokenRevoked() {
        AuthTokenService authTokenService = new AuthTokenService();
        String token = authTokenService.issueToken(1L, "admin", "ADMIN");

        authTokenService.revoke(token);

        assertThat(authTokenService.getUser(token)).isNull();
    }
}
