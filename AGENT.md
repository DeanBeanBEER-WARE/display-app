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
2. **Video Mode:** ExoPlayer (Media3) seamless loop (`VideoPlayerController.kt`). Videos are streamed directly from the VPS URL (no downloading) and are always muted (`volume = 0f`).

### Multi-Display Support & Remote Configuration
The app is entirely remote-controlled and supports multiple devices through a single config.
- **Config URL:** `http://159.195.69.206/config.json` (Managed in `PlayerActivity.kt`).
- **Local Display ID:** Each device stores a unique string ID in SharedPreferences (`DisplayIdStore.kt`).
- **Setup Screen:** If no ID is set, or if the config specifies `"require_id_setup": true`, the `DisplayIdSetupActivity` is launched so the user can enter/update the device's ID. *Note: If `require_id_setup` is left as `true` in the remote config, the device will ALWAYS return to the setup screen on launch/reboot. It must be manually set to `false` on the server once the ID is entered to allow the Kiosk loop to run.*
- **Cache-Busting:** The app appends a timestamp to the URL (`?t=<timestamp>`) to bypass any Nginx/Provider caching and guarantee the freshest config is loaded.
- **Cleartext HTTP:** Because the VPS does not have an SSL certificate yet, `android:usesCleartextTraffic="true"` is explicitly set in `AndroidManifest.xml` to allow `http://` traffic (which is blocked by default in Android 9+).
- **Fallback Behavior:** If the network request fails (e.g. timeout), the `ConfigManager.kt` falls back to the last successfully parsed JSON saved in SharedPreferences.

### Example JSON Format (config.json)
```json
{
  "version": 1,
  "displays": [
    {
      "id": "lobby",
      "require_id_setup": false,
      "mode": "web",
      "video_url": null,
      "web_url": "https://example.com",
      "reload_interval_sec": 0,
      "reset_token": "123"
    },
    {
      "id": "room",
      "require_id_setup": true,
      "mode": "video",
      "video_url": "http://159.195.69.206/noadmin.mp4",
      "web_url": null,
      "reload_interval_sec": 0
    }
  ]
}
```
*Note: The `video_url` must be a valid, fully qualified absolute HTTP/HTTPS path for ExoPlayer to stream it.*

### Reset tokens & Periodic Polling
Each display object in the configuration can optionally include a `reset_token` field (which should be a string, e.g., `"1"`, `"2025-03-13T09:30:00Z"`, etc.). 
- **Behavior:** The app stores the last seen `reset_token` locally per device. On startup or when a config is reloaded, if the remote config includes a `reset_token` that differs from the locally stored one, the app will perform a reset. It stops any playing video or web view, updates its saved token, and completely reinitializes the display.
- **Usage:** This allows remote forcing of a "clean slate" on a specific display without requiring user interaction on the device itself. Simply alter the `reset_token` string for that screen in `config.json` on the server.
- **Polling:** To ensure the app notices changes to the config while running, `PlayerActivity` utilizes Kotlin Coroutines to periodically poll the server. The interval is defined by `reload_interval_sec`. If `reload_interval_sec` is `0` or missing, a fallback interval of 60 seconds is used to ensure the app never permanently loses touch with the server. All reset checks from the polling mechanism must be run on the Main Thread (`Dispatchers.Main`) to safely interact with UI components like ExoPlayer and WebView.

## Important Constraints & Rules for Agents
- **Do not refactor the config logic** to read local raw/asset files unless explicitly requested. The remote VPS URL is intentional.
- **Do not remove `usesCleartextTraffic="true"`** from the Manifest, as it breaks the VPS connection.
- **ExoPlayer Settings:** In `VideoPlayerController.kt`, `setBufferDurationsMs()` is used instead of the deprecated `setBufferMs()`. Do not downgrade or change the Media3 API usage without good reason.
- **WebView Settings:** `setAppCacheEnabled` was removed from `WebViewController.kt` because it causes compile errors in modern Android versions (deprecated). Do not re-add it.
- **Immersive Mode:** `PlayerActivity.kt` handles hiding the status/navigation bars. It contains legacy code for `< API 30` and modern `WindowInsetsController` code.
- **Deployment:** The user tests this app on a local Emulator (`emulator-5554`) via `adb install` and runs it via `adb shell monkey -p de.displayware.app 1`. Provide exact shell commands when asking the user to deploy.

## Auto-Start (Kiosk Boot)
The app runs automatically after a device reboot to maintain the digital signage loop via `BootReceiver.kt`.
- **System Constraints:** It uses `Intent.ACTION_BOOT_COMPLETED`. In Android 10+, starting an Activity from a background receiver is heavily restricted, so we added `SYSTEM_ALERT_WINDOW` permission to allow it to draw over other apps (some Kiosk/MDM environments automatically grant this).
- **Testing Warning:** Emulators will frequently block manual `adb shell am broadcast -a android.intent.action.BOOT_COMPLETED` due to Android security (UID 2000 permission denial). 
- **Workaround:** We introduced a custom intent (`de.displayware.app.TEST_BOOT_RECEIVER`) in the Manifest. To safely test the background Activity start logic without restarting the emulator, run:
  ```bash
  adb shell am broadcast -a de.displayware.app.TEST_BOOT_RECEIVER -n de.displayware.app/.boot.BootReceiver
  ```

## Server-Side Context
The user hosts the config file on an Ubuntu VPS (`159.195.69.206`) using `Nginx`.
- The `config.json` is located in `/home/signage/public_html/`.
- Nginx root is pointed to this directory.
- Edits to the config file are done by the user (usually via FileZilla with the user `signage`).

## Current State / Next Steps
The app currently successfully fetches the JSON from the VPS and updates the View. Any new feature should be built around this existing, working remote-fetch foundation.


