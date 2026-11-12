# DeepCalc Android interaction regression checklist

This checklist is for the final Android device pass. Unit tests cover the pure keyboard-routing and row-identity rules; the IME itself still needs a physical-device check because keyboard animation and resize behaviour depend on the Android version and installed keyboard.

## Calculation keyboard

- [ ] Fresh launch focuses the saved calculation row and shows only the DeepCalc keypad.
- [ ] Tapping a different calculation row does not briefly open the Android keyboard.
- [ ] Moving between calculation rows preserves the currently selected calculation keyboard mode.
- [ ] The header switch opens the Android keyboard for the focused calculation field.
- [ ] Switching back to the DeepCalc keypad waits until the Android IME inset reaches zero.
- [ ] The two keyboard surfaces are never rendered together.
- [ ] The `#` and `$` keypad keys still insert their symbol and switch to the Android keyboard for continued typing.
- [ ] Cursor placement, text selection, paste, paired parentheses, long-press square root and long-press clear still work.

## Notes and focus

- [ ] Opening a note always focuses its note field and opens the Android keyboard.
- [ ] Moving from a note to a calculation closes the Android keyboard before the DeepCalc keypad appears.
- [ ] Notes never show the DeepCalc keypad.
- [ ] The active calculation row remains visible above the keypad after adding a line.
- [ ] The active Notes field remains visible above the Android keyboard while typing and while its content grows.
- [ ] The final row/note can be brought fully into view with breathing room above the keyboard.

## Row insertion and deletion

- [ ] Pressing Enter inserts a row, keeps notes aligned and focuses the new row.
- [ ] Rapidly backspacing through empty rows does not duplicate, skip or misalign line numbers.
- [ ] Focus and cursor move to the intended surviving row after each deletion.
- [ ] Expanded notes remain attached to their original calculation after insertions/deletions above them.
- [ ] Line references are shifted correctly when rows are inserted or removed.
- [ ] Deleting the only row leaves one empty, editable row.

## Layout and existing features

- [ ] Overflow menu rows are compact, readable and all actions remain reachable on a short screen.
- [ ] Custom keypad stays attached to the bottom of the available app viewport with no large empty strip beneath it.
- [ ] The menu, settings, guide, history, save/rename dialogs, copy actions, Share chooser and About screen still work.
- [ ] Light, Dark, AMOLED and all five accent palettes still render correctly.
- [ ] Rotation, background/foreground and process recreation preserve the active sheet and history.

## Build gate

Run from `DeepCalc-Android/`:

```bash
gradle --no-daemon :app:testDebugUnitTest
gradle --no-daemon :app:assembleDebug :app:assembleRelease
gradle --no-daemon -Pdeepcalc.abiSplits=false :app:bundleRelease
```

Do not publish the APK until the build passes and the device checklist above has been exercised on the target phone.
