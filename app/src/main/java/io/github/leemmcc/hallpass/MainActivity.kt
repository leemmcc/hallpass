package io.github.leemmcc.hallpass

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager

class MainActivity : Activity() {

    private lateinit var passView: PassView
    private lateinit var settings: Settings

    private val handler = Handler(Looper.getMainLooper())
    private var touchDownAt = 0L
    private var touchDownInCorner = false

    private val tick = object : Runnable {
        override fun run() {
            refresh()
            handler.postDelayed(this, 1_000L)
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
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) applyImmersiveMode()
    }

    private fun refresh() {
        val now = System.currentTimeMillis()
        val state = Pass.stateAt(now, settings.outStartMillis, settings.cooldownEndMillis)
        val elapsed = ElapsedFormat.format(Pass.elapsedIn(now, settings.outStartMillis))
        passView.render(state, elapsed)
    }

    private fun handleTouch(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                touchDownAt = System.currentTimeMillis()
                touchDownInCorner =
                    CornerTarget.contains(event.x, event.y, passView.width, passView.height)
            }

            MotionEvent.ACTION_UP -> {
                val now = System.currentTimeMillis()
                val outStart = settings.outStartMillis
                val state = Pass.stateAt(now, outStart, settings.cooldownEndMillis)
                val millisInState =
                    if (state == PassState.YELLOW) Pass.elapsedIn(now, outStart) else Long.MAX_VALUE

                when (
                    TouchRouter.route(
                        inCorner = touchDownInCorner,
                        heldMillis = now - touchDownAt,
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
        }
        return true
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
            runCatching { startLockTask() }
        }
    }
}