<context>
1. Previous Conversation:
  - The conversation started with implementing an auto-start feature via a `BootReceiver` for an Android Kiosk App (`de.displayware.app`) so that the app opens automatically upon device boot.
  - Due to Android 10+ background restriction policies, a `SYSTEM_ALERT_WINDOW` permission was added and a specific test intent (`de.displayware.app.TEST_BOOT_RECEIVER`) was configured to bypass ADB shell execution blocks during emulator testing.
  - Following the boot setup, the ExoPlayer functionality was modified to stream MP4 videos directly from a remote HTTP/HTTPS URL and mute the playback rather than downloading the video locally.
  - The user then requested a multi-display support mechanism without writing any code themselves. This involved shifting from a single JSON configuration object to an array of `displays` within a root JSON object.

2. Current Work:
  - Implementing multi-display support entirely through code generation.
  - The JSON schema parsing was updated (`DisplayConfig.kt`, `ConfigManager.kt`) to support multiple display objects via Moshi.
  - A local SharedPreferences wrapper (`DisplayIdStore.kt`) was added to handle device-specific display IDs.
  - A new setup screen (`DisplayIdSetupActivity.kt`, `activity_display_id_setup.xml`) was created for the user to configure the display ID if one isn't present or if explicitly forced by the config.
  - The main logic in `PlayerActivity.kt` was adapted to route the application flow dynamically based on the current display ID and the `require_id_setup` flag fetched from the VPS JSON.
  - Documentation (`AGENT.md`, `README.md`) was updated extensively to reflect the multi-display requirements, the remote video streaming specifics, and testing caveats.

3. Key Technical Concepts:
  - **Android Components**: BroadcastReceivers (`BOOT_COMPLETED`), intent filters, `SharedPreferences`, `Activity` routing.
  - **ExoPlayer (Media3)**: Direct URL streaming (`MediaItem.fromUri`), looping (`REPEAT_MODE_ALL`), volume manipulation.
  - **Networking & Parsing**: `HttpURLConnection` and Moshi JSON adapter (`KotlinJsonAdapterFactory`) parsing nested arrays.
  - **Kiosk Specifics**: Overcoming Android background limitations, bypassing adb uid restrictions, immersive mode.

4. Relevant Files and Code:
  - `app/src/main/java/de/displayware/app/boot/BootReceiver.kt`: Handles `BOOT_COMPLETED` and `TEST_BOOT_RECEIVER` custom intents to auto-launch `PlayerActivity`.
  - `app/src/main/java/de/displayware/app/player/VideoPlayerController.kt`: Updated to accept `url: String` instead of `File`, setting `volume = 0f` and retaining repeat loops.
  - `app/src/main/java/de/displayware/app/config/DisplayConfig.kt`: Defined data classes `DisplayConfigRoot` and `DisplayEntry` to handle multiple configuration blocks.
  - `app/src/main/java/de/displayware/app/config/ConfigManager.kt`: Now uses `HttpURLConnection` to fetch JSON, decodes via Moshi to `DisplayConfigRoot`.
  - `app/src/main/java/de/displayware/app/config/DisplayIdStore.kt`: Wraps SharedPreferences logic for setting and retrieving the `display_id`.
  - `app/src/main/java/de/displayware/app/ui/DisplayIdSetupActivity.kt`: UI for ID setup that saves the inputted ID and triggers `PlayerActivity`.
  - `app/src/main/res/layout/activity_display_id_setup.xml`: Layout for the Setup Screen.
  - `app/src/main/java/de/displayware/app/ui/PlayerActivity.kt`: Uses `resolveDisplayAndMaybeStartSetup` logic to route the viewer based on config conditions.
  - `app/src/main/AndroidManifest.xml`: Includes `SYSTEM_ALERT_WINDOW` permission, `BootReceiver` configurations, and declares `DisplayIdSetupActivity`.
  - `AGENT.md` & `README.md`: Act as extensive developer hand-off context containing JSON examples and execution warnings.

5. Problem Solving:
  - **ADB Permission Denial (UID 2000)**: Simulating `BOOT_COMPLETED` through ADB was blocked. A custom action (`de.displayware.app.TEST_BOOT_RECEIVER`) was added to safely trigger the background logic in the emulator.
  - **Video Streaming Failure**: Explained that raw IP addresses without SSL fail with `https://`. Recommended switching to `http://` which ExoPlayer allows natively if `usesCleartextTraffic=true` is set.
  - **Dependency Conflicts**: A first implementation of `ConfigManager` used `OkHttpClient`, resulting in build errors. It was successfully rolled back to `HttpURLConnection` to respect constraints.
  - **Infinite Setup Loop**: Addressed user confusion regarding `require_id_setup: true`. Clarified that leaving it `true` in remote JSON forces the loop intentionally; no code changes were needed per user request.

6. Pending Tasks and Next Steps:
  - All requested tasks are 100% complete and successfully pushed to the `main` GitHub branch.
  - Await further instructions from the user.
</context>
<task_progress>
- [x] Create `DisplayIdStore.kt`
- [x] Create `activity_display_id_setup.xml` layout
- [x] Create `DisplayIdSetupActivity.kt`
- [x] Update `DisplayConfig.kt` to the new list-based JSON schema
- [x] Update `ConfigManager.kt` to parse the new schema
- [x] Update `PlayerActivity.kt` to route based on Display ID
- [x] Add `DisplayIdSetupActivity` to `AndroidManifest.xml`
- [x] Fix setup infinite loop by passing an intent extra from setup to player (Skipped per user request)
- [x] Update AGENT.md with multi-display logic
- [x] Commit and push changes to GitHub
</task_progress>