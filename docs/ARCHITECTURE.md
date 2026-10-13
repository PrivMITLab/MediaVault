# MediaVault — Architecture & Engineering Specification

This document details the software architecture, design patterns, and component interactions in MediaVault.

---

## 1. High-Level Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                      PRESENTATION LAYER                         │
│  ┌───────────────────────┐ ┌─────────────────────────────────┐  │
│  │ Calculator Disguise   │ │ Master Lock Screen              │  │
│  │ (Secret PIN Evaluator)│ │ (Keypad + BiometricPrompt)      │  │
│  └───────────┬───────────┘ └────────────────┬────────────────┘  │
│              │                              │                   │
│              ▼                              ▼                   │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │                     MainVaultScaffold                     │  │
│  │  ┌───────────┐ ┌───────────┐ ┌───────────┐ ┌───────────┐  │  │
│  │  │AudioScreen│ │VideoScreen│ │PhotoScreen│ │ Settings  │  │  │
│  │  └─────┬─────┘ └─────┬─────┘ └─────┬─────┘ └─────┬─────┘  │  │
│  └────────┼─────────────┼─────────────┼─────────────┼────────┘  │
│           ▼             ▼             ▼             ▼           │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │                      VaultViewModel                       │  │
│  │    StateFlows: filteredAudio, filteredVideos, isDecoyMode │  │
│  └──────────────────────────────┬────────────────────────────┘  │
└─────────────────────────────────┼───────────────────────────────┘
                                  │
┌─────────────────────────────────▼───────────────────────────────┐
│                       SERVICES & DATA                           │
│  ┌─────────────────────────┐       ┌─────────────────────────┐  │
│  │  MediaPlaybackService   │       │     SecurityManager     │  │
│  │  (MediaSessionService + │       │   (Master / Decoy /     │  │
│  │   ExoPlayer Engine)     │       │    Panic PIN Evaluator) │  │
│  └────────────┬────────────┘       └────────────┬────────────┘  │
│               │                                 │               │
│               ▼                                 ▼               │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │                      VaultRepository                      │  │
│  │   Coordinates Room Cache, VaultStorage, and Metadata      │  │
│  └─────────────┬───────────────────────────────┬─────────────┘  │
│                │                               │                │
│                ▼                               ▼                │
│  ┌───────────────────────────┐   ┌───────────────────────────┐  │
│  │     SQLite / Room DB      │   │    VaultStorageManager    │  │
│  │   (media_items, playlists│   │   (filesDir/vault/.nomedia│  │
│  │    playlist_items)        │   │    streaming SAF import)  │  │
│  └───────────────────────────┘   └───────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 2. Component Design & Patterns

### 1. Presentation & State Management
- **Jetpack Compose (Material 3)**: Pure Compose UI with zero legacy XML layouts.
- **MVVM Pattern**: `VaultViewModel` exposes immutable `StateFlow` streams (`filteredAudio`, `filteredVideos`, `filteredPhotos`). Composables subscribe via `collectAsState()`.
- **Decoy Mode Interceptor**: When the user unlocks using the Decoy PIN, `isDecoyMode` emits `true`, causing all UI lists to emit empty sets, rendering an unpopulated vault.

### 2. Media Playback & AndroidX Media3
- **Decoupled Architecture**: `PlayerManager` acts as the single source of truth for playback state across Compose screens.
- **`MediaPlaybackService`**: Inherits `MediaSessionService`. Manages background notifications and keeps the audio pipeline running when the screen is turned off.
- **Immediate Foreground Contract**: Calls `startForeground()` within milliseconds of creation to ensure strict Android OS compliance.

### 3. Local Database & Persistence
- **Room Database (`VaultDatabase`)**:
  - `media_items`: Caches metadata (title, artist, album, duration, resolution, size, favorite state).
  - `playlists` & `playlist_items`: Many-to-many relationship for custom audio playlists.
- **TypeConverters**: Serializes `MediaType` enum to SQLite-compatible representations.
