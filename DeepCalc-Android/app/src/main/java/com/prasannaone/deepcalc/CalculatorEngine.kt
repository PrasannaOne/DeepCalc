package com.prasannaone.deepcalc

import java.util.Locale
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/**
 * DeepCalc's expression engine, ported from the current app.js tokenizer,
 * recursive-descent parser and evaluator. Keep this behaviour aligned with
 * the web reference. Do not replace it with Kotlin eval or a third-party parser.
 */
object CalculatorEngine {
    data class LineResult(val kind: Kind, val text: String, val value: Double? = null) {
        enum class Kind { EMPTY, SUCCESS, ERROR }
    }

    private data class Token(val type: Type, val value: String, val position: Int)
    private enum class Type {
        NUMBER, VARIABLE, LINE_REF, PLUS, MINUS, MULTIPLY, DIVIDE, PERCENT,
        POWER, EQUALS, LPAREN, RPAREN, SQRT, EOF
    }

    private sealed interface Node {
        data class Number(val value: Double) : Node
        data class Variable(val name: String) : Node
        data class LineRef(val number: Int) : Node
        data class UnaryMinus(val operand: Node) : Node
        data class Binary(val operator: String, val left: Node, val right: Node) : Node
        data class Percent(val operand: Node, val base: Node?) : Node
        data class Assignment(val name: String, val expression: Node) : Node
        data class Function(val name: String, val argument: Node) : Node
    }

    private class CalcException(message: String) : Exception(message)

    private fun significantDigits(literal: String): Int {
        val digits = literal.filter(Char::isDigit)
        val first = digits.indexOfFirst { it != '0' }
        if (first < 0) return 0
        val last = digits.indexOfLast { it != '0' }
        return last - first + 1
    }

    private fun tokenize(source: String): List<Token> {
        val result = mutableListOf<Token>()
        var p = 0
        while (p < source.length) {
            val c = source[p]
            if (c.isWhitespace()) { p++; continue }
            val start = p
            if (c in '0'..'9' || (c == '.' && source.getOrNull(p + 1)?.let { it in '0'..'9' } == true)) {
                val value = StringBuilder()
                var decimal = false
                while (p < source.length) {
                    val current = source[p]
                    when {
                        current in '0'..'9' -> { value.append(current); p++ }
                        current == '.' && !decimal -> { decimal = true; value.append(current); p++ }
                        else -> break
                    }
                }
                result += Token(Type.NUMBER, value.toString(), start)
                continue
            }
            if (c == '$') {
                p++
                val next = source.getOrNull(p) ?: throw CalcException("Invalid token: $")
                if (next in '0'..'9') {
                    val value = StringBuilder()
                    while (source.getOrNull(p)?.let { it in '0'..'9' } == true) value.append(source[p++])
                    result += Token(Type.LINE_REF, value.toString(), start)
                } else if (next.isLetter() && next.code < 128) {
                    val value = StringBuilder()
                    while (source.getOrNull(p)?.let { (it.isLetterOrDigit() || it == '_') && it.code < 128 } == true) value.append(source[p++])
                    result += Token(Type.VARIABLE, value.toString(), start)
                } else throw CalcException("Invalid token: $")
                continue
            }
            val single = when (c) {
                '+' -> Type.PLUS
                '-', '−' -> Type.MINUS
                '*', '×' -> Type.MULTIPLY
                '/', '÷' -> Type.DIVIDE
                '%' -> Type.PERCENT
                '^' -> Type.POWER
                '=' -> Type.EQUALS
                '(' -> Type.LPAREN
                ')' -> Type.RPAREN
                else -> null
            }
            if (single != null) {
                result += Token(single, c.toString(), start); p++; continue
            }
            if (c.isLetter() && c.code < 128) {
                val value = StringBuilder()
                while (source.getOrNull(p)?.let { it.isLetterOrDigit() && it.code < 128 || it == '_' } == true) value.append(source[p++])
                if (value.toString().lowercase(Locale.ROOT) == "sqrt") result += Token(Type.SQRT, value.toString(), start)
                else throw CalcException("Invalid token: $value")
                continue
            }
            throw CalcException("Invalid token: $c")
        }
        result += Token(Type.EOF, "", source.length)
        return result
    }

