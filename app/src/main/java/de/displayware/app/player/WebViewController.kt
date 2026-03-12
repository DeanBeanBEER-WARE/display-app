package de.displayware.app.player

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.View
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient

/**
 * Controller for WebView display.
 * Configured for Kiosk use with JavaScript and local storage enabled.
 */
class WebViewController(private val webView: WebView) {

    init {
        setupWebView()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            useWideViewPort = true
            loadWithOverviewMode = true
            databaseEnabled = true
        }

        // Fullscreen/Immersive-Verhalten (verhindert Bouncen)
        webView.overScrollMode = WebView.OVER_SCROLL_NEVER

        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                webView.visibility = View.VISIBLE
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                // Handle error (could show a local fallback page)
            }
        }
    }

    /**
     * Loads the specified URL.
     */
    fun loadUrl(url: String) {
        webView.loadUrl(url)
        webView.visibility = View.VISIBLE
    }

    /**
     * Hides the WebView.
     */
    fun hide() {
        webView.visibility = View.GONE
    }
}
