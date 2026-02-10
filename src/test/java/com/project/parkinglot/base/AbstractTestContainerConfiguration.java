package com.project.parkinglot.base;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public abstract class AbstractTestContainerConfiguration {

    static MySQLContainer<?> MYSQL_CONTAINER = new MySQLContainer<>("mysql:8.0.33");
    static boolean CONTAINER_STARTED = false;

    @BeforeAll
    static void beforeAll() {
        // Allow forcing behavior via environment variable or system property.
        // If USE_TESTCONTAINERS is set to "false" (case-insensitive), skip testcontainers and use H2 fallback.
        String useTcEnv = System.getenv("USE_TESTCONTAINERS");
        String useTcProp = System.getProperty("use.testcontainers");
        boolean useTestcontainers = true;
        if (useTcEnv != null) {
            useTestcontainers = !"false".equalsIgnoreCase(useTcEnv.trim());
        } else if (useTcProp != null) {
            useTestcontainers = !"false".equalsIgnoreCase(useTcProp.trim());
        }

        if (!useTestcontainers) {
            CONTAINER_STARTED = false;
            System.out.println("[TestContainers] Disabled via USE_TESTCONTAINERS=false. Falling back to H2 in-memory for tests.");
            return;
        }

        try {
            MYSQL_CONTAINER.withReuse(true);
            MYSQL_CONTAINER.start();
            CONTAINER_STARTED = true;
            System.out.println("[TestContainers] MySQL container started: " + MYSQL_CONTAINER.getJdbcUrl());
        } catch (Exception e) {
            CONTAINER_STARTED = false;
            System.out.println("[TestContainers] Docker not available or failed to start containers. Falling back to H2 in-memory for tests. Reason: " + e.getMessage());
        }
    }

    @DynamicPropertySource
    private static void overrideProps(DynamicPropertyRegistry dynamicPropertyRegistry) {
        if (CONTAINER_STARTED) {
            dynamicPropertyRegistry.add("spring.datasource.username", MYSQL_CONTAINER::getUsername);
            dynamicPropertyRegistry.add("spring.datasource.password", MYSQL_CONTAINER::getPassword);
            dynamicPropertyRegistry.add("spring.datasource.url", MYSQL_CONTAINER::getJdbcUrl);
        } else {
            // Fall back to an in-memory H2 database when Docker/Testcontainers is not available.
            dynamicPropertyRegistry.add("spring.datasource.url", () -> "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1");
            dynamicPropertyRegistry.add("spring.datasource.username", () -> "sa");
            dynamicPropertyRegistry.add("spring.datasource.password", () -> "");
            dynamicPropertyRegistry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
            dynamicPropertyRegistry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        }
    }

}
