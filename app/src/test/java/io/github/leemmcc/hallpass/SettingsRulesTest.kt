package io.github.leemmcc.hallpass

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsRulesTest {

    @Test
    fun zeroClampsUpToMinimum() {
        assertEquals(1, SettingsRules.clampDurationMinutes(0))
    }

    @Test
    fun negativeClampsUpToMinimum() {
        assertEquals(1, SettingsRules.clampDurationMinutes(-10))
    }

    @Test
    fun oversizedClampsDownToMaximum() {
        assertEquals(60, SettingsRules.clampDurationMinutes(999))
    }

    @Test
    fun inRangeValuesAreUnchanged() {
        assertEquals(1, SettingsRules.clampDurationMinutes(1))
        assertEquals(5, SettingsRules.clampDurationMinutes(5))
        assertEquals(60, SettingsRules.clampDurationMinutes(60))
    }

    @Test
    fun defaultIsFiveMinutes() {
        assertEquals(5, SettingsRules.DEFAULT_MINUTES)
    }

    @Test
    fun fourDigitPinIsValid() {
        assertTrue(SettingsRules.isValidPin("1234"))
        assertTrue(SettingsRules.isValidPin("0000"))
    }

    @Test
    fun wrongLengthPinIsInvalid() {
        assertFalse(SettingsRules.isValidPin("123"))
        assertFalse(SettingsRules.isValidPin("12345"))
        assertFalse(SettingsRules.isValidPin(""))
    }

    @Test
    fun nonNumericPinIsInvalid() {
        assertFalse(SettingsRules.isValidPin("12a4"))
        assertFalse(SettingsRules.isValidPin("abcd"))
        assertFalse(SettingsRules.isValidPin("12 4"))
    }

    @Test
    fun defaultPinIsFourDigitsAndValid() {
        assertTrue(SettingsRules.isValidPin(SettingsRules.DEFAULT_PIN))
    }

    @Test
    fun negativeTapGuardClampsUpToZero() {
        assertEquals(0, SettingsRules.clampTapGuardSeconds(-5))
    }

    @Test
    fun zeroTapGuardIsUnchanged() {
        // 0 is a legitimate value: it disables the guard.
        assertEquals(0, SettingsRules.clampTapGuardSeconds(0))
    }

    @Test
    fun inRangeTapGuardIsUnchanged() {
        assertEquals(10, SettingsRules.clampTapGuardSeconds(10))
    }

    @Test
    fun oversizedTapGuardClampsDownToMaximum() {
        assertEquals(60, SettingsRules.clampTapGuardSeconds(999))
    }

    @Test
    fun defaultTapGuardIsTenSeconds() {
        assertEquals(10, SettingsRules.DEFAULT_TAP_GUARD_SECONDS)
    }
}
