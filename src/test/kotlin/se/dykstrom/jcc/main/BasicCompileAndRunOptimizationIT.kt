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
import java.util.Arrays.asList

/**
 * Compile-and-run integration tests for Basic, specifically for testing optimization.
 *
 * @author Johan Dykstrom
 */
class BasicCompileAndRunOptimizationIT : AbstractIntegrationTest() {

    @Test
    fun shouldReplaceAddOneWithInc() {
        val source = asList(
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
        val source = asList(
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
        val source = asList(
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
        val source = asList(
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
        val source = asList(
                "foo = 17 + 2",
                "print foo"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile, "-O1")
        runAndAssertSuccess(sourceFile, "19\n", 0)
    }

    @Test
    fun shouldReplaceAddFloatsWithLiteral() {
        val source = asList(
                "foo = 17.5 + 2.3",
                "print foo"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile, "-O1")
        runAndAssertSuccess(sourceFile, "19.800000\n", 0)
    }

    @Test
    fun shouldReplaceAddStringsWithLiteral() {
        val source = asList(
                "foo = \"A\" + \"B\"",
                "print foo"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile, "-O1")
        runAndAssertSuccess(sourceFile, "AB\n", 0)
    }
}
