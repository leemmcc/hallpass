package io.github.leemmcc.hallpass

import android.app.Activity
import android.app.AlertDialog
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

    /**
     * Confirm, then leave the app entirely.
     *
     * Screen pinning blocks an app from closing itself, so unpinning has to
     * come first -- that is the whole point of the button, since the manual
     * route is Back+Overview followed by the device lock PIN.
     *
     * Note what this trades: exiting used to require the *device* PIN, which a
     * student is unlikely to know. It now requires the *app* PIN, so the app
     * is only as hard to close as that PIN is to guess.
     *
     * Confirmed first because a fumbled tap leaves the wall showing the
     * launcher instead of the pass, which is exactly the kind of thing that
     * goes unnoticed.
     */
    protected fun confirmAndExitApp() {
        // The idle timer has to be held off while the dialog is up. Touches on
        // a dialog land on its own window, not this activity's, so they never
        // reach onUserInteraction and never restart the timer -- leave it
        // running and a teacher interrupted mid-decision comes back to find
        // the question silently withdrawn.
        pauseIdleTimer()
        AlertDialog.Builder(this)
            .setTitle("Exit Hall Pass?")
            .setMessage("The pass display will close and the screen will be unpinned.")
            .setPositiveButton("Exit") { _, _ -> exitApp() }
            .setNegativeButton("Cancel", null)
            .setOnDismissListener { restartIdleTimer() }
            .show()
    }

    private fun exitApp() {
        // Throws if the task was never pinned; that is a fine reason to carry
        // on and close anyway.
        runCatching { stopLockTask() }
        // finishAffinity, not finish: MainActivity is still below this screen
        // in the task, and finishing only this one would drop straight back to
        // the pass display.
        finishAffinity()
    }

    private fun pauseIdleTimer() {
        handler.removeCallbacks(closeOnIdle)
    }

    private fun restartIdleTimer() {
        handler.removeCallbacks(closeOnIdle)
        handler.postDelayed(closeOnIdle, IDLE_TIMEOUT_MILLIS)
    }

    private companion object {
        const val IDLE_TIMEOUT_MILLIS = 60_000L
    }
}
