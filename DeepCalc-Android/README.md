# DeepCalc Android

A separate native Android implementation of DeepCalc, built with Kotlin and Jetpack Compose. This project is based on the current DeepCalc web app behaviour and is kept separate from the website/PWA source.

## Included features

- Multi-line notepad calculator with live per-line results.
- DeepCalc expression syntax: `+`, `−`, `×`, `÷`, `%`, `^`, `sqrt(...)`, variables (`$name`), line references (`$1`) and comments (`#`).
- Contextual percentages and line-reference shifting when lines are inserted or removed.
- Per-line notes, custom calculator keyboard, system keyboard toggle and long-press actions.
- Active-sheet autosave and History (up to 100 sheets), with open, rename and delete.
- Copy Sheet, Copy Complete, copy expression/result, share, Guide and About.
- System, Light, Dark and AMOLED themes with five accent palettes.
- Adaptive Compose layout using available width and scrollable content.

## Project structure

This folder is intended to live at `DeepCalc-Android/` in the existing repository. The repository-level GitHub Actions workflow is supplied separately as `android-native.yml`; GitHub only discovers workflow files under the repository root's `.github/workflows/` directory.

## Build

Requirements: Java 17, Android SDK platform 36, Android Build Tools 36.0.0 and Gradle 8.14.3.

```bash
gradle --no-daemon :app:testDebugUnitTest :app:assembleDebug :app:assembleRelease :app:bundleRelease
```

The project uses Android Gradle Plugin 8.13.2, Kotlin 2.3.21 and Jetpack Compose. GitHub Actions installs Gradle 8.14.3 for the build.

## Signing

GitHub Actions can sign the release APK/AAB when these repository secrets are configured: `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS` and `ANDROID_KEY_PASSWORD`. No keystore or password is included in this source archive.

## Verification status

This is a native port preview, not yet a verified one-to-one replacement. Focused JUnit tests are included for calculator behaviour, but they have **not yet been run in an Android/Gradle build**. This environment does not have the Android SDK or Gradle, so no APK has been compiled here. UI parity, screen-size behaviour, persistence across process death, accessibility and device testing remain to be verified.

The current web DeepCalc remains the reference implementation. The native project does not modify or replace the web app.
