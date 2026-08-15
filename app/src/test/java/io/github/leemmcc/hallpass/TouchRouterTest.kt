package io.github.leemmcc.hallpass

import org.junit.Assert.assertEquals
import org.junit.Test

class TouchRouterTest {

    private val longHold = 3_000L
    private val shortTap = 100L
    private val settled = 10_000L   // comfortably past the tap guard

    // --- Long-press opens settings in every state, and changes nothing else ---

    @Test
    fun longPressInCornerOpensSettingsFromGreen() {
        assertEquals(
            TouchAction.OPEN_SETTINGS,
            TouchRouter.route(true, longHold, PassState.GREEN, settled)
        )
    }

    @Test
    fun longPressInCornerOpensSettingsFromYellow() {
        assertEquals(
            TouchAction.OPEN_SETTINGS,
            TouchRouter.route(true, longHold, PassState.YELLOW, settled)
        )
    }

    @Test
    fun longPressInCornerOpensSettingsFromRed() {
        // Settings must be reachable during a cooldown, or the teacher would
        // have to wait out the timer in order to change the timer.
        assertEquals(
            TouchAction.OPEN_SETTINGS,
            TouchRouter.route(true, longHold, PassState.RED, settled)
        )
    }

    @Test
    fun longPressOutsideCornerIsJustAnOrdinaryTap() {
        assertEquals(
            TouchAction.GO_OUT,
            TouchRouter.route(false, longHold, PassState.GREEN, settled)
        )
    }

    @Test
    fun holdJustShortOfThresholdIsNotALongPress() {
        assertEquals(
            TouchAction.GO_OUT,
            TouchRouter.route(true, 2_999L, PassState.GREEN, settled)
        )
    }

    @Test
    fun holdExactlyAtThresholdIsALongPress() {
        assertEquals(
            TouchAction.OPEN_SETTINGS,
            TouchRouter.route(true, 3_000L, PassState.GREEN, settled)
        )
    }

    // --- Ordinary taps drive the cycle ---

    @Test
    fun tapInGreenSendsStudentOut() {
        assertEquals(
            TouchAction.GO_OUT,
            TouchRouter.route(false, shortTap, PassState.GREEN, settled)
        )
    }

    @Test
    fun shortTapInCornerIsAnOrdinaryTapNotADeadZone() {
        assertEquals(
            TouchAction.GO_OUT,
            TouchRouter.route(true, shortTap, PassState.GREEN, settled)
        )
    }

    @Test
    fun tapInYellowAfterGuardReturnsStudent() {
        assertEquals(
            TouchAction.RETURN,
            TouchRouter.route(false, shortTap, PassState.YELLOW, settled)
        )
    }

    @Test
    fun tapInRedIsInert() {
        assertEquals(
            TouchAction.IGNORE,
            TouchRouter.route(false, shortTap, PassState.RED, settled)
        )
        assertEquals(
            TouchAction.IGNORE,
            TouchRouter.route(false, longHold, PassState.RED, settled)
        )
    }

    // --- The tap guard ---

    @Test
    fun tapOneSecondIntoYellowIsIgnored() {
        // The leaving student's finger is still on the screen.
        assertEquals(
            TouchAction.IGNORE,
            TouchRouter.route(false, shortTap, PassState.YELLOW, 1_000L)
        )
    }

    @Test
    fun tapThreeSecondsIntoYellowReturns() {
        assertEquals(
            TouchAction.RETURN,
            TouchRouter.route(false, shortTap, PassState.YELLOW, 3_000L)
        )
    }

    @Test
    fun guardBoundaryIsInclusive() {
        assertEquals(
            TouchAction.IGNORE,
            TouchRouter.route(false, shortTap, PassState.YELLOW, 1_999L)
        )
        assertEquals(
            TouchAction.RETURN,
            TouchRouter.route(false, shortTap, PassState.YELLOW, 2_000L)
        )
    }

    @Test
    fun longPressBeatsTheTapGuard() {
        // Settings must open even in the first two seconds of yellow.
        assertEquals(
            TouchAction.OPEN_SETTINGS,
            TouchRouter.route(true, longHold, PassState.YELLOW, 500L)
        )
    }
}
