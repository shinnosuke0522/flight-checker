package com.shinnosuke0522.flight.checker.domain.ticket.service

import arrow.core.Either
import arrow.core.nonEmptyListOf
import arrow.core.raise.either
import arrow.core.raise.ensure
import com.shinnosuke0522.flight.checker.domain.base.model.AggregateVersion
import com.shinnosuke0522.flight.checker.domain.base.model.DomainEventId
import com.shinnosuke0522.flight.checker.domain.base.model.DomainEventMeta
import com.shinnosuke0522.flight.checker.domain.shared.model.FlightIdentity
import com.shinnosuke0522.flight.checker.domain.ticket.model.Ticket
import com.shinnosuke0522.flight.checker.domain.ticket.model.TicketAlreadyRegisteredError
import com.shinnosuke0522.flight.checker.domain.ticket.model.TicketDuplicatePolicy
import com.shinnosuke0522.flight.checker.domain.ticket.model.TicketError
import com.shinnosuke0522.flight.checker.domain.ticket.model.TicketId
import com.shinnosuke0522.flight.checker.domain.ticket.model.TicketRegistered
import com.shinnosuke0522.flight.checker.domain.ticket.model.UserId
import java.time.Instant

/**
 * [Ticket] 集約の新規生成（登録）を担当するドメインサービス。
 */
object TicketFactory {

    /**
     * チケット登録のためのハンドラ（純粋関数）を生成する。
     *
     * @param duplicatePolicy 重複登録を許容するか判断するためのビジネスルール（外部依存）
     */
    fun makeRegisteredHandler(
        duplicatePolicy: TicketDuplicatePolicy
    ): (UserId, FlightIdentity, Instant) -> Either<TicketError, Pair<Ticket, TicketRegistered>> {
        return { userId, flightIdentity, occurredAt ->
            either {
                // ポリシーに基づき、ビジネスルール（重複禁止）を検証
                ensure(!duplicatePolicy.isDuplicate(userId, flightIdentity)) {
                    TicketAlreadyRegisteredError(userId, flightIdentity)
                }

                val event = TicketRegistered(
                    id = DomainEventId.generate(),
                    aggregateId = TicketId.generate(),
                    sequenceNumber = AggregateVersion().nextVersion().value,
                    meta = DomainEventMeta.forRootEvent { occurredAt },
                    userId = userId,
                    flightIdentity = flightIdentity
                )
                val ticket = Ticket.replay(nonEmptyListOf(event))
                Pair(ticket, event)
            }
        }
    }
}
