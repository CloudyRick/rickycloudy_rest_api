package dev.rickcloudy.restapi.config;

import org.flywaydb.core.Flyway;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlywayConfig {
    private final Flyway flyway;

    public FlywayConfig(Flyway flyway) {
        this.flyway = flyway;
    }

    @Bean
    public CommandLineRunner flywayRunner() {
        return args -> {
            flyway.baseline();
            flyway.migrate();
        };
    }
}
