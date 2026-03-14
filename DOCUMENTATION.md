# Display App Documentation (for Admin-App / Updater Agents)

This documentation provides a comprehensive overview of the `de.displayware.app` (Display App), outlining its technical stack, architecture, and core functionalities. It is intended to serve as a factual reference point for developers and AI agents building connected administrative or updater applications.

## 1. Project Overview & Tech Stack

The Display App is an Android-based Digital Signage kiosk application designed to run continuously on remote, low-end hardware (e.g., 2 GB RAM Android devices).

**Core Technology Stack:**
*   **Platform:** Android (Min SDK 28 / Android 9)
*   **Language:** Kotlin (1.9.22)
*   **Build System:** Gradle (8.6) / Android Gradle Plugin (8.2.2)
*   **Media Playback:** AndroidX Media3 (ExoPlayer)
*   **JSON Parsing:** Moshi (via `KotlinJsonAdapterFactory`)
*   **Networking:** Native `java.net.HttpURLConnection` (for config fetching and video downloading; OkHttp is NOT utilized to maintain a low dependency footprint)
*   **Concurrency:** Kotlin Coroutines (`lifecycleScope`, `Dispatchers.IO`, `Dispatchers.Main`)

## 2. Architecture & Components

The application is structured around a single-activity architecture (`PlayerActivity.kt`), which dynamically switches between two primary display modes based on a remote JSON configuration.

### 2.1 Display Modes

1.  **Web Mode (`WebViewController.kt`)**
    *   Displays content via a fullscreen Android `WebView`.
    *   Caching is intentionally disabled to ensure fresh content is always loaded from the target URL.
2.  **Video Mode (`VideoPlayerController.kt` & `VideoCacheManager.kt`)**
    *   Utilizes ExoPlayer for seamless, muted video playback in an endless loop (`REPEAT_MODE_ALL`).
    *   **Caching Mechanism:** Videos are not streamed directly. Instead, `VideoCacheManager.kt` downloads the video file from the provided HTTP/HTTPS URL to the device's internal storage (`context.filesDir`). The ExoPlayer then plays the local file. The app handles temporary files during the download process to prevent corrupted playback and falls back to previously cached files if network connectivity is lost during a subsequent download.

### 2.2 Configuration Management

The app's behavior is entirely driven by a remote JSON file hosted on a VPS (`http://159.195.69.206/config.json`).

*   **Fetching:** `ConfigManager.kt` fetches the JSON via `HttpURLConnection`. A timestamp cache-buster (`?t=<timestamp>`) is appended to bypass any proxy or Nginx caching.
*   **Cleartext Traffic:** The app explicitly allows HTTP traffic (`android:usesCleartextTraffic="true"`) to connect to the VPS, which currently lacks an SSL certificate.
*   **Fallback:** If the remote fetch fails, the app falls back to the last successfully parsed configuration stored in `SharedPreferences`.

**JSON Structure (Array of Displays):**
```json
{
  "version": 1,
  "displays": [
    {
      "id": "lobby_screen",
      "require_id_setup": false,
      "mode": "video",
      "video_url": "http://159.195.69.206/promo.mp4",
      "web_url": null,
      "reload_interval_sec": 60,
      "reset_token": "token_v2"
    }
  ]
}
```

### 2.3 Display Identification & Routing

A single configuration file dictates the behavior of multiple physical screens.

*   **Display ID Store:** `DisplayIdStore.kt` manages a unique string ID for the local device, stored securely in `SharedPreferences`.
*   **Setup Routing:** On startup, `PlayerActivity` cross-references the local ID with the `displays` array in the JSON config. If the local ID is missing, not found in the config, or if the config explicitly marks `"require_id_setup": true`, the app routes to `DisplayIdSetupActivity.kt`. Here, an administrator can manually input the device's designated ID.

### 2.4 State Management & Kiosk Features

*   **Polling Loop:** `PlayerActivity` uses a coroutine job to poll the `config.json` at intervals defined by `reload_interval_sec` (defaulting to 60s if omitted or 0).
*   **Remote Reset (`reset_token`):** Each display configuration can include an arbitrary string `reset_token`. The app stores the last seen token locally. During the polling loop, if the remote `reset_token` differs from the local one, the app immediately halts playback, updates the local token, and performs a hard restart of the `PlayerActivity`. This enables remote administration to force a clean UI state.
*   **Immersive Mode:** The app enforces full-screen immersive mode, hiding status and navigation bars across modern and legacy Android versions.
*   **Auto-Start on Boot:** `BootReceiver.kt` listens for `Intent.ACTION_BOOT_COMPLETED` to launch the app automatically when the device powers on. The app requests the `SYSTEM_ALERT_WINDOW` permission to bypass background-start restrictions enforced in Android 10+.

## 3. Current State & Known Constraints

*   **Signature:** The app is currently built using the standard Android Debug Keystore (`assembleDebug`).
*   **Deployment:** The current deployment workflow relies on manual ADB installation on target devices/emulators.
*   **Testing:** Due to ADB permission restrictions (UID 2000), simulating a boot event requires a custom intent action: `adb shell am broadcast -a de.displayware.app.TEST_BOOT_RECEIVER -n de.displayware.app/.boot.BootReceiver`.
