/*
 * Copyright (C) 2025 Johan Dykstrom
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package se.dykstrom.jcc.main

import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import se.dykstrom.jcc.main.Language.COL

/**
 * Compile-and-run integration tests for COL.
 *
 * @author Johan Dykstrom
 */
@Tag("LLVM")
class ColCompileAndRunIT : AbstractIntegrationTests() {

    @Test
    fun shouldPrintlnLiteral() {
        val source = listOf(
            "call println(7)",
            "call println(-7)",
            "call println(5.3)",
            "call println(-5.3)",
            "call println(true)",
            "call println(false)",
        )
        val sourcePath = createSourceFile(source, COL)
        compileAndAssertSuccess(sourcePath, language = COL)
        runAndAssertSuccess(
            listOf(),
            listOf(
                "7",
                "-7",
                "5.300000",
                "-5.300000",
                "true",
                "false",
            ),
        )
    }

    @Test
    fun shouldPrintlnLiteralsWithTypeSuffix() {
        val source = listOf(
            "call println(17i32)",
            "call println(17i64)",
            "call println(-2147483648i32)",
            "call println(10_000i32)",
            "call println(17f32)",
            "call println(17f64)",
            "call println(1.5f32)",
            "call println(1.5f64)",
            // 5.3 is not exactly representable in single precision, so the compiler
            // must round it to the nearest float (5.30000019...) to emit a valid LLVM
            // constant. The expected output still reads "5.300000" because printf's
            // %f prints six decimals, which hides the rounding error.
            "call println(5.3f32)",
            "call println(1E9f32)",
            "call println(1E9f64)",
        )
        val sourcePath = createSourceFile(source, COL)
        compileAndAssertSuccess(sourcePath, language = COL)
        runAndAssertSuccess(
            listOf(),
            listOf(
                "17",
                "17",
                "-2147483648",
                "10000",
                "17.000000",
                "17.000000",
                "1.500000",
                "1.500000",
                "5.300000",
                "1000000000.000000",
                "1000000000.000000",
            ),
        )
    }

    @Test
    fun shouldPassLiteralsWithTypeSuffixToFunctions() {
        val source = listOf(
            "fun sumI32(a as i32, b as i32) -> i32 := a + b",
            "fun sumF32(a as f32, b as f32) -> f32 := a + b",
            "call println(sumI32(17i32, 18i32))",
            "call println(sumF32(1.5f32, 5.3f32))",
        )
        val sourcePath = createSourceFile(source, COL)
        compileAndAssertSuccess(sourcePath, language = COL)
        runAndAssertSuccess(
            listOf(),
            listOf(
                "35",
                "6.800000",
            ),
        )
    }

    @Test
    fun shouldPrintlnExpressions() {
        val source = listOf(
            // Arithmetic operators
            "call println(1 + 2 + 3)",
            "call println(7 - 3 - 10)",
            "call println(10_000 - 1_000)",
            "call println(0b00010)",
            "call println(0xfe)",
            "call println(0.99)",
            "call println(1E9)",
            "call println(1 * 2 * 3)",
            "call println(10.0 / 2.0)",
            "call println(10 div 3)",
            "call println(10 mod 3)",
            "call println(10 * -(10 - 2))",
            // Bitwise operators
            "call println(6 & 3)",
            "call println(6 | 3)",
            "call println(6 ^ 3)",
            "call println(~0)",
            // Relational operators
            "call println(0 == 1)",
            "call println(24 == 24)",
            "call println(2.345 == 2.345)",
            "call println(0 != 1)",
            "call println(0 < 1)",
            "call println(0 <= 1)",
            "call println(0 > 1)",
            "call println(0 >= 1)",
            "call println(1.0 >= 1.0)",
            "call println(true == false)",
            "call println(true != false)",
            // Logical operators
            "call println(true and false)",
            "call println(false and true)",
            "call println(0 > -1 and -1 > -2)",
            "call println(true or false)",
            "call println(false or true)",
            "call println(false xor false)",
            "call println(false xor 1 != 0)",
            "call println(not false)",
            "call println(not (1.0 > 0.5))",
        )
        val sourcePath = createSourceFile(source, COL)
        compileAndAssertSuccess(sourcePath, language = COL)
        runAndAssertSuccess(listOf(), listOf(
            // Arithmetic operators
            "6",
            "-6",
            "9000",
            "2",
            "254",
            "0.990000",
            "1000000000.000000",
            "6",
            "5.000000",
            "3",
            "1",
            "-80",
            // Bitwise operators
            "2",
            "7",
            "5",
            "-1",
            // Relational operators
            "false",
            "true",
            "true",
            "true",
            "true",
            "true",
            "false",
            "false",
            "true",
            "false",
            "true",
            // Logical operators
            "false",
            "false",
            "true",
            "true",
            "true",
            "false",
            "true",
            "true",
            "false",
        ))
    }

