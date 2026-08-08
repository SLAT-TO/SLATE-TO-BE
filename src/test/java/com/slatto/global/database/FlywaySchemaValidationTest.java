package com.slatto.global.database;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("flyway-ci")
@EnabledIfEnvironmentVariable(named = "FLYWAY_SCHEMA_VALIDATION", matches = "true")
class FlywaySchemaValidationTest {

	@Test
	void migratedSchemaMatchesJpaEntities() {
	}
}
