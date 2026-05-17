package com.shinnosuke0522.flight.checker.domain.base.error

/**
 * アプリケーション全体で扱うエラーの基底インターフェース。
 *
 * すべてのエラーは「例外として投げる」のではなく、
 * 値として扱い、呼び出し側で明示的にハンドリングすることを前提とする。
 *
 * - 想定内の失敗 → Error（この型）として返す
 * - 想定外の障害（バグ・インフラ障害） → Throwable
 *
 * cause はエラーの発生原因を保持するためのものであり、
 * 外部エラーや例外との関連付けに利用する。
 */
interface Error {
    val message: String
    val cause: Cause?

    val rootMessage: String
        get() = when (val c = cause) {
            is Cause.ErrorCause -> c.value.rootMessage
            is Cause.ThrowableCause -> c.value.message ?: c.value.toString()
            null -> message
        }

    fun toCause(): Cause.ErrorCause = Cause.ErrorCause(this)

    sealed interface Cause {
        /**
         * ドメインエラーやリモートエラーなど、
         * Errorとして表現された原因。
         */
        @JvmInline
        value class ErrorCause(val value: Error) : Cause

        /**
         * 例外（Throwable）を原因として保持する。
         *
         * 主にインフラ層や外部ライブラリからの例外をラップする用途。
         */
        @JvmInline
        value class ThrowableCause(val value: Throwable) : Cause
    }
}

/**
 * 外部システム（API / DB / SDK など）との通信・依存に起因するエラー。
 */
interface RemoteError : Error

/**
 * Throwable を Error.Cause として扱うための拡張関数。
 */
fun Throwable.toCause(): Error.Cause.ThrowableCause =
    Error.Cause.ThrowableCause(this)
