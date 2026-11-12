<p align="center">
  <a href="https://prasannaone.github.io/DeepCalc/">
    <img src="https://img.shields.io/badge/OPEN%20DEEPCALC-LIVE%20APP-7650c8?style=for-the-badge" alt="Open DeepCalc">
  </a>
  <a href="https://github.com/PrasannaOne/DeepCalc/releases/latest">
    <img src="https://img.shields.io/github/v/release/PrasannaOne/DeepCalc?display_name=tag&style=for-the-badge" alt="Latest release">
  </a>
  <a href="https://github.com/PrasannaOne/DeepCalc/actions/workflows/android-native.yml">
    <img src="https://github.com/PrasannaOne/DeepCalc/actions/workflows/android-native.yml/badge.svg" alt="Android build">
  </a>
</p>

<h1 align="center"><a href="https://prasannaone.github.io/DeepCalc/">DeepCalc ↗</a></h1>
<p align="center"><strong>Write it. Work it out. See it unfold.</strong></p>
<p align="center">
  <a href="https://github.com/PrasannaOne/DeepCalc">Source</a> ·
  <a href="LICENSE">MIT License</a> ·
  <a href="https://github.com/tigrino/soulcalc">Based on SoulCalc</a> ·
  <a href="https://github.com/PrasannaOne/DeepCalc/releases">Releases</a>
</p>

<p align="center">
  <a href="https://prasannaone.github.io/DeepCalc/"><img src="og-image.png" alt="DeepCalc app preview" width="100%"></a>
</p>

**DeepCalc is a notepad-style calculator** for working through connected calculations. Write expressions line by line, reuse variables and previous results, add notes, and see results update as you edit.

The project includes two separate editions:

- **Web / PWA:** the browser-based calculator published through GitHub Pages.
- **Android:** a standalone app built with Kotlin and Jetpack Compose, with its own expression engine and native interface.

## Get DeepCalc

- 🌐 **[Open the web app](https://prasannaone.github.io/DeepCalc/)**
- 📱 **[Download the latest Android release](https://github.com/PrasannaOne/DeepCalc/releases/latest)**

### Android release downloads

Open the latest GitHub Release and expand **Assets**.

| File | What it is |
|---|---|
| `DeepCalc-Universal.apk` | Recommended APK for most Android devices |
| `DeepCalc-arm64-v8a.apk` | APK for 64-bit ARM devices |
| `DeepCalc-armeabi-v7a.apk` | APK for 32-bit ARM devices |
| `DeepCalc-x86.apk` | APK for x86 devices |
| `DeepCalc-x86_64.apk` | APK for 64-bit x86 devices |
| `DeepCalc-release.aab` | Android App Bundle for store distribution |
| `SHA256SUMS.txt` | SHA-256 checksums for release files |

For a normal Android installation, choose **DeepCalc-Universal.apk**. Android may ask you to allow installation from the browser or file manager you used to download it.

## Features

- **Calculation sheets:** work through multi-line calculations with live results.
- **Variables:** define reusable values such as `$price = 49.99`.
- **Previous results:** refer to earlier answers with `$1`, `$2`, and so on.
- **Notes:** add separate notes without affecting calculation results.
- **Editing:** use the built-in keypad or your device keyboard.
- **History:** save named snapshots and reopen, rename or delete them.
- **Themes:** choose from System, Light, Dark and AMOLED appearances, with accent palettes.
- **Local-first storage:** your sheets stay on your device.

Exact feature availability and presentation can differ between the Web/PWA and Android editions.

## Quick examples

| Input | Result |
|---|---:|
| `12 + 8` | `20` |
| `(12 + 8) × 2` | `40` |
| `2 ^ 8` | `256` |
| `sqrt(81)` | `9` |
| `100 + 10%` | `110` |

### Variables and previous results

```text
$price = 49.99
$quantity = 3
$price × $quantity
```

```text
100 + 50
$1 × 2
$1 + $2
```

Start a comment line with `#`:

```text
# Monthly budget
$income = 5000
$rent = 1500
$income - $rent
```

## Your data and privacy

- Calculations, notes, preferences and saved sheets are stored locally.
- There is no account or cloud sync.
- Web/PWA data and Android app data live in separate app environments and do not automatically sync.
- Clearing browser/site data or clearing Android app storage can erase locally saved work. Keep a separate copy of anything important.

The Android app does not request the Android `INTERNET` permission. See the Android project README for its implementation and privacy details.

## Web / PWA

Open [DeepCalc](https://prasannaone.github.io/DeepCalc/) in a supported browser. Use **Install app** or **Add to Home Screen** if your browser offers it.

The first visit needs an internet connection. The service worker caches the app shell for later offline startup, subject to browser storage policies.

## Android app

The Android edition is a standalone Kotlin and Jetpack Compose application. It is **not a WebView wrapper** and does not simply package the web app.

- Application ID: `com.prasannaone.deepcalc`
- Minimum Android version: Android 6.0 (API 23)
- Target / compile SDK: 36
- Build stack: Kotlin, Jetpack Compose, Gradle 9.7.0 and JDK 21

The expression engine, editor, keypad, themes and local persistence are implemented in the Android source under `DeepCalc-Android/`.

## Automated Android builds

The workflow at [`.github/workflows/android-native.yml`](.github/workflows/android-native.yml) runs tests, builds the Android APK variants and AAB, and publishes a GitHub Release from `main`.

Release signing uses GitHub Actions secrets. Signing keys and passwords must never be committed to the repository. The release workflow creates a version tag automatically from the Android app's `versionName`; increment the app version before publishing a new release.

- [Latest Android release](https://github.com/PrasannaOne/DeepCalc/releases/latest)
- [Android build history](https://github.com/PrasannaOne/DeepCalc/actions/workflows/android-native.yml)

## Project structure

| Path | Purpose |
|---|---|
| `index.html`, `styles.css`, `app.js` | Web/PWA application |
| `manifest.webmanifest` | PWA metadata and launch settings |
| `service-worker.js` | Web app caching and offline lifecycle |
| `DeepCalc-Android/` | Standalone Kotlin Android project |
| `.github/workflows/android-native.yml` | Android CI, builds and release publishing |
| `LICENSE` | MIT licence and attribution |
| `README.md` | Project overview and documentation |

## Run the web app locally

From the repository root:

```bash
python -m http.server 8000
```

Then open [http://localhost:8000](http://localhost:8000). A local server is recommended instead of opening `index.html` directly. Service workers require HTTPS or localhost.

## Build the Android app locally

Requirements: JDK 21, Android SDK Platform 36, Android Build Tools 36.0.0 and Gradle 9.7.0.

From `DeepCalc-Android/`:

```bash
gradle --no-daemon :app:testDebugUnitTest
gradle --no-daemon :app:assembleRelease
```

To build a universal APK rather than ABI-split APKs:

```bash
gradle --no-daemon -Pdeepcalc.abiSplits=false :app:assembleRelease
```

See [`DeepCalc-Android/README.md`](DeepCalc-Android/README.md) for Android architecture, source layout and maintenance guidance.

## Credits

DeepCalc is developed by [PrasannaOne](https://github.com/PrasannaOne).

The project builds on [SoulCalc](https://github.com/tigrino/soulcalc), originally created by **Albert “Tigr” Zenkoff**. The original MIT licence and attribution are retained in this repository.

---

<p align="center"><strong>Built with care, one calculation at a time. ❤️</strong></p>
