package de.displayware.app.config

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.*
import org.junit.Test

class DisplayConfigTest {

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    private val adapter = moshi.adapter(DisplayConfig::class.java)

    @Test
    fun testValidVideoConfig() {
        val json = """
            {
                "version": 1,
                "mode": "video",
                "video_url": "https://example.com/video.mp4",
                "web_url": null,
                "reload_interval_sec": 0
            }
        """.trimIndent()
        
        val config = adapter.fromJson(json)
        assertNotNull(config)
        assertTrue(config!!.isValid())
        assertEquals("video", config.mode)
    }

    @Test
    fun testInvalidVideoConfig() {
        val json = """
            {
                "version": 1,
                "mode": "video",
                "video_url": null,
                "web_url": null
            }
        """.trimIndent()
        
        val config = adapter.fromJson(json)
        assertNotNull(config)
        assertFalse(config!!.isValid())
    }

    @Test
    fun testValidWebConfig() {
        val json = """
            {
                "version": 1,
                "mode": "web",
                "video_url": null,
                "web_url": "https://example.com"
            }
        """.trimIndent()
        
        val config = adapter.fromJson(json)
        assertNotNull(config)
        assertTrue(config!!.isValid())
        assertEquals("web", config.mode)
    }

    @Test
    fun testUnknownMode() {
        val json = """
            {
                "version": 1,
                "mode": "unknown",
                "video_url": "something",
                "web_url": "something"
            }
        """.trimIndent()
        
        val config = adapter.fromJson(json)
        assertNotNull(config)
        assertFalse(config!!.isValid())
    }
}
