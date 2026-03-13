# Display App (MVP Kiosk)

Eine minimalistische, robuste Android‑Kiosk‑App für Werbedisplays. Optimiert für Android 9 und Geräte mit geringem RAM (2 GB).

## Features
- **Remote Config**: Lädt Konfiguration (JSON) von einer URL.
- **Multi-Display Support**: Steuert unbegrenzt viele Displays zentral über ein einziges JSON-File per Display-ID.
- **Video Loop**: Streamt MP4-Videos direkt von einer URL im Endlos-Loop (gemutet).
- **Web View**: Zeigt Webseiten im Fullscreen an.
- **Immersive Mode**: Versteckt Systembars (Status/Navigation).
- **Auto-Start**: Startet automatisch nach System-Boot.
- **Offline Fallback**: Nutzt die letzte gültige Konfiguration/Video, wenn kein Netzwerk verfügbar ist.

## JSON Config Schema (Multi-Display)
Die App erwartet ein JSON an der konfigurierten URL (siehe `PlayerActivity.kt` -> `configUrlBase`). Jedes Gerät sucht nach dem Start in der Liste `displays` nach seiner lokalen ID.

```json
{
  "version": 1,
  "displays": [
    {
      "id": "reception",
      "require_id_setup": false,
      "mode": "video",
      "video_url": "http://dein-server.de/video.mp4",
      "web_url": null,
      "reload_interval_sec": 0
    }
  ]
}
```

- `id`: Eindeutiger Name des Displays.
- `require_id_setup`: Wenn `true`, zwingt es das Gerät bei Start in die Setup-Maske. (Muss nach Konfiguration auf `false` gesetzt werden).
- `mode`: Entweder `"video"` oder `"web"`.
- `video_url`: Absolute URL zum MP4-Stream (z.B. `http://.../video.mp4`). Wird live gestreamt.
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

### Build und Neuinstallation der App
Projekt klonen und in das Verzeichnis wechseln:
```bash
./gradlew :app:assembleDebug --no-daemon && adb shell am force-stop de.displayware.app && adb install -r app/build/outputs/apk/debug/app-debug.apk && adb shell monkey -p de.displayware.app 1
```

### Installation auf Emulator/Gerät
```bash
./gradlew :app:assembleDebug --no-daemon   
adb install -r app/build/outputs/apk/debug/app-debug.apk     
```

### Installation auf Emulator/Gerät mit spezifischer Nummer
```bash  
adb -s emulator-5556 install -r app/build/outputs/apk/debug/app-debug.apk     
```

### App manuell starten (via adb)
```bash
adb shell am start -n de.displayware.app/.ui.PlayerActivity
```

### Neustart der App Einstellungen
```bash
adb shell monkey -p de.displayware.app   

adb shell am broadcast -a de.displayware.app.TEST_BOOT_RECEIVER -n de.displayware.app/.boot.BootReceiver
```

### Neustart der App Einstellungen eines bestimmten Users
```bash
adb -s emulator-5554 shell monkey -p de.displayware.app 1
```

### App beenden (via adb)
Da es sich um eine Kiosk-App handelt, lässt sie sich am besten über das Terminal beenden:
```bash
adb shell am force-stop de.displayware.app
```

### Kommandozyklus für App Update
```bash
scp /Volumes/Privat/VSC/display-app/app/build/outputs/apk/debug/app-debug.apk \
signage@159.195.69.206:/home/signage/public_html/fdroid/repo/

cd /home/signage/public_html/fdroid

fdroid update

http://159.195.69.206/fdroid/repo/
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
