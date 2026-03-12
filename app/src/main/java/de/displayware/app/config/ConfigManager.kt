package de.displayware.app.config

import android.content.Context
import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException

private const val TAG = "ConfigManager"
private const val PREFS_NAME = "display_config_prefs"
private const val KEY_LAST_CONFIG = "last_valid_config"

/**
 * Manages loading, parsing, and persisting the display configuration.
 */
class ConfigManager(private val context: Context) {

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    private val adapter = moshi.adapter(DisplayConfig::class.java)
    private val client = OkHttpClient()

    /**
     * Fetches the configuration from the remote URL.
     * Fallback to local config if remote fails.
     */
    fun fetchConfig(url: String): DisplayConfig? {
        val request = Request.Builder().url(url).build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Unexpected code $response")

                val body = response.body?.string() ?: return null
                val config = adapter.fromJson(body)

                if (config != null && config.isValid()) {
                    saveLocalConfig(body)
                    return config
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching config from $url, falling back to local: ${e.message}")
        }

        return getLocalConfig()
    }

    private fun saveLocalConfig(json: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LAST_CONFIG, json).apply()
    }

    private fun getLocalConfig(): DisplayConfig? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_LAST_CONFIG, null) ?: return null
        return try {
            adapter.fromJson(json)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing local config: ${e.message}")
            null
        }
    }

    /**
     * Downloads a video file to local storage.
     */
    fun downloadVideo(url: String): File? {
        val videoFile = File(context.filesDir, "videos/current.mp4")
        videoFile.parentFile?.mkdirs()

        val request = Request.Builder().url(url).build()
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                
                response.body?.byteStream()?.use { input ->
                    videoFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                return videoFile
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading video: ${e.message}")
            if (videoFile.exists()) return videoFile // Fallback to existing file
        }
        return null
    }
}
