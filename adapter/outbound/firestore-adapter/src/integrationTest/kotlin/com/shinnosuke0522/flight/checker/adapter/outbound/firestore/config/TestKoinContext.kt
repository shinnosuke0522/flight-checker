package com.shinnosuke0522.flight.checker.adapter.outbound.firestore.config

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.shinnosuke0522.flight.checker.adapter.outbound.firestore.testfixture.config.FirestoreContainerConfig
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.dsl.module

object TestKoinContext {
    fun start() {
        if (GlobalContext.getOrNull() == null) {
            startKoin {
                modules(
                    FirestoreContainerConfig.testModule,
                    firestoreAdapterModule,
                    module {
                        single { jacksonObjectMapper() }
                    }
                )
            }
        }
    }
}
