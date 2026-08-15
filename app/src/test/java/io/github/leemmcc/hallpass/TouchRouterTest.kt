package io.github.leemmcc.hallpass

import org.junit.Assert.assertEquals
import org.junit.Test

class TouchRouterTest {

    private val longHold = 3_000L
    private val shortTap = 100L
    private val settled = Long.MAX_VALUE  // comfortably past any tap guard
    private val guard = 10_000L           // an arbitrary guard, unrelated to the case under test

    // --- Long-press opens settings in every state, and changes nothing else ---

    @Test
    fun longPressInCornerOpensSettingsFromGreen() {
        assertEquals(
            TouchAction.OPEN_SETTINGS,
            TouchRouter.route(true, longHold, PassState.GREEN, settled, guard)
        )
    }

    @Test
    fun longPressInCornerOpensSettingsFromYellow() {
        assertEquals(
            TouchAction.OPEN_SETTINGS,
            TouchRouter.route(true, longHold, PassState.YELLOW, settled, guard)
        )
    }

    @Test
    fun longPressInCornerOpensSettingsFromRed() {
        // Settings must be reachable during a cooldown, or the teacher would
        // have to wait out the timer in order to change the timer.
        assertEquals(
            TouchAction.OPEN_SETTINGS,
            TouchRouter.route(true, longHold, PassState.RED, settled, guard)
        )
    }

    @Test
    fun longPressOutsideCornerIsJustAnOrdinaryTap() {
        assertEquals(
            TouchAction.GO_OUT,
            TouchRouter.route(false, longHold, PassState.GREEN, settled, guard)
        )
    }

    @Test
    fun holdJustShortOfThresholdIsNotALongPress() {
        assertEquals(
            TouchAction.GO_OUT,
            TouchRouter.route(true, 2_999L, PassState.GREEN, settled, guard)
        )
    }

    @Test
    fun holdExactlyAtThresholdIsALongPress() {
        assertEquals(
            TouchAction.OPEN_SETTINGS,
            TouchRouter.route(true, 3_000L, PassState.GREEN, settled, guard)
        )
    }

    // --- Ordinary taps drive the cycle ---

    @Test
    fun tapInGreenSendsStudentOut() {
        assertEquals(
            TouchAction.GO_OUT,
            TouchRouter.route(false, shortTap, PassState.GREEN, settled, guard)
        )
    }

    @Test
    fun shortTapInCornerIsAnOrdinaryTapNotADeadZone() {
        assertEquals(
            TouchAction.GO_OUT,
            TouchRouter.route(true, shortTap, PassState.GREEN, settled, guard)
        )
    }

    @Test
    fun tapInYellowAfterGuardReturnsStudent() {
        assertEquals(
            TouchAction.RETURN,
            TouchRouter.route(false, shortTap, PassState.YELLOW, settled, guard)
        )
    }

    @Test
    fun tapInRedIsInert() {
        assertEquals(
            TouchAction.IGNORE,
            TouchRouter.route(false, shortTap, PassState.RED, settled, guard)
        )
        assertEquals(
            TouchAction.IGNORE,
            TouchRouter.route(false, longHold, PassState.RED, settled, guard)
        )
    }

    // --- The tap guard, YELLOW (the original case: a returning student) ---

    @Test
    fun tapOneSecondIntoYellowIsIgnored() {
        // The leaving student's finger is still on the screen.
        assertEquals(
            TouchAction.IGNORE,
            TouchRouter.route(false, shortTap, PassState.YELLOW, 1_000L, 2_000L)
        )
    }

    @Test
    fun tapThreeSecondsIntoYellowReturns() {
        assertEquals(
            TouchAction.RETURN,
            TouchRouter.route(false, shortTap, PassState.YELLOW, 3_000L, 2_000L)
        )
    }

    @Test
    fun guardBoundaryIsInclusive() {
        assertEquals(
            TouchAction.IGNORE,
            TouchRouter.route(false, shortTap, PassState.YELLOW, 1_999L, 2_000L)
        )
        assertEquals(
            TouchAction.RETURN,
            TouchRouter.route(false, shortTap, PassState.YELLOW, 2_000L, 2_000L)
        )
    }

    @Test
    fun longPressBeatsTheTapGuard() {
        // Settings must open even in the first two seconds of yellow.
        assertEquals(
            TouchAction.OPEN_SETTINGS,
            TouchRouter.route(true, longHold, PassState.YELLOW, 500L, 2_000L)
        )
    }

    // --- The tap guard, GREEN (the bug this feature fixes: a stray tap on
    // a screen that just turned green from red must not start yellow) ---

    @Test
    fun greenBelowTheGuardIsIgnored() {
        assertEquals(
            TouchAction.IGNORE,
            TouchRouter.route(false, shortTap, PassState.GREEN, 9_999L, 10_000L)
        )
    }

    @Test
    fun greenAtTheGuardBoundaryGoesOut() {
        assertEquals(
            TouchAction.GO_OUT,
            TouchRouter.route(false, shortTap, PassState.GREEN, 10_000L, 10_000L)
        )
    }

    @Test
    fun yellowBelowTheGuardIsIgnoredAtTheBoundary() {
        assertEquals(
            TouchAction.IGNORE,
            TouchRouter.route(false, shortTap, PassState.YELLOW, 9_999L, 10_000L)
        )
    }

    @Test
    fun yellowAtTheGuardBoundaryReturns() {
        assertEquals(
            TouchAction.RETURN,
            TouchRouter.route(false, shortTap, PassState.YELLOW, 10_000L, 10_000L)
        )
    }

    @Test
    fun cornerLongPressBelowTheGuardStillOpensSettings() {
        // Settings must stay reachable during a lockout, or a mis-set
        // lockout could make the app unadministrable.
        assertEquals(
            TouchAction.OPEN_SETTINGS,
            TouchRouter.route(true, longHold, PassState.GREEN, 0L, 10_000L)
        )
    }

    @Test
    fun zeroGuardDisablesTheLockout() {
        // 0 is a legitimate value: it disables the guard entirely, so a tap
        // at the very instant of the state change is not ignored.
        assertEquals(
            TouchAction.GO_OUT,
            TouchRouter.route(false, shortTap, PassState.GREEN, 0L, 0L)
        )
    }
}
