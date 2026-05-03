package com.shinnosuke0522.flight.checker.domain.travel.model

/**
 * 旅程内における個別のフライトの状態。
 */
enum class FlightSegmentStatus {
    NORMAL, // 正常
    DISRUPTED, // 欠航・遅延等のトラブル発生
    CHANGE_REQUIRED, // 代替便の確保など、ユーザーによる対応が必要な状態
}