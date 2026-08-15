package io.github.leemmcc.hallpass

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager

/**
 * Shared lifecycle for the two screens behind the PIN gate.
 *
 * Neither may be left standing open on a wall-mounted tablet. If the teacher
 * is interrupted mid-change, an editable "Change PIN" field sits at child
 * height for the rest of the period, and a child who sets a PIN nobody knows
 * locks her out of her own tablet for good -- recovery means exiting screen
 * pinning with the device lock PIN and reinstalling. So these screens close
 * themselves both ways: when they stop being the foreground screen, and after
 * a minute with nobody touching them.
 *
 * They also hold the screen awake, which MainActivity does and these did not:
 * without it the tablet's ordinary timeout blanks the wall.
 */
abstract class AutoCloseActivity : Activity() {

    private val handler = Handler(Looper.getMainLooper())
    private val closeOnIdle = Runnable { finish() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onResume() {
        super.onResume()
        restartIdleTimer()
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        restartIdleTimer()
    }

    /**
     * onStop, not onPause. The screen-pinning confirmation is a system dialog
     * that pauses this activity without stopping it; finishing on pause would
     * tear down the screen underneath the dialog the teacher is answering.
     */
    override fun onStop() {
        super.onStop()
        handler.removeCallbacks(closeOnIdle)
        finish()
    }

    private fun restartIdleTimer() {
        handler.removeCallbacks(closeOnIdle)
        handler.postDelayed(closeOnIdle, IDLE_TIMEOUT_MILLIS)
    }

    private companion object {
        const val IDLE_TIMEOUT_MILLIS = 60_000L
    }
}
