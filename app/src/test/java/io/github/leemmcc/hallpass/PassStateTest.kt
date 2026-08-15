package io.github.leemmcc.hallpass

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PassStateTest {

    private val now = 1_000_000L

    @Test
    fun bothTimestampsNullIsGreen() {
        assertEquals(PassState.GREEN, Pass.stateAt(now, null, null))
    }

    @Test
    fun studentOutWithNoCooldownIsYellow() {
        assertEquals(PassState.YELLOW, Pass.stateAt(now, now - 30_000L, null))
    }

    @Test
    fun liveCooldownIsRed() {
        assertEquals(PassState.RED, Pass.stateAt(now, null, now + 60_000L))
    }

    @Test
    fun cooldownEndBoundaryIsGreenNotRed() {
        // At exactly the end instant the cooldown is over.
        assertEquals(PassState.GREEN, Pass.stateAt(now, null, now))
    }

    @Test
    fun expiredCooldownIsGreen() {
        assertEquals(PassState.GREEN, Pass.stateAt(now, null, now - 1L))
    }

    @Test
    fun redOutranksYellowWhileCooldownIsLive() {
        // Both timestamps set: the cooldown wins.
        assertEquals(PassState.RED, Pass.stateAt(now, now - 300_000L, now + 60_000L))
    }

    // --- Transitions, and the invariant they exist to guarantee ---

    @Test
    fun goOutMarksStudentOutAndClearsAnyCooldown() {
        val t = Pass.goOut(now)
        assertEquals(now, t.outStartMillis)
        assertNull(t.cooldownEndMillis)
        assertEquals(PassState.YELLOW, Pass.stateAt(now, t.outStartMillis, t.cooldownEndMillis))
    }

    @Test
    fun returnStudentClearsOutStart() {
        // The invariant. If this ever returns non-null, the tablet strands
        // itself in YELLOW when the cooldown expires.
        val t = Pass.returnStudent(now, 5)
        assertNull(t.outStartMillis)
    }

    @Test
    fun returnStudentSetsCooldownEndFromMinutes() {
        val t = Pass.returnStudent(now, 5)
        assertEquals(now + 300_000L, t.cooldownEndMillis)
    }

    @Test
    fun returnStudentPutsUsInRedForTheWholeCooldown() {
        val t = Pass.returnStudent(now, 5)
        assertEquals(PassState.RED, Pass.stateAt(now, t.outStartMillis, t.cooldownEndMillis))
        assertEquals(
            PassState.RED,
            Pass.stateAt(now + 299_999L, t.outStartMillis, t.cooldownEndMillis)
        )
    }

    @Test
    fun expiringCooldownLandsOnGreenNotYellow() {
        // The regression this file exists to catch, tested end to end:
        // transition through returnStudent, then let the cooldown expire.
        val t = Pass.returnStudent(now, 5)
        assertEquals(
            PassState.GREEN,
            Pass.stateAt(now + 300_000L, t.outStartMillis, t.cooldownEndMillis)
        )
        assertEquals(
            PassState.GREEN,
            Pass.stateAt(now + 999_999L, t.outStartMillis, t.cooldownEndMillis)
        )
    }

    @Test
    fun resetReturnsToGreenFromAnywhere() {
        val t = Pass.reset()
        assertNull(t.outStartMillis)
        assertNull(t.cooldownEndMillis)
        assertEquals(PassState.GREEN, Pass.stateAt(now, t.outStartMillis, t.cooldownEndMillis))
    }

    @Test
    fun fullCycleGreenToYellowToRedToGreen() {
        var t = PassTimestamps(null, null)
        assertEquals(PassState.GREEN, Pass.stateAt(now, t.outStartMillis, t.cooldownEndMillis))

        t = Pass.goOut(now)
        assertEquals(PassState.YELLOW, Pass.stateAt(now + 60_000L, t.outStartMillis, t.cooldownEndMillis))

        t = Pass.returnStudent(now + 120_000L, 5)
        assertEquals(PassState.RED, Pass.stateAt(now + 120_000L, t.outStartMillis, t.cooldownEndMillis))

        assertEquals(
            PassState.GREEN,
            Pass.stateAt(now + 420_000L, t.outStartMillis, t.cooldownEndMillis)
        )
    }

    @Test
    fun elapsedIsZeroWhenNobodyIsOut() {
        assertEquals(0L, Pass.elapsedIn(now, null))
    }

    @Test
    fun elapsedCountsUpFromOutStart() {
        assertEquals(90_000L, Pass.elapsedIn(now, now - 90_000L))
    }

    @Test
    fun elapsedClampsToZeroIfClockMovedBackwards() {
        assertEquals(0L, Pass.elapsedIn(now, now + 5_000L))
    }

    // --- The tap guard's own clock reading ---
    //
    // guardElapsedIn exists because elapsedIn's clamp is right for the display
    // and wrong for the guard: a clamped 0 would hold the guard shut for the
    // whole of a backwards clock jump and leave the return tap inert.

    @Test
    fun guardElapsedIsUnboundedWhenNobodyIsOut() {
        assertEquals(Long.MAX_VALUE, Pass.guardElapsedIn(now, null))
    }

    @Test
    fun guardElapsedCountsUpFromOutStart() {
        assertEquals(90_000L, Pass.guardElapsedIn(now, now - 90_000L))
    }

    @Test
    fun guardElapsedTreatsBackwardsClockAsGuardAlreadySatisfied() {
        // Unlike elapsedIn, which clamps this to 0.
        assertEquals(Long.MAX_VALUE, Pass.guardElapsedIn(now, now + 5_000L))
        assertEquals(0L, Pass.elapsedIn(now, now + 5_000L))
    }

    @Test
    fun guardElapsedStillGatesTheReturnTapAtTwoSeconds() {
        assertEquals(
            TouchAction.IGNORE,
            TouchRouter.route(
                inCorner = false,
                heldMillis = 10L,
                state = PassState.YELLOW,
                millisSinceChange = Pass.guardElapsedIn(now, now - 1_999L),
                tapGuardMillis = 2_000L
            )
        )
        assertEquals(
            TouchAction.RETURN,
            TouchRouter.route(
                inCorner = false,
                heldMillis = 10L,
                state = PassState.YELLOW,
                millisSinceChange = Pass.guardElapsedIn(now, now - 2_000L),
                tapGuardMillis = 2_000L
            )
        )
    }

    @Test
    fun backwardsClockLeavesTheReturnTapWorking() {
        // The bug this function exists to prevent: yellow running forever
        // because every tap is swallowed by the guard.
        assertEquals(
            TouchAction.RETURN,
            TouchRouter.route(
                inCorner = false,
                heldMillis = 10L,
                state = PassState.YELLOW,
                millisSinceChange = Pass.guardElapsedIn(now, now + 600_000L),
                tapGuardMillis = 2_000L
            )
        )
    }

    // --- millisSinceLastChange: the tap guard's clock reading after any
    // state change, not just entry into YELLOW ---

    @Test
    fun millisSinceLastChangeIsUnboundedWhenBothTimestampsAreNull() {
        assertEquals(Long.MAX_VALUE, Pass.millisSinceLastChange(now, null, null))
    }

    @Test
    fun millisSinceLastChangeReadsOutStartWhenSet() {
        assertEquals(
            30_000L,
            Pass.millisSinceLastChange(now, now - 30_000L, null)
        )
    }

    @Test
    fun millisSinceLastChangeReadsCooldownEndWhenOutStartIsNull() {
        // GREEN just arrived: outStart is null, cooldownEnd is in the past.
        assertEquals(
            5_000L,
            Pass.millisSinceLastChange(now, null, now - 5_000L)
        )
    }

    @Test
    fun millisSinceLastChangePrefersOutStartWhenBothAreSet() {
        assertEquals(
            10_000L,
            Pass.millisSinceLastChange(now, now - 10_000L, now - 999_999L)
        )
    }

    @Test
    fun millisSinceLastChangeTreatsBackwardsClockAsNoRecentChange() {
        assertEquals(Long.MAX_VALUE, Pass.millisSinceLastChange(now, now + 5_000L, null))
        assertEquals(Long.MAX_VALUE, Pass.millisSinceLastChange(now, null, now + 5_000L))
    }
}
