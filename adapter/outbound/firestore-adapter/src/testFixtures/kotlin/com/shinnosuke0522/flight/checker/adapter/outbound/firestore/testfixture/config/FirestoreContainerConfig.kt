package com.shinnosuke0522.flight.checker.adapter.outbound.firestore.testfixture.config

import com.shinnosuke0522.flight.checker.adapter.outbound.firestore.config.FirestoreProperties
import com.shinnosuke0522.flight.checker.common.gcp.config.GcpProps
import org.koin.dsl.module
import org.testcontainers.containers.FirestoreEmulatorContainer
import org.testcontainers.utility.DockerImageName

object FirestoreContainerConfig {

    val container: FirestoreEmulatorContainer = FirestoreEmulatorContainer(
        DockerImageName.parse("gcr.io/google.com/cloudsdktool/cloud-sdk:emulators")
    )

    val testModule = module {
        single<GcpProps> {
            GcpProps(
                enabled = false,
                projectId = "test-project",
            )
        }
        single<FirestoreProperties> {
            FirestoreProperties(
                emulatorHost = container.emulatorEndpoint
            )
        }
    }

    init {
        container.start()
    }
}
