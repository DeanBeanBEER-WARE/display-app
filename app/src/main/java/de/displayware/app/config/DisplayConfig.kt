package de.displayware.app.config

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Data class representing the display configuration.
 */
@JsonClass(generateAdapter = true)
data class DisplayConfig(
    @Json(name = "version") val version: Int,
    @Json(name = "mode") val mode: String, // "video" or "web"
    @Json(name = "video_url") val videoUrl: String?,
    @Json(name = "web_url") val webUrl: String?,
    @Json(name = "reload_interval_sec") val reloadIntervalSec: Int = 0
) {
    /**
     * Checks if the configuration is valid for the selected mode.
     */
    fun isValid(): Boolean {
        return when (mode) {
            "video" -> !videoUrl.isNullOrBlank()
            "web" -> !webUrl.isNullOrBlank()
            else -> false
        }
    }
}
