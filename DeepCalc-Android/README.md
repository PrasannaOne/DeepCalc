# DeepCalc Native Android

A standalone native Android edition of DeepCalc built with Kotlin and Jetpack Compose. It lives in `DeepCalc-Android/` and does not replace or modify the web/PWA application at the repository root.

## Features

- Multiline notepad calculator with live per-line results.
- DeepCalc expression syntax: `+`, `−`, `×`, `÷`, `%`, `^`, `sqrt(...)`, variables (`$name`), line references (`$1`) and comments (`#`).
- Contextual percentages and line-reference shifting when rows are inserted or removed.
- Per-line notes, automatic calculator keyboard for calculation fields, system keyboard for notes, manual keyboard switching, cursor operations and long-press actions.
- Autosaved active sheet and local History (up to 100 sheets), with open, rename and delete.
- Copy Sheet, Copy Complete, copy expression/result from the line number or result, GitHub repository shortcut, Guide and About.
- System, Light, Dark and AMOLED themes with five accent palettes.
- Adaptive Compose layout with scrollable content and width-aware sizing.

## Toolchain

This project uses a pinned, mutually compatible modern toolchain:

- Android Gradle Plugin 9.3.0
- Gradle 9.7.0
- Kotlin 2.4.21 with the Compose compiler Gradle plugin
- Android SDK 36 / Android 16
- Java 17 app bytecode target; GitHub Actions builds on Temurin JDK 21
- Jetpack Compose stable BOM 2026.06.01 (Compose 1.11.x)
- AndroidX Lifecycle 2.10.0, Activity Compose 1.13.0 and Coroutines 1.11.0

AGP 9's built-in Kotlin support is enabled. The legacy `org.jetbrains.kotlin.android` plugin is intentionally not applied. Gradle 9.7.0 is selected because it is within Kotlin 2.4.21's documented fully supported Gradle range. Android's AGP documentation lists Gradle 9.5.0 as the minimum for AGP 9.3.0.

## Build

Requirements: JDK 21, Android SDK Platform 36 and Android Build Tools 36.0.0. The repository-level GitHub Actions workflow installs these and runs:

```bash
gradle --no-daemon :app:testDebugUnitTest :app:assembleDebug :app:assembleRelease :app:bundleRelease
```

The release APK is a universal APK unless ABI splits are explicitly added. The release AAB is also generated. GitHub Actions signs release outputs when the repository secrets `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS` and `ANDROID_KEY_PASSWORD` are configured. No signing key or password is included in this source archive.

## One-ZIP update workflow

Upload one file named exactly `DeepCalc-Android-Source.zip` to the repository root. The repository-level workflow extracts the archive's top-level `DeepCalc-Android/` folder, replaces the existing native source folder, removes the uploaded ZIP from the repository and starts the Android build. Keep `.github/workflows/android-native.yml` at the repository root; GitHub does not discover workflow files nested inside the native project directory.

## Verification status

The previous CI run reached Android dependency metadata checks and failed because Compose 1.12.1 requires compileSdk 37 while the runner installed API 36. This source revision pins the stable Compose 1.11 line and keeps compileSdk/targetSdk at API 36. Calculator-engine tests were corrected to match the actual contextual-percentage evaluation in the source. The next GitHub Actions run must verify dependency resolution, compilation, unit tests, and APK/AAB packaging. The previous CI run completed the unit tests and generated debug/release APKs and the release AAB. This source revision still requires a fresh CI run. UI parity, device testing, accessibility and process-death testing require runtime validation on Android devices or emulators.


## Launcher icon

The native Android launcher icon is generated from the repository-root `icon-512.png` asset during the build. This keeps the Android app icon identical to the existing web app artwork without duplicating or modifying the PWA assets.
