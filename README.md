# Display App (MVP Kiosk)

Eine minimalistische, robuste Android‑Kiosk‑App für Werbedisplays. Optimiert für Android 9 und Geräte mit geringem RAM (2 GB).

## Features
- **Remote Config**: Lädt Konfiguration (JSON) von einer URL.
- **Video Loop**: Spielt MP4-Videos im Endlos-Loop (lokales Caching).
- **Web View**: Zeigt Webseiten im Fullscreen an.
- **Immersive Mode**: Versteckt Systembars (Status/Navigation).
- **Auto-Start**: Startet automatisch nach System-Boot.
- **Offline Fallback**: Nutzt die letzte gültige Konfiguration/Video, wenn kein Netzwerk verfügbar ist.

## JSON Config Schema
Die App erwartet ein JSON an der konfigurierten URL (siehe `PlayerActivity.kt` -> `configUrl`):

```json
{
  "version": 1,
  "mode": "video",
  "video_url": "https://dein-server.de/video.mp4",
  "web_url": null,
  "reload_interval_sec": 0
}
```

- `mode`: Entweder `"video"` oder `"web"`.
- `video_url`: URL zum MP4 (wird lokal unter `files/videos/current.mp4` gespeichert).
- `web_url`: URL zur Webseite für den Web-Modus.

## Setup & Build (macOS)

### Voraussetzungen
- Android Studio (Panda oder neuer)
- Android SDK 34 installiert
- Android Emulator (AVD) erstellt (empfohlen: API 28/29 für Android 9 Test)

### Build via CLI
Projekt klonen und in das Verzeichnis wechseln:
```bash
./gradlew assembleDebug
```

### Installation auf Emulator/Gerät
```bash
./gradlew :app:assembleDebug --no-daemon   
adb install -r app/build/outputs/apk/debug/app-debug.apk     
```

### App manuell starten (via adb)
```bash
adb shell am start -n de.displayware.app/.ui.PlayerActivity
```

### Neustart der App Einstellungen
```bash
adb shell monkey -p de.displayware.app   
```

### App beenden (via adb)
Da es sich um eine Kiosk-App handelt, lässt sie sich am besten über das Terminal beenden:
```bash
adb shell am force-stop de.displayware.app
```

## Projektstruktur
- `ui/`: `PlayerActivity` (Einstiegspunkt, Fullscreen-Logik).
- `player/`: Controller für ExoPlayer (Video) und WebView.
- `config/`: `ConfigManager` für HTTP-Requests und JSON-Parsing (Moshi).
- `boot/`: `BootReceiver` für den Auto-Start.

## Limitierungen (MVP)
- Nur ein Item (Video oder URL), keine Playlist.
- Kein automatisches Refreshing während der Laufzeit (nur beim Start).
- Kein Authentifizierungs-Support für URLs.
