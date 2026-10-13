# MediaVault — Security Architecture

## 1. Threat Model & Principles

MediaVault is designed for **total on-device confidentiality**. It assumes:
1. **Adversary with Physical Device Access**: Someone inspecting the device, gallery, or file manager cannot discover vault files.
2. **Third-Party App Snooping**: Apps scanning `MediaStore` or external storage have zero access to vault content.
3. **Duress / Coerced Unlock**: The user may be forced to enter an unlock code; the app provides plausible deniability via Decoy PIN and emergency destruction via Panic PIN.
4. **Zero Cloud Trust**: No credentials, tokens, or files are ever transmitted off the device.

---

## 2. Storage Sandbox & MediaStore Shield

### Internal FilesDir Isolation
Media files reside exclusively under:
```
/data/user/0/<applicationId>/files/vault/
```
Under Linux UID isolation on Android:
- Other applications cannot read, write, or list files in this directory on non-rooted devices.
- Files persist across app updates, but are uninstalled if the app is removed.

### `.nomedia` Anti-Indexing Barrier
When initialized, MediaVault creates an empty `.nomedia` file:
```
/data/user/0/<applicationId>/files/vault/.nomedia
```
This flag tells the Android OS MediaScanner daemon to skip the directory during indexing scans, preventing any files from appearing in `MediaStore.Images`, `MediaStore.Video`, or `MediaStore.Audio`.

---

## 3. Cryptographic Implementation

### Salted SHA-256 PIN Hashing
PINs are never stored in plaintext:
1. When a PIN is created or updated, a random salt `S` is generated.
2. The hash is computed using `SHA-256(PIN + ":" + S)`.
3. The resultant 64-character hexadecimal digest and salt are stored in application-private `SharedPreferences`.

### Hardware KeyStore AES-256 GCM (`VaultCipher`)
When AES-256 Hardware Encryption is enabled:
- **Key Storage**: Keys are generated using Android KeyStore with alias `MediaVaultMasterKey`. The key material never leaves the secure enclave / hardware security module (HSM).
- **Cipher**: `AES/GCM/NoPadding` (Galois/Counter Mode with 256-bit key length and 128-bit authentication tag).
- **Initialization Vector (IV)**: A unique 12-byte IV is generated per encrypted file and prepended to the ciphertext.
- **Streaming**: Encryption and decryption operate in 64KB chunks to ensure multi-gigabyte media streams without exceeding RAM limitations.

---

## 4. Operating System Protections

### `FLAG_SECURE`
Every Activity in MediaVault enforces:
```kotlin
window.setFlags(
    WindowManager.LayoutParams.FLAG_SECURE,
    WindowManager.LayoutParams.FLAG_SECURE
)
```
This guarantees:
1. Screenshots and screen recordings are blocked at the Android SurfaceFlinger level.
2. The Android Recent Apps task switcher displays a blank/black thumbnail instead of revealing private media.

### Network Isolation
MediaVault requests **zero network permissions**:
- No `android.permission.INTERNET`
- No `android.permission.ACCESS_NETWORK_STATE`
The Android OS sandbox mathematically prohibits MediaVault from establishing socket connections, sending telemetry, or uploading data.
