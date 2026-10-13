# Contributing to MediaVault

Thank you for your interest in contributing to **MediaVault**! This project is dedicated to providing an impenetrable, 100% offline private media vault for Android users.

---

## 🛡️ Guiding Principles for Contributors

1. **Strict Offline Guarantee**: Never introduce dependencies or code that request network permissions (`INTERNET`, `ACCESS_NETWORK_STATE`) or send analytics/crash reports.
2. **Zero Insecure Storage**: Never write media to external public storage. All media must stay within `context.filesDir + "/vault/"`.
3. **`FLAG_SECURE` Compliance**: All Activities must maintain `FLAG_SECURE` to prevent screenshot leaks and task switcher surveillance.

---

## 🛠️ Development Setup

1. Fork and clone the repository.
2. Open the project in Android Studio (Ladybug / Meerkat or newer).
3. Ensure you have the Android 14/15 SDK and JDK 17 installed.
4. Run unit and Robolectric tests before submitting changes:
   ```bash
   gradle :app:testDebugUnitTest
   ```

---

## 📝 Pull Request Guidelines

1. Create a descriptive branch: `feature/your-feature-name` or `bugfix/issue-description`.
2. Keep PRs focused on a single feature or bug fix.
3. Update `CHANGELOG.md` with your additions under the `[Unreleased]` section.
4. Ensure all unit tests pass with zero regressions.
