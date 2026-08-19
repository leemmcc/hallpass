package io.github.leemmcc.hallpass

import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class SettingsActivity : AutoCloseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val settings = Settings(this)

        val resetButton = Button(this).apply {
            text = "Reset to green"
            setOnClickListener {
                settings.reset()
                toast("Reset to green")
            }
        }

        val minutesField = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(settings.cooldownMinutes.toString())
        }

        val saveMinutes = Button(this).apply {
            text = "Save cooldown"
            setOnClickListener {
                val requested = minutesField.text.toString().toIntOrNull()
                if (requested == null) {
                    toast("Enter a number")
                } else {
                    settings.cooldownMinutes = requested
                    // Read back: the setter clamps, so show what was actually stored.
                    minutesField.setText(settings.cooldownMinutes.toString())
                    toast("Cooldown: ${settings.cooldownMinutes} min")
                }
            }
        }

        val tapGuardField = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(settings.tapGuardSeconds.toString())
        }

        val saveTapGuard = Button(this).apply {
            text = "Save tap lockout"
            setOnClickListener {
                val requested = tapGuardField.text.toString().toIntOrNull()
                if (requested == null) {
                    toast("Enter a number")
                } else {
                    settings.tapGuardSeconds = requested
                    // Read back: the setter clamps, so show what was actually stored.
                    tapGuardField.setText(settings.tapGuardSeconds.toString())
                    toast("Tap lockout: ${settings.tapGuardSeconds} sec")
                }
            }
        }

        // Both PIN fields are masked, exactly as PinActivity's entry is. The
        // whole justification for having a PIN is that a student who watches
        // the gesture still cannot get in; showing the new PIN in clear text on
        // a wall-mounted screen at the moment it is set gives that away to the
        // entire room.
        val pinField = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            hint = "New 4-digit PIN"
        }

        // And because they are masked, a single mistyped digit would otherwise
        // silently set a PIN the teacher does not know. Hence the confirmation.
        val pinConfirmField = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            hint = "Confirm new PIN"
        }

        val savePin = Button(this).apply {
            text = "Change PIN"
            setOnClickListener {
                val candidate = pinField.text.toString()
                val confirmation = pinConfirmField.text.toString()
                when {
                    !SettingsRules.isValidPin(candidate) -> toast("PIN must be exactly 4 digits")
                    candidate != confirmation -> toast("PINs do not match")
                    else -> {
                        settings.pin = candidate
                        pinField.setText("")
                        pinConfirmField.setText("")
                        toast("PIN changed")
                    }
                }
            }
        }

        val pinNow = Button(this).apply {
            text = "Pin app to screen"
            setOnClickListener {
                // Every other control here toasts, so silence reads as success.
                // Screen pinning is the only thing between the class and the
                // rest of the tablet -- a failure that says nothing leaves the
                // teacher walking away from an unpinned wall.
                runCatching { startLockTask() }
                    .onSuccess { toast("Pinning requested") }
                    .onFailure { toast("Could not pin - check device settings") }
            }
        }

        val autoPin = CheckBox(this).apply {
            text = "Auto-pin on launch"
            isChecked = settings.autoPin
            setOnCheckedChangeListener { _, checked -> settings.autoPin = checked }
        }

        // Last, and well away from "Reset to green" at the top: this one is
        // rare and closes the classroom display.
        val exit = Button(this).apply {
            text = "Exit app"
            setOnClickListener { confirmAndExitApp() }
        }

        val done = Button(this).apply {
            text = "Done"
            setOnClickListener { finish() }
        }

        // Scrollable: the control list has grown past what a short screen shows
        // at large font sizes, and a control you cannot reach is a control you
        // do not have.
        val form = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(48, 48, 48, 48)
                addView(resetButton)
                addView(TextView(this@SettingsActivity).apply { text = "Cooldown (minutes, 1-60)" })
                addView(minutesField)
                addView(saveMinutes)
                addView(TextView(this@SettingsActivity).apply { text = "Tap lockout (seconds, 0-60)" })
                addView(tapGuardField)
                addView(saveTapGuard)
                addView(pinField)
                addView(pinConfirmField)
                addView(savePin)
                addView(pinNow)
                addView(autoPin)
                addView(exit)
                addView(done)
        }
        setContentView(ScrollView(this).apply { addView(form) })
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
