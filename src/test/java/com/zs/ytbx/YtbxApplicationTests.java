package com.zs.ytbx;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@Disabled("依赖外部数据库，CI 中由控制器测试覆盖主要行为")
@SpringBootTest
class YtbxApplicationTests {

    @Test
    void contextLoads() {
    }

}
