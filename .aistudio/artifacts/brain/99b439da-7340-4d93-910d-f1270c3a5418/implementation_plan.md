# MediaVault — Offline Private Media Player & Hidden Vault

MediaVault is an offline, zero-telemetry Android application that securely isolates private media (audio, video, photos) in hidden app-internal storage guarded by `.nomedia` indexing shields, biometric/PIN access control, and a full-featured Media3 background playback engine.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> The following architectural parameters have been established based on your preferences:
> - **Storage Strategy**: Files are imported directly into the application's private internal storage directory (`context.filesDir + "/vault/"`) with `.nomedia` marking, ensuring media is completely hidden from external Gallery and File Manager apps without high-latency decryption delays during multi-gigabyte video playback.
> - **Authentication & Visual Presentation**: Clean, dark security lock screen with a 4-6 digit numeric PIN (hashed via SHA-256) paired with BiometricPrompt (fingerprint/face recognition) and immediate background auto-lock.
> - **Complete Offline Operation**: Zero internet permissions declared; 100% of indexing, playback, and metadata extraction happens locally on device.

---

## 1. Overview & Core Concept

- **What It Does**: Provides an impenetrable media shelter. Files imported via the Android Storage Access Framework (SAF) or system share sheet are moved into protected internal storage, purged from the public MediaStore, and made playable exclusively within MediaVault.
- **Target Audience & Persona**: Privacy-conscious individuals seeking confidentiality for personal recordings, photos, documents, and media playlists without relying on cloud services.
- **Key Value**: True privacy without sacrificing playback quality: smooth 4K/60fps video playback with gesture controls, background audio playback with lock screen media notifications, and a responsive photo viewer with pinch-to-zoom.

---

## 2. User Experience & Visual Design

### Key User Flows

1. **First Launch & Security Setup**:
   - The user is greeted by a streamlined security enrollment flow.
   - User creates a 4-to-6 digit master PIN with confirmation.
   - User optionally enables Biometric Unlock (Fingerprint / Face ID).
2. **Vault Navigation (4 Main Tabs)**:
   - **Audio Tab**: Lists music tracks, recently played carousel, quick playlist creation, search bar, sort options (title, artist, duration, date), and a persistent mini-player bar. Tapping opens the full-screen music player with scrubber, queue, repeat/shuffle, and equalizer.
   - **Video Tab**: Grid/list of imported videos with video thumbnails, durations, resolution tags, and search. Tapping launches the custom ExoPlayer video interface supporting gesture controls (brightness/volume sliders, aspect ratio fit/fill, playback speeds 0.5x–2.0x).
   - **Photos Tab**: Adaptive grid with multi-column zoom toggle, date headers, and instant photo viewer supporting pinch-to-zoom, pan, swipe navigation, and secure file info inspection.
   - **Settings Tab**: Vault statistics (storage used, file counts), Change PIN, Biometric toggle, Auto-lock timeout, Export/Backup tool, and Clear Vault emergency option.
3. **SAF Media Import Flow**:
   - Floating action button (FAB) or TopBar import action opens the system document picker (`ACTION_OPEN_DOCUMENT` with multiple select).
   - Real-time streaming copy to internal storage with a progress indicator dialog, followed by MediaStore deletion prompt/trigger.
4. **Background Media Playback**:
   - When playing audio and leaving the app or turning off the screen, Media3 `MediaSessionService` continues uninterrupted playback and displays standard Android lock-screen controls.

### Visual Identity & Theme

- **Aesthetic Direction**: Premium Deep Obsidian & Stealth Indigo. Polished, minimalist surfaces that convey security, privacy, and media focus.
- **Color Palette**:
  - Background: Obsidian Black (`#0D0F14`) & Pure Black (`#000000`)
  - Surface: Elevated Charcoal (`#161922`), Card borders (`#252A36`)
  - Primary / Accent: Electric Indigo (`#6366F1`) & Cobalt Glow (`#4F46E5`)
  - Secondary Accent: Emerald Privacy indicator (`#10B981`)
  - Text: Pure White (`#F9FAFB`), Muted Fog (`#94A3B8`)
- **Typography & Layout**:
  - Crisp typography hierarchy with high-contrast numerical readouts.
  - Generous 48dp touch targets, smooth rounded surfaces (16dp cards, 24dp dialogs), and subtle edge gradients.
- **Interactive Feedback**:
  - Smooth spring transitions between mini-player and expanded player.
  - Haptic feedback on PIN keypad presses.
  - Multi-select floating bottom action bar for batch deletion and playlist assignment.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Direct Internal Storage vs On-The-Fly Encryption**
  - *Chosen Approach*: Store media directly in `context.filesDir/vault/` with a `.nomedia` shield and SHA-256 PIN authentication.
  - *Why*: On Android, `context.filesDir` is completely sandboxed by Linux kernel UID permissions and inaccessible to non-rooted apps. Storing files directly allows ExoPlayer to stream 4K 60fps videos and high-res FLAC audio without multi-gigabyte memory buffering or disk decryption overhead.
