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

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import se.dykstrom.jcc.main.Language.BASIC

/**
 * Compile-and-run integration tests for the BASIC LLVM backend.
 *
 * @author Johan Dykstrom
 */
@Tag("LLVM")
class BasicLlvmCompileAndRunIT : AbstractIntegrationTests() {

    @Test
    fun shouldPrintLiterals() {
        val source = listOf(
            "REM Print literals!",
            "PRINT 7",
            "PRINT -7",
            "PRINT 5.3",
            "PRINT -5.3",
            "PRINT 1; 3.14; 1000",
            "PRINT \"foo\"",
        )
        val sourcePath = createSourceFile(source, BASIC)
        compileLlvmAndAssertSuccess(sourcePath)
        runLlvmAndAssertSuccess(
            listOf(),
            listOf(
                "7",
                "-7",
                "5.300000",
                "-5.300000",
                "13.1400001000",
                "foo",
            ),
        )
    }

    @Test
    fun shouldPrintArithmeticIntExpressions() {
        val source = listOf(
            "PRINT 8 + 7",
            "PRINT 8 - 7",
            "PRINT 8 * 7",
            "PRINT 8 \\ 7",
            "PRINT 8 MOD 7",
            "PRINT -(8 * 7)",
        )
        val sourcePath = createSourceFile(source, BASIC)
        compileLlvmAndAssertSuccess(sourcePath)
        runLlvmAndAssertSuccess(
            listOf(),
            listOf(
                "15",
                "1",
                "56",
                "1",
                "1",
                "-56",
            ),
        )
    }

    @Test
    fun shouldPrintBitwiseIntExpressions() {
        val source = listOf(
            "PRINT 5 AND 7",
            "PRINT 5 EQV 5",
            "PRINT 5 IMP 5",
            "PRINT 5 OR 7",
            "PRINT 5 XOR 7",
            "PRINT NOT 0",
        )
        val sourcePath = createSourceFile(source, BASIC)
        compileLlvmAndAssertSuccess(sourcePath)
        runLlvmAndAssertSuccess(
            listOf(),
            listOf(
                "5",
                "-1",
                "-1",
                "7",
                "2",
                "-1",
            ),
        )
    }

