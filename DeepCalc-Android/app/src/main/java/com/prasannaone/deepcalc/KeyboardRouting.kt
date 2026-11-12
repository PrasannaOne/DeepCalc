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
