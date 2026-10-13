# Changelog

All notable changes to **MediaVault** will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.1.0] - 2026-10-13

### Added
- **App Disguise Mode**: Fully functional scientific Calculator disguise that unlocks the vault upon entering secret PIN followed by `=`.
- **Decoy PIN (Duress Mode)**: Alternate PIN that opens a completely empty, safe vault to protect user data under duress.
- **Panic PIN (Silent Wipe)**: Emergency PIN that triggers an instant, silent purge of all vault files and database records.
- **Hardware AES-256 GCM**: Android KeyStore-backed streaming cryptographic cipher (`VaultCipher`).
- **Comprehensive Documentation**: Added `USER_GUIDE.md`, `SECURITY.md`, and `ARCHITECTURE.md`.

### Fixed
- Fixed `ForegroundServiceDidNotStartInTimeException` by guaranteeing immediate `startForeground()` invocation during service creation.

---

## [1.0.0] - 2026-10-13

### Added
- Initial release of MediaVault.
- Private internal storage isolation (`filesDir + "/vault/"`) with `.nomedia` indexing shield.
- All-in-one media player powered by AndroidX Media3 (ExoPlayer).
- Background audio playback with notification controls and audio focus management.
- Hardware equalizer with presets and 5-band customization.
- Sleep timer with live countdown.
- Full-screen gesture-controlled video player.
- High-res photo viewer with pinch-to-zoom (up to 4x) and pan gestures.
- Salted SHA-256 Master PIN lock and Biometric unlock.
- SAF document picker with streaming multi-gigabyte import.