    @Test
    fun shouldPrintlnNestedLogicalAnd() {
        val source = listOf(
            "call println(foo(3, i32(0)))",
            "",
            "fun foo(a as i64, dummy as i32) -> i64 :=",
            "  if (a > 0 and true) and (if a < 3 then true else false) then",
            "    if a == 1 then",
            "      foo(a - 1, println(10))",
            "    else",
            "      foo(a - 1, println(20))",
            "  else",
            "    if a == 0 and 0 == a then",
            "      -1",
            "    else",
            "      foo(a - 1, println(30))",
        )
        val sourcePath = createSourceFile(source, COL)
        compileAndAssertSuccess(sourcePath, language = COL)
        runAndAssertSuccess(listOf(), listOf(
            "30",
            "20",
            "10",
            "-1"
        ))
    }

    @Test
    fun shouldPrintlnNestedLogicalOr() {
        val source = listOf(
            "call println(bar(4, i32(0)))",
            "",
            "fun bar(a as i64, dummy as i32) -> i64 :=",
            "  if a == 0 then",
            "    -1",
            "  else if (a < 2 or  false) or (a > 3 or false) then",
            "    if a == 1 then",
            "      bar(a - 1, println(10))",
            "    else",
            "      bar(a - 1, println(40))",
            "  else if a == 3 or a == 3 then",
            "    bar(a - 1, println(30))",
            "  else",
            "    bar(a - 1, println(20))"
        )
        val sourcePath = createSourceFile(source, COL)
        compileAndAssertSuccess(sourcePath, language = COL)
        runAndAssertSuccess(listOf(), listOf(
            "40",
            "30",
            "20",
            "10",
            "-1"
        ))
    }

    @Test
    fun shouldCallIntrinsicFunctions() {
        val source = listOf(
            // Rounding
            "call println(ceil(3.7))",
            "call println(ceil(f32(3.7)))",
            "call println(floor(3.7))",
            "call println(floor(f32(3.7)))",
            "call println(round(3.7))",
            "call println(round(f32(3.7)))",
            "call println(round(-3.7))",
            "call println(round(f32(-3.7)))",
            "call println(trunc(3.7))",
            "call println(trunc(f32(3.7)))",
            "call println(trunc(-3.7))",
            "call println(trunc(f32(-3.7)))",
            // Math
            "call println(abs(-5))",
            "call println(abs(i32(-5)))",
            "call println(abs(-3.3))",
            "call println(abs(f32(-3.3)))",
            "call println(max(3, 9))",
            "call println(min(3, 9))",
            "call println(max(3.88, 3.87))",
            "call println(min(-0.3, -0.7))",
            "call println(sqrt(4.0))",
            "call println(sqrt(f32(4.0)))",
        )
        val sourcePath = createSourceFile(source, COL)
        compileAndAssertSuccess(sourcePath, language = COL)
        runAndAssertSuccess(
            listOf(),
            listOf(
                // Rounding
                "4.000000",
                "4.000000",
                "3.000000",
                "3.000000",
                "4.000000",
                "4.000000",
                "-4.000000",
                "-4.000000",
                "3.000000",
                "3.000000",
                "-3.000000",
                "-3.000000",
                // Math
                "5",
                "5",
                "3.300000",
                "3.300000",
                "9",
                "3",
                "3.880000",
                "-0.700000",
                "2.000000",
                "2.000000",
            ),
        )
    }

    @Test
    fun shouldCallMathFunctions() {
        val source = listOf(
            // Group 1 + 2 intrinsics
            "call println(pow(2.0, 10.0))",
            "call println(pow(f32(2.0), f32(10.0)))",
            "call println(sin(0.0))",
            "call println(cos(0.0))",
            "call println(tan(0.0))",
            "call println(atan(0.0))",
            "call println(exp(0.0))",
            "call println(log(1.0))",
            "call println(exp2(3.0))",
            "call println(log2(8.0))",
            "call println(log10(1000.0))",
            "call println(fma(2.0, 3.0, 4.0))",
            // Group 3 libm
            "call println(cbrt(27.0))",
            "call println(cbrt(f32(27.0)))",
            "call println(fmod(10.0, 3.0))",
            "call println(fmod(f32(10.0), f32(3.0)))",
        )
        val sourcePath = createSourceFile(source, COL)
        compileAndAssertSuccess(sourcePath, language = COL)
        runAndAssertSuccess(
            listOf(),
            listOf(
                "1024.000000",
                "1024.000000",
                "0.000000",
                "1.000000",
                "0.000000",
                "0.000000",
                "1.000000",
                "0.000000",
                "8.000000",
                "3.000000",
                "3.000000",
                "10.000000",
                "3.000000",
                "3.000000",
                "1.000000",
                "1.000000",
            ),
        )
    }

