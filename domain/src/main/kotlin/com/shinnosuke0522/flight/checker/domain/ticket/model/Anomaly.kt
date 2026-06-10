package com.shinnosuke0522.flight.checker.domain.ticket.model

/**
 * チケットが検知したフライトの異常（不整合）の詳細を表す値オブジェクト。
 */
sealed interface Anomaly {
    /** 同一性を判断するための要約文字列（ロギングやデバッグ用） */
    fun toSummary(): String
}

/** 欠航 */
data object AnomalyCanceled : Anomaly {
    override fun toSummary(): String = "CANCELED"
}

/** 遅延 */
data class AnomalyDelayed(
    val estimatedDepartureTime: String // 時刻の文字列表記（ISO-8601など）
) : Anomaly {
    override fun toSummary(): String = "DELAYED:$estimatedDepartureTime"
}

/** 動静不明・不確実 */
data class AnomalyUncertain(
    val reason: String
) : Anomaly {
    override fun toSummary(): String = "UNCERTAIN:$reason"
}
