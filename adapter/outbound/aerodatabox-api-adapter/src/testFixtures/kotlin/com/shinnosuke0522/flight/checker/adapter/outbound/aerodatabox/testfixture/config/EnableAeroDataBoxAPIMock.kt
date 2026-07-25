package com.shinnosuke0522.flight.checker.adapter.outbound.aerodatabox.testfixture.config

import com.shinnosuke0522.flight.checker.adapter.outbound.aerodatabox.config.AeroDataBoxAPIConstants
import org.springframework.context.annotation.Import
import org.springframework.test.context.TestPropertySource

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Import(AeroDataBoxMockConfig::class)
@TestPropertySource(
    properties = [
        "${AeroDataBoxAPIConstants.PREFIX}.enabled=true"
    ]
)
annotation class EnableAeroDataBoxAPIMock
