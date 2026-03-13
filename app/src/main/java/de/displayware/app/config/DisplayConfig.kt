package de.displayware.app.config

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Root configuration representing the whole config.json on the server.
 */
@JsonClass(generateAdapter = true)
data class DisplayConfigRoot(
    @Json(name = "version") val version: Int,
    @Json(name = "displays") val displays: List<DisplayEntry>
)

/**
 * Configuration for a specific display unit.
 */
@JsonClass(generateAdapter = true)
data class DisplayEntry(
    @Json(name = "id") val id: String,
    @Json(name = "require_id_setup") val requireIdSetup: Boolean,
    @Json(name = "mode") val mode: String,
    @Json(name = "video_url") val videoUrl: String?,
    @Json(name = "web_url") val webUrl: String?,
    @Json(name = "reload_interval_sec") val reloadIntervalSec: Int
)
