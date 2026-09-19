# AetherAudio Pro

An offline-first Android music player: Kotlin + Jetpack Compose (Material 3), Media3/ExoPlayer
playback, Room-backed local library, and a real on-device equalizer. Built from the
`AetherAudio Pro` system architecture / UI-UX blueprint and the follow-up screen list
(bottom navigation, mini player, full library browser, folder-scoped scanning, queue management,
audio output panel, equalizer, synced lyrics, metadata editor).

## Quick start

**Option A — GitHub Actions (no local Android Studio needed)**
1. Push this whole folder to a new GitHub repo (see "Push to GitHub" below).
2. Go to the repo's **Actions** tab → the `Build APK` workflow runs automatically on push.
3. Open the finished run → download the `AetherAudioPro-debug-apk` artifact → unzip → install
   the `.apk` on an Android 12+ device (enable "install unknown apps" for whatever app you use
   to open it).
4. To get a release build, trigger the workflow manually from the Actions tab
   ("Run workflow") — it also builds an **unsigned** release APK
   (`AetherAudioPro-release-unsigned-apk`). Unsigned release APKs can't be installed as-is;
   see "Signing a release build" below if you want one you can distribute.

**Option B — Android Studio**
1. Open this folder in Android Studio (Koala/2024.1+ recommended).
2. If prompted about a missing Gradle wrapper, let Android Studio generate one (or run
   `gradle wrapper --gradle-version 8.9` once if you have Gradle installed locally) — it isn't
   committed here so the repo stays free of binary jar files.
3. Let it sync, then Run on a device/emulator running API 31+.

## Push to GitHub

```bash
cd AetherAudioPro
git init
git add .
git commit -m "AetherAudio Pro: initial scaffold"
git branch -M main
git remote add origin https://github.com/<you>/<repo>.git
git push -u origin main
```

The Actions workflow (`.github/workflows/build-apk.yml`) needs no secrets for the debug build —
it just runs `gradle assembleDebug` using a provisioned Gradle distribution (no committed
`gradlew`/wrapper jar required).

## What's genuinely implemented (not mocked)

- **Navigation shell** — bottom bar on phones, a `NavigationRail` on wide/unfolded screens
  (`WindowSizeClass`), with a persistent mini player docked above it.
- **Now Playing** — the screen you specifically asked to be full of motion: tap-to-spin vinyl
  rotation, a pulsing Play/Pause ring, an animated draggable waveform seekbar, swipe-to-skip and
  swipe-down-to-collapse gestures, side-fade marquee title text.
- **Library** — Songs / Albums / Artists / Genres / Folders / Recently Added / Favorites tabs,
  all backed by a real Room cache.
- **Folder-scoped scanning** — you pick folders via the system folder picker (Storage Access
  Framework); only tracks under those folders are indexed. WhatsApp Audio, Call Recordings and
  Voice Notes are always skipped, even inside a selected tree.
- **Queue** — reorder (up/down), swipe-style remove, clear-keep-current, add/play-next.
- **Playback engine** — Media3 `ExoPlayer` in a `MediaSessionService`: system notification,
  lock-screen controls, Bluetooth/wired media-button handling, and auto-pause on headset
  disconnect, all for free from Media3's session APIs.
- **Equalizer** — a real, working chain using Android's built-in `Equalizer` / `BassBoost` /
  `PresetReverb` audio effects attached to the player's audio session. Presets (Flat/Bass/Vocal/
  Rock/Classical/Custom), live band sliders, and a frequency-curve visualization.
- **Audio Output screen** — reads actual `AudioManager`/`AudioDeviceInfo` data: connected
  speaker/Bluetooth/wired/USB devices, their reported sample rates and channel counts.
- **Search** — offline, live, across title/artist/album/genre/folder path.
- **Lyrics** — parses standard `.lrc` sidecar files and auto-scrolls to the active line, with a
  clean fullscreen view.
