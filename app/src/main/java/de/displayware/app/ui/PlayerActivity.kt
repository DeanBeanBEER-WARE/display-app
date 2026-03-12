package de.displayware.app.ui

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
import de.displayware.app.player.VideoPlayerController
import de.displayware.app.player.WebViewController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Main Activity of the Display App.
 * Handles Immersive Fullscreen and switches between Video and Web modes.
 */
class PlayerActivity : AppCompatActivity() {

    private lateinit var configManager: ConfigManager
    private lateinit var videoController: VideoPlayerController
    private lateinit var webController: WebViewController
    private lateinit var progressBar: ProgressBar

    // Change this to your actual config URL
    private val configUrl = "https://raw.githubusercontent.com/DeanBeanBEER-WARE/display-app/main/example-config.json"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)
        
        setupImmersiveMode()
        
        progressBar = findViewById(R.id.progressBar)
        configManager = ConfigManager(this)
        videoController = VideoPlayerController(this, findViewById(R.id.videoPlayerView))
        webController = WebViewController(findViewById(R.id.webView))

        loadAndStart()
    }

    private fun loadAndStart() {
        progressBar.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            val config = withContext(Dispatchers.IO) {
                configManager.fetchConfig(configUrl)
            }

            if (config != null) {
                when (config.mode) {
                    "video" -> {
                        val videoFile = withContext(Dispatchers.IO) {
                            config.videoUrl?.let { configManager.downloadVideo(it) }
                        }
                        if (videoFile != null && videoFile.exists()) {
                            webController.hide()
                            videoController.playVideo(videoFile)
                        }
                    }
                    "web" -> {
                        videoController.stop()
                        config.webUrl?.let { webController.loadUrl(it) }
                    }
                }
            }
            progressBar.visibility = View.GONE
        }
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
        videoController.release()
    }
}
