/*
 * Copyright (C) 2019 Johan Dykstrom
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

import org.junit.Test

/**
 * Compile-and-run integration tests for Basic, specifically for testing optimization.
 *
 * @author Johan Dykstrom
 */
class BasicCompileAndRunOptimizationIT : AbstractIntegrationTest() {

    @Test
    fun shouldReplaceAddOneWithInc() {
        val source = listOf(
                "foo = 17",
                "foo = foo + 1",
                "print foo"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile, "-O1")
        runAndAssertSuccess(sourceFile, "18\n", 0)
    }

    @Test
    fun shouldReplaceSubOneWithDec() {
        val source = listOf(
                "foo = 17",
                "foo = foo - 1",
                "print foo"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile, "-O1")
        runAndAssertSuccess(sourceFile, "16\n", 0)
    }

    @Test
    fun shouldReplaceAddTwoWithAddAssign() {
        val source = listOf(
                "foo = 17",
                "foo = foo + 2",
                "print foo"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile, "-O1")
        runAndAssertSuccess(sourceFile, "19\n", 0)
    }

    @Test
    fun shouldReplaceSubTwoWithSubAssign() {
        val source = listOf(
                "foo = 17",
                "foo = foo - 2",
                "print foo"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile, "-O1")
        runAndAssertSuccess(sourceFile, "15\n", 0)
    }

    @Test
    fun shouldReplaceAddIntegersWithLiteral() {
        val source = listOf(
                "foo = 17 + 2",
                "print foo",
                "bar = 0 + 0",
                "print bar"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile, "-O1")
        runAndAssertSuccess(sourceFile, "19\n0\n", 0)
    }

    @Test
    fun shouldReplaceSubIntegersWithLiteral() {
        val source = listOf(
                "foo = 12345 - 6789",
                "print foo",
                "bar = 10 - 10 - 10",
                "print bar",
                "tee = 999 - 0",
                "print tee"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile, "-O1")
        runAndAssertSuccess(sourceFile, "5556\n-10\n999\n", 0)
    }

    @Test
    fun shouldReplaceAddFloatsWithLiteral() {
        val source = listOf(
                "foo = 17.5 + 2.3",
                "print foo"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile, "-O1")
        runAndAssertSuccess(sourceFile, "19.800000\n", 0)
    }

    @Test
    fun shouldReplaceAddIntegerAndFloatWithLiteral() {
        val source = listOf(
                "foo = 17 + 2.3",
                "print foo"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile, "-O1")
        runAndAssertSuccess(sourceFile, "19.300000\n", 0)
    }

    @Test
    fun shouldReplaceSubIntegerAndFloatWithLiteral() {
        val source = listOf(
                "foo = 17 - 2.3",
                "print foo",
                "bar = 4.77 - 0",
                "print bar"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile, "-O1")
        runAndAssertSuccess(sourceFile, "14.700000\n4.770000\n", 0)
    }

    @Test
    fun shouldReplaceAddStringsWithLiteral() {
        val source = listOf(
                "foo = \"A\" + \"B\"",
                "print foo"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile, "-O1")
        runAndAssertSuccess(sourceFile, "AB\n", 0)
    }

    @Test
    fun shouldRemoveAddWithZero() {
        val source = listOf(
                "bar = 5",
                "foo = 0 + bar",
                "print foo"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile, "-O1")
        runAndAssertSuccess(sourceFile, "5\n", 0)
    }

    @Test
    fun shouldRemoveMulWithOne() {
        val source = listOf(
                "bar = 5",
                "foo = 1 * bar",
                "print foo"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile, "-O1")
        runAndAssertSuccess(sourceFile, "5\n", 0)
    }

    @Test
    fun shouldRemoveDivByOne() {
        val source = listOf(
                "bar = 5.5",
                "foo = bar / 1",
                "print foo"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile, "-O1")
        runAndAssertSuccess(sourceFile, "5.500000\n", 0)
    }

    @Test
    fun shouldReplaceMulLiteralsWithSingleLiteral() {
        val source = listOf(
                "foo% = 17 * 2",
                "print foo%",
                "bar# = 5 * 3.0",
                "print bar#",
                "axe# = 3.1 * 3.0",
                "print axe#"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile, "-O1")
        runAndAssertSuccess(sourceFile, "34\n15.000000\n9.300000\n", 0)
    }

    @Test
    fun shouldReplaceIDivLiteralsWithSingleLiteral() {
        val source = listOf(
                "foo% = 17 \\ 2",
                "print foo%",
                "bar% = 33 \\ 11",
                "print bar%",
                "axe = 2048 \\ 4",
                "print axe",
                "tee = 4711 \\ 1",
                "print tee"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile, "-O1")
        runAndAssertSuccess(sourceFile, "8\n3\n512\n4711\n", 0)
    }

    @Test
    fun shouldReplaceDivLiteralsWithSingleLiteral() {
        val source = listOf(
                "foo = 17 / 2",
                "print foo",
                "bar = 1 / 3",
                "print bar",
                "axe# = 25.0 / 4",
                "print axe#",
                "tee = 3.7 / 2.0",
                "print tee"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile, "-O1")
        runAndAssertSuccess(sourceFile, "8.500000\n0.333333\n6.250000\n1.850000\n", 0)
    }

    @Test
    fun shouldReplaceMulWithZeroWithJustZero() {
        val source = listOf(
                "bar = 5",
                "foo = bar * 0",
                "print foo"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile, "-O1")
        runAndAssertSuccess(sourceFile, "0\n", 0)
    }

    @Test
    fun shouldReplaceZeroDividedBySomethingWithJustZero() {
        val source = listOf(
                "bar = 5.5",
                "foo = 0.0 / bar",
                "print foo"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile, "-O1")
        runAndAssertSuccess(sourceFile, "0.000000\n", 0)
    }

    /**
     * We cannot optimize away a function call, because the function call may have side effects.
     */
    @Test
    fun shouldNotReplaceMulFunctionCallWithZeroWithJustZero() {
        val source = listOf(
                "foo = sgn(5) * 0",
                "print foo"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile, "-O1")
        runAndAssertSuccess(sourceFile, "0\n", 0)
    }

    /**
     * We cannot optimize away a function call, because the function call may have side effects.
     */
    @Test
    fun shouldNotReplaceIDivZeroByFunctionCallWithJustZero() {
        val source = listOf(
                "foo = 0 \\ sgn(5)",
                "print foo"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile, "-O1")
        runAndAssertSuccess(sourceFile, "0\n", 0)
    }

    @Test
    fun shouldReplaceMulWithPowerOfTwoWithShift() {
        val source = listOf(
                "bar = 5",
                "foo = bar * 2",
                "print foo",
                "foo = bar * 8",
                "print foo",
                "foo = bar * 2048",
                "print foo"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile, "-O1")
        runAndAssertSuccess(sourceFile, "10\n40\n10240\n", 0)
    }

    @Test
    fun shouldOptimizeRandomizeExpression() {
        val source = listOf(
                "randomize 1000",
                "print rnd()",
                "randomize 500 + 500",
                "print rnd()"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile, "-O1")
        runAndAssertSuccess(sourceFile, "0.237960\n0.237960\n", 0)
    }
}
