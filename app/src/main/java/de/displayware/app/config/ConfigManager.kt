package de.displayware.app.config

import android.content.Context
import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class ConfigManager(private val context: Context) {

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val adapter = moshi.adapter(DisplayConfigRoot::class.java)

    companion object {
        private const val PREFS_NAME = "config_prefs"
        private const val KEY_LAST_CONFIG = "last_valid_config"
        private const val TAG = "ConfigManager"
    }

    /**
     * Fetches the configuration from the given URL using HttpURLConnection. 
     * If it fails, falls back to the last successfully parsed config from SharedPreferences.
     */
    fun fetchConfig(urlString: String): DisplayConfigRoot? {
        return try {
            Log.d(TAG, "Fetching config from $urlString")
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val jsonStr = reader.use { it.readText() }
                
                // Parse JSON string to Object
                val parsedConfig = adapter.fromJson(jsonStr)
                if (parsedConfig != null) {
                    saveConfigToPrefs(jsonStr)
                    return parsedConfig
                }
            } else {
                Log.e(TAG, "Failed to fetch config, code: ${connection.responseCode}")
            }
            getFallbackConfig()
        } catch (e: Exception) {
            Log.e(TAG, "Exception fetching config: ${e.message}")
            getFallbackConfig()
        }
    }

    /**
     * Returns the matching display entry for a given display ID.
     */
    fun getEntryForDisplay(configRoot: DisplayConfigRoot, displayId: String): DisplayEntry? {
        return configRoot.displays.find { it.id == displayId }
    }

    private fun saveConfigToPrefs(jsonStr: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LAST_CONFIG, jsonStr).apply()
    }

    private fun getFallbackConfig(): DisplayConfigRoot? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_LAST_CONFIG, null)
        return if (jsonStr != null) {
            try {
                Log.d(TAG, "Using fallback config")
                adapter.fromJson(jsonStr)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse fallback config: ${e.message}")
                null
            }
        } else {
            Log.e(TAG, "No fallback config found")
            null
        }
    }
}
