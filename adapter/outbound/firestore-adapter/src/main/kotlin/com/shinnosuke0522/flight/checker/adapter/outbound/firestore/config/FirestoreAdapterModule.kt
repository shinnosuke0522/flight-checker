package com.shinnosuke0522.flight.checker.adapter.outbound.firestore.config

import com.shinnosuke0522.flight.checker.adapter.outbound.firestore.flight.FlightInfoEventFirestorePayloadCodec
import com.shinnosuke0522.flight.checker.adapter.outbound.firestore.flight.FlightInfoRepositoryFirestoreImpl
import com.shinnosuke0522.flight.checker.adapter.outbound.firestore.ticket.TicketEventFirestorePayloadCodec
import com.shinnosuke0522.flight.checker.adapter.outbound.firestore.ticket.TicketRepositoryFirestoreImpl
import com.shinnosuke0522.flight.checker.domain.flight.info.model.FlightInfoRepository
import com.shinnosuke0522.flight.checker.domain.flight.ticket.model.TicketRepository
import org.koin.dsl.module

val firestoreAdapterModule = module {
    single { FlightInfoEventFirestorePayloadCodec(get()) }
    single { TicketEventFirestorePayloadCodec(get()) }
    single<FlightInfoRepository> { FlightInfoRepositoryFirestoreImpl(get(), get()) }
    single<TicketRepository> { TicketRepositoryFirestoreImpl(get(), get()) }
}
