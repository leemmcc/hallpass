package io.github.leemmcc.hallpass

import android.app.Activity
import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class SettingsActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val settings = Settings(this)

        val resetButton = Button(this).apply {
            text = "Reset to green"
            setOnClickListener {
                settings.reset()
                Toast.makeText(this@SettingsActivity, "Reset to green", Toast.LENGTH_SHORT).show()
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
                    Toast.makeText(this@SettingsActivity, "Enter a number", Toast.LENGTH_SHORT)
                        .show()
                } else {
                    settings.cooldownMinutes = requested
                    // Read back: the setter clamps, so show what was actually stored.
                    minutesField.setText(settings.cooldownMinutes.toString())
                    Toast.makeText(
                        this@SettingsActivity,
                        "Cooldown: ${settings.cooldownMinutes} min",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        val pinField = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            hint = "New 4-digit PIN"
        }

        val savePin = Button(this).apply {
            text = "Change PIN"
            setOnClickListener {
                val candidate = pinField.text.toString()
                if (SettingsRules.isValidPin(candidate)) {
                    settings.pin = candidate
                    pinField.setText("")
                    Toast.makeText(this@SettingsActivity, "PIN changed", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(
                        this@SettingsActivity,
                        "PIN must be exactly 4 digits",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        val pinNow = Button(this).apply {
            text = "Pin app to screen"
            setOnClickListener { runCatching { startLockTask() } }
        }

        val autoPin = CheckBox(this).apply {
            text = "Auto-pin on launch"
            isChecked = settings.autoPin
            setOnCheckedChangeListener { _, checked -> settings.autoPin = checked }
        }

        val done = Button(this).apply {
            text = "Done"
            setOnClickListener { finish() }
        }

        setContentView(
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(48, 48, 48, 48)
                addView(resetButton)
                addView(TextView(this@SettingsActivity).apply { text = "Cooldown (minutes, 1-60)" })
                addView(minutesField)
                addView(saveMinutes)
                addView(pinField)
                addView(savePin)
                addView(pinNow)
                addView(autoPin)
                addView(done)
            }
        )
    }
}
