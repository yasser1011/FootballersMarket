package com.yasser.footballersmarket.testcotainer;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

import java.io.File;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
//@ActiveProfiles("integrationtest")
//@Testcontainers
public abstract class BaseIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withCopyFileToContainer(
                    MountableFile.forHostPath("src/test/resources/init.sql"),
                    "/docker-entrypoint-initdb.d/init.sql"
            );

    static {
        // 3. Manually starting the container in a static block instead of @Testcontainers annotation
        postgres.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Spring Boot 2.7.5 asks the container for its default credentials here:
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        registry.add("spring.jpa.show-sql", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        // tests must not depend on the developer's .env flags (seeder fills the shared db)
        registry.add("worldcup.seed", () -> "false");
        registry.add("logging.level.org.hibernate.SQL", () -> "DEBUG");
        registry.add("logging.level.org.hibernate.type.descriptor.sql.BasicBinder", () -> "TRACE");
    }
}