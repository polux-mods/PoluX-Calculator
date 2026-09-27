package ua.polux.smartcalc

import kotlin.math.*
import java.util.Locale

class MathEngine {

    fun evaluate(raw: String, angleMode: AngleMode = AngleMode.DEG): Double {
        val normalized = normalize(raw)
        if (normalized.isBlank()) error("Порожній вираз")
        val parser = Parser(normalized, angleMode)
        val value = parser.parseExpression()
        parser.skipSpaces()
        if (!parser.atEnd()) error("Неочікуваний символ: ${parser.peek()}")
        if (value.isNaN() || value.isInfinite()) error("Невизначене значення")
        return value
    }

    fun solve(raw: String, angleMode: AngleMode = AngleMode.DEG): String {
        val input = normalize(raw)
        if (input.count { it == '=' } == 1 && Regex("""[xX]""").containsMatchIn(input)) {
            try {
                val sides = input.split("=")
                val left = affine(sides[0], angleMode)
                val right = affine(sides[1], angleMode)
                val a = left.first - right.first
                val b = right.second - left.second
                if (abs(a) < 1e-12) {
                    return if (abs(b) < 1e-12) "∞ розв’язків" else "Немає розв’язків"
                }
                return format(b / a)
            } catch (_: Exception) {
                // Fall back to ordinary evaluation.
            }
        }
        return format(evaluate(input, angleMode))
    }

    private fun affine(s: String, mode: AngleMode): Pair<Double, Double> {
        val compact = s.replace(" ", "").replace("X", "x")
        if (!compact.contains('x')) return 0.0 to evaluate(compact, mode)
        val terms = Regex("""([+-]?)(?:(\d+(?:\.\d+)?)\*)?x""").findAll(
            compact.replace("-", "+-")
        )
        var a = 0.0
        var b = 0.0
        var consumed = ""
        for (m in terms) {
            val sign = if (m.groupValues[1] == "-") -1.0 else 1.0
            val coeff = m.groupValues[2].ifBlank { "1" }.toDouble()
            a += sign * coeff
            consumed += m.value
        }
        val remainder = compact.replace(Regex("""[+-]?(?:(\d+(?:\.\d+)?)\*)?x"""), "")
            .trim('+')
        if (remainder.isNotBlank()) b = evaluate(remainder, mode)
        return a to b
    }

    private fun normalize(s: String): String {
        return s.trim()
            .replace('×', '*')
            .replace('·', '*')
            .replace('÷', '/')
            .replace('−', '-')
            .replace('–', '-')
            .replace(',', '.')
            .replace("π", "pi")
            .replace("τ", "tau")
            .replace("√", "sqrt")
            .replace("²", "^2")
            .replace("³", "^3")
            .replace("⁴", "^4")
            .replace("⁵", "^5")
            .replace("⁶", "^6")
            .replace("⁷", "^7")
            .replace("⁸", "^8")
            .replace("⁹", "^9")
            .replace("⁰", "^0")
            .replace("％", "%")
            .replace("**", "^")
            .replace(Regex("""(?i)\bAns\b"""), "ans")
    }

    private fun format(x: Double): String {
        if (abs(x - round(x)) < 1e-10 && abs(x) < 1e15) return String.format(Locale.US, "%.0f", x)
        return String.format(Locale.US, "%.10f", x).trimEnd('0').trimEnd('.')
    }

    enum class AngleMode { DEG, RAD }

