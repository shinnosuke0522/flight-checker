package com.shinnosuke0522.flight.checker.domain.travel.model

import arrow.core.Either
import arrow.core.raise.either
import com.shinnosuke0522.flight.checker.domain.base.error.ValidationError
import com.shinnosuke0522.flight.checker.domain.shared.value.FlightIdentity
import java.time.LocalDate

/**
 * 旅程内における個別のフライトの状態。
 */
enum class FlightSegmentStatus {
    NORMAL,             // 正常
    DISRUPTED,          // 欠航・遅延等のトラブル発生
    CHANGE_REQUIRED,    // 代替便の確保など、ユーザーによる対応が必要な状態
}

data class FlightSegment (
    val identity: FlightIdentity,
    val status: FlightSegmentStatus = FlightSegmentStatus.NORMAL,
) {
    companion object {
        fun create(
            rawFlightCode: String,
            departureDate: LocalDate
        ): Either<ValidationError, FlightSegment> = either {
            FlightSegment(
                identity = FlightIdentity.create(
                    rawFlightCode = rawFlightCode,
                    departureDate = departureDate
                ).bind(),
                status = FlightSegmentStatus.NORMAL,
            )
        }
    }
}