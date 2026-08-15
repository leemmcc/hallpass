package io.github.leemmcc.hallpass

import org.junit.Assert.assertEquals
import org.junit.Test

class ElapsedFormatTest {

    @Test
    fun zeroIsZeroZeroZero() {
        assertEquals("0:00", ElapsedFormat.format(0))
    }

    @Test
    fun secondsArePaddedToTwoDigits() {
        assertEquals("0:07", ElapsedFormat.format(7_000))
    }

    @Test
    fun subSecondRemaindersAreTruncatedNotRounded() {
        assertEquals("0:00", ElapsedFormat.format(999))
        assertEquals("0:07", ElapsedFormat.format(7_999))
    }

    @Test
    fun rollsOverAtSixtySeconds() {
        assertEquals("0:59", ElapsedFormat.format(59_000))
        assertEquals("1:00", ElapsedFormat.format(60_000))
    }

    @Test
    fun minutesDoNotWrapPastAnHour() {
        // 72 minutes 15 seconds -- must read 72:15, never 12:15
        assertEquals("72:15", ElapsedFormat.format(72 * 60_000L + 15_000L))
    }

    @Test
    fun negativeInputClampsToZero() {
        assertEquals("0:00", ElapsedFormat.format(-5_000))
    }
}
