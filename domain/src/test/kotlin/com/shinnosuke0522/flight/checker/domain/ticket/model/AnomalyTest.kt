package com.shinnosuke0522.flight.checker.domain.ticket.model

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class AnomalyTest : FunSpec({
    context("Anomalyの要約文字列(toSummary)が正しく生成されること") {
        test("欠航 (AnomalyCanceled)") {
            AnomalyCanceled.toSummary() shouldBe "CANCELED"
        }

        test("遅延 (AnomalyDelayed)") {
            val anomaly = AnomalyDelayed("2026-05-01T10:00:00Z")
            anomaly.toSummary() shouldBe "DELAYED:2026-05-01T10:00:00Z"
        }

        test("不確実 (AnomalyUncertain)") {
            val anomaly = AnomalyUncertain("Weather condition")
            anomaly.toSummary() shouldBe "UNCERTAIN:Weather condition"
        }
    }
})
