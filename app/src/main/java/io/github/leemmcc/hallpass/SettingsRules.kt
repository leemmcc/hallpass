package io.github.leemmcc.hallpass

object SettingsRules {

    const val MIN_MINUTES = 1
    const val MAX_MINUTES = 60
    const val DEFAULT_MINUTES = 5
    const val DEFAULT_PIN = "1234"

    const val MIN_TAP_GUARD_SECONDS = 0
    const val MAX_TAP_GUARD_SECONDS = 60
    const val DEFAULT_TAP_GUARD_SECONDS = 10

    fun clampDurationMinutes(value: Int): Int = value.coerceIn(MIN_MINUTES, MAX_MINUTES)

    fun clampTapGuardSeconds(value: Int): Int =
        value.coerceIn(MIN_TAP_GUARD_SECONDS, MAX_TAP_GUARD_SECONDS)

    fun isValidPin(pin: String): Boolean = pin.length == 4 && pin.all { it.isDigit() }
}
