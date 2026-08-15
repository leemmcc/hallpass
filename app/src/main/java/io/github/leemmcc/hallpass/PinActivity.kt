package io.github.leemmcc.hallpass

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class PinActivity : AutoCloseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val settings = Settings(this)

        val entry = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            hint = "PIN"
            gravity = Gravity.CENTER
        }

        // Both actions require the correct PIN. Factored out so neither can
        // drift into checking it differently.
        fun withCorrectPin(action: () -> Unit) {
            if (entry.text.toString() == settings.pin) {
                action()
            } else {
                Toast.makeText(this, "Incorrect PIN", Toast.LENGTH_SHORT).show()
                entry.setText("")
            }
        }

        // First and most prominent: clearing a stuck yellow, or ending a
        // cooldown early, is the only thing ever needed urgently mid-class.
        // Going through the settings screen for it costs an extra screen and
        // tap at exactly the wrong moment.
        //
        // Still behind the PIN. A no-PIN shortcut would be discovered by a
        // student eventually, and "skip the cooldown" is the one thing the red
        // screen exists to prevent.
        val reset = Button(this).apply {
            text = "Reset to green"
            setOnClickListener {
                withCorrectPin {
                    settings.reset()
                    Toast.makeText(this@PinActivity, "Reset to green", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }

        val openSettings = Button(this).apply {
            text = "Open settings"
            setOnClickListener {
                withCorrectPin {
                    startActivity(Intent(this@PinActivity, SettingsActivity::class.java))
                    finish()
                }
            }
        }

        val cancel = Button(this).apply {
            text = "Cancel"
            setOnClickListener { finish() }
        }

        setContentView(
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(48, 96, 48, 48)
                addView(TextView(this@PinActivity).apply {
                    text = "Enter PIN"
                    textSize = 24f
                    gravity = Gravity.CENTER
                })
                addView(entry)
                addView(reset)
                addView(openSettings)
                addView(cancel)
            }
        )
    }
}
