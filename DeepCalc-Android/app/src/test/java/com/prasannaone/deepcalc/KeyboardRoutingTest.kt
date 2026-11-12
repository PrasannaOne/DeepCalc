package com.prasannaone.deepcalc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyboardRoutingTest {
    @Test fun calculationUsesDeepCalcKeypadByDefault() {
        val route = resolveKeyboardRoute(
            ActiveEditor.Calculation(0), CalculationKeyboardMode.DEEPCALC, null
        )
        assertTrue(route.showCalculatorKeyboard)
        assertFalse(route.showSystemKeyboard)
    }

    @Test fun notesAlwaysUseOnlyTheAndroidKeyboard() {
        for (mode in CalculationKeyboardMode.values()) {
            val route = resolveKeyboardRoute(ActiveEditor.Note(3), mode, null)
            assertFalse(route.showCalculatorKeyboard)
            assertTrue(route.showSystemKeyboard)
        }
    }

    @Test fun manuallySwitchedCalculationUsesOnlyTheAndroidKeyboard() {
        val route = resolveKeyboardRoute(
            ActiveEditor.Calculation(2), CalculationKeyboardMode.ANDROID, null
        )
        assertFalse(route.showCalculatorKeyboard)
        assertTrue(route.showSystemKeyboard)
    }

    @Test fun nonSaveDialogsHideBothKeyboards() {
        val route = resolveKeyboardRoute(
            ActiveEditor.Note(1), CalculationKeyboardMode.DEEPCALC, "settings"
        )
        assertEquals(KeyboardRoute(false, false), route)
    }

    @Test fun saveDialogUsesOnlyTheAndroidKeyboard() {
        val route = resolveKeyboardRoute(
            ActiveEditor.Calculation(0), CalculationKeyboardMode.DEEPCALC, "save"
        )
        assertFalse(route.showCalculatorKeyboard)
        assertTrue(route.showSystemKeyboard)
    }

    @Test fun movingBetweenCalculationRowsPreservesManualAndroidKeyboardChoice() {
        val nextMode = calculationModeAfterFocusChange(
            ActiveEditor.Calculation(2), CalculationKeyboardMode.ANDROID
        )
        assertEquals(CalculationKeyboardMode.ANDROID, nextMode)
    }

    @Test fun returningFromNotesRestoresDeepCalcKeyboardChoice() {
        val nextMode = calculationModeAfterFocusChange(
            ActiveEditor.Note(2), CalculationKeyboardMode.ANDROID
        )
        assertEquals(CalculationKeyboardMode.DEEPCALC, nextMode)
    }

    @Test fun returningFromNoEditorRestoresDeepCalcKeyboardChoice() {
        val nextMode = calculationModeAfterFocusChange(
            ActiveEditor.None, CalculationKeyboardMode.ANDROID
        )
        assertEquals(CalculationKeyboardMode.DEEPCALC, nextMode)
    }

    @Test fun calculatorKeypadNeverRendersWhileAndroidImeIsVisible() {
        val customRoute = KeyboardRoute(showCalculatorKeyboard = true, showSystemKeyboard = false)
        assertFalse(shouldShowCalculatorKeyboard(customRoute, imeVisible = true, keypadReady = true))
        assertFalse(shouldShowCalculatorKeyboard(customRoute, imeVisible = false, keypadReady = false))
        assertTrue(shouldShowCalculatorKeyboard(customRoute, imeVisible = false, keypadReady = true))
    }

    @Test fun calculatorKeypadNeverRendersForSystemKeyboardRoute() {
        val systemRoute = KeyboardRoute(showCalculatorKeyboard = false, showSystemKeyboard = true)
        assertFalse(shouldShowCalculatorKeyboard(systemRoute, imeVisible = false, keypadReady = true))
    }

    @Test fun noEditorShowsNoKeyboard() {
        assertEquals(
            KeyboardRoute(false, false),
            resolveKeyboardRoute(ActiveEditor.None, CalculationKeyboardMode.DEEPCALC, null)
        )
    }
}
