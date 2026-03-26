package com.github.shinnosuke0522.flight.checker

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import

@Import(TestcontainersConfiguration::class)
@SpringBootTest
class FlightCheckerApplicationTests {

	@Test
	fun contextLoads() {
	}

}
