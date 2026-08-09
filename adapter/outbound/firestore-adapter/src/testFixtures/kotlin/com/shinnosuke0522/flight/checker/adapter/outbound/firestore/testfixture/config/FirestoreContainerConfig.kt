package com.shinnosuke0522.flight.checker.adapter.outbound.firestore.testfixture.config

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.testcontainers.containers.FirestoreEmulatorContainer
import org.testcontainers.utility.DockerImageName

@TestConfiguration
class FirestoreContainerConfig {

    @Bean
    fun firestoreEmulatorContainer(): FirestoreEmulatorContainer = container

    companion object {
        val container: FirestoreEmulatorContainer = FirestoreEmulatorContainer(
            DockerImageName.parse("gcr.io/google.com/cloudsdktool/cloud-sdk:emulators")
        )

        init {
            container.start()
            System.setProperty("spring.cloud.gcp.firestore.emulator.enabled", "true")
            System.setProperty("spring.cloud.gcp.firestore.host-port", container.emulatorEndpoint)
            System.setProperty("spring.cloud.gcp.firestore.project-id", "test-project")
        }
    }
}
