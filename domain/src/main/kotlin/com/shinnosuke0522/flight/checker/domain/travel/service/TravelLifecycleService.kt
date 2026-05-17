package com.shinnosuke0522.flight.checker.domain.travel.service

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import com.shinnosuke0522.flight.checker.domain.base.event.DomainEventId
import com.shinnosuke0522.flight.checker.domain.base.event.DomainEventMeta
import com.shinnosuke0522.flight.checker.domain.travel.error.TravelAlreadyCanceled
import com.shinnosuke0522.flight.checker.domain.travel.error.TravelAlreadyCompleted
import com.shinnosuke0522.flight.checker.domain.travel.error.TravelAlreadyStartedError
import com.shinnosuke0522.flight.checker.domain.travel.error.TravelBusinessRuleError
import com.shinnosuke0522.flight.checker.domain.travel.error.TravelNotStartedError
import com.shinnosuke0522.flight.checker.domain.travel.event.TravelCanceled
import com.shinnosuke0522.flight.checker.domain.travel.event.TravelCompleted
import com.shinnosuke0522.flight.checker.domain.travel.event.TravelStarted
import com.shinnosuke0522.flight.checker.domain.travel.model.Travel
import com.shinnosuke0522.flight.checker.domain.travel.model.TravelStatus
import java.time.Instant

object TravelLifecycleService {
    fun start(
        travel: Travel,
        occurredAt: Instant
    ): Either<TravelBusinessRuleError, Pair<Travel, TravelStarted>> = either {
        travel.ensureUpdatable().bind()
        ensure(travel.status != TravelStatus.STARTED) { TravelAlreadyStartedError(travel.id) }
        val event = TravelStarted(
            id = DomainEventId.generate(),
            aggregateId = travel.id,
            sequenceNumber = travel.version.nextVersion().value,
            meta = DomainEventMeta.forRootEvent { occurredAt }
        )
        val newState = travel.apply(event)
        Pair(newState, event)
    }

    fun complete(
        travel: Travel,
        occurredAt: Instant
    ): Either<TravelBusinessRuleError, Pair<Travel, TravelCompleted>> = either {
        travel.ensureUpdatable().bind()
        ensure(travel.status != TravelStatus.PLANNED) { TravelNotStartedError(travel.id) }
        val event = TravelCompleted(
            id = DomainEventId.generate(),
            aggregateId = travel.id,
            sequenceNumber = travel.version.nextVersion().value,
            meta = DomainEventMeta.forRootEvent { occurredAt }
        )
        val newState = travel.apply(event)
        Pair(newState, event)
    }

    fun cancel(
        travel: Travel,
        occurredAt: Instant
    ): Either<TravelBusinessRuleError, Pair<Travel, TravelCanceled>> = either {
        travel.ensureUpdatable().bind()
        val event = TravelCanceled(
            id = DomainEventId.generate(),
            aggregateId = travel.id,
            sequenceNumber = travel.version.nextVersion().value,
            meta = DomainEventMeta.forRootEvent { occurredAt }
        )
        val newState = travel.apply(event)
        Pair(newState, event)
    }
}
