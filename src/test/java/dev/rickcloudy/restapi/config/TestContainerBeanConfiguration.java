package dev.rickcloudy.restapi.config;

import dev.rickcloudy.restapi.repository.UserRepositoryTest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.ClassRule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.devtools.restart.RestartScope;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.r2dbc.connection.init.DatabasePopulator;
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.MySQLR2DBCDatabaseContainer;

@TestConfiguration(proxyBeanMethods = false)
public class TestContainerBeanConfiguration {
    private static final Logger log = LogManager.getLogger(TestContainerBeanConfiguration.class);

    @Value("classpath:sql/drop_all_table.sql")
    private Resource dropAllTablesScript;

    @ClassRule
    public static MySQLContainer<?> mySQLContainer() {
        return new MySQLContainer<>("mysql:8.0.36")
                .withDatabaseName("testDatabase")
                .withUsername("testUser")
                .withPassword("testSecret");
    }

    @DynamicPropertySource
    public static void postgresqlProperties(DynamicPropertyRegistry registry) {
        MySQLContainer<?> container = mySQLContainer();
        registry.add("spring.r2dbc.url", container::getJdbcUrl);
        registry.add("spring.r2dbc.username", container::getUsername);
        registry.add("spring.r2dbc.password", container::getPassword);
        registry.add("spring.flyway.url", container::getJdbcUrl);
        registry.add("spring.flyway.user", container::getUsername);
        registry.add("spring.flyway.password", container::getPassword);
    }
}
