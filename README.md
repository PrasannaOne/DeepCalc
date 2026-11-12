<div align="center">

# DeepCalc

**Write it. Work it out. See it unfold.**

[Open Web App](https://prasannaone.github.io/DeepCalc/) · [Android Releases](https://github.com/PrasannaOne/DeepCalc/releases/latest) · [Source Code](https://github.com/PrasannaOne/DeepCalc)

[![Android build](https://github.com/PrasannaOne/DeepCalc/actions/workflows/android-native.yml/badge.svg)](https://github.com/PrasannaOne/DeepCalc/actions/workflows/android-native.yml)

![DeepCalc preview](og-image.png)

</div>

DeepCalc is a notepad-style calculator for working through calculations line by line. Write expressions, reuse variables and earlier results, add notes, and keep your work organised.

**This repository contains two distinct applications:**

1. **DeepCalc Web**, a browser-based application that can be installed as a Progressive Web App (PWA).
2. **DeepCalc for Android**, a separate native Android application built with Kotlin and Jetpack Compose.

They share the DeepCalc idea and visual identity, but they are separate implementations. They do not share app storage or automatically synchronise data.

---

## Choose your DeepCalc

| | Web / PWA | Native Android |
|---|---|---|
| Open / install | [Open Web App](https://prasannaone.github.io/DeepCalc/) | [Download Android app](https://github.com/PrasannaOne/DeepCalc/releases/latest) |
| Technology | HTML, CSS and JavaScript | Kotlin and Jetpack Compose |
| Runs in | Modern web browsers | Android 6.0 and later |
| Install | Browser's Install app / Add to Home Screen option | Install the signed APK |
| Offline use | App shell is cached after the first successful visit | Works locally; no Internet permission |
| Data | Browser-local storage | Device-local storage |
| Sync between editions | Not available | Not available |

## 1. DeepCalc Web (PWA)

**[Open DeepCalc Web ↗](https://prasannaone.github.io/DeepCalc/)**

The web edition runs directly in a browser. On supported browsers, you can install it to your home screen and use it in a standalone app window.

### Web features

- Line-by-line calculations with live results.
- Reusable variables and references to earlier results.
- Separate notes attached to calculation lines.
- Built-in calculator keypad and optional system keyboard.
- Save named sheets in local History.
- Light, Dark and AMOLED themes with accent palettes.
- Service-worker caching for offline startup after the app has been loaded successfully.
- Automatic checks for newly deployed web updates.

### Install the PWA

1. Open [prasannaone.github.io/DeepCalc](https://prasannaone.github.io/DeepCalc/).
2. Open your browser's menu.
3. Choose **Install app** or **Add to Home Screen**, if offered.

The first visit requires an internet connection. Later offline availability depends on the browser retaining the app's cached files and local storage.

### Run the web edition locally

From the repository root:

```bash
python -m http.server 8000
```

Open [http://localhost:8000](http://localhost:8000). A local server is recommended because service workers require HTTPS or localhost.

---

## 2. DeepCalc for Android (Native)

**[Download the latest Android release ↗](https://github.com/PrasannaOne/DeepCalc/releases/latest)**

The Android edition is a standalone native application. It has its own Kotlin expression engine, editor, keypad, themes and local persistence. It is **not a WebView wrapper and is not the old Capacitor package**.

### Android features

- Live results alongside each calculation line.
- Reusable variables and references to earlier results.
- Powers, square roots, percentages and comments.
- Separate notes for individual calculation lines.
- Dedicated DeepCalc keypad for calculations and Android keyboard for notes.
- Keyboard switching, focus handling and editor scrolling.
- Autosaved active sheet and local History for up to 100 saved sheets.
- Copy Sheet, Copy Complete and Android's native Share menu.
- System, Light, Dark and AMOLED themes with five accent palettes.

### Download options

Open the [latest GitHub Release](https://github.com/PrasannaOne/DeepCalc/releases/latest) and expand **Assets**.

| Download | Intended use |
|---|---|
| **DeepCalc-Universal.apk** | Recommended for most Android phones and tablets |
| DeepCalc-arm64-v8a.apk | 64-bit ARM devices |
| DeepCalc-armeabi-v7a.apk | 32-bit ARM devices |
| DeepCalc-x86.apk | 32-bit x86 devices |
| DeepCalc-x86_64.apk | 64-bit x86 devices |
| DeepCalc-release.aab | App Bundle for store distribution, not direct installation |
| SHA256SUMS.txt | SHA-256 checksums for release files |

For most users, download **DeepCalc-Universal.apk**. Android may ask you to allow installation from the browser or file manager.

**Legacy build note:** older Capacitor-based DeepCalc APKs may still exist in old downloads, backups or third-party listings. They are not the current native Android edition. Use the APKs attached to the latest GitHub Release linked above.

### Android technical details

- Application ID: `com.prasannaone.deepcalc`
- Minimum Android version: Android 6.0 (API 23)
- Compile / target SDK: 36
- Language and UI: Kotlin and Jetpack Compose
- Build: Android Gradle Plugin 9.3.0, Gradle 9.7.0 and JDK 21

### Build the Android app locally

From `DeepCalc-Android/` with JDK 21 and Android SDK Platform 36 installed:

```bash
gradle --no-daemon :app:testDebugUnitTest
gradle --no-daemon :app:assembleRelease
```

To build a universal APK instead of ABI-split APKs:

```bash
gradle --no-daemon -Pdeepcalc.abiSplits=false :app:assembleRelease
```

See [DeepCalc-Android/README.md](DeepCalc-Android/README.md) for Android architecture, source layout, privacy details and maintenance guidance.

---

## Working with calculations

Both editions support the core notepad-calculator workflow. For example:

```text
# Order total
$price = 49.99
$quantity = 3
$price * $quantity
```

You can reference earlier successful results with `$1`, `$2`, and so on. Start a comment line with `#`.

A few basic expressions:

| Expression | Result |
|---|---:|
| `12 + 8` | `20` |
| `(12 + 8) * 2` | `40` |
| `2 ^ 8` | `256` |
| `sqrt(81)` | `9` |
| `100 + 10%` | `110` |

The two editions are maintained independently, so small differences in interface and feature availability may exist.

## Privacy and storage

- There is no DeepCalc account or built-in cloud sync.
- Web/PWA sheets and settings are stored in the browser's local storage.
- Android sheets and settings are stored locally by the Android app.
- Data does not automatically move between the Web/PWA and Android editions.
- Clearing browser/site data or Android app storage can erase locally saved work. Export or copy anything important before clearing data.
- The native Android manifest does not request the `INTERNET` permission. The app contains no advertising or analytics SDKs.

The Web/PWA needs a network connection for its first load and for receiving new deployments. Its cached app shell can support later offline startup, subject to browser storage policies.

## Repository layout

```text
.
├── index.html
├── styles.css
├── app.js
├── manifest.webmanifest
├── service-worker.js
├── icon-*.png
├── og-image.png
├── DeepCalc-Android/
│   ├── app/
│   ├── fastlane/
│   ├── fdroid/
│   └── README.md
├── .github/
│   └── workflows/
│       └── android-native.yml
├── LICENSE
└── README.md
```

- The repository root contains the **Web/PWA** source.
- `DeepCalc-Android/` contains the independent **native Android** project.
- `.github/workflows/android-native.yml` tests, builds and publishes Android releases.

## Releases and versioning

Android releases are built by GitHub Actions when relevant native source or workflow files change on `main`. The workflow runs unit tests, builds APK variants and an Android App Bundle, then publishes the release assets to GitHub Releases.

The release tag is generated from the Android app's `versionName`. Before publishing a new Android version, update both `versionName` and `versionCode` in `DeepCalc-Android/app/build.gradle.kts`.

- [Latest Android release](https://github.com/PrasannaOne/DeepCalc/releases/latest)
- [Android build workflow](https://github.com/PrasannaOne/DeepCalc/actions/workflows/android-native.yml)
- [All releases](https://github.com/PrasannaOne/DeepCalc/releases)

## Credits and licence

DeepCalc is maintained by [PrasannaOne](https://github.com/PrasannaOne).

The project builds on [SoulCalc](https://github.com/tigrino/soulcalc), originally created by Albert “Tigr” Zenkoff. The original MIT licence and attribution are retained in the repository. See [LICENSE](LICENSE).

---

<div align="center">

**Made with care, one calculation at a time. ❤️**

</div>