    // TODO: Enable when variables have been fully implemented.
    @Disabled("Variables not implemented yet")
    @Test
    fun shouldPrintTruthTable() {
        val source = listOf(
            "DIM F AS INTEGER, T AS INTEGER",
            "LET F = 0 : LET T = NOT(F)",
            "PRINT \"T AND T = \"; T AND T",
            "PRINT \"T AND F = \"; T AND F",
            "PRINT \"F AND T = \"; F AND T",
            "PRINT \"F AND F = \"; F AND F",
            "PRINT \"T EQV T = \"; T EQV T",
            "PRINT \"T EQV F = \"; T EQV F",
            "PRINT \"F EQV T = \"; F EQV T",
            "PRINT \"F EQV F = \"; F EQV F",
            "PRINT \"T IMP T = \"; T IMP T",
            "PRINT \"T IMP F = \"; T IMP F",
            "PRINT \"F IMP T = \"; F IMP T",
            "PRINT \"F IMP F = \"; F IMP F",
            "PRINT \"T OR T  = \"; T OR T",
            "PRINT \"T OR F  = \"; T OR F",
            "PRINT \"F OR T  = \"; F OR T",
            "PRINT \"F OR F  = \"; F OR F",
            "PRINT \"T XOR T = \"; T XOR T",
            "PRINT \"T XOR F = \"; T XOR F",
            "PRINT \"F XOR T = \"; F XOR T",
            "PRINT \"F XOR F = \"; F XOR F",
            "PRINT \"NOT F   = \"; NOT F",
            "PRINT \"NOT T   = \"; NOT T"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileLlvmAndAssertSuccess(sourceFile)
        runLlvmAndAssertSuccess(
            listOf(),
            listOf(
                "T AND T = -1",
                "T AND F = 0",
                "F AND T = 0",
                "F AND F = 0",
                "T EQV T = -1",
                "T EQV F = 0",
                "F EQV T = 0",
                "F EQV F = -1",
                "T IMP T = -1",
                "T IMP F = 0",
                "F IMP T = -1",
                "F IMP F = -1",
                "T OR T  = -1",
                "T OR F  = -1",
                "F OR T  = -1",
                "F OR F  = 0",
                "T XOR T = 0",
                "T XOR F = -1",
                "F XOR T = -1",
                "F XOR F = 0",
                "NOT F   = -1",
                "NOT T   = 0"
            )
        )
    }

    @Test
    fun shouldPrintRelationalIntExpressions() {
        val source = listOf(
            "PRINT 5 = 7",
            "PRINT 5 <> 7",
            "PRINT 5 < 7",
            "PRINT 5 <= 7",
            "PRINT 5 > 7",
            "PRINT 5 >= 7",
        )
        val sourcePath = createSourceFile(source, BASIC)
        compileLlvmAndAssertSuccess(sourcePath)
        runLlvmAndAssertSuccess(
            listOf(),
            listOf(
                "0",
                "-1",
                "-1",
                "-1",
                "0",
                "0",
            ),
        )
    }

    @Test
    fun shouldPrintArithmeticFloatExpressions() {
        val source = listOf(
            "PRINT 8.1 + 7.9",
            "PRINT 8.1 - 7.9",
            "PRINT 8.0 * 7.0",
            "PRINT 8.0 / 16.0",
            "PRINT 8.0 MOD 7.0",
            "PRINT 8.0 ^ 2.0",
            "PRINT -(8.0 * 7.0)",
        )
        val sourcePath = createSourceFile(source, BASIC)
        compileLlvmAndAssertSuccess(sourcePath)
        runLlvmAndAssertSuccess(
            listOf(),
            listOf(
                "16.000000",
                "0.200000",
                "56.000000",
                "0.500000",
                "1.000000",
                "64.000000",
                "-56.000000",
            ),
        )
    }

    @Test
    fun shouldPrintRelationalFloatExpressions() {
        val source = listOf(
            "PRINT 5.0 = 7.0",
            "PRINT 5.0 <> 7.0",
            "PRINT 5.0 < 7.0",
            "PRINT 5.0 <= 7.0",
            "PRINT 5.0 > 7.0",
            "PRINT 5.0 >= 7.0",
        )
        val sourcePath = createSourceFile(source, BASIC)
        compileLlvmAndAssertSuccess(sourcePath)
        runLlvmAndAssertSuccess(
            listOf(),
            listOf(
                "0",
                "-1",
                "-1",
                "-1",
                "0",
                "0",
            ),
        )
    }

    @Test
    fun shouldCallLlvmIntrinsicFunctions() {
        val source = listOf(
            // Rounding
            "PRINT cdbl(5)",
            "PRINT cdbl(5.9)",
            "PRINT cint(5.1)",
            "PRINT cint(5.9)",
            "PRINT cint(4)",
            // TODO: Test functions fix and int when they have been rewritten to f64->f64.
            // Math
            "PRINT abs(-5)",
            "PRINT abs(-3.3)",
            "PRINT atn(1.0)",
            "PRINT cos(0.0)",
            "PRINT exp(0.0)",
            "PRINT log(2.71828183)",
            "PRINT sin(0.0)",
            "PRINT sqr(4.0)",
            "PRINT tan(0.78539816)",
        )
        val sourcePath = createSourceFile(source, BASIC)
        compileLlvmAndAssertSuccess(sourcePath)
        runLlvmAndAssertSuccess(
            listOf(),
            listOf(
                // Rounding
                "5.000000",
                "5.900000",
                "5",
                "6",
                "4",
                // Math
                "5",
                "3.300000",
                "0.785398",
                "1.000000",
                "1.000000",
                "1.000000",
                "0.000000",
                "2.000000",
                "1.000000",
            ),
        )
    }

    @Test
    fun shouldCallLibcIntrinsicFunctions() {
        val source = listOf(
            "PRINT len(\"\")",
            "PRINT len(\"hello\")",
            "PRINT val(\"7\")",
            "PRINT val(\"3.14\")",
        )
        val sourcePath = createSourceFile(source, BASIC)
        compileLlvmAndAssertSuccess(sourcePath)
        runLlvmAndAssertSuccess(
            listOf(),
            listOf(
                "0",
                "5",
                "7.000000",
                "3.140000",
            ),
        )
    }

    @Test
    fun shouldPrintUndefinedVariables() {
        val source = listOf(
            "PRINT a%",
            "PRINT f#",
            "PRINT s$",
        )
        val sourcePath = createSourceFile(source, BASIC)
        compileLlvmAndAssertSuccess(sourcePath)
        runLlvmAndAssertSuccess(
            listOf(),
            listOf(
                "0",
                "0.000000",
                "",
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
        compileLlvmAndAssertSuccess(sourcePath)
        runLlvmAndAssertSuccess(
            listOf(),
            listOf(
                "10",
                "40",
            ),
        )
    }
}
