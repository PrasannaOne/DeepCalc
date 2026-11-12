# DeepCalc Android final source pass

## Changes in this pass

- Calculation fields are read-only while the DeepCalc keypad owns input. This prevents the value-based `BasicTextField` from launching Android's IME when the user moves between calculation rows. Programmatic keypad edits still update each `TextFieldValue`; switching to Android keyboard mode makes the field editable again.
- Keyboard route changes and IME visibility are handled by separate effects. The custom keypad is gated by the actual IME inset, so the two keyboards cannot render together. Dismissing the Android keyboard manually with Back is not automatically undone.
- Notes remain on the Android keyboard. Returning from Notes to a calculation waits for the IME inset to reach zero before showing the custom keypad.
- System IME padding is applied only while the active route uses the Android keyboard and the IME is visible. The custom keypad therefore does not retain an invisible keyboard-sized gap.
- Calculation rows now have stable IDs and stable Compose keys. Focus requesters and bring-into-view requesters are keyed to the row identity instead of its changing list index.
- Active calculation and note rows are brought into view after focus, after text growth, and when IME visibility changes. Long fixed focus delays were removed.
- Overflow items use a compact custom row instead of Material's oversized default menu items.
- Local storage, palette definitions, keyboard/menu components and row identity helpers were separated into focused source files. `CalculatorEngine.kt` was not modified.

## Local checks performed

- 26 existing/new pure Kotlin unit-test methods were executed through a local JVM harness: passed.
- Representative calculation-engine cases, keyboard-route transitions, the no-overlapping-keypads rule and stable row insertion/deletion checks: passed.
- Kotlin source parser pass: no syntax/parser diagnostics were reported.
- Original `CalculatorEngine.kt` checksum: unchanged.
- Source archive integrity: checked after packaging.

## Checks not possible in this environment

A full Android Gradle build was not run here: this environment has no Gradle installation or Android SDK, and external dependency downloads are unavailable. The local JVM harness is not a substitute for `:app:testDebugUnitTest`, `assembleDebug`, `assembleRelease` or `bundleRelease`.

The IME, animation, viewport and device-specific keyboard behaviour also cannot be certified without installing this build on the target Android phone. Use `INTERACTION-REGRESSION-CHECKLIST.md` before publishing. Do not label the release build as device-verified until those checks pass.
