package dev.rickcloudy.restapi.helper;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class FlywayRepairRunner implements CommandLineRunner {
    @Autowired
    private Flyway flyway;
    @Override
    public void run(String... args) throws Exception {
        flyway.baseline();
        flyway.repair();
    }
}
