package com.shinnosuke0522.flight.checker.adapter.outbound.firestore.config

import com.google.cloud.NoCredentials
import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.FirestoreOptions
import com.shinnosuke0522.flight.checker.adapter.outbound.firestore.flight.FlightInfoEventFirestorePayloadCodec
import com.shinnosuke0522.flight.checker.adapter.outbound.firestore.flight.FlightInfoRepositoryFirestoreImpl
import com.shinnosuke0522.flight.checker.adapter.outbound.firestore.ticket.TicketEventFirestorePayloadCodec
import com.shinnosuke0522.flight.checker.adapter.outbound.firestore.ticket.TicketRepositoryFirestoreImpl
import com.shinnosuke0522.flight.checker.common.gcp.config.GcpProps
import com.shinnosuke0522.flight.checker.domain.flight.info.model.FlightInfoRepository
import com.shinnosuke0522.flight.checker.domain.flight.ticket.model.TicketRepository
import org.koin.dsl.module

val firestoreAdapterModule = module {
    single<Firestore> {
        val gcpProps = get<GcpProps>()

        if (!gcpProps.enabled) {
            val firestoreProps = get<FirestoreProperties>()
            // ローカル起動（エミュレータ）
            requireNotNull(firestoreProps.emulatorHost) { "emulatorHost must be provided when GCP is disabled." }
            FirestoreOptions.getDefaultInstance().toBuilder()
                .setProjectId(gcpProps.projectId)
                .setHost(firestoreProps.emulatorHost)
                .setCredentials(NoCredentials.getInstance())
                .build()
                .service
        } else {
            // 本番環境用 (GCP)
            FirestoreOptions.getDefaultInstance().toBuilder()
                .setProjectId(gcpProps.projectId)
                // credentials はデフォルト環境から自動取得される
                .build()
                .service
        }
    }

    single { FlightInfoEventFirestorePayloadCodec(get()) }
    single { TicketEventFirestorePayloadCodec(get()) }
    single<FlightInfoRepository> { FlightInfoRepositoryFirestoreImpl(get(), get()) }
    single<TicketRepository> { TicketRepositoryFirestoreImpl(get(), get()) }
}
