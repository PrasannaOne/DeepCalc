package com.prasannaone.deepcalc

import org.junit.Assert.assertEquals
import org.junit.Test

class CalculatorEngineTest {
    private fun result(line: String): String = CalculatorEngine.evaluateSheet(listOf(line)).first.single().text

    @Test fun contextualPercentagesMatchDeepCalc() {
        assertEquals("110", result("100 + 10%"))
        assertEquals("90", result("100 - 10%"))
        assertEquals("10", result("100 × 10%"))
        assertEquals("60.5", result("50 + 10% + 10%"))
        assertEquals("0.1", result("10%"))
    }

    @Test fun variablesAndLineReferencesWorkInSheetOrder() {
        val (results, _) = CalculatorEngine.evaluateSheet(listOf("$" + "price = 49.99", "$" + "quantity = 3", "$" + "price × $" + "quantity", "$3 × 2"))
        assertEquals("49.99", results[0].text)
        assertEquals("3", results[1].text)
        assertEquals("149.97", results[2].text)
        assertEquals("299.94", results[3].text)
    }

    @Test fun rootsPowersAndUnaryMinusMatchParserOrder() {
        assertEquals("9", result("sqrt(81)"))
        assertEquals("256", result("2 ^ 8"))
        assertEquals("4", result("-2 ^ 2"))
    }

    @Test fun commentsAndEmptyLinesDoNotCreateResults() {
        val (results, _) = CalculatorEngine.evaluateSheet(listOf("# budget", "", "12 + 8"))
        assertEquals(CalculatorEngine.LineResult.Kind.EMPTY, results[0].kind)
        assertEquals(CalculatorEngine.LineResult.Kind.EMPTY, results[1].kind)
        assertEquals("20", results[2].text)
    }

    @Test fun errorsAreShownInDeepCalcStyle() {
        assertEquals("?", result("1 +"))
        assertEquals("? \$missing", result("$" + "missing"))
        assertEquals("∞", result("1 / 0"))
        assertEquals("NaN", result("0 / 0"))
        assertEquals("? digits", result("12345678901234567"))
    }

    @Test fun invalidInfinityGlyphIsNotMistakenForDivisionByZero() {
        assertEquals("?", result("∞"))
        assertEquals("?", result("-∞"))
    }

    @Test fun exponentiationIsRightAssociativeAndParenthesesAreRespected() {
        assertEquals("512", result("2 ^ 3 ^ 2"))
        assertEquals("64", result("(2 ^ 3) ^ 2"))
    }

    @Test fun failedLinesDoNotBecomeValidLineReferences() {
        val results = CalculatorEngine.evaluateSheet(listOf("1 +", "12 + 8", "$1", "$2")).first
        assertEquals("?", results[0].text)
        assertEquals("20", results[1].text)
        assertEquals("? $1", results[2].text)
        assertEquals("20", results[3].text)
    }

    @Test fun numberFormattingMatchesJavaScriptPrecisionBoundaries() {
        assertEquals("1000000000", result("1000000000"))
        assertEquals("10000000000", result("10000000000"))
        assertEquals("0.000001", result("0.000001"))
        assertEquals("1e-7", result("0.0000001"))
        assertEquals("999999999999999", result("999999999999999"))
        assertEquals("1e+15", result("1000000000000000"))
    }

    @Test fun unicodeDigitsAreRejectedLikeTheWebLexer() {
        assertEquals("?", result("１２ + 3"))
        assertEquals("?", result("١٢ + 3"))
    }

    @Test fun lineReferenceShiftsFollowDeepCalcRules() {
        assertEquals("$3 + $10", CalculatorEngine.shiftReferencesForInsert("$2 + $9", 2))
        assertEquals("$2 + $8", CalculatorEngine.shiftReferencesForRemove("$2 + $9", 2))
    }
}
