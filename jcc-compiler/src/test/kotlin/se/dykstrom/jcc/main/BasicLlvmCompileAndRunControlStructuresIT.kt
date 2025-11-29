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
import se.dykstrom.jcc.main.Language.BASIC

/**
 * Compile-and-run integration tests for the BASIC LLVM backend, focusing on control structures.
 *
 * @author Johan Dykstrom
 */
@Tag("LLVM")
class BasicLlvmCompileAndRunControlStructuresIT : AbstractIntegrationTests() {

    @Test
    fun whileLoop() {
        val source = listOf(
            "DIM a AS INTEGER",
            "a = 0",
            "WHILE a < 5",
            "  PRINT a",
            "  a = a + 1",
            "WEND"
        )
        val sourcePath = createSourceFile(source, BASIC)
        compileLlvmAndAssertSuccess(sourcePath, BASIC)
        runLlvmAndAssertSuccess(
            listOf(),
            listOf(
                "0",
                "1",
                "2",
                "3",
                "4",
            ),
        )
    }

    @Test
    fun nestedWhileLoop() {
        val source = listOf(
            "DIM a AS INTEGER, b AS INTEGER",
            "a = 0",
            "WHILE a < 3",
            "  b = 0",
            "  WHILE b < 3",
            "    PRINT a; b",
            "    b = b + 1",
            "  WEND",
            "  a = a + 1",
            "WEND"
        )
        val sourcePath = createSourceFile(source, BASIC)
        compileLlvmAndAssertSuccess(sourcePath, BASIC)
        runLlvmAndAssertSuccess(
            listOf(),
            listOf(
                "00",
                "01",
                "02",
                "10",
                "11",
                "12",
                "20",
                "21",
                "22",
            ),
        )
    }

    @Test
    fun ifInt() {
        val source = listOf(
            "DIM a AS INTEGER",
            "a = 0",
            "WHILE a < 5",
            "  IF a MOD 2 = 0 THEN",
            "    PRINT a",
            "  END IF",
            "  a = a + 1",
            "WEND"
        )
        val sourcePath = createSourceFile(source, BASIC)
        compileLlvmAndAssertSuccess(sourcePath, BASIC)
        runLlvmAndAssertSuccess(
            listOf(),
            listOf(
                "0",
                "2",
                "4",
            ),
        )
    }

    @Test
    fun simpleNestedIf() {
        val source = listOf(
            "IF 0 THEN",
            "  IF 1 THEN",
            "    PRINT 1",
            "  ELSE",
            "    PRINT 2",
            "  END IF",
            "ELSE",
            "  IF 3 THEN",
            "    PRINT 3",
            "  ELSE",
            "    PRINT 4",
            "  END IF",
            "END IF",
        )
        val sourcePath = createSourceFile(source, BASIC)
        compileLlvmAndAssertSuccess(sourcePath, BASIC)
        runLlvmAndAssertSuccess(
            listOf(),
            listOf(
                "3",
            ),
        )
    }

    @Test
    fun nestedIfAndMore() {
        val source = listOf(
            "f# = 3.5",
            "IF f# > 0.0 AND f# < 10.0 THEN",
            "  PRINT \"between\"",
            "END IF",
            "IF f3 > 10.0 THEN",
            "  PRINT \"greater\"",
            "END IF",
            "IF f3 > 10.0 THEN",
            "  PRINT \"greater\"",
            "ELSE",
            "  PRINT \"not greater\"",
            "END IF",
            "IF f# <= 10.0 THEN",
            "  IF 7.0 > f# THEN",
            "    PRINT \"less than 7\"",
            "  ELSE",
            "    PRINT \"greater than 7\"",
            "  END IF",
            "END IF",
        )
        val sourcePath = createSourceFile(source, BASIC)
        compileLlvmAndAssertSuccess(sourcePath, BASIC)
        runLlvmAndAssertSuccess(
            listOf(),
            listOf(
                "between",
                "not greater",
                "less than 7",
            ),
        )
    }

    @Test
    fun shouldGotoLabel() {
        val source = listOf(
            "10 PRINT 10",
            "20 GOTO 40",
            "30 PRINT 30",
            "40 PRINT 40",
        )
        val sourcePath = createSourceFile(source, BASIC)
        compileLlvmAndAssertSuccess(sourcePath, BASIC)
        runLlvmAndAssertSuccess(
            listOf(),
            listOf(
                "10",
                "40",
            ),
        )
    }

    @Test
    fun shouldGosubLabel() {
        val source = listOf(
            "10 PRINT 10",
            "20 GOSUB 100",
            "30 PRINT 30",
            "40 GOSUB 200",
            "50 PRINT 50",
            "60 END",
            "",
            "100 PRINT 100",
            "110 RETURN",
            "200 PRINT 200",
            "210 RETURN",
        )
        val sourcePath = createSourceFile(source, BASIC)
        compileLlvmAndAssertSuccess(sourcePath, BASIC)
        runLlvmAndAssertSuccess(
            listOf(),
            listOf(
                "10",
                "100",
                "30",
                "200",
                "50",
            ),
        )
    }
}