    @Test
    fun shouldPrintlnIfExpression() {
        val source = listOf(
            "call println(if true then 7 else 13)",
            "call println(if false then 7 else 13)",
            "call println(if 0 != 1 then 7 else 13)",
            "call println(if 0 == 1 then 7 else 13)",
            "call println(if floor(3.9) < 4.0 then ceil(3.9) else trunc(3.9))",
            "call println(if true then (if false then 1 else 2) else (if false then 3 else 4))",
        )
        val sourcePath = createSourceFile(source, COL)
        compileAndAssertSuccess(sourcePath, language = COL)
        runAndAssertSuccess(
            listOf(),
            listOf(
                "7",
                "13",
                "7",
                "13",
                "4.000000",
                "2",
            ),
        )
    }

    @Test
    fun shouldPrintlnNestedIfExpressionInFunction() {
        val source = listOf(
            "call println(foo(3, i32(0)))",
            "",
            "fun foo(a as i64, dummy as i32) -> i64 :=",
            "  if a < 0 then",
            "    -1",
            "  else if a < 2 then",
            "    if a == 0 then",
            "      foo(a - 1, println(10))",
            "    else",
            "      foo(a - 1, println(11))",
            "  else",
            "    if a == 2 then",
            "      foo(a - 1, println(12))",
            "    else",
            "      foo(a - 1, println(13))",
        )
        val sourcePath = createSourceFile(source, COL)
        compileAndAssertSuccess(sourcePath, language = COL)
        runAndAssertSuccess(listOf(), listOf(
            "13",
            "12",
            "11",
            "10",
            "-1"
        ))
    }

    @Test
    fun shouldFollowIeee754Semantics() {
        val source = listOf(
            // Computing values via functions prevents constant folding;
            // literal division by zero is a compile-time error
            "fun zero() -> f64 := 0.0",
            "fun nan() -> f64 := 0.0 / zero()",
            "call println(1.0 / zero())",
            "call println(-1.0 / zero())",
            "call println(1.0 / zero() > 1E308)",
            // All comparisons with NaN are false, except != which is true
            "call println(nan() == nan())",
            "call println(nan() != nan())",
            "call println(nan() < 0.0)",
            "call println(nan() >= 0.0)",
        )
        val sourcePath = createSourceFile(source, COL)
        compileAndAssertSuccess(sourcePath, language = COL)
        runAndAssertSuccess(listOf(), listOf(
            "inf",
            "-inf",
            "true",
            "false",
            "true",
            "false",
            "false",
        ))
    }

    @Test
    fun shouldEvaluateLeftToRight() {
        val source = listOf(
            // Function call arguments evaluate left to right
            "call min(println(1), println(2))",
            // Binary operands evaluate left to right; println returns the
            // number of characters printed, so the sum is 2 + 2 = 4
            "call println(println(3) + println(4))",
        )
        val sourcePath = createSourceFile(source, COL)
        compileAndAssertSuccess(sourcePath, language = COL)
        runAndAssertSuccess(listOf(), listOf(
            "1",
            "2",
            "3",
            "4",
            "4",
        ))
    }

    @Test
    fun shouldPrintlnVal() {
        val source = listOf(
            "val limit as i64 := 10_000",
            "val phi := 1.618",
            "val yes := true",
            "call println(limit)",
            "call println(phi)",
            "call println(yes)",
        )
        val sourcePath = createSourceFile(source, COL)
        compileAndAssertSuccess(sourcePath, language = COL)
        runAndAssertSuccess(listOf(), listOf(
            "10000",
            "1.618000",
            "true",
        ))
    }

    @Test
    fun shouldUseValInExpressionsAndArguments() {
        val source = listOf(
            "val a := 17",
            "val b := a + 1",
            "call println(a + b)",
            "call println(max(a, b))",
            "call println(if a < b then a else b)",
        )
        val sourcePath = createSourceFile(source, COL)
        compileAndAssertSuccess(sourcePath, language = COL)
        runAndAssertSuccess(listOf(), listOf(
            "35",
            "18",
            "17",
        ))
    }

