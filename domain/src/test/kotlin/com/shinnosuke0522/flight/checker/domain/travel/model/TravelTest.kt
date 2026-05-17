package com.shinnosuke0522.flight.checker.domain.travel.model

import arrow.core.nonEmptyListOf
import com.shinnosuke0522.flight.checker.domain.base.event.CorrelationId
import com.shinnosuke0522.flight.checker.domain.base.event.DomainEventId
import com.shinnosuke0522.flight.checker.domain.base.event.DomainEventMeta
import com.shinnosuke0522.flight.checker.domain.base.model.AggregateVersion
import com.shinnosuke0522.flight.checker.domain.shared.primitive.FlightIdentity
import com.shinnosuke0522.flight.checker.domain.travel.error.FlightDateOutsideScheduleError
import com.shinnosuke0522.flight.checker.domain.travel.error.TravelAlreadyCanceled
import com.shinnosuke0522.flight.checker.domain.travel.error.TravelAlreadyCompleted
import com.shinnosuke0522.flight.checker.domain.travel.error.TravelAlreadyStartedError
import com.shinnosuke0522.flight.checker.domain.travel.error.TravelNotStartedError
import com.shinnosuke0522.flight.checker.domain.travel.event.FlightSegmentAdded
import com.shinnosuke0522.flight.checker.domain.travel.event.FlightSegmentChangeRequired
import com.shinnosuke0522.flight.checker.domain.travel.event.FlightSegmentDisrupted
import com.shinnosuke0522.flight.checker.domain.travel.event.FlightSegmentRemoved
import com.shinnosuke0522.flight.checker.domain.travel.event.TravelCanceled
import com.shinnosuke0522.flight.checker.domain.travel.event.TravelCompleted
import com.shinnosuke0522.flight.checker.domain.travel.event.TravelPlanned
import com.shinnosuke0522.flight.checker.domain.travel.event.TravelStarted
import com.shinnosuke0522.flight.checker.domain.travel.service.TravelFlightService
import com.shinnosuke0522.flight.checker.domain.travel.service.TravelLifecycleService
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import java.time.Instant
import java.time.LocalDate

