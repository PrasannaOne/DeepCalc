# DeepCalc Android final-release checklist

This checklist distinguishes source changes from checks that require a real Android device or CI run.

| Area | Final source state | Verification status |
|---|---|---|
| Calculation engine | Preserved without changes in this release pass | Existing JUnit suite passed on the previous GitHub build; rerun against this release source |
| Keyboard focus | Calculation fields default to the DeepCalc keypad; notes always use Android IME; header switch applies to calculation fields | Requires device verification |
| Keyboard viewport | Focused calculation and note rows use bring-into-view requests; app layout applies IME insets | Requires device verification on short and tall screens |
| Share | Native Android chooser shares the tagline and repository URL | Requires device verification |
| About | Product description, Made by Prasanna, Kotlin/Compose/Material 3 stack and SoulCalc attribution | Source updated |
| Settings | Theme and five palettes compacted into one non-scrollable page | Requires device verification on narrow/short screens |
| Autosave and History | Existing local persistence and 100-sheet history retained | Regression test required |
| Themes and palettes | System, Light, Dark, AMOLED; Purple, Sapphire, Jade, Ruby, Gold | Regression test required |
| APK/AAB packaging | Existing GitHub Actions workflow builds all ABI APKs and a separate AAB | Previous source passed CI; final source must pass CI after upload |
| F-Droid | MIT metadata draft and Fastlane listing assets included | Submission/tag/review still required |

## Release verification still required

- Upload the final source ZIP and wait for the repository's Android workflow to complete successfully.
- Install the ARM64 APK and test calculation input, note input, keyboard switching, last-line visibility, rotation, split-screen and short-height layouts.
- Test Share and confirm Android's native share chooser appears with the exact tagline and repository URL.
- Confirm About credits and Settings fit on a single page at phone width and with larger font settings.
- Save, reopen, rename and delete sheets; verify notes and active-sheet autosave survive app restart.
- Verify the release APK's package ID, version name/code and signature before publishing.
- Create the `v2.1.0` tag on the exact final source commit before preparing the F-Droid submission.

Do not describe the final source as device-verified or F-Droid-approved until those checks have actually passed.
