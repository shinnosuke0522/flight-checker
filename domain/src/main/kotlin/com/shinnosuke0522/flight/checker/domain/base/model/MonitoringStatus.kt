package com.shinnosuke0522.flight.checker.domain.base.model

/**
 * 監視（能動的追跡）の状態。
 */
enum class MonitoringStatus {
    IDLE,       // 監視前
    ACTIVE,     // 監視中
    STOPPED     // 監視停止
}
