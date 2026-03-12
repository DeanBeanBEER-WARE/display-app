# Agent Context: Display App (MVP Kiosk)

This file (`AGENT.md`) serves as a hand-off document for future AI agents working on this Android project. It documents the current state of the architecture, key decisions, and constraints.

## Project Overview
This is a minimalist, robust Android Kiosk app for Digital Signage screens. It is optimized for low-RAM devices (2 GB) and targets Android 9+ (API 28+).
- **Package Name:** `de.displayware.app`
- **Main Activity:** `PlayerActivity.kt`
- **Build System:** Gradle 8.6, AGP 8.2.2, Kotlin 1.9.22

## Key Features & Architecture
The app switches between two modes based on a remote JSON configuration:
1. **Web Mode:** Fullscreen WebView without cache (`WebViewController.kt`).
2. **Video Mode:** ExoPlayer (Media3) seamless loop (`VideoPlayerController.kt`).

### Remote Configuration (The VPS Setup)
The app is entirely remote-controlled. It expects a `config.json` at startup.
- **Config URL:** `http://159.195.69.206/config.json` (Managed in `PlayerActivity.kt`).
- **Cache-Busting:** The app appends a timestamp to the URL (`?t=<timestamp>`) to bypass any Nginx/Provider caching and guarantee the freshest config is loaded.
- **Cleartext HTTP:** Because the VPS does not have an SSL certificate yet, `android:usesCleartextTraffic="true"` is explicitly set in `AndroidManifest.xml` to allow `http://` traffic (which is blocked by default in Android 9+).
- **Fallback Behavior:** If the network request fails (e.g. timeout), the `ConfigManager.kt` falls back to the last successfully parsed JSON saved in SharedPreferences.

### Example JSON Format (config.json)
```json
{
  "version": 1,
  "mode": "web",
  "web_url": "https://www.wikipedia.org",
  "video_url": null,
  "reload_interval_sec": 0
}
```

## Important Constraints & Rules for Agents
- **Do not refactor the config logic** to read local raw/asset files unless explicitly requested. The remote VPS URL is intentional.
- **Do not remove `usesCleartextTraffic="true"`** from the Manifest, as it breaks the VPS connection.
- **ExoPlayer Settings:** In `VideoPlayerController.kt`, `setBufferDurationsMs()` is used instead of the deprecated `setBufferMs()`. Do not downgrade or change the Media3 API usage without good reason.
- **WebView Settings:** `setAppCacheEnabled` was removed from `WebViewController.kt` because it causes compile errors in modern Android versions (deprecated). Do not re-add it.
- **Immersive Mode:** `PlayerActivity.kt` handles hiding the status/navigation bars. It contains legacy code for `< API 30` and modern `WindowInsetsController` code.
- **Deployment:** The user tests this app on a local Emulator (`emulator-5554`) via `adb install` and runs it via `adb shell monkey -p de.displayware.app 1`. Provide exact shell commands when asking the user to deploy.

## Server-Side Context
The user hosts the config file on an Ubuntu VPS (`159.195.69.206`) using `Nginx`.
- The `config.json` is located in `/home/signage/public_html/`.
- Nginx root is pointed to this directory.
- Edits to the config file are done by the user (usually via FileZilla with the user `signage`).

## Current State / Next Steps
The app currently successfully fetches the JSON from the VPS and updates the View. Any new feature should be built around this existing, working remote-fetch foundation.