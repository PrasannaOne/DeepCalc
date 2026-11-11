# DeepCalc Native Android

A standalone native Android edition of DeepCalc built with Kotlin and Jetpack Compose. It lives in `DeepCalc-Android/` and does not replace or modify the web/PWA application at the repository root.

## Features

- Multiline notepad calculator with live per-line results.
- DeepCalc expression syntax: `+`, `−`, `×`, `÷`, `%`, `^`, `sqrt(...)`, variables (`$name`), line references (`$1`) and comments (`#`).
- Contextual percentages and line-reference shifting when rows are inserted or removed.
- Per-line notes, a dedicated calculator keyboard, Android system keyboard for notes, manual keyboard switching, cursor operations and long-press actions.
- Autosaved active sheet and local History (up to 100 sheets), with open, rename and delete.
- Copy Sheet, Copy Complete, copy expression/result from the line number or result, GitHub repository shortcut, Guide and About.
- System, Light, Dark and AMOLED themes with five accent palettes.
- Adaptive Compose layout with scrollable content and width-aware sizing.

## Toolchain

- Android Gradle Plugin 9.3.0
- Gradle 9.7.0
- Kotlin 2.4.21 with the Compose compiler Gradle plugin
- Android SDK 36 / Android 16
- Java 17 app bytecode target; GitHub Actions builds on Temurin JDK 21
- Jetpack Compose stable BOM 2026.06.01
- AndroidX Lifecycle 2.10.0, Activity Compose 1.13.0 and Coroutines 1.11.0

AGP 9's built-in Kotlin support is enabled. The legacy `org.jetbrains.kotlin.android` plugin is intentionally not applied.

## Release builds

Release builds enable R8 code optimisation and resource shrinking. Unused library code and resources are removed while preserving the app's referenced features.

The release build produces:
- `DeepCalc-Universal.apk`: one APK containing all supported ABIs.
- `DeepCalc-arm64-v8a.apk`: for modern 64-bit ARM phones.
- `DeepCalc-armeabi-v7a.apk`: for older 32-bit ARM devices.
- `DeepCalc-x86.apk` and `DeepCalc-x86_64.apk`: for compatible Intel-based devices and emulators.
- `DeepCalc-release.aab`: for Google Play distribution; Play generates device-targeted installs from the bundle.

For a normal modern Android phone, choose `DeepCalc-arm64-v8a.apk`. Use the universal APK when the device architecture is unknown. Do not install the `x86` variants on ARM phones.

The GitHub Actions artifact also contains a size report showing each APK's file size, uncompressed DEX size and native-library size. Release signing is applied when the repository secrets `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS` and `ANDROID_KEY_PASSWORD` are configured. No signing key or password is included in this source archive.

## Build

Requirements: JDK 21, Android SDK Platform 36 and Android Build Tools 36.0.0. The repository-level GitHub Actions workflow runs the APK and AAB builds separately. The APK invocation leaves ABI splits enabled; the AAB invocation uses `-Pdeepcalc.abiSplits=false` because the bundle already carries all ABIs and should not generate parallel APK split outputs in the same invocation.

## One-ZIP source update workflow

Upload one file named exactly `DeepCalc-Android-Source.zip` to the repository root. The archive must contain the top-level `DeepCalc-Android/` folder. The repository-level workflow extracts and replaces the existing native source folder, removes the uploaded ZIP from the repository and builds the app.

The root workflow file `.github/workflows/android-native.yml` must be updated separately; GitHub does not discover workflow files nested inside the native project folder.

## Launcher icon

The native app uses an Android adaptive launcher icon built from the existing DeepCalc artwork. API 26+ devices use separate foreground and background layers; API 23–25 devices use matching density-specific PNG fallbacks. The existing PWA icons and assets are not modified.