    @Test
    fun shouldInitializeValFromFunctionCall() {
        val source = listOf(
            // Functions are hoisted, so a val initializer may call a function defined later
            "val seventeen := bump(16)",
            "call println(seventeen)",
            "val elapsed := millis() - millis()",
            "call println(elapsed <= 0)",
            "fun bump(n as i64) -> i64 := n + 1",
        )
        val sourcePath = createSourceFile(source, COL)
        compileAndAssertSuccess(sourcePath, language = COL)
        runAndAssertSuccess(listOf(), listOf(
            "17",
            "true",
        ))
    }

    @Test
    fun shouldCallFunctionTypedVal() {
        val source = listOf(
            "fun inc(n as i64) -> i64 := n + 1",
            "fun inc(x as f64) -> f64 := x + 1.0",
            // The declared type resolves the overloaded function reference
            "val incI as (i64) -> i64 := inc",
            "val incF as (f64) -> f64 := inc",
            "call println(incI(16))",
            "call println(incF(0.5))",
        )
        val sourcePath = createSourceFile(source, COL)
        compileAndAssertSuccess(sourcePath, language = COL)
        runAndAssertSuccess(listOf(), listOf(
            "17",
            "1.500000",
        ))
    }

    @Test
    fun shouldSkipWhileBodyWhenConditionIsFalse() {
        val source = listOf(
            "while false do",
            "    call println(999)",
            "end",
            "call println(1)",
        )
        val sourcePath = createSourceFile(source, COL)
        compileAndAssertSuccess(sourcePath, language = COL)
        // The body never runs, so only the statement after the loop prints
        runAndAssertSuccess(listOf(), listOf("1"))
    }

    @Test
    fun shouldRunWhileLoopUntilDeadline() {
        // COL has no mutable state, so a terminating loop needs a side-effecting
        // condition. millis() advances on every call, so the loop ends once 50 ms
        // have passed. The body prints nothing (the call result is discarded), so the
        // output is deterministic: the loop must terminate and the trailing println run.
        val source = listOf(
            "fun nop(x as i64) -> i64 := x",
            "val start := millis()",
            "while millis() - start < 50 do",
            "    call nop(1)",
            "end",
            "call println(1)",
        )
        val sourcePath = createSourceFile(source, COL)
        compileAndAssertSuccess(sourcePath, language = COL)
        runAndAssertSuccess(listOf(), listOf("1"))
    }

    @Test
    fun shouldRunDeepTailRecursionAtDefaultOptimization() {
        // Without become this overflows the stack at the default -O0; become guarantees the tail
        // call (musttail), turning the recursion into a loop that runs in constant stack
        val source = listOf(
            "fun count(n as i64, acc as i64) -> i64 :=",
            "    if n <= 0 then acc else become count(n - 1, acc + 1)",
            "fun count(n as i64) -> i64 :=",
            "    become count(n, 0)",
            "call println(count(100000000))",
        )
        val sourcePath = createSourceFile(source, COL)
        compileAndAssertSuccess(sourcePath, language = COL)
        runAndAssertSuccess(listOf(), listOf(
            "100000000",
        ))
    }

    @Test
    fun shouldRunMutualTailRecursionAtDepth() {
        // even/odd tail-call each other; tailcc on COL-internal functions makes the cross-function
        // musttail valid, and it runs in constant stack at the default -O0
        val source = listOf(
            "fun even(n as i64) -> bool := if n == 0 then true else become odd(n - 1)",
            "fun odd(n as i64) -> bool := if n == 0 then false else become even(n - 1)",
            "call println(even(10000000))",
            "call println(odd(10000000))",
        )
        val sourcePath = createSourceFile(source, COL)
        compileAndAssertSuccess(sourcePath, language = COL)
        runAndAssertSuccess(listOf(), listOf(
            "true",
            "false",
        ))
    }

    @Test
    fun shouldRunBecomeInNestedIfExpressions() {
        // become appears in the branches of nested (else-if) if-expressions; each branch must
        // terminate with its own musttail + ret, so tail context has to thread through the nesting
        val source = listOf(
            "fun classify(n as i64, acc as i64) -> i64 :=",
            "    if n <= 0 then acc",
            "    else if n mod 2 == 0 then become classify(n - 1, acc + 2)",
            "    else if n mod 3 == 0 then become classify(n - 1, acc + 3)",
            "    else become classify(n - 1, acc + 1)",
            "fun classify(n as i64) -> i64 :=",
            "    become classify(n, 0)",
            "call println(classify(10))",
            // Deep enough to overflow the stack without guaranteed tail calls
            "call println(classify(100000000))",
        )
        val sourcePath = createSourceFile(source, COL)
        compileAndAssertSuccess(sourcePath, language = COL)
        runAndAssertSuccess(listOf(), listOf(
            "19",
            "183333334",
        ))
    }
}
