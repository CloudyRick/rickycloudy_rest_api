package dev.rickcloudy.restapi.config;

import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test-container")
@Import(TestContainerBeanConfiguration.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public @interface UnitTestingContainerized {
}