- **Metadata editor** — writes Title/Artist/Album/Year/Track/Genre back through MediaStore's
  ContentResolver (works without extra permissions on API 29+).
- **Theme** — restrained "liquid glass" panels (low-alpha fill + hairline border, not heavy blur
  everywhere), bold large headers, dark/light/AMOLED-true-black, and a `Palette`-derived accent
  color pulled from the current track's artwork.

## Scoped out (scaffolded, not fully built) — and why

The original brief also asks for a few things that are genuinely substantial standalone
engineering efforts, not small features. Rather than fake them with something that looks right
but doesn't work, they're left as clearly-marked seams:

- **Real-time pitch-preserving tempo stretch, independent pitch shift, BPM auto-detection, a
  true custom 32-band parametric EQ.** These need a native (NDK/C++) audio pipeline — typically
  built on FFmpeg or a phase-vocoder library, cross-compiled per ABI, bridged over JNI, and
  spliced into ExoPlayer's renderer chain. That's realistically a multi-week specialized-audio
  project on its own. The seam for it is `NativeDspEngine` in
  `playback/AudioEffectsManager.kt` — implement it and wire it into `PlaybackService` when
  you're ready to tackle it. What ships today is a genuinely functional (if platform-limited,
  usually 5-6 band) equalizer via Android's built-in AudioFX instead.
- **Android Auto compatibility.** Needs its own manifest declarations, a
  `MediaLibraryService` browse tree, and testing in the Android Auto desktop head unit. The
  current `PlaybackService` is a regular `MediaSessionService`, which is most of the plumbing
  Android Auto needs, but the browse-tree/car-screen work itself isn't done.
- **Full ID3v2 tag writing** (embedded lyrics frames, replacing embedded cover art bytes).
  MediaStore's ContentResolver (used today) can rewrite its own index columns but can't rewrite
  arbitrary ID3 frames inside the file. A dedicated tagging library would be the next step.
- **Per-track waveform amplitude rendering.** The Now Playing seekbar currently renders a
  stylistically-correct but synthetic waveform. A real one means decoding each file once at
  import time and caching amplitude peaks — straightforward, just not done here.

## Project layout

```
app/src/main/kotlin/com/aetheraudio/pro/
  data/            Room entities, DAOs, MusicRepository (MediaStore scanning), SettingsRepository
  playback/        PlaybackService (Media3), PlayerController (StateFlow API), AudioEffectsManager
  ui/theme/        Color/Type/Theme — the visual identity
  ui/navigation/   AetherApp (adaptive shell), Destinations
  ui/nowplaying/   The main Now Playing screen + its animations
  ui/home/ ui/library/ ui/folders/ ui/playlists/ ui/settings/ ui/search/
  ui/eq/ ui/audiooutput/ ui/lyrics/ ui/metadata/
  ui/components/   MiniPlayer, QueueBottomSheet, AlbumArt, MarqueeText
```

## Signing a release build

The release build type has no signing config attached, so `assembleRelease` produces an
**unsigned** APK you can't install directly. To make an installable release build:

1. Generate a keystore: `keytool -genkeypair -v -keystore release.keystore -alias aether -keyalg RSA -keysize 2048 -validity 10000`
2. Add a `signingConfigs { create("release") { ... } }` block to `app/build.gradle.kts`
   pointing at it, and reference it from `buildTypes.release.signingConfig`.
3. For CI signing, store the keystore (base64) and passwords as GitHub Actions secrets rather
   than committing them.

## Permissions

- `READ_MEDIA_AUDIO` (API 33+) / `READ_EXTERNAL_STORAGE` (API ≤32) — to read the audio files in
  folders you select.
- `POST_NOTIFICATIONS`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK` — for the playback notification.
- `BLUETOOTH_CONNECT`, `MODIFY_AUDIO_SETTINGS` — for output-device detection and DSP.

No internet permission is requested; the app is 100% offline by design.
