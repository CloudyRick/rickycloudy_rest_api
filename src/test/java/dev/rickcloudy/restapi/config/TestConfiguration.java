package dev.rickcloudy.restapi.config;

import io.r2dbc.spi.ConnectionFactory;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.r2dbc.connection.init.CompositeDatabasePopulator;
import org.springframework.r2dbc.connection.init.ConnectionFactoryInitializer;
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator;
import org.springframework.test.context.ActiveProfiles;
import org.apache.logging.log4j.LogManager;


@Configuration
@ActiveProfiles("test-container")
public class TestConfiguration {

    private final Logger log = LogManager.getLogger(TestConfiguration.class);
    @Bean
    public ConnectionFactoryInitializer initializer(ConnectionFactory connectionFactory) {
        ConnectionFactoryInitializer initializer = new ConnectionFactoryInitializer();
        initializer.setConnectionFactory(connectionFactory);
        log.debug("Connection factory {}", connectionFactory);
        CompositeDatabasePopulator populator = new CompositeDatabasePopulator();
        populator.addPopulators(new ResourceDatabasePopulator(new ClassPathResource("drop_all_table.sql")));
        initializer.setDatabasePopulator(populator);

        return initializer;
    }

}
