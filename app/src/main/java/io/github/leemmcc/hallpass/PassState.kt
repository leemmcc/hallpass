package io.github.leemmcc.hallpass

enum class PassState { GREEN, YELLOW, RED }

/** The two persisted timestamps that all state is derived from. */
data class PassTimestamps(val outStartMillis: Long?, val cooldownEndMillis: Long?)

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

    /** GREEN -> YELLOW. A student has left. */
    fun goOut(nowMillis: Long): PassTimestamps =
        PassTimestamps(outStartMillis = nowMillis, cooldownEndMillis = null)

    /**
     * YELLOW -> RED. A student has returned and the cooldown begins.
     *
     * Clearing outStartMillis here is load-bearing: if it survived, the
     * expiring cooldown would fall back to YELLOW instead of GREEN and strand
     * the tablet in a state only the settings screen could clear.
     */
    fun returnStudent(nowMillis: Long, cooldownMinutes: Int): PassTimestamps =
        PassTimestamps(
            outStartMillis = null,
            cooldownEndMillis = nowMillis + cooldownMinutes * 60_000L
        )

    /** Teacher escape hatch: back to GREEN from any state. */
    fun reset(): PassTimestamps =
        PassTimestamps(outStartMillis = null, cooldownEndMillis = null)
}