class TravelTest : DescribeSpec({
    describe("ドメインの振る舞い") {
        context("Given: 旅行スケジュール期間外のフライトが含まれている場合") {
            val departureDate = LocalDate.of(2026, 5, 1)
            val returnDate = LocalDate.of(2026, 5, 5)
            val flightSegments = nonEmptyListOf(
                FlightSegment(
                    FlightIdentity.create("JL123", LocalDate.of(2026, 5, 6)).shouldBeRight()
                )
            )

            context("When: 旅行情報の整合性を検証（オブジェクト生成）しようとすると") {
                val result = Travel(
                    id = TravelId.generate(),
                    version = AggregateVersion(),
                    name = TravelName("Trip").shouldBeRight(),
                    schedule = RoundTripSchedule(departureDate, returnDate).shouldBeRight(),
                    flights = Flights(flightSegments),
                    status = TravelStatus.PLANNED
                )

                it("Then: スケジュール不整合 (FlightDateOutsideScheduleError) として拒絶されること") {
                    result.shouldBeLeft().any { it is FlightDateOutsideScheduleError } shouldBe true
                }
            }
        }

        context("Given: 計画済み (PLANNED) の旅行がある場合") {
            val (_, travel) = createSampleTravel()
            val now = Instant.now()

            context("When: 旅行を開始すると") {
                val result = TravelLifecycleService.start(travel, now)

                it("Then: 成功し、ステータスが開始済み (STARTED) になり、開始イベントが発行されること") {
                    val (updatedTravel, event) = result.shouldBeRight()
                    updatedTravel.status shouldBe TravelStatus.STARTED
                    updatedTravel.version shouldBe travel.version.nextVersion()
                    event.aggregateId shouldBe travel.id
                    event.sequenceNumber shouldBe updatedTravel.version.value
                }
            }

            context("When: 旅行を完了させようとすると") {
                val result = TravelLifecycleService.complete(travel, now)

                it("Then: 開始されていないエラー (TravelNotStartedError) が返されること") {
                    result shouldBeLeft TravelNotStartedError(travel.id)
                }
            }

            context("When: 旅行をキャンセルすると") {
                val result = TravelLifecycleService.cancel(travel, now)

                it("Then: 成功し、ステータスがキャンセル (CANCELED) になり、キャンセルイベントが発行されること") {
                    val (updatedTravel, event) = result.shouldBeRight()
                    updatedTravel.status shouldBe TravelStatus.CANCELED
                    updatedTravel.version shouldBe travel.version.nextVersion()
                    event.aggregateId shouldBe travel.id
                    event.sequenceNumber shouldBe updatedTravel.version.value
                }
            }

            context("When: 新しいフライトを追加すると") {
                val flightIdentity = FlightIdentity.create("NH999", travel.schedule.departureDate).shouldBeRight()
                val result = TravelFlightService.addFlight(travel, flightIdentity, now)

                it("Then: 成功し、フライトが追加され、フライト追加イベントが発行されること") {
                    val (updatedTravel, event) = result.shouldBeRight()
                    updatedTravel.flights.flightSegments.any { it.identity == flightIdentity } shouldBe true
                    updatedTravel.version shouldBe travel.version.nextVersion()
                    event.flightIdentity shouldBe flightIdentity
                }
            }

            context("When: 既存のフライトを削除すると") {
                val flightIdentity = travel.flights.flightSegments.head.identity
                val result = TravelFlightService.removeFlight(travel, flightIdentity, now)

                it("Then: 削除後のフライトが0件になる場合はエラーとなること（NonEmptyListの制約）") {
                    result.shouldBeLeft()
                }
            }
        }

        context("Given: すでに開始されている (STARTED) 旅行がある場合") {
            val (_, initialTravel) = createSampleTravel()
            val now = Instant.now()
            val (startedTravel, _) = TravelLifecycleService.start(initialTravel, now).shouldBeRight()

            context("When: 再度開始しようとすると") {
                val result = TravelLifecycleService.start(startedTravel, now)

                it("Then: 旅行開始済みエラー (TravelAlreadyStartedError) が返されること") {
                    result shouldBeLeft TravelAlreadyStartedError(startedTravel.id)
                }
            }

            context("When: 旅行を完了させると") {
                val result = TravelLifecycleService.complete(startedTravel, now)

                it("Then: 成功し、ステータスが完了 (COMPLETED) になり、完了イベントが発行されること") {
                    val (updatedTravel, event) = result.shouldBeRight()
                    updatedTravel.status shouldBe TravelStatus.COMPLETED
                    updatedTravel.version shouldBe startedTravel.version.nextVersion()
                    event.aggregateId shouldBe startedTravel.id
                    event.sequenceNumber shouldBe updatedTravel.version.value
                }
            }

            context("When: 旅行をキャンセルすると") {
                val result = TravelLifecycleService.cancel(startedTravel, now)

                it("Then: 成功し、ステータスがキャンセル (CANCELED) になり、キャンセルイベントが発行されること") {
                    val (updatedTravel, event) = result.shouldBeRight()
                    updatedTravel.status shouldBe TravelStatus.CANCELED
                    updatedTravel.version shouldBe startedTravel.version.nextVersion()
                    event.aggregateId shouldBe startedTravel.id
                    event.sequenceNumber shouldBe updatedTravel.version.value
                }
            }

            context("When: フライトを異常状態 (DISRUPTED) としてマークすると") {
                val flightIdentity = startedTravel.flights.flightSegments.head.identity
                val reason = "Weather conditions"
                val result = TravelFlightService.markFlightDisrupted(startedTravel, flightIdentity, reason, now)

                it("Then: 成功し、当該フライトの状態が DISRUPTED に更新されること") {
                    val (updatedTravel, event) = result.shouldBeRight()
                    updatedTravel.flights.flightSegments.find { it.identity == flightIdentity }?.status shouldBe FlightSegmentStatus.DISRUPTED
                    event.reason shouldBe reason
                }
            }

            context("When: フライトを要変更 (CHANGE_REQUIRED) としてマークすると") {
                val flightIdentity = startedTravel.flights.flightSegments.head.identity
                val result = TravelFlightService.markFlightChangeRequired(startedTravel, flightIdentity, now)

                it("Then: 成功し、当該フライトの状態が CHANGE_REQUIRED に更新されること") {
                    val (updatedTravel, _) = result.shouldBeRight()
                    updatedTravel.flights.flightSegments.find { it.identity == flightIdentity }?.status shouldBe FlightSegmentStatus.CHANGE_REQUIRED
                }
            }
        }

        context("Given: すでにキャンセルされている (CANCELED) 旅行がある場合") {
            val (_, initialTravel) = createSampleTravel()
            val now = Instant.now()
            val (canceledTravel, _) = TravelLifecycleService.cancel(initialTravel, now).shouldBeRight()

            context("When: 開始しようとすると") {
                val result = TravelLifecycleService.start(canceledTravel, now)

                it("Then: すでにキャンセル済みエラー (TravelAlreadyCanceled) が返されること") {
                    result shouldBeLeft TravelAlreadyCanceled(canceledTravel.id)
                }
            }

            context("When: 完了させようとすると") {
                val result = TravelLifecycleService.complete(canceledTravel, now)

                it("Then: すでにキャンセル済みエラー (TravelAlreadyCanceled) が返されること") {
                    result shouldBeLeft TravelAlreadyCanceled(canceledTravel.id)
                }
            }

            context("When: 再度キャンセルしようとすると") {
                val result = TravelLifecycleService.cancel(canceledTravel, now)

                it("Then: すでにキャンセル済みエラー (TravelAlreadyCanceled) が返されること") {
                    result shouldBeLeft TravelAlreadyCanceled(canceledTravel.id)
                }
            }
        }

        context("Given: すでに完了している (COMPLETED) 旅行がある場合") {
            val (_, initialTravel) = createSampleTravel()
            val now = Instant.now()
            val (startedTravel, _) = TravelLifecycleService.start(initialTravel, now).shouldBeRight()
            val (completedTravel, _) = TravelLifecycleService.complete(startedTravel, now).shouldBeRight()

            context("When: 開始しようとすると") {
                val result = TravelLifecycleService.start(completedTravel, now)

                it("Then: すでに完了済みエラー (TravelAlreadyCompleted) が返されること") {
                    result shouldBeLeft TravelAlreadyCompleted(completedTravel.id)
                }
            }

            context("When: 再度完了させようとすると") {
                val result = TravelLifecycleService.complete(completedTravel, now)

                it("Then: すでに完了済みエラー (TravelAlreadyCompleted) が返されること") {
                    result shouldBeLeft TravelAlreadyCompleted(completedTravel.id)
                }
            }

            context("When: キャンセルしようとすると") {
                val result = TravelLifecycleService.cancel(completedTravel, now)

                it("Then: すでに完了済みエラー (TravelAlreadyCompleted) が返されること") {
                    result shouldBeLeft TravelAlreadyCompleted(completedTravel.id)
                }
            }
        }
    }

    describe("状態の復元") {
        context("Given: 旅行計画が作成された場合") {
            val travelId = TravelId.generate()
            val name = TravelName("Test Trip").shouldBeRight()
            val schedule = OneWayTripSchedule(LocalDate.of(2026, 5, 1))
            val flightSegments = nonEmptyListOf(
                FlightSegment(
                    FlightIdentity.create("JL123", LocalDate.of(2026, 5, 1)).shouldBeRight()
                )
            )
            val flights = Flights(flightSegments)

            val event = TravelPlanned(
                id = DomainEventId.generate(),
                aggregateId = travelId,
                sequenceNumber = 0,
                meta = DomainEventMeta(Instant.now(), CorrelationId.generate()),
                name = name,
                schedule = schedule,
                flights = flights
            )

            context("When: 計画イベントを適用して状態を復元すると") {
                val travel = Travel.replay(nonEmptyListOf(event))

                it("Then: 計画済み (PLANNED) ステータスとして正しく再構築されること") {
                    travel.id shouldBe travelId
                    travel.name shouldBe name
                    travel.schedule shouldBe schedule
                    travel.flights shouldBe flights
                    travel.status shouldBe TravelStatus.PLANNED
                    travel.version shouldBe AggregateVersion(0)
                }
            }
        }

        context("Given: 計画済みの旅行が開始された場合") {
            val (plannedEvent, initialTravel) = createSampleTravel()
            val startedEvent = TravelStarted(
                id = DomainEventId.generate(),
                aggregateId = initialTravel.id,
                sequenceNumber = 1,
                meta = DomainEventMeta(Instant.now(), CorrelationId.generate())
            )

            context("When: 開始イベントを含めて状態を復元すると") {
                val updatedTravel = Travel.replay(nonEmptyListOf(plannedEvent, startedEvent))

                it("Then: ステータスが開始済み (STARTED) に更新されること") {
                    updatedTravel.status shouldBe TravelStatus.STARTED
                    updatedTravel.version shouldBe AggregateVersion(1)
                }
            }
        }

        context("Given: 計画から開始まで複数の変更履歴がある場合") {
            val travelId = TravelId.generate()
            val name = TravelName("Multi Event Trip").shouldBeRight()
            val schedule = OneWayTripSchedule(LocalDate.of(2026, 6, 1))
            val flightSegments = nonEmptyListOf(
                FlightSegment(
                    FlightIdentity.create("NH999", LocalDate.of(2026, 6, 1)).shouldBeRight()
                )
            )
            val flights = Flights(flightSegments)

            val plannedEvent = TravelPlanned(
                id = DomainEventId.generate(),
                aggregateId = travelId,
                sequenceNumber = 0,
                meta = DomainEventMeta(Instant.now(), CorrelationId.generate()),
                name = name,
                schedule = schedule,
                flights = flights
            )
            val startedEvent = TravelStarted(
                id = DomainEventId.generate(),
                aggregateId = travelId,
                sequenceNumber = 1,
                meta = DomainEventMeta(Instant.now(), CorrelationId.generate())
            )

            val events = nonEmptyListOf(plannedEvent, startedEvent)

            context("When: 全履歴を順に適用して状態を復元すると") {
                val replayedTravel = Travel.replay(events)

                it("Then: 最終的な事実がすべて反映された状態で復元されること") {
                    replayedTravel.id shouldBe travelId
                    replayedTravel.status shouldBe TravelStatus.STARTED
                    replayedTravel.version shouldBe AggregateVersion(1)
                    replayedTravel.name shouldBe name
                }
            }
        }
    }
})

private fun createSampleTravel(): Pair<TravelPlanned, Travel> {
    val departureDate = LocalDate.of(2026, 5, 1)
    val name = TravelName("Sample").shouldBeRight()
    val schedule = OneWayTripSchedule(departureDate)
    val flights = Flights(
        nonEmptyListOf(
            FlightSegment(
                FlightIdentity.create("JL123", departureDate).shouldBeRight()
            )
        )
    )
    val event = TravelPlanned(
        id = DomainEventId.generate(),
        aggregateId = TravelId.generate(),
        sequenceNumber = 0,
        meta = DomainEventMeta(Instant.now(), CorrelationId.generate()),
        name = name,
        schedule = schedule,
        flights = flights
    )
    return event to Travel.replay(nonEmptyListOf(event))
}