    private class Parser(private val tokens: List<Token>) {
        private var pos = 0
        private fun current() = tokens[pos]
        private fun take(type: Type): Token {
            if (current().type != type) throw CalcException("Unexpected token: ${current().value}")
            return tokens[pos++]
        }
        fun parse(): Node {
            val node = expression()
            if (current().type != Type.EOF) throw CalcException("Unexpected token: ${current().value}")
            return node
        }
        private fun expression(): Node {
            if (current().type == Type.VARIABLE && tokens.getOrNull(pos + 1)?.type == Type.EQUALS) {
                val name = take(Type.VARIABLE).value
                take(Type.EQUALS)
                return Node.Assignment(name, computation())
            }
            return computation()
        }
        private fun computation(): Node {
            var left = term()
            while (current().type == Type.PLUS || current().type == Type.MINUS) {
                val op = if (current().type == Type.PLUS) "ADD" else "SUBTRACT"
                pos++
                val right = term()
                val adjusted = if (right is Node.Percent && right.base == null) right.copy(base = left) else right
                left = Node.Binary(op, left, adjusted)
            }
            return left
        }
        private fun term(): Node {
            var left = factor()
            while (current().type == Type.MULTIPLY || current().type == Type.DIVIDE) {
                val op = if (current().type == Type.MULTIPLY) "MULTIPLY" else "DIVIDE"
                pos++
                left = Node.Binary(op, left, factor())
            }
            return left
        }
        private fun factor(): Node {
            val base = power()
            if (current().type == Type.PERCENT) { pos++; return Node.Percent(base, null) }
            return base
        }
        private fun power(): Node {
            val base = unary()
            if (current().type == Type.POWER) { pos++; return Node.Binary("POWER", base, power()) }
            return base
        }
        private fun unary(): Node {
            if (current().type == Type.MINUS) { pos++; return Node.UnaryMinus(unary()) }
            return primary()
        }
        private fun primary(): Node {
            val token = current()
            return when (token.type) {
                Type.NUMBER -> {
                    pos++
                    if (significantDigits(token.value) > 15) throw CalcException("Too many significant digits: ${token.value}")
                    Node.Number(token.value.toDouble())
                }
                Type.VARIABLE -> { pos++; Node.Variable(token.value) }
                Type.LINE_REF -> {
                    pos++
                    val n = token.value.toLongOrNull()
                    if (n == null || n < 0L || n > Int.MAX_VALUE) throw CalcException("Invalid line reference: $${token.value}")
                    Node.LineRef(n.toInt())
                }
                Type.LPAREN -> {
                    pos++
                    val node = computation()
                    if (current().type != Type.RPAREN) throw CalcException("Expected ')'")
                    pos++
                    node
                }
                Type.SQRT -> {
                    pos++
                    if (current().type != Type.LPAREN) throw CalcException("Expected '(' after sqrt")
                    pos++
                    val argument = computation()
                    if (current().type != Type.RPAREN) throw CalcException("Expected ')'")
                    pos++
                    Node.Function("sqrt", argument)
                }
                Type.EOF -> throw CalcException("Unexpected end of input")
                else -> throw CalcException("Unexpected token: ${token.value}")
            }
        }
    }

    private data class Scope(val variables: MutableMap<String, Double>, val lineResults: MutableMap<Int, Double>)

