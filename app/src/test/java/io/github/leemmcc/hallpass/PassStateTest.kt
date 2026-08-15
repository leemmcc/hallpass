package io.github.leemmcc.hallpass

import org.junit.Assert.assertEquals
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

    @Test
    fun expiringCooldownFallsThroughToGreenNotBackToYellow() {
        // This is the regression this whole file exists to catch. Entering RED
        // is supposed to clear outStart; if it ever fails to, an expiring
        // cooldown would drop back into YELLOW and strand the tablet forever.
        assertEquals(PassState.GREEN, Pass.stateAt(now, null, now - 1L))
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
}
