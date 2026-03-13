package de.displayware.app.player

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Manages downloading and caching of video files for offline playback.
 * Ensures that files are fully downloaded to a temporary location before
 * replacing any existing cached file, preventing corrupted playbacks.
 */
class VideoCacheManager(private val context: Context) {

    companion object {
        private const val TAG = "VideoCacheManager"
        private const val PREFS_NAME = "video_cache_prefs"
        private const val KEY_LAST_URL_PREFIX = "last_video_url_"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Downloads a video from the given URL and saves it locally for the specific displayId.
     * If the URL has not changed since the last successful download, it returns the existing file.
     * 
     * @param url The remote URL of the video to download.
     * @param displayId The unique ID of the current display (used for filename).
     * @return A valid local [File] if successful or if a cached version exists, null otherwise.
     */
    suspend fun downloadVideo(url: String, displayId: String): File? = withContext(Dispatchers.IO) {
        val lastUrlKey = KEY_LAST_URL_PREFIX + displayId
        val lastUrl = prefs.getString(lastUrlKey, null)
        
        val finalFile = File(context.filesDir, "video_$displayId.mp4")
        
        // If the URL hasn't changed and the file exists, return the cached file
        if (url == lastUrl && finalFile.exists()) {
            Log.d(TAG, "URL unchanged and file exists. Using cached video: ${finalFile.absolutePath}")
            return@withContext finalFile
        }

        val tempFile = File(context.filesDir, "video_${displayId}_temp.mp4")
        
        Log.d(TAG, "Downloading new video from $url to ${tempFile.absolutePath}")
        
        try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                Log.e(TAG, "Server returned HTTP ${connection.responseCode} ${connection.responseMessage}")
                return@withContext if (finalFile.exists()) finalFile else null
            }

            val inputStream: InputStream = connection.inputStream
            val outputStream = FileOutputStream(tempFile)

            val buffer = ByteArray(4096)
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
            }

            outputStream.flush()
            outputStream.close()
            inputStream.close()
            connection.disconnect()

            // Download successful, replace old file safely
            if (finalFile.exists()) {
                finalFile.delete()
            }
            val renamed = tempFile.renameTo(finalFile)
            
            if (renamed) {
                Log.d(TAG, "Download complete. Saved to ${finalFile.absolutePath}")
                prefs.edit().putString(lastUrlKey, url).apply()
                return@withContext finalFile
            } else {
                Log.e(TAG, "Failed to rename temp file to final file.")
                if (tempFile.exists()) tempFile.delete()
                return@withContext if (finalFile.exists()) finalFile else null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading video: ${e.message}")
            if (tempFile.exists()) tempFile.delete()
            // Fallback to existing file if available
            return@withContext if (finalFile.exists()) finalFile else null
        }
    }
}
