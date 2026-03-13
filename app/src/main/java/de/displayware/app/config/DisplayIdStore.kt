package de.displayware.app.config

import android.content.Context

class DisplayIdStore(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "display_prefs"
        private const val KEY_DISPLAY_ID = "display_id"
    }

    fun getDisplayId(): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_DISPLAY_ID, null)
    }

    fun setDisplayId(id: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_DISPLAY_ID, id).apply()
    }

    fun clearDisplayId() {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_DISPLAY_ID).apply()
    }
}
