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

    @Test fun noEditorShowsNoKeyboard() {
        assertEquals(
            KeyboardRoute(false, false),
            resolveKeyboardRoute(ActiveEditor.None, CalculationKeyboardMode.DEEPCALC, null)
        )
    }
}
