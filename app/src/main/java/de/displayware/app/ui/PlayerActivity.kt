package de.displayware.app.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import de.displayware.app.R
import de.displayware.app.config.ConfigManager
import de.displayware.app.config.DisplayConfigRoot
import de.displayware.app.config.DisplayEntry
import de.displayware.app.config.DisplayIdStore
import de.displayware.app.player.VideoCacheManager
import de.displayware.app.player.VideoPlayerController
import de.displayware.app.player.WebViewController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

/**
 * Main Activity of the Display App.
 * Handles Immersive Fullscreen and switches between Video and Web modes.
 */
class PlayerActivity : AppCompatActivity() {

    private lateinit var configManager: ConfigManager
    private lateinit var displayIdStore: DisplayIdStore
    private lateinit var videoController: VideoPlayerController
    private lateinit var videoCacheManager: VideoCacheManager
    private lateinit var webController: WebViewController
    private lateinit var progressBar: ProgressBar
    
    private var configPollingJob: Job? = null

    // Change this to your actual config URL
    private val configUrlBase = "http://159.195.69.206/config.json"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)
        
        setupImmersiveMode()
        
        progressBar = findViewById(R.id.progressBar)
        configManager = ConfigManager(this)
        displayIdStore = DisplayIdStore(this)
        videoCacheManager = VideoCacheManager(this)
        videoController = VideoPlayerController(this, findViewById(R.id.videoPlayerView))
        webController = WebViewController(findViewById(R.id.webView))

        loadAndStart()
    }

    private fun loadAndStart() {
        progressBar.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            val configRoot = withContext(Dispatchers.IO) {
                // Add timestamp cache-buster to ensure we always get the freshest config from VPS
                val currentUrl = "$configUrlBase?t=${System.currentTimeMillis()}"
                configManager.fetchConfig(currentUrl)
            }

            if (configRoot != null) {
                val entryToPlay = resolveDisplayAndMaybeStartSetup(configRoot)
                
                if (entryToPlay != null) {
                    checkAndPerformResetIfNeeded(entryToPlay)
                    
                    when (entryToPlay.mode) {
                        "video" -> {
                            entryToPlay.videoUrl?.let { url ->
                                webController.hide()
                                val displayId = displayIdStore.getDisplayId() ?: "unknown"
                                progressBar.visibility = View.VISIBLE
                                val localFile = videoCacheManager.downloadVideo(url, displayId)
                                if (localFile != null) {
                                    videoController.playLocalVideoFile(localFile)
                                } else {
                                    videoController.stop()
                                }
                                progressBar.visibility = View.GONE
                            }
                        }
                        "web" -> {
                            videoController.stop()
                            entryToPlay.webUrl?.let { webController.loadUrl(it) }
                        }
                    }
                    
                    val pollInterval = if (entryToPlay.reloadIntervalSec > 0) {
                        entryToPlay.reloadIntervalSec
                    } else {
                        // Fallback polling of 60 seconds if 0 is provided, 
                        // so we never completely lose touch with the server.
                        60
                    }
                    startConfigPolling(pollInterval)
                }
            } else {
                // If config couldn't be loaded at all, we might want to still show setup if we have no ID
                if (displayIdStore.getDisplayId() == null) {
                    startDisplayIdSetup()
                }
            }
            progressBar.visibility = View.GONE
        }
    }

    /**
     * Resolves the current display ID against the config.
     * Returns the [DisplayEntry] if it should be played, or null if Setup Activity was started.
     */
    private fun resolveDisplayAndMaybeStartSetup(configRoot: DisplayConfigRoot): DisplayEntry? {
        val displayId = displayIdStore.getDisplayId()
        
        if (displayId == null) {
            startDisplayIdSetup()
            return null
        }

        val entry = configManager.getEntryForDisplay(configRoot, displayId)
        
        if (entry == null) {
            // ID exists locally but not in config -> ID invalid, go to setup
            startDisplayIdSetup()
            return null
        }

        if (entry.requireIdSetup) {
            // Config explicitly requested setup screen for this ID
            startDisplayIdSetup()
            return null
        }

        // Everything valid, return entry for playback
        return entry
    }

    private fun checkAndPerformResetIfNeeded(entry: DisplayEntry) {
        val currentToken = entry.resetToken
        if (currentToken != null) {
            val lastToken = displayIdStore.getLastResetToken()
            if (currentToken != lastToken) {
                performConfigResetForDisplay(entry)
            }
        }
    }

    private fun performConfigResetForDisplay(entry: DisplayEntry) {
        // Stop current media
        videoController.stop()
        webController.hide()
        
        // Update the token
        displayIdStore.setLastResetToken(entry.resetToken)
        
        // Restart the activity to ensure a completely fresh state
        val intent = Intent(this, PlayerActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
    }

    private fun startConfigPolling(intervalSec: Int) {
        configPollingJob?.cancel()
        configPollingJob = lifecycleScope.launch {
            while (isActive) {
                delay(intervalSec * 1000L)
                val currentUrl = "$configUrlBase?t=${System.currentTimeMillis()}"
                val configRoot = withContext(Dispatchers.IO) {
                    configManager.fetchConfig(currentUrl)
                }
                if (configRoot != null) {
                    val entry = resolveDisplayAndMaybeStartSetup(configRoot)
                    if (entry != null) {
                        withContext(Dispatchers.Main) {
                            checkAndPerformResetIfNeeded(entry)
                        }
                    }
                }
            }
        }
    }

    private fun startDisplayIdSetup() {
        startActivity(Intent(this, DisplayIdSetupActivity::class.java))
        finish()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            setupImmersiveMode()
        }
    }

    private fun setupImmersiveMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN)
            
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onDestroy() {
        super.onDestroy()
        configPollingJob?.cancel()
        videoController.release()
    }
}
