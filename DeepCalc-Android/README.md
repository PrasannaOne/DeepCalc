# DeepCalc Native Android

A standalone native Android edition of DeepCalc built with Kotlin and Jetpack Compose. It lives in `DeepCalc-Android/` and does not replace or modify the web/PWA application at the repository root.

## Features

- Multiline notepad calculator with live per-line results.
- DeepCalc expression syntax: `+`, `−`, `×`, `÷`, `%`, `^`, `sqrt(...)`, variables (`$name`), line references (`$1`) and comments (`#`).
- Contextual percentages and line-reference shifting when rows are inserted or removed.
- Per-line notes, custom calculator keyboard, system keyboard toggle, cursor operations and long-press actions.
- Autosaved active sheet and local History (up to 100 sheets), with open, rename and delete.
- Copy Sheet, Copy Complete, copy expression/result, share, Guide and About.
- System, Light, Dark and AMOLED themes with five accent palettes.
- Adaptive Compose layout with scrollable content and width-aware sizing.

## Toolchain

This project uses a pinned, mutually compatible modern toolchain:

- Android Gradle Plugin 9.3.0
- Gradle 9.7.0
- Kotlin 2.4.21 with the Compose compiler Gradle plugin
- Android SDK 37 / Android 17
- Java 17 app bytecode target; GitHub Actions builds on Temurin JDK 21
- Jetpack Compose stable BOM 2026.09.00
- AndroidX Lifecycle 2.11.0, Activity Compose 1.13.0 and Coroutines 1.11.0

AGP 9's built-in Kotlin support is enabled. The legacy `org.jetbrains.kotlin.android` plugin is intentionally not applied. Gradle 9.7.0 is selected because it is within Kotlin 2.4.21's documented fully supported Gradle range. Android's AGP documentation lists Gradle 9.5.0 as the minimum for AGP 9.3.0.

## Build

Requirements: JDK 21, Android SDK Platform 37 and Android Build Tools 36.0.0. The repository-level GitHub Actions workflow installs these and runs:

```bash
gradle --no-daemon :app:testDebugUnitTest :app:assembleDebug :app:assembleRelease :app:bundleRelease
```

The release APK is a universal APK unless ABI splits are explicitly added. The release AAB is also generated. GitHub Actions signs release outputs when the repository secrets `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS` and `ANDROID_KEY_PASSWORD` are configured. No signing key or password is included in this source archive.

## One-ZIP update workflow

Upload one file named exactly `DeepCalc-Android-Source.zip` to the repository root. The repository-level workflow extracts the archive's top-level `DeepCalc-Android/` folder, replaces the existing native source folder, removes the uploaded ZIP from the repository and starts the Android build. Keep `.github/workflows/android-native.yml` at the repository root; GitHub does not discover workflow files nested inside the native project directory.

## Verification status

The previous CI run compiled the Kotlin and Android sources and reached unit tests; 10 of 11 tests passed. The remaining failure was an incorrect test expectation that contradicted the current web app's own formatter. This source archive corrects that expectation and adds integer-format boundary coverage. The next GitHub Actions run is the authoritative verification of the updated AGP/Kotlin/Gradle/SDK toolchain and complete APK/AAB build. UI parity, device testing, accessibility and process-death testing still require runtime validation on Android devices or emulators.
