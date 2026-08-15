package io.github.leemmcc.hallpass

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CornerTargetTest {

    // Portrait 1080x1920: target side = 0.15 * 1080 = 162.
    // Bottom-left means x <= 162 and y >= 1920 - 162 = 1758.

    @Test
    fun bottomLeftCornerHits() {
        assertTrue(CornerTarget.contains(10f, 1910f, 1080, 1920))
    }

    @Test
    fun tooFarRightMisses() {
        assertFalse(CornerTarget.contains(200f, 1910f, 1080, 1920))
    }

    @Test
    fun tooHighUpMisses() {
        assertFalse(CornerTarget.contains(10f, 1700f, 1080, 1920))
    }

    @Test
    fun exactBoundaryHits() {
        assertTrue(CornerTarget.contains(162f, 1758f, 1080, 1920))
    }

    @Test
    fun justOutsideBoundaryMisses() {
        assertFalse(CornerTarget.contains(163f, 1758f, 1080, 1920))
        assertFalse(CornerTarget.contains(162f, 1757f, 1080, 1920))
    }

    @Test
    fun otherCornersMiss() {
        assertFalse(CornerTarget.contains(10f, 10f, 1080, 1920))       // top-left
        assertFalse(CornerTarget.contains(1070f, 10f, 1080, 1920))     // top-right
        assertFalse(CornerTarget.contains(1070f, 1910f, 1080, 1920))   // bottom-right
    }

    @Test
    fun landscapeUsesShorterDimension() {
        // Landscape 1920x1080: side = 0.15 * 1080 = 162, y >= 918.
        assertTrue(CornerTarget.contains(10f, 1070f, 1920, 1080))
        assertFalse(CornerTarget.contains(10f, 900f, 1920, 1080))
    }

    @Test
    fun centreOfScreenNeverHits() {
        assertFalse(CornerTarget.contains(540f, 960f, 1080, 1920))
    }
}
