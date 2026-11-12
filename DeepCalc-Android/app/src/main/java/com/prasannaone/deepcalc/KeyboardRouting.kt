package com.prasannaone.deepcalc

/** The one editor that currently owns text input. */
sealed interface ActiveEditor {
    data class Calculation(val index: Int) : ActiveEditor
    data class Note(val index: Int) : ActiveEditor
    data object None : ActiveEditor
}

/** Keyboard choice applies to calculation fields only. Notes always use Android's IME. */
enum class CalculationKeyboardMode {
    DEEPCALC,
    ANDROID
}

data class KeyboardRoute(
    val showCalculatorKeyboard: Boolean,
    val showSystemKeyboard: Boolean
)

/** Preserve a manual keyboard choice while moving between calculation rows.
 * Returning from Notes or a non-editor target restores the DeepCalc keypad.
 */
fun calculationModeAfterFocusChange(
    previousEditor: ActiveEditor,
    currentMode: CalculationKeyboardMode
): CalculationKeyboardMode = if (previousEditor is ActiveEditor.Calculation) {
    currentMode
} else {
    CalculationKeyboardMode.DEEPCALC
}

/** Never render DeepCalc's keypad while Android reports a visible IME. */
fun shouldShowCalculatorKeyboard(
    route: KeyboardRoute,
    imeVisible: Boolean,
    keypadReady: Boolean
): Boolean = route.showCalculatorKeyboard && !imeVisible && keypadReady

/** Resolves the two keyboard surfaces from one source of truth. */
fun resolveKeyboardRoute(
    editor: ActiveEditor,
    calculationMode: CalculationKeyboardMode,
    activeDialog: String?
): KeyboardRoute {
    if (activeDialog != null && activeDialog != "save") return KeyboardRoute(false, false)
    if (activeDialog == "save") return KeyboardRoute(false, true)

    return when (editor) {
        is ActiveEditor.Calculation -> when (calculationMode) {
            CalculationKeyboardMode.DEEPCALC -> KeyboardRoute(true, false)
            CalculationKeyboardMode.ANDROID -> KeyboardRoute(false, true)
        }
        is ActiveEditor.Note -> KeyboardRoute(false, true)
        ActiveEditor.None -> KeyboardRoute(false, false)
    }
}
