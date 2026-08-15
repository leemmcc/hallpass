package io.github.leemmcc.hallpass

enum class PassState { GREEN, YELLOW, RED }

/**
 * The whole state machine. State is derived from two persisted timestamps
 * rather than stored, so a reboot mid-cooldown or mid-trip resumes correctly
 * instead of resetting.
 */
object Pass {

    fun stateAt(nowMillis: Long, outStartMillis: Long?, cooldownEndMillis: Long?): PassState =
        when {
            cooldownEndMillis != null && nowMillis < cooldownEndMillis -> PassState.RED
            outStartMillis != null -> PassState.YELLOW
            else -> PassState.GREEN
        }

    fun elapsedIn(nowMillis: Long, outStartMillis: Long?): Long {
        if (outStartMillis == null) return 0L
        val delta = nowMillis - outStartMillis
        return if (delta < 0L) 0L else delta
    }
}
