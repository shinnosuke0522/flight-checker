package com.shinnosuke0522.flight.checker.adapter.outbound.firestore.config

import com.google.cloud.spring.autoconfigure.core.GcpContextAutoConfiguration
import com.google.cloud.spring.autoconfigure.firestore.GcpFirestoreAutoConfiguration
import com.shinnosuke0522.flight.checker.adapter.outbound.firestore.testfixture.config.FirestoreContainerConfig
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Import
import org.springframework.test.context.junit.jupiter.SpringExtension

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@ExtendWith(SpringExtension::class)
@Import(FirestoreContainerConfig::class, FirestoreAdapterConfig::class, DataFirestoreTestConfig::class)
@ImportAutoConfiguration(GcpFirestoreAutoConfiguration::class, GcpContextAutoConfiguration::class)
@ComponentScan(
    basePackages = ["com.shinnosuke0522.flight.checker.adapter.outbound.firestore"]
)
annotation class DataFirestoreTest
