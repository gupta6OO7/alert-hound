package com.incident.alerthound;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "alert-hound.kafka.admin.auto-create-topics=false",
        "spring.kafka.admin.auto-create=false",
        "spring.kafka.listener.auto-startup=false"
})
class AlertHoundApplicationTests {

    @Test
    void contextLoads() {
    }

}
