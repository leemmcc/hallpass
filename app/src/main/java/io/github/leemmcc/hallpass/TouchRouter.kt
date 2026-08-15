package io.github.leemmcc.hallpass

enum class TouchAction { GO_OUT, RETURN, OPEN_SETTINGS, IGNORE }

/**
 * Decides what a touch means. Deliberately separate from MainActivity: the
 * activity measures where and how long, this decides the consequence, and
 * only this part needs tests.
 */
object TouchRouter {

    const val LONG_PRESS_MILLIS = 3_000L

    fun route(
        inCorner: Boolean,
        heldMillis: Long,
        state: PassState,
        millisSinceChange: Long,
        tapGuardMillis: Long
    ): TouchAction {
        // A completed long-press is consumed: it opens settings and never
        // also advances the state, or every trip to settings would send a
        // phantom student out of the room. It is never guarded -- settings
        // must stay reachable at all times.
        if (inCorner && heldMillis >= LONG_PRESS_MILLIS) return TouchAction.OPEN_SETTINGS

        if (millisSinceChange < tapGuardMillis) return TouchAction.IGNORE

        return when (state) {
            PassState.GREEN -> TouchAction.GO_OUT
            PassState.YELLOW -> TouchAction.RETURN
            PassState.RED -> TouchAction.IGNORE
        }
    }
}
