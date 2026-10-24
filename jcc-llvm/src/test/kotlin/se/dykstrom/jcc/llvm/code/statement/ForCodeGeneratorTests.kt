/*
 * Copyright (C) 2026 Johan Dykstrom
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

package se.dykstrom.jcc.llvm.code.statement

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import se.dykstrom.jcc.common.ast.*
import se.dykstrom.jcc.common.symbols.Scope.NONE
import se.dykstrom.jcc.common.symbols.SymbolTable
import se.dykstrom.jcc.common.types.F64
import se.dykstrom.jcc.llvm.LlvmTests.Companion.FL_0_0
import se.dykstrom.jcc.llvm.LlvmTests.Companion.assertContainsInOrder
import se.dykstrom.jcc.llvm.LlvmTests.Companion.generate
import se.dykstrom.jcc.llvm.LlvmTests.Companion.countOccurrences
import se.dykstrom.jcc.llvm.LlvmTests.Companion.FL_1_0
import se.dykstrom.jcc.llvm.LlvmTests.Companion.IDENT_F64_X
import se.dykstrom.jcc.llvm.LlvmTests.Companion.IDENT_F64_Y
import se.dykstrom.jcc.llvm.LlvmTests.Companion.IDENT_I32_K
import se.dykstrom.jcc.llvm.LlvmTests.Companion.IDENT_I64_I
import se.dykstrom.jcc.llvm.LlvmTests.Companion.IDENT_I64_J
import se.dykstrom.jcc.llvm.LlvmTests.Companion.IDENT_I64_M
import se.dykstrom.jcc.llvm.LlvmTests.Companion.IDENT_I64_N
import se.dykstrom.jcc.llvm.LlvmTests.Companion.IDENT_I64_S
import se.dykstrom.jcc.llvm.LlvmTests.Companion.IDE_F64_Y
import se.dykstrom.jcc.llvm.LlvmTests.Companion.IDE_I64_M
import se.dykstrom.jcc.llvm.LlvmTests.Companion.IDE_I64_N
import se.dykstrom.jcc.llvm.LlvmTests.Companion.IDE_I64_S
import se.dykstrom.jcc.llvm.LlvmTests.Companion.IL_1
import se.dykstrom.jcc.llvm.LlvmTests.Companion.IL_2
import se.dykstrom.jcc.llvm.LlvmTests.Companion.IL_5
import se.dykstrom.jcc.llvm.LlvmTests.Companion.IL_I32_0
import se.dykstrom.jcc.llvm.LlvmTests.Companion.IL_I32_3
import se.dykstrom.jcc.llvm.LlvmTests.Companion.IL_M1
import se.dykstrom.jcc.llvm.LlvmTests.Companion.INE_F64_X
import se.dykstrom.jcc.llvm.LlvmTests.Companion.INE_I32_K
import se.dykstrom.jcc.llvm.LlvmTests.Companion.INE_I64_I
import se.dykstrom.jcc.llvm.LlvmTests.Companion.INE_I64_J
import se.dykstrom.jcc.llvm.code.Line
import se.dykstrom.jcc.llvm.code.LoopStack
import se.dykstrom.jcc.llvm.code.TestLlvmCodeGenerator

class ForCodeGeneratorTests {

    private val symbolTable = SymbolTable()
    private val codeGenerator = TestLlvmCodeGenerator(symbolTable)

    @Test
    fun shouldGenerateLoopWithConstantPositiveStep() {
        symbolTable.addGlobal(IDENT_I64_I, "0")

        val result = generate(codeGenerator, ForStatement(INE_I64_I, IL_1, IL_5, IL_2, listOf()))

        assertContainsInOrder(result, listOf(
            "L0:",
            "store i64 5, ptr %_.for.L0.end",
            "store i64 2, ptr %_.for.L0.step",
            "store i64 1, ptr @_i",
            "br label %L1",
            "L1:",
            "%0 = load i64, ptr @_i",
            "%1 = load i64, ptr %_.for.L0.end",
            "%2 = icmp sle i64 %0, %1",
            "br i1 %2, label %L2, label %L4",
            "L2:",
            "br label %L3",
            "L3:",
            "%3 = load i64, ptr @_i",
            "%4 = load i64, ptr %_.for.L0.step",
            "%5 = add i64 %3, %4",
            "store i64 %5, ptr @_i",
            "br label %L1",
            "L4:",
        ))
        assertFalse(result.contains("slt"))
    }

    @Test
    fun shouldGenerateLoopWithConstantNegativeStep() {
        symbolTable.addGlobal(IDENT_I64_I, "0")

        val result = generate(codeGenerator, ForStatement(INE_I64_I, IL_5, IL_1, IL_M1, listOf()))

        assertContainsInOrder(result, listOf(
            "store i64 -1, ptr %_.for.L0.step",
            "%2 = icmp sge i64 %0, %1",
            "br i1 %2, label %L2, label %L4",
        ))
        assertFalse(result.contains("slt"))
    }

    @Test
    fun shouldUseStepOneIfNoStep() {
        symbolTable.addGlobal(IDENT_I64_I, "0")

        val result = generate(codeGenerator, ForStatement(INE_I64_I, IL_1, IL_5, null, listOf()))

        assertContainsInOrder(result, listOf(
            "store i64 1, ptr %_.for.L0.step",
            "%2 = icmp sle i64 %0, %1",
        ))
    }

    @Test
    fun shouldTestSignOfNonConstantStep() {
        symbolTable.addGlobal(IDENT_I64_I, "0")
        symbolTable.addGlobal(IDENT_I64_S, "0")

        val result = generate(codeGenerator, ForStatement(INE_I64_I, IL_1, IL_5, IDE_I64_S, listOf()))

        assertContainsInOrder(result, listOf(
            "%0 = load i64, ptr @_s",
            "store i64 %0, ptr %_.for.L0.step",
            "L1:",
            "%1 = load i64, ptr @_i",
            "%2 = load i64, ptr %_.for.L0.end",
            "%3 = load i64, ptr %_.for.L0.step",
            "%4 = icmp slt i64 %3, 0",
            "br i1 %4, label %L6, label %L5",
            "L5:",
            "%5 = icmp sle i64 %1, %2",
            "br i1 %5, label %L2, label %L4",
            "L6:",
            "%6 = icmp sge i64 %1, %2",
            "br i1 %6, label %L2, label %L4",
            "L2:",
        ))
    }

    @Test
    fun shouldEvaluateStartEndAndStepOnceBeforeAssigningCounter() {
        symbolTable.addGlobal(IDENT_I64_I, "0")
        symbolTable.addGlobal(IDENT_I64_M, "0")
        symbolTable.addGlobal(IDENT_I64_N, "0")
        symbolTable.addGlobal(IDENT_I64_S, "0")

        val result = generate(codeGenerator, ForStatement(INE_I64_I, IDE_I64_M, IDE_I64_N, IDE_I64_S, listOf()))

        assertContainsInOrder(result, listOf(
            "L0:",
            "%0 = load i64, ptr @_m",
            "%1 = load i64, ptr @_n",
            "store i64 %1, ptr %_.for.L0.end",
            "%2 = load i64, ptr @_s",
            "store i64 %2, ptr %_.for.L0.step",
            "store i64 %0, ptr @_i",
            "L1:",
        ))
        assertEquals(1, countOccurrences(result, "ptr @_m"))
        assertEquals(1, countOccurrences(result, "ptr @_n"))
        assertEquals(1, countOccurrences(result, "ptr @_s"))
    }

    @Test
    fun shouldAllocateSlotsInEntryBlock() {
        symbolTable.addGlobal(IDENT_I64_I, "0")

        val result = generate(codeGenerator, ForStatement(INE_I64_I, IL_1, IL_5, null, listOf()))

        assertContainsInOrder(result, listOf(
            "entry:",
            "%_.for.L0.end = alloca i64",
            "%_.for.L0.step = alloca i64",
            "br label %L0",
            "L0:",
        ))
    }

    @Test
    fun shouldUseDistinctSlotsForNestedLoops() {
        symbolTable.addGlobal(IDENT_I64_I, "0")
        symbolTable.addGlobal(IDENT_I64_J, "0")
        val inner = ForStatement(INE_I64_J, IL_1, IL_2, null, listOf())

        val result = generate(codeGenerator, ForStatement(INE_I64_I, IL_1, IL_5, null, listOf(inner)))

        assertContainsInOrder(result, listOf(
            "%_.for.L0.end = alloca i64",
            "%_.for.L0.step = alloca i64",
            "%_.for.L5.end = alloca i64",
            "%_.for.L5.step = alloca i64",
            "store i64 1, ptr @_i",
            "store i64 1, ptr @_j",
        ))
    }

    @Test
    fun shouldEndBodyThatEndsWithBranchWithoutExtraBranch() {
        symbolTable.addGlobal(IDENT_I64_I, "0")
        val body = listOf(GotoStatement("out"))

        val result = generate(codeGenerator, 
            ForStatement(INE_I64_I, IL_1, IL_5, null, body),
            LabelledStatement("out", CommentStatement(0, 0, "out"))
        )

        assertContainsInOrder(result, listOf(
            "L2:",
            "br label %.out",
            "Suppress branch to L3",
            "L3:",
        ))
    }

    @Test
    fun shouldGenerateLoopWithFloatCounter() {
        symbolTable.addGlobal(IDENT_F64_X, "0.0")

        val result = generate(codeGenerator, ForStatement(INE_F64_X, FL_0_0, FL_1_0, null, listOf()))

        assertContainsInOrder(result, listOf(
            "store double 1.0, ptr %_.for.L0.end",
            "store double 1.0, ptr %_.for.L0.step",
            "store double 0.0, ptr @_x",
            "%2 = fcmp ole double %0, %1",
            "%5 = fadd double %3, %4",
        ))
    }

    @Test
    fun shouldTestSignOfNonConstantFloatStep() {
        symbolTable.addGlobal(IDENT_F64_X, "0.0")
        symbolTable.addGlobal(IDENT_F64_Y, "0.0")

        val result = generate(codeGenerator, ForStatement(INE_F64_X, FL_0_0, FL_1_0, IDE_F64_Y, listOf()))

        assertContainsInOrder(result, listOf(
            "%4 = fcmp olt double %3, 0.0",
            "%5 = fcmp ole double %1, %2",
            "%6 = fcmp oge double %1, %2",
        ))
    }

    @Test
    fun shouldFoldConstantStepBeforeChoosingDirection() {
        symbolTable.addGlobal(IDENT_F64_X, "0.0")
        val step = CastToFloatExpression(IL_M1, F64.INSTANCE)

        val result = generate(codeGenerator, ForStatement(INE_F64_X, FL_1_0, FL_0_0, step, listOf()))

        assertTrue(result.contains("fcmp oge double"))
        assertFalse(result.contains("fcmp olt"))
    }

    @Test
    fun shouldGenerateLoopWithI32Counter() {
        symbolTable.addGlobal(IDENT_I32_K, "0")

        val result = generate(codeGenerator, ForStatement(INE_I32_K, IL_I32_0, IL_I32_3, null, listOf()))

        assertContainsInOrder(result, listOf(
            "store i32 1, ptr %_.for.L0.step",
            "%2 = icmp sle i32 %0, %1",
            "%5 = add i32 %3, %4",
        ))
    }

    @Test
    fun shouldGenerateExclusiveLoops() {
        symbolTable.addGlobal(IDENT_I64_I, "0")
        symbolTable.addGlobal(IDENT_I64_S, "0")
        symbolTable.addGlobal(IDENT_F64_X, "0.0")

        val result = generate(codeGenerator, 
            ForStatement(0, 0, INE_I64_I, IL_1, IL_5, IDE_I64_S, false, listOf()),
            ForStatement(0, 0, INE_F64_X, FL_0_0, FL_1_0, null, false, listOf()),
        )

        assertContainsInOrder(result, listOf(
            "icmp slt i64 %1, %2",
            "icmp sgt i64 %1, %2",
            "fcmp olt double",
        ))
    }

    @Test
    fun shouldRejectUnknownCounter() {
        val statement = ForStatement(INE_I64_I, IL_1, IL_5, null, listOf())
        assertThrows<IllegalStateException> { generate(codeGenerator, statement) }
    }

    @Test
    fun shouldRejectLoopOutsideFunction() {
        symbolTable.addGlobal(IDENT_I64_I, "0")
        val statement = ForStatement(INE_I64_I, IL_1, IL_5, null, listOf())
        assertThrows<IllegalStateException> {
            ForCodeGenerator(codeGenerator, LoopStack(), NONE).toLlvm(statement, mutableListOf<Line>(), symbolTable)
        }
    }
}
