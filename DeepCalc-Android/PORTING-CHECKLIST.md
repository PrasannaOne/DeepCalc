# DeepCalc Android release checklist

This file separates source-level checks from checks that still require Android CI or a real device.

| Area | Source state | Verification |
|---|---|---|
| Calculation engine | Preserved unchanged from the uploaded source | 17 JVM test methods passed in the local harness; rerun the Gradle test task in Android CI |
| Keyboard ownership | One `ActiveEditor` plus one keyboard route; notes always route to Android IME | Routing smoke tests passed |
| Keyboard hand-off | Custom keypad is removed before the Android IME is requested; keypad waits for IME hide before returning | Device animation check still required |
| Focused-row visibility | Input and note rows request bring-into-view after window/IME resizing, with a second visibility pass and 32dp trailing scroll room | Device check still required |
| Screen resizing | Edge-to-edge insets are handled by Compose; window uses `adjustResize` | Device check still required |
| Share | Android `ACTION_SEND` chooser shares the tagline and repository URL | Device check still required |
| About | Product description, Made by Prasanna, Kotlin/Compose/Material 3 stack and SoulCalc attribution | XML/source checks passed |
| Settings | Theme and five palettes fit in one compact, non-scrollable page | Small-screen/device check still required |
| Data | Active sheet autosave and local History (up to 100 sheets) retained | Regression test still required |
| Themes | System, Light, Dark and AMOLED; Purple, Sapphire, Jade, Ruby and Gold | Device check still required |
| Packaging | ABI APKs and separate AAB remain configured; version 2.1.1 / code 5 | Android CI build still required for this revision |
| F-Droid | MIT metadata template and Fastlane listing assets included | Requires tagged source commit and F-Droid review |

## Release verification required

1. Run `:app:testDebugUnitTest`, `:app:assembleDebug`, `:app:assembleRelease` and `:app:bundleRelease` on Android CI.
2. Install the ARM64 release APK on a physical phone. Test input-to-input, input-to-note, note-to-input, manual keyboard switching and rapid repeated tapping.
3. Test the last calculation and last note on short and tall screens, with gesture and three-button navigation, display-size changes, portrait/landscape and split-screen.
4. Confirm the two keyboard surfaces never appear together, and that the focused editor remains visibly above the active keyboard with a small gap.
5. Test Android's native Share chooser and verify the exact tagline and repository URL.
6. Confirm About credits and Settings fit at narrow phone width and larger display/font settings.
7. Save, reopen, rename and delete sheets; confirm notes and active-sheet autosave survive app restart.
8. Verify APK package ID, version name/code and signing certificate. Create the `v2.1.1` tag only on the exact final source commit.

Do not describe the source as device-verified or F-Droid-approved until those checks pass.
