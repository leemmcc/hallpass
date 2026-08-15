package io.github.leemmcc.hallpass

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class PinActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val settings = Settings(this)

        val entry = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            hint = "PIN"
            gravity = Gravity.CENTER
        }

        val unlock = Button(this).apply {
            text = "Unlock"
            setOnClickListener {
                if (entry.text.toString() == settings.pin) {
                    startActivity(Intent(this@PinActivity, SettingsActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this@PinActivity, "Incorrect PIN", Toast.LENGTH_SHORT).show()
                    entry.setText("")
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
                addView(unlock)
                addView(cancel)
            }
        )
    }
}
