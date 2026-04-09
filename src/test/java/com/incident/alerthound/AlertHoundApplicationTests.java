package com.incident.alerthound;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "alert-hound.kafka.admin.auto-create-topics=false",
        "spring.kafka.admin.auto-create=false",
        "spring.kafka.listener.auto-startup=false",
        "spring.datasource.url=jdbc:h2:mem:alerthound-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class AlertHoundApplicationTests {

    @Test
    void contextLoads() {
    }

}
