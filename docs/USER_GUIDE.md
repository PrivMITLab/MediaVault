# MediaVault — User Guide

Welcome to **MediaVault**. This guide explains how to secure, play, and disguise your private media files.

---

## 1. Initial Setup

1. **Create Master PIN**: On first launch, create a 4-to-6 digit Master PIN.
2. **Confirm PIN**: Re-enter the same PIN to activate your vault.
3. **Biometrics (Optional)**: If your phone supports fingerprint or face recognition, enable Biometric Unlock in Settings for instant entry.

---

## 2. Importing Media Files

### Via Storage Access Framework (SAF)
1. Tap the floating **`+`** button at the bottom right.
2. The Android document picker will appear.
3. Select one or more songs, videos, or photos (multi-select is supported).
4. MediaVault streams each file directly into your private internal sandbox (`/files/vault/`) and marks it with a `.nomedia` shield.
5. The original file is removed from public storage so it vanishes from Google Photos and Gallery apps immediately.

### Via System Share Sheet
1. In any app (such as WhatsApp, Downloads, or File Manager), tap **Share**.
2. Choose **MediaVault**.
3. MediaVault will automatically import and hide the file upon app opening.

---

## 3. Disguise Mode (Calculator Cover)

Want to completely hide the existence of the vault?

1. Go to **Settings** > **Stealth & Disguise**.
2. Turn on **Calculator Disguise**.
3. Next time the app opens, it presents a **fully functioning scientific calculator**.
4. To open the real vault:
   - Type your **PIN** and press **`=`** (e.g. `1234 =`).
   - Alternatively, long-press the calculator display screen for 2 seconds.

---

## 4. Duress & Emergency Modes

### Decoy PIN (Fake Vault)
- In **Settings** > **Decoy PIN**, set an alternate PIN (e.g. `0000`).
- If you are ever forced by someone to unlock the vault, enter the **Decoy PIN**.
- The app will open an **empty decoy vault** (showing 0 files), keeping your genuine media completely safe and undiscovered.

### Panic PIN (Silent Vault Wipe)
- In **Settings** > **Panic PIN**, set an emergency PIN (e.g. `9999`).
- If entered on the lock screen or calculator, MediaVault **silently purges all files and database records** within seconds.

---

## 5. Media Playback & Controls

### Music & Audio
- **Background Playback**: Audio continues playing with your screen locked or while using other apps.
- **Equalizer**: Open the player and tap **EQ** to adjust audio presets (Rock, Pop, Bass Boost, Vocal) or manually tune frequency bands.
- **Sleep Timer**: Tap **Sleep** to set an automatic timer (15m, 30m, 45m, 60m) after which audio playback stops.
- **Speed**: Adjust audio playback speed between 0.5x and 2.0x.

### Video Player
- Tap any video to open the full-screen player.
- **Aspect Ratio**: Cycle between Fit, Zoom, and Fill modes.
- **Gesture Seeking**: Tap ±10s buttons or scrub the progress bar.
- **Screen Lock**: Tap the Lock icon on the top right to disable touch input while watching.

### Photos
- Double-tap or pinch to zoom up to 4x.
- Swipe left/right between pictures.
- Tap **Share** to securely send a single photo to another application without exposing the vault directory.
