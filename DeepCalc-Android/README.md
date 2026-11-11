# DeepCalc for Android

**DeepCalc is a native Android calculation workspace**, made by Prasanna. It is not a WebView wrapper. The Android app is built with Kotlin and Jetpack Compose, with a dedicated expression engine and a purpose-built calculator keyboard.

## What it does

- Work through calculations line by line, with live results beside each expression.
- Define reusable variables such as `$tax = 0.08` and reference earlier results with `$1`, `$2`, and so on.
- Use powers, square roots, percentages, comments, cursor editing and paired parentheses.
- Attach a separate note to any calculation without affecting its result.
- Switch between the DeepCalc keypad for calculations and the Android keyboard for notes. The header switch can temporarily select the Android keyboard for a calculation field.
- Autosave the active sheet locally and keep up to 100 named sheets in History.
- Copy a sheet or the complete sheet with notes; Share opens Android's native share chooser with the app tagline and source link.
- Choose System, Light, Dark or AMOLED appearance, with five accent palettes.

## Privacy

- The Android manifest does not request the `INTERNET` permission.
- The app includes no advertising, analytics, Firebase or Google Play Services SDKs.
- Calculations, notes, preferences and saved sheets are stored locally on the device.
- The Share action only sends the app tagline and public repository URL to another app after the user chooses a share target.

## Technology

- Kotlin and Jetpack Compose
- Material 3
- AndroidX Activity Compose and Lifecycle
- Kotlin Coroutines
- Android SDK 36; minimum Android API 23

The expression parser and evaluator live in `app/src/main/java/com/prasannaone/deepcalc/CalculatorEngine.kt`. The UI, local sheet storage, keyboard coordination, menus and dialogs live in `MainActivity.kt`.

## Build

Build requirements: JDK 21, Android SDK Platform 36, Android Build Tools 36.0.0 and Gradle 9.7.0. The Android Gradle Plugin is 9.3.0, which requires Gradle 9.5.0 or newer.

From this directory:

```bash
gradle --no-daemon :app:testDebugUnitTest
gradle --no-daemon :app:assembleRelease
```

To create a single universal APK instead of ABI split APKs:

```bash
gradle --no-daemon -Pdeepcalc.abiSplits=false :app:assembleRelease
```

The repository-level GitHub Actions workflow runs unit tests, debug/release APK builds, and a separate AAB build. The APK job creates universal, ARM64, ARMv7, x86 and x86_64 variants. The AAB is intended for Google Play; F-Droid builds and signs its own APK from source.

## F-Droid preparation

The project uses the MIT licence and its runtime manifest requests no network permission. Fastlane listing text and icon assets are included in `fastlane/metadata/android/en-US/`. A draft F-Droid metadata file is included under `fdroid/` for maintainers to copy into the `fdroiddata` repository.

Before submitting, publish the final source commit and create the matching `v2.1.0` Git tag. Then replace the tag reference in the F-Droid metadata draft with the exact release tag/commit. F-Droid's maintainers still need to review the project and verify that their build environment supports the project's Android Gradle Plugin and Gradle versions; inclusion is not automatic.

## Credits and licence

DeepCalc's Android edition is made by PrasannaOne. It builds on the original SoulCalc project by Albert “Tigr” Zenkoff. The MIT licence and original attribution are retained; see `LICENSE`.
