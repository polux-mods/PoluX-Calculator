package com.example.supercalc

import kotlin.math.*

object MathEngine {

    fun eval(expr: String): String {
        return try {
            val cleaned = expr.replace("×", "*")
                .replace("÷", "/")
                .replace(",", ".")
                .replace(" ", "")
            val result = parseExpression(cleaned)
            if (result == result.toLong().toDouble()) {
                result.toLong().toString()
            } else {
                String.format("%.6f", result).trimEnd('0').trimEnd('.')
            }
        } catch (e: Exception) {
            "Помилка"
        }
    }

    // --- УНІКАЛЬНІ МАТЕМАТИЧНІ ФУНКЦІЇ ---

    // 1. Кількість кроків у Гіпотезі Коллатца (3n + 1)
    private fun collatzSteps(n: Long): Long {
        if (n <= 0) return 0
        var count = 0L
        var curr = n
        while (curr > 1L && count < 10000) {
            curr = if (curr % 2L == 0L) curr / 2L else 3L * curr + 1L
            count++
        }
        return count
    }

    // 2. Цифровий корінь (послідовне додавання цифр до однієї)
    private fun digitalRoot(n: Long): Long {
        var num = abs(n)
        while (num >= 10) {
            var sum = 0L
            while (num > 0) {
                sum += num % 10
                num /= 10
            }
            num = sum
        }
        return num
    }

    // 3. Перевірка чи число просте (1 - так, 0 - ні)
    private fun isPrime(n: Long): Double {
        if (n <= 1) return 0.0
        if (n <= 3) return 1.0
        if (n % 2 == 0L || n % 3 == 0L) return 0.0
        var i = 5L
        while (i * i <= n) {
            if (n % i == 0L || n % (i + 2) == 0L) return 0.0
            i += 6
        }
        return 1.0
    }

    // 4. N-не число Фібоначчі
    private fun fibonacci(n: Int): Double {
        if (n <= 0) return 0.0
        if (n == 1 || n == 2) return 1.0
        var a = 1L
        var b = 1L
        for (i in 3..min(n, 90)) {
            val temp = a + b
            a = b
            b = temp
        }
        return b.toDouble()
    }

    // 5. Суперфакторіал: sf(n) = 1! * 2! * ... * n!
    private fun superFactorial(n: Int): Double {
        if (n < 1 || n > 15) return 0.0
        var prod = 1.0
        var currentFact = 1.0
        for (i in 1..n) {
            currentFact *= i
            prod *= currentFact
        }
        return prod
    }

    // Рекурсивний парсер математичних виразів
    private fun parseExpression(str: String): Double {
        var pos = -1
        var ch = 0

        fun nextChar() {
            ch = if (++pos < str.length) str[pos].code else -1
        }

        fun eat(charToEat: Int): Boolean {
            while (ch == ' '.code) nextChar()
            if (ch == charToEat) {
                nextChar()
                return true
            }
            return false
        }

        fun parseFactor(): Double
        fun parseTerm(): Double
        fun parseExpr(): Double

        fun parseFactor(): Double {
            if (eat('+'.code)) return parseFactor()
            if (eat('-'.code)) return -parseFactor()

            var x: Double
            val startPos = pos
            if (eat('('.code)) {
                x = parseExpr()
                eat(')'.code)
            } else if ((ch in '0'.code..'9'.code) || ch == '.'.code) {
                while ((ch in '0'.code..'9'.code) || ch == '.'.code) nextChar()
                x = str.substring(startPos, pos).toDouble()
            } else if (ch in 'a'.code..'z'.code || ch in 'A'.code..'Z'.code) {
                while (ch in 'a'.code..'z'.code || ch in 'A'.code..'Z'.code) nextChar()
                val func = str.substring(startPos, pos).lowercase()
                eat('('.code)
                val arg = parseExpr()
                eat(')'.code)
                x = when (func) {
                    "sqrt" -> sqrt(arg)
                    "sin" -> sin(Math.toRadians(arg))
                    "cos" -> cos(Math.toRadians(arg))
                    "tan" -> tan(Math.toRadians(arg))
                    "collatz" -> collatzSteps(arg.toLong()).toDouble()
                    "digroot" -> digitalRoot(arg.toLong()).toDouble()
                    "isprime" -> isPrime(arg.toLong())
                    "fib" -> fibonacci(arg.toInt())
                    "superfact" -> superFactorial(arg.toInt())
                    else -> error("Невідома функція")
                }
            } else {
                error("Символ помилки")
            }

            if (eat('^'.code)) x = x.pow(parseFactor())

            return x
        }

        fun parseTerm(): Double {
            var x = parseFactor()
            while (true) {
                if (eat('*'.code)) x *= parseFactor()
                else if (eat('/'.code)) x /= parseFactor()
                else return x
            }
        }

        fun parseExpr(): Double {
            var x = parseTerm()
            while (true) {
                if (eat('+'.code)) x += parseTerm()
                else if (eat('-'.code)) x -= parseTerm()
                else return x
            }
        }

        nextChar()
        val x = parseExpr()
        if (pos < str.length) error("Помилка синтаксису")
        return x
    }
}
