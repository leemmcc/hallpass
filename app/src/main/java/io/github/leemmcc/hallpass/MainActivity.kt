package io.github.leemmcc.hallpass

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager

class MainActivity : Activity() {

    private lateinit var passView: PassView
    private lateinit var settings: Settings

    private val handler = Handler(Looper.getMainLooper())

    /** Uptime, not wall clock -- see handleTouch. */
    private var touchDownAt = 0L
    private var touchDownInCorner = false

    private val tick = object : Runnable {
        override fun run() {
            refresh()
            handler.postDelayed(this, millisToNextTick())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings = Settings(this)
        passView = PassView(this)
        setContentView(passView)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        passView.setOnTouchListener { _, event -> handleTouch(event) }
    }

    override fun onResume() {
        super.onResume()
        applyImmersiveMode()
        if (settings.autoPin) requestLockTask()
        handler.post(tick)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(tick)
        clearTouch()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) applyImmersiveMode()
    }

    private fun refresh() {
        val now = System.currentTimeMillis()
        settings.clearExpiredCooldown(now)
        val state = Pass.stateAt(now, settings.outStartMillis, settings.cooldownEndMillis)
        val elapsed = ElapsedFormat.format(Pass.elapsedIn(now, settings.outStartMillis))
        passView.render(state, elapsed)
    }

    /**
     * Re-posting a flat 1000ms from inside run() accumulates the cost of every
     * refresh and is not aligned to anything, so the display eventually skips a
     * visible second. Aim at the next elapsed-second boundary instead, so the
     * digits change at the moment the reading actually changes.
     */
    private fun millisToNextTick(): Long {
        val anchor = settings.outStartMillis ?: 0L
        val delta = System.currentTimeMillis() - anchor
        val phase = ((delta % 1_000L) + 1_000L) % 1_000L
        return 1_000L - phase
    }

    private fun handleTouch(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                // uptimeMillis, not the wall clock: the hold duration is purely
                // in-memory, and a clock jump inside a 3-second press would
                // otherwise measure garbage. The persisted timestamps below
                // stay on the wall clock deliberately, for reboot resilience.
                touchDownAt = SystemClock.uptimeMillis()
                touchDownInCorner =
                    CornerTarget.contains(event.x, event.y, passView.width, passView.height)
            }

            MotionEvent.ACTION_UP -> {
                val heldMillis = SystemClock.uptimeMillis() - touchDownAt
                val inCorner = touchDownInCorner
                // Clear before routing: opening settings leaves this activity,
                // and a stale corner flag would make the next bare ACTION_UP
                // look like a completed long-press.
                clearTouch()
                routeTouch(inCorner, heldMillis)
            }

            // A cancelled gesture is not a tap. Drop it without routing.
            MotionEvent.ACTION_CANCEL -> clearTouch()
        }
        return true
    }

    private fun clearTouch() {
        touchDownAt = 0L
        touchDownInCorner = false
    }

    private fun routeTouch(inCorner: Boolean, heldMillis: Long) {
        val now = System.currentTimeMillis()
        val outStart = settings.outStartMillis
        val state = Pass.stateAt(now, outStart, settings.cooldownEndMillis)
        // guardElapsedIn, not elapsedIn: elapsedIn's clamp is correct for the
        // display and would jam the guard shut across a backwards clock jump.
        val millisInState =
            if (state == PassState.YELLOW) Pass.guardElapsedIn(now, outStart) else Long.MAX_VALUE

        when (
            TouchRouter.route(
                inCorner = inCorner,
                heldMillis = heldMillis,
                state = state,
                millisInState = millisInState
            )
        ) {
            TouchAction.GO_OUT -> { settings.goOut(now); refresh() }
            TouchAction.RETURN -> { settings.returnStudent(now); refresh() }
            TouchAction.OPEN_SETTINGS ->
                startActivity(Intent(this, PinActivity::class.java))
            TouchAction.IGNORE -> Unit
        }
    }

    @Suppress("DEPRECATION")
    private fun applyImmersiveMode() {
        // WindowInsetsController does not exist on Android 8; these flags are
        // deprecated on API 30+ but remain the correct choice at minSdk 26.
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
    }

    private fun requestLockTask() {
        val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        @Suppress("DEPRECATION")
        val alreadyPinned = am.lockTaskModeState != ActivityManager.LOCK_TASK_MODE_NONE
        if (!alreadyPinned) {
            // Without device-owner privileges this shows a system confirmation
            // dialog rather than pinning silently. That is expected.
            //
            // Logged rather than toasted: this runs on every launch, and a
            // toast on every launch is noise. The settings screen's explicit
            // "Pin app to screen" button is the one that reports to the user.
            runCatching { startLockTask() }.onFailure {
                Log.w(TAG, "Auto-pin failed; the tablet is not pinned", it)
            }
        }
    }

    private companion object {
        const val TAG = "Hallpass"
    }
}
