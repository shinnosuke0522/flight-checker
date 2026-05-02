package com.shinnosuke0522.flight.checker.domain.flight.model

/**
 * 監視（能動的追跡）の状態。
 */
enum class MonitoringStatus {
    IDLE,       // 監視前
    ACTIVATED,  // 監視中（有効化済み）
    COMPLETED,  // 監視正常終了（完了済み）
    FAILED      // 監視失敗
}