    private class Parser(
        private val s: String,
        private val mode: AngleMode
    ) {
        var pos = 0

        fun parseExpression(): Double {
            var v = parseTerm()
            while (true) {
                skipSpaces()
                when {
                    eat('+') -> v += parseTerm()
                    eat('-') -> v -= parseTerm()
                    else -> return v
                }
            }
        }

        private fun parseTerm(): Double {
            var v = parsePower()
            while (true) {
                skipSpaces()
                when {
                    eat('*') -> v *= parsePower()
                    eat('/') -> v /= parsePower()
                    eat('%') -> v %= parsePower()
                    shouldImplicitMultiply() -> v *= parsePower()
                    else -> return v
                }
            }
        }

        private fun parsePower(): Double {
            var v = parseUnary()
            skipSpaces()
            if (eat('^')) v = v.pow(parsePower())
            return v
        }

        private fun parseUnary(): Double {
            skipSpaces()
            if (eat('+')) return parseUnary()
            if (eat('-')) return -parseUnary()
            return parsePostfix()
        }

        private fun parsePostfix(): Double {
            var v = parsePrimary()
            while (true) {
                skipSpaces()
                if (eat('!')) v = gamma(v + 1.0)
                else if (eat('%')) v /= 100.0
                else return v
            }
        }

        private fun parsePrimary(): Double {
            skipSpaces()
            if (eat('(')) {
                val v = parseExpression()
                requireEat(')')
                return v
            }
            if (pos >= s.length) error("Очікувалося число")
            if (s[pos].isDigit() || s[pos] == '.') return number()

            val name = identifier()
            if (name.isNotEmpty()) {
                val lower = name.lowercase(Locale.US)
                skipSpaces()
                if (eat('(')) {
                    val args = mutableListOf<Double>()
                    skipSpaces()
                    if (!eat(')')) {
                        do {
                            args += parseExpression()
                            skipSpaces()
                        } while (eat(','))
                        requireEat(')')
                    }
                    return function(lower, args)
                }
                return constant(lower)
            }
            error("Неочікуваний символ: ${s[pos]}")
        }

        private fun number(): Double {
            val start = pos
            while (pos < s.length && (s[pos].isDigit() || s[pos] == '.')) pos++
            return s.substring(start, pos).toDouble()
        }

        private fun identifier(): String {
            val start = pos
            while (pos < s.length && (s[pos].isLetter() || s[pos] == '_')) pos++
            return s.substring(start, pos)
        }

        private fun constant(n: String): Double = when (n) {
            "pi" -> PI
            "tau" -> 2 * PI
            "e" -> E
            "phi" -> (1 + sqrt(5.0)) / 2
            "ans" -> 0.0
            else -> error("Невідома константа: $n")
        }

        private fun function(n: String, a: List<Double>): Double {
            fun one() = a.singleOrNull() ?: error("$n потребує 1 аргумент")
            fun two() = if (a.size == 2) a[0] to a[1] else error("$n потребує 2 аргументи")
            fun angle(x: Double) = if (mode == AngleMode.DEG) Math.toRadians(x) else x
            fun out(x: Double) = if (mode == AngleMode.DEG) Math.toDegrees(x) else x

            return when (n) {
                "sqrt" -> sqrt(one())
                "cbrt" -> cbrt(one())
                "abs" -> abs(one())
                "floor" -> floor(one())
                "ceil" -> ceil(one())
                "round" -> round(one())
                "frac" -> one() - floor(one())
                "exp" -> exp(one())
                "ln" -> ln(one())
                "log", "log10" -> if (n == "log10" && a.size == 1) log10(one())
                    else if (a.size == 1) log10(one())
                    else log(two().second, two().first)
                "sin" -> sin(angle(one()))
                "cos" -> cos(angle(one()))
                "tan" -> tan(angle(one()))
                "asin" -> out(asin(one()))
                "acos" -> out(acos(one()))
                "atan" -> out(atan(one()))
                "sinh" -> sinh(one())
                "cosh" -> cosh(one())
                "tanh" -> tanh(one())
                "asinh" -> asinh(one())
                "acosh" -> acosh(one())
                "atanh" -> atanh(one())
                "hypot" -> hypot(two().first, two().second)
                "root" -> two().second.pow(1.0 / two().first)
                "pow" -> two().first.pow(two().second)
                "mod" -> two().first % two().second
                "min" -> a.minOrNull() ?: error("min")
                "max" -> a.maxOrNull() ?: error("max")
                "gcd" -> gcd(two().first.toLong(), two().second.toLong()).toDouble()
                "lcm" -> lcm(two().first.toLong(), two().second.toLong()).toDouble()
                "ncr", "comb" -> choose(two().first.toInt(), two().second.toInt()).toDouble()
                "npr", "perm" -> perm(two().first.toInt(), two().second.toInt()).toDouble()
                "fact", "factorial" -> gamma(one() + 1)
                "fib" -> fibonacci(one().toInt()).toDouble()
                "isprime" -> if (isPrime(one().toLong())) 1.0 else 0.0
                "sign", "sgn" -> sign(one())
                "sigmoid" -> 1.0 / (1.0 + exp(-one()))
                else -> error("Невідома функція: $n")
            }
        }

        private fun gamma(z: Double): Double {
            if (z < 0.5) return PI / (sin(PI * z) * gamma(1 - z))
            var x = 0.9999999999999971
            val p = doubleArrayOf(
                676.5203681218851, -1259.1392167224028, 771.32342877765313,
                -176.61502916214059, 12.507343278686905, -0.13857109526572012,
                9.9843695780195716e-6, 1.5056327351493116e-7
            )
            var zz = z - 1
            for (i in p.indices) x += p[i] / (zz + i + 1)
            val t = zz + p.size - 0.5
            return sqrt(2 * PI) * t.pow(zz + 0.5) * exp(-t) * x
        }

        private fun gcd(a0: Long, b0: Long): Long {
            var a = abs(a0); var b = abs(b0)
            while (b != 0L) { val t = a % b; a = b; b = t }
            return a
        }

        private fun lcm(a: Long, b: Long) = if (a == 0L || b == 0L) 0L else abs(a / gcd(a, b) * b)

        private fun choose(n: Int, r: Int): Long {
            if (r < 0 || r > n) error("Некоректні nCr")
            var k = min(r, n - r)
            var result = 1L
            for (i in 1..k) result = result / i * (n - k + i)
            return result
        }

        private fun perm(n: Int, r: Int): Long {
            if (r < 0 || r > n) error("Некоректні nPr")
            var result = 1L
            for (i in 0 until r) result *= (n - i)
            return result
        }

        private fun fibonacci(n: Int): Long {
            if (n < 0) error("fib(n) для n>=0")
            var a = 0L; var b = 1L
            repeat(n) { val t = a + b; a = b; b = t }
            return a
        }

        private fun isPrime(n: Long): Boolean {
            if (n < 2) return false
            if (n % 2L == 0L) return n == 2L
            var d = 3L
            while (d * d <= n) { if (n % d == 0L) return false; d += 2 }
            return true
        }

        fun skipSpaces() { while (pos < s.length && s[pos].isWhitespace()) pos++ }
        fun atEnd() = pos >= s.length
        fun peek() = if (pos < s.length) s[pos] else '\u0000'

        private fun eat(c: Char): Boolean {
            skipSpaces()
            if (pos < s.length && s[pos] == c) { pos++; return true }
            return false
        }

        private fun requireEat(c: Char) {
            if (!eat(c)) error("Очікувалося '$c'")
        }

        private fun shouldImplicitMultiply(): Boolean {
            skipSpaces()
            return pos < s.length && (s[pos] == '(' || s[pos].isDigit() || s[pos].isLetter() || s[pos] == '.')
        }
    }
}
