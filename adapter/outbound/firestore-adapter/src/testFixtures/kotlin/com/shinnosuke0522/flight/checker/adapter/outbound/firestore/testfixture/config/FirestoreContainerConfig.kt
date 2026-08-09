package com.shinnosuke0522.flight.checker.adapter.outbound.firestore.testfixture.config

import com.google.cloud.NoCredentials
import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.FirestoreOptions
import org.koin.dsl.module
import org.testcontainers.containers.FirestoreEmulatorContainer
import org.testcontainers.utility.DockerImageName

object FirestoreContainerConfig {

    val container: FirestoreEmulatorContainer = FirestoreEmulatorContainer(
        DockerImageName.parse("gcr.io/google.com/cloudsdktool/cloud-sdk:emulators")
    )

    val testModule = module {
        single<Firestore> { createFirestore() }
    }

    init {
        container.start()
    }

    fun createFirestore(): Firestore {
        val options = FirestoreOptions.getDefaultInstance().toBuilder()
            .setHost(container.emulatorEndpoint)
            .setCredentials(NoCredentials.getInstance())
            .setProjectId("test-project")
            .build()
        return options.service
    }
}
