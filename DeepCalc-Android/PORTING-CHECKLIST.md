# DeepCalc Android port checklist

The current DeepCalc web app is the behavioural reference. The native project is isolated in `DeepCalc-Android/`.

| Area | Native implementation | Status |
|---|---|---|
| Multi-line sheet and live results | `MainActivity.kt` + `CalculatorEngine.kt` | Implemented in source; device comparison pending |
| Expression parser and evaluator | `CalculatorEngine.kt` | Implemented; targeted JVM smoke checks passed; full CI test rerun pending |
| Contextual percentages | `CalculatorEngine.kt` | Test cases authored; 10/11 passed in the previous CI attempt; corrected expectation and expanded boundary cases pending rerun |
| Powers, roots, unary minus and formatting | `CalculatorEngine.kt` | Test cases authored; 10/11 passed in the previous CI attempt; corrected expectation and expanded boundary cases pending rerun |
| Variables and line references | `CalculatorEngine.kt` | Test cases authored; 10/11 passed in the previous CI attempt; corrected expectation and expanded boundary cases pending rerun |
| Comments and empty lines | `CalculatorEngine.kt` | Implemented; comparison pending |
| Reference shifting | `CalculatorEngine.kt` + line editing | Implemented; comparison pending |
| Per-line notes and custom keyboard | `MainActivity.kt` | Implemented in source; device comparison pending |
| Long press, paste, cursor and paired parentheses | `MainActivity.kt` | Implemented in source; device testing pending |
| Copy, Guide, About and repository shortcut | `MainActivity.kt` | Implemented in source; device testing pending |
| Autosave and History (100 sheets) | Local SharedPreferences | Implemented in source; process-death test pending |
| Themes and five accent palettes | Compose palette mapping | Implemented in source; visual comparison pending |
| Phone/tablet/landscape adaptation | Compose constraints and scrolling | Implemented in source; visual test matrix pending |

## Required checks before calling it complete

- Run the GitHub Actions build and JUnit test task successfully.
- Compare the same expression corpus in web and native versions, including result formatting and error strings.
- Test cursor replacement, line insertion/deletion, reference shifts, note focus, keyboard switching and long press.
- Save, close, reopen and edit sheets; verify notes and all History entries survive process death.
- Test narrow and large phones, tablets, portrait, landscape, split-screen, short-height windows and larger system fonts.
- Test TalkBack, touch targets, clipboard and rotation while editing.

Do not claim full parity until these checks pass.

- Verify the adaptive icon and its legacy launcher fallbacks on Android 6 through current Android versions.
- Verify that release R8/resource shrinking preserves calculator, notes, history, themes and clipboard behavior.
- Compare universal and per-ABI APK sizes; confirm the AAB builds separately with ABI APK splits disabled.