- **Decision 2: Architecture & Service Boundary**
  - *Chosen Approach*: Media3 `MediaSessionService` paired with an application-level Player Connection controller.
  - *Why*: Ensures standard Android OS integration, foreground service survival during screen lock, proper audio focus management (calls, notifications), and lock screen media widget controls.
- **Decision 3: Room Database for Media Metadata**
  - *Chosen Approach*: Local SQLite Room database caching title, artist, duration, mimeType, favorite status, playlists, and relative file paths.
  - *Why*: Instant sub-millisecond filtering, searching, and sorting across thousands of files without scanning physical storage on every screen launch.

---

## 4. Technical Architecture & Data Strategy

### Architecture & Component Diagram

```
┌────────────────────────────────────────────────────────┐
│                      UI LAYER                          │
│  ┌──────────────┐ ┌──────────────┐ ┌────────────────┐  │
│  │ Lock Screen  │ │ Main Scaffold│ │ Media Viewers  │  │
│  │ (PIN / Bio)  │ │ (Audio/Video/│ │ (ExoPlayer &   │  │
│  │              │ │  Photo/Set)  │ │  Photo Zoom)   │  │
│  └──────┬───────┘ └──────┬───────┘ └───────┬────────┘  │
│         │                │                 │           │
│         ▼                ▼                 ▼           │
│  ┌──────────────────────────────────────────────────┐  │
│  │                VIEWMODEL LAYER                   │  │
│  │  VaultViewModel / PlayerViewModel / LockViewModel│  │
│  └───────────────────────┬──────────────────────────┘  │
└──────────────────────────┼─────────────────────────────┘
                           │
┌──────────────────────────▼─────────────────────────────┐
│                    SERVICE & DATA                      │
│  ┌──────────────────────┐   ┌────────────────────────┐ │
│  │ MediaPlaybackService │   │   VaultRepository      │ │
│  │ (Media3 ExoPlayer +  │   │  (SAF Stream Import +  │ │
│  │  MediaSession)       │   │   Metadata Extractor)  │ │
│  └──────────────────────┘   └───────────┬────────────┘ │
│                                         │              │
│         ┌───────────────────────────────┴──────────┐   │
│         ▼                                          ▼   │
│  ┌───────────────┐                       ┌───────────┐ │
│  │ Room Database │                       │ FilesDir  │ │
│  │ (Media Items, │                       │ /vault/   │ │
│  │  Playlists)   │                       │ (.nomedia)│ │
│  └───────────────┘                       └───────────┘ │
└────────────────────────────────────────────────────────┘
```

### Data Model & Entities

1. **`MediaItemEntity`**:
   - `id: Long` (Primary key)
   - `filePath: String` (Internal vault storage path)
   - `fileName: String`
   - `mediaType: MediaType` (`AUDIO`, `VIDEO`, `PHOTO`)
   - `mimeType: String`
   - `sizeBytes: Long`
   - `durationMs: Long` (0 for photos)
   - `title: String?`, `artist: String?`, `album: String?`
   - `width: Int`, `height: Int`
   - `dateAdded: Long`
   - `isFavorite: Boolean`
2. **`PlaylistEntity` & `PlaylistItemCrossRef`**:
   - Support custom audio and video playlists with reordering.
3. **`VaultPreferences` (DataStore & Encrypted Storage)**:
   - Salted SHA-256 PIN hash.
   - Biometric authentication enabled flag.
   - Auto-lock timeout setting.
   - Sort criteria & view style preferences.

---

## Execution Steps (Upon Approval)

1. **Dependencies & Manifest**:
   - Update `gradle/libs.versions.toml` and `app/build.gradle.kts` to enable `media3-exoplayer`, `media3-session`, `media3-ui`, `biometric`, `coil-compose`, `room`, and `navigation-compose`.
   - Update `AndroidManifest.xml` with permissions (`FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`, `POST_NOTIFICATIONS`, `USE_BIOMETRIC`, `WAKE_LOCK`), `MediaPlaybackService`, and `FLAG_SECURE` window protections.
2. **Core Storage & Database**:
   - Implement `VaultStorageManager` to initialize `/vault/`, manage `.nomedia`, stream SAF input streams to internal storage, and safely export.
   - Implement Room entities, DAOs, and repository layer with metadata extraction (`MediaMetadataRetriever`).
3. **Security & Lock Layer**:
   - Implement PIN hash authentication, biometric prompt wrapper, and auto-lock lifecycle observer.
4. **Background Media Playback**:
   - Implement `MediaPlaybackService` extending `MediaSessionService` with ExoPlayer instance, notification controls, and audio focus.
   - Build `PlayerController` to bind Compose UI with the active playback session.
5. **Jetpack Compose UI Screens**:
   - **Lock Screen**: PIN keypad with biometric shortcut button.
   - **Audio Screen & Player**: Track list, search, sort, playlists, persistent mini-player, and full-screen player dialog with EQ presets.
   - **Video Screen & Player**: Video grid/list, full-screen playback with custom controls, speed, and aspect ratio.
   - **Photos Screen & Viewer**: Photo grid, high-res viewer with gesture zoom/pan and file details.
   - **Settings Screen**: Vault stats, security preferences, and export/backup.
6. **Compile & Verification**:
   - Verify compilation using `compile_applet`.
