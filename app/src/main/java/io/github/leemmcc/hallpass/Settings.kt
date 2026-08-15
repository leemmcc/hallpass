package io.github.leemmcc.hallpass

import android.content.Context

/**
 * Thin SharedPreferences wrapper. Deliberately has no logic worth testing --
 * the rules live in SettingsRules, the state derivation lives in Pass.
 *
 * Absent timestamps are stored as NONE rather than removed, so reads stay
 * branch-free.
 */
class Settings(context: Context) {

    private val prefs = context.getSharedPreferences("hallpass", Context.MODE_PRIVATE)

    var cooldownMinutes: Int
        get() = prefs.getInt(KEY_MINUTES, SettingsRules.DEFAULT_MINUTES)
        set(value) = prefs.edit()
            .putInt(KEY_MINUTES, SettingsRules.clampDurationMinutes(value))
            .apply()

    var pin: String
        get() = prefs.getString(KEY_PIN, SettingsRules.DEFAULT_PIN) ?: SettingsRules.DEFAULT_PIN
        set(value) {
            if (SettingsRules.isValidPin(value)) prefs.edit().putString(KEY_PIN, value).apply()
        }

    var autoPin: Boolean
        get() = prefs.getBoolean(KEY_AUTO_PIN, false)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_PIN, value).apply()

    val outStartMillis: Long?
        get() = prefs.getLong(KEY_OUT_START, NONE).takeIf { it != NONE }

    val cooldownEndMillis: Long?
        get() = prefs.getLong(KEY_COOLDOWN_END, NONE).takeIf { it != NONE }

    /**
     * The three transitions delegate to the tested pure functions in Pass.
     * This class only persists what they return -- deliberately, so the
     * invariant that RETURN clears outStart lives in unit-tested code rather
     * than in this untested plumbing.
     */
    fun goOut(nowMillis: Long) = persist(Pass.goOut(nowMillis))

    fun returnStudent(nowMillis: Long) =
        persist(Pass.returnStudent(nowMillis, cooldownMinutes))

    fun reset() = persist(Pass.reset())

    private fun persist(timestamps: PassTimestamps) {
        prefs.edit()
            .putLong(KEY_OUT_START, timestamps.outStartMillis ?: NONE)
            .putLong(KEY_COOLDOWN_END, timestamps.cooldownEndMillis ?: NONE)
            .apply()
    }

    private companion object {
        const val NONE = -1L
        const val KEY_MINUTES = "cooldown_minutes"
        const val KEY_PIN = "pin"
        const val KEY_AUTO_PIN = "auto_pin"
        const val KEY_OUT_START = "out_start"
        const val KEY_COOLDOWN_END = "cooldown_end"
    }
}
