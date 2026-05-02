package com.shinnosuke0522.flight.checker.domain.base.error

/**
 * 具体的な標準バリデーションエラーの実装。
 */

data class CannotBeBlankError(
    override val valueName: String,
) : ValueValidationError {
    override val value: String? = null
    override val cause: Error.Cause? = null
    override val message: String = "$valueName cannot be blank"
}

data class TooLongError(
    override val valueName: String,
    override val value: String,
    val maxLength: Int,
    override val cause: Error.Cause? = null
) : ValueValidationError {
    override val message: String =
        "$valueName must be at most $maxLength characters (was ${value.length})"
}

data class InvalidFormatError(
    override val valueName: String,
    override val value: String,
    val regex: Regex,
    override val cause: Error.Cause? = null
) : ValueValidationError {
    override val message: String =
        "$valueName has an invalid format: \"$value\" (expected: ${regex.pattern})"
}

data class InvalidValueError(
    override val valueName: String,
    override val value: String,
    override val cause: Error.Cause
) : ValueValidationError {
    override val message: String = "Invalid $valueName value: $value"
}

data class UnKnownValueError(
    override val valueName: String,
    override val value: String,
    override val cause: Error.Cause? = null
) : ValueValidationError {
    override val message: String = "Unknown $valueName value: $value"
}
