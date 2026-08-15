package io.github.leemmcc.hallpass

/**
 * Formats an elapsed duration for the yellow-state display.
 *
 * Minutes are deliberately unbounded: a student gone 72 minutes must read
 * "72:15", not "12:15". A wrapped number would be worse than useless in
 * exactly the situation that matters.
 */
object ElapsedFormat {

    fun format(millis: Long): String {
        val safe = if (millis < 0L) 0L else millis
        val totalSeconds = safe / 1000L
        return "%d:%02d".format(totalSeconds / 60L, totalSeconds % 60L)
    }
}
