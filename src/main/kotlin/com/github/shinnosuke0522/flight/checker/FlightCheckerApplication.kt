package com.github.shinnosuke0522.flight.checker

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class FlightCheckerApplication

fun main(args: Array<String>) {
	runApplication<FlightCheckerApplication>(*args)
}
