# MediaVault 🛡️🔒

> **A completely private, offline media player and hidden vault for Android.**

[![Android](https://img.shields.io/badge/Platform-Android-3DDC84.svg?style=flat&logo=android)](https://www.android.com/)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin%202.2-7F52FF.svg?style=flat&logo=kotlin)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4.svg?style=flat&logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![Media3](https://img.shields.io/badge/Audio%2FVideo-AndroidX%20Media3%20ExoPlayer-FBBC04.svg?style=flat)](https://developer.android.com/media/media3)
[![Security](https://img.shields.io/badge/Security-AES--256%20%7C%20FLAG__SECURE-10B981.svg?style=flat)](https://developer.android.com/privacy-and-security)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

---

## 🌟 Overview

**MediaVault** is built for complete confidentiality. Any media file (audio, video, or photo) added to MediaVault becomes **completely invisible** to all other apps on your device — Google Photos, Gallery apps, WhatsApp media scanners, and File Managers.

The files can **only** be played, viewed, or decrypted from inside MediaVault.

### 🚫 100% Offline Integrity
- **Zero Internet Permissions** declared in `AndroidManifest.xml`.
- **Zero Tracking / Analytics / Telemetry**.
- Your files never touch external servers or cloud storage.

---

## ⚡ Core Features

### 1. 🗄️ Hidden & Sandboxed Storage
- **Internal Private App Storage**: Files are stored exclusively in `context.filesDir + "/vault/"`, which is isolated by Linux kernel UID sandbox permissions.
- **`.nomedia` Indexing Shield**: Guaranteed `.nomedia` barrier halts Android MediaScanner from indexing any files in the directory.
- **SAF Streaming Import**: Stream multi-gigabyte 4K videos or lossless FLAC music directly without memory exhaustion.

### 2. 🎭 App Disguise Mode (Calculator Cover)
- Disguises the app icon and interface as a fully functional, working **Calculator**.
- Enter your secret PIN followed by `=` to reveal the real vault!
- Long-pressing the calculator screen also offers an emergency gesture transition.

### 3. 🔐 Multi-Tier Security
- **Master PIN**: 4 to 6-digit numeric PIN hashed with a salted SHA-256 algorithm.
- **Biometric Unlock**: Fingerprint and Face Unlock via `AndroidX BiometricPrompt`.
- **Decoy PIN**: Unlocks an empty fake vault (0 files) to protect your privacy under duress.
- **Panic PIN**: Entering this emergency PIN immediately and silently wipes all files and database records.
- **`FLAG_SECURE`**: Blocks screenshots, screen recordings, and obscures recent app switcher previews.
- **Auto-Lock**: Automatic re-locking when app is backgrounded (Immediate, 30s, 1m, 5m).

### 4. 🎵 All-in-One Media Player
- **Audio Formats**: MP3, AAC, WAV, OGG, FLAC, M4A.
- **Video Formats**: MP4, MKV, AVI, MOV, WebM.
- **Photo Formats**: JPG, PNG, GIF, WebP, BMP.
- **Background Playback**: Powered by Media3 `MediaSessionService` with lock-screen media controls, foreground service survival, and audio focus ducking/pausing.
- **Equalizer**: Android hardware equalizer with presets and 5-band dB sliders.
- **Sleep Timer**: Auto-pause playback after 15, 30, 45, 60 minutes, or custom timer.
- **Gesture Video Player**: Aspect ratio toggles (Fit, Zoom, Fill), 0.5x–2.0x playback speeds, seek buttons, and screen lock overlay.
- **Photo Viewer**: High-resolution zoom/pan gesture engine with secure `FileProvider` sharing.

---

## 📂 Project Architecture

```
com.example/
├── data/
│   ├── db/              # Room Database, Converters, MediaDao
│   ├── model/           # MediaItem, Playlist, MediaType entities
│   ├── repository/      # VaultRepository & metadata extraction
│   └── storage/         # VaultStorageManager (SAF streaming, .nomedia)
├── playback/
│   ├── MediaPlaybackService.kt   # Foreground MediaSessionService
│   └── PlayerManager.kt          # ExoPlayer, Equalizer, Sleep Timer
├── security/
│   ├── SecurityManager.kt        # PIN hashing, Decoy & Panic PINs, Biometrics
│   └── VaultCipher.kt            # Android KeyStore AES-256 GCM streaming
├── ui/
│   ├── components/      # CommonDialogs, MediaCards, VaultNavigation
│   ├── screens/
│   │   ├── audio/       # Audio list, playlists, full-screen player
│   │   ├── disguise/    # Fully functional Calculator Disguise
│   │   ├── lock/        # Master PIN lock screen with keypad
│   │   ├── photos/      # Grid & pinch-to-zoom photo viewer
│   │   ├── settings/    # Vault security, encryption, and storage tools
│   │   └── video/       # Video gallery & custom ExoPlayer screen
│   ├── theme/           # Obsidian Black & Stealth Indigo theme
│   └── viewmodel/       # VaultViewModel & StateFlow architecture
└── MainActivity.kt      # FLAG_SECURE & Edge-to-edge Compose host
```

---

## 🚀 Building & Running

### Requirements
- Android SDK 24+ (Android 7.0 Nougat or newer)
- Android Studio Ladybug / Meerkat or Gradle 9.x
- Kotlin 2.2+

### Run Tests
```bash
gradle :app:testDebugUnitTest
```

### Assemble APK
```bash
gradle assembleDebug
```

---

## 📖 Documentation
Detailed guides are available in the [`docs/`](docs/) directory:
- [User Guide](docs/USER_GUIDE.md) — How to import, lock, disguise, and manage media.
- [Security Architecture](docs/SECURITY.md) — Cryptographic design, KeyStore specs, duress modes.
- [Architecture & Design](docs/ARCHITECTURE.md) — Component diagrams and MVVM data lifecycle.

---

## 📄 License
This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.
