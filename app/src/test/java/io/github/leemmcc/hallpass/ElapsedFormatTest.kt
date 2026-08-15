package io.github.leemmcc.hallpass

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class ElapsedFormatTest {

    @Test
    fun digitsAreAsciiUnderAnyDefaultLocale() {
        // ar-EG renders "%d" as Eastern Arabic numerals through the default
        // locale. The wall display must stay ASCII regardless of device locale.
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("ar-EG"))
            assertEquals("72:15", ElapsedFormat.format(72 * 60_000L + 15_000L))
            assertEquals("0:07", ElapsedFormat.format(7_000))
        } finally {
            Locale.setDefault(original)
        }
    }

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
