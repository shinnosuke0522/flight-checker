package com.github.shinnosuke0522.flight.checker

import org.springframework.boot.fromApplication
import org.springframework.boot.with


fun main(args: Array<String>) {
	fromApplication<FlightCheckerApplication>().with(TestcontainersConfiguration::class).run(*args)
}
