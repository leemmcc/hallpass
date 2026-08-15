package io.github.leemmcc.hallpass

/**
 * The invisible bottom-left hotspot that opens settings. Sized off the
 * shorter screen dimension so it stays roughly thumb-sized in either
 * orientation.
 */
object CornerTarget {

    const val FRACTION = 0.15f

    fun contains(x: Float, y: Float, width: Int, height: Int): Boolean {
        val side = FRACTION * minOf(width, height)
        return x <= side && y >= height - side
    }
}
