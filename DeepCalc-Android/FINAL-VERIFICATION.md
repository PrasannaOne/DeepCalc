# DeepCalc Android verification log

This file distinguishes the last verified repository build from the current source-only
maintenance pass. A successful earlier build does not automatically verify later edits.

## Last verified repository build

- Commit: `72a8c2b9c5a7ee77093fd1ca5bb402bb984a2591`
- GitHub Actions run: [43148228661](https://github.com/PrasannaOne/DeepCalc/actions/runs/43148228661)
- Android unit-test task, Debug APK build, Release APK build and Release AAB build all
  completed successfully in that run.
- The run verified the source at that commit, before the maintenance changes described
  below.

## Maintenance pass — 2026-11-12

- Kept `CalculatorEngine.kt`, the application ID, version, manifest permissions,
  SharedPreferences names, saved-sheet JSON structure, theme definitions and build
  variants unchanged.
- Consolidated active-sheet persistence into one state-keyed effect. Input callbacks no
  longer issue an extra save using a pre-update snapshot.
- Made the Notes visibility requester cover the complete calculation-plus-note row
  block. It now reacts to editor and IME changes, not every character typed.
- Kept stable row IDs and per-row focus requesters intact.
- Changed the saved-sheet title property to immutable state, named the preference keys,
  documented the legacy keyboard-preference cleanup and added signing-key ignores.
- Added `.editorconfig` and maintainer notes describing the source boundaries and safe
  change rules.
- Locally compiled the pure Kotlin engine/routing/row-identity sources and passed 11
  focused smoke assertions.

## Verification still required for this maintenance pass

- Run the full Android Gradle unit tests and APK/AAB builds after this source revision is
  committed or uploaded. The local smoke checks are not a substitute for Android CI.
- Install the resulting APK on a real Android phone and complete
  `INTERACTION-REGRESSION-CHECKLIST.md`, with special attention to opening Notes on the
  final calculation row, keyboard hand-off, rapid row deletion and restoring saved data.
- No claim of device verification or F-Droid approval is made by this log.
