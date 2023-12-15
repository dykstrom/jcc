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

import org.junit.jupiter.api.Test
import se.dykstrom.jcc.main.Language.BASIC

/**
 * Compile-and-run integration tests for Basic, specifically for testing (line) input statements.
 *
 * @author Johan Dykstrom
 */
class BasicCompileAndRunInputIT : AbstractIntegrationTests() {

    @Test
    fun shouldInputString() {
        val source = listOf(
                "dim msg as string",
                "line input msg",
                "print \"-\"; msg; \"-\""
        )
        val expected = listOf(
                "-HELLO!-"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, listOf("HELLO!"), expected)
    }

    @Test
    fun shouldInputEmptyString() {
        val source = listOf(
                "dim msg as string",
                "line input msg",
                "print \"-\"; msg; \"-\""
        )
        val expected = listOf(
                "--"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, listOf(""), expected)
    }

    @Test
    fun shouldInputStringWithPrompt() {
        val source = listOf(
                "dim msg as string",
                "line input \"What? \"; msg",
                "print \"-\"; msg; \"-\""
        )
        val expected = listOf(
                "What? -HELLO!-"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, listOf("HELLO!"), expected)
    }

    @Test
    fun shouldInputTwoStrings() {
        val source = listOf(
                "dim msg as string",
                "line input msg",
                "print \"-\"; msg; \"-\"",
                "line input msg",
                "print \"-\"; msg; \"-\""
        )
        val expected = listOf(
                "-a-",
                "-b-"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, listOf("a", "b"), expected)
    }
}