    private fun evaluate(node: Node, scope: Scope): Double = when (node) {
        is Node.Number -> node.value
        is Node.Variable -> scope.variables[node.name] ?: throw CalcException("? $${node.name}")
        is Node.LineRef -> scope.lineResults[node.number] ?: throw CalcException("? $${node.number}")
        is Node.UnaryMinus -> -evaluate(node.operand, scope)
        is Node.Percent -> {
            val percentage = evaluate(node.operand, scope) / 100.0
            if (node.base != null) evaluate(node.base, scope) * percentage else percentage
        }
        is Node.Assignment -> evaluate(node.expression, scope).also { scope.variables[node.name] = it }
        is Node.Function -> {
            val arg = evaluate(node.argument, scope)
            if (node.name == "sqrt") {
                if (arg < 0.0) throw CalcException("NaN")
                kotlin.math.sqrt(arg)
            } else throw CalcException("Unknown function: ${node.name}")
        }
        is Node.Binary -> {
            val left = evaluate(node.left, scope)
            val right = evaluate(node.right, scope)
            when (node.operator) {
                "ADD" -> left + right
                "SUBTRACT" -> left - right
                "MULTIPLY" -> left * right
                "DIVIDE" -> if (right == 0.0) {
                    if (left == 0.0) throw CalcException("NaN")
                    throw CalcException(if (left < 0.0) "-∞" else "∞")
                } else left / right
                "POWER" -> Math.pow(left, right)
                else -> throw CalcException("Evaluation error")
            }
        }
    }

    private fun formatNumber(value: Double): String {
        if (value.isNaN()) return "NaN"
        if (value.isInfinite()) return if (value > 0) "∞" else "-∞"
        if (value % 1.0 == 0.0 && kotlin.math.abs(value) < 1e15) return value.toLong().toString()

        // Number.toPrecision(10) in the web reference, followed by its trailing-zero trim.
        val rounded = BigDecimal.valueOf(value).round(MathContext(10, RoundingMode.HALF_UP)).stripTrailingZeros()
        val exponent = rounded.precision() - rounded.scale() - 1
        return if (exponent >= -6 && exponent < 10) {
            rounded.toPlainString()
        } else {
            rounded.toString().replace('E', 'e')
        }
    }

    private fun formatError(message: String): String {
        if (message == "∞" || message == "-∞" || message == "NaN") return message
        if (message.contains("too many significant digits", true)) return "? digits"
        if (message.startsWith("?")) return message
        if (message.contains("invalid line reference", true)) {
            val ref = Regex("\\$\\d+").find(message)?.value
            return if (ref != null) "? $ref" else "?"
        }
        return "?"
    }

    fun evaluateSheet(lines: List<String>): Pair<List<LineResult>, Map<String, Double>> {
        val scope = Scope(linkedMapOf(), linkedMapOf())
        val results = lines.mapIndexed { index, input ->
            val trimmed = input.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                LineResult(LineResult.Kind.EMPTY, "")
            } else {
                try {
                    val tokens = tokenize(input)
                    if (tokens.size == 1) LineResult(LineResult.Kind.EMPTY, "")
                    else {
                        val value = evaluate(Parser(tokens).parse(), scope)
                        scope.lineResults[index + 1] = value
                        LineResult(LineResult.Kind.SUCCESS, formatNumber(value), value)
                    }
                } catch (e: Exception) {
                    LineResult(LineResult.Kind.ERROR, formatError(e.message ?: "Evaluation error"))
                }
            }
        }
        return results to scope.variables.toMap()
    }

    fun shiftReferencesForInsert(line: String, insertedLineNumber: Int): String =
        Regex("\\$(\\d+)").replace(line) { match ->
            val number = match.groupValues[1].toLongOrNull()
            if (number != null && number <= 9_007_199_254_740_991L && number >= insertedLineNumber) "$${number + 1}" else match.value
        }

    fun shiftReferencesForRemove(line: String, removedLineNumber: Int): String =
        Regex("\\$(\\d+)").replace(line) { match ->
            val number = match.groupValues[1].toLongOrNull()
            if (number != null && number <= 9_007_199_254_740_991L && number > removedLineNumber) "$${number - 1}" else match.value
        }
}
