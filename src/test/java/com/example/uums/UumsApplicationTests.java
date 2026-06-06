package com.example.uums;

/**
 * Spring Boot integration smoke test for UUMS.
 * Verifies the full application context (beans, config, repositories) loads
 * without errors — a basic sanity check that the app can start.
 */
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class UumsApplicationTests {

	@Test
	void contextLoads() {
	}

}
