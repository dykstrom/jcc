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

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import se.dykstrom.jcc.common.ast.AssignStatement
import se.dykstrom.jcc.common.ast.BooleanLiteral.TRUE
import se.dykstrom.jcc.common.ast.ForStatement
import se.dykstrom.jcc.common.ast.LoopExitStatement
import se.dykstrom.jcc.common.ast.WhileStatement
import se.dykstrom.jcc.common.symbols.SymbolTable
import se.dykstrom.jcc.llvm.LlvmTests.Companion.IDENT_I64_I
import se.dykstrom.jcc.llvm.LlvmTests.Companion.IDENT_I64_J
import se.dykstrom.jcc.llvm.LlvmTests.Companion.IL_1
import se.dykstrom.jcc.llvm.LlvmTests.Companion.IL_5
import se.dykstrom.jcc.llvm.LlvmTests.Companion.INE_I64_I
import se.dykstrom.jcc.llvm.LlvmTests.Companion.INE_I64_J
import se.dykstrom.jcc.llvm.LlvmTests.Companion.assertContainsInOrder
import se.dykstrom.jcc.llvm.LlvmTests.Companion.generate
import se.dykstrom.jcc.llvm.code.TestLlvmCodeGenerator

class LoopExitCodeGeneratorTests {

    private val symbolTable = SymbolTable()
    private val codeGenerator = TestLlvmCodeGenerator(symbolTable)

    @Test
    fun shouldExitFor() {
        symbolTable.addGlobal(IDENT_I64_I, "0")

        // FOR labels: L0 = init, L1 = test, L2 = body, L3 = increment, L4 = after
        val result = generate(codeGenerator, ForStatement(INE_I64_I, IL_1, IL_5, null, listOf(EXIT_FOR)))

        assertContainsInOrder(result, listOf(
            "L2:",
            "; EXIT ForStatement",
            "br label %L4",
            "Suppress branch to L3",
            "L3:",
        ))
    }

    @Test
    fun shouldExitWhile() {
        // WHILE labels: L0 = before, L1 = inside, L2 = after
        val result = generate(codeGenerator, WhileStatement(TRUE, listOf(EXIT_WHILE)))

        assertContainsInOrder(result, listOf(
            "L1:",
            "; EXIT WhileStatement",
            "br label %L2",
            "Suppress branch to L0",
            "L2:",
        ))
    }

    @Test
    fun shouldExitForFromWhileInFor() {
        symbolTable.addGlobal(IDENT_I64_I, "0")

        // FOR labels: L0-L4, WHILE labels: L5 = before, L6 = inside, L7 = after
        val result = generate(codeGenerator, ForStatement(INE_I64_I, IL_1, IL_5, null, listOf(WhileStatement(TRUE, listOf(EXIT_FOR)))))

        assertContainsInOrder(result, listOf(
            "L6:",
            "; EXIT ForStatement",
            "br label %L4",
            "L7:",
        ))
    }

    @Test
    fun shouldExitInnerForOfNestedPair() {
        symbolTable.addGlobal(IDENT_I64_I, "0")
        symbolTable.addGlobal(IDENT_I64_J, "0")

        // Outer FOR labels: L0-L4, inner FOR labels: L5-L9
        val inner = ForStatement(INE_I64_J, IL_1, IL_5, null, listOf(EXIT_FOR))
        val result = generate(codeGenerator, ForStatement(INE_I64_I, IL_1, IL_5, null, listOf(inner)))

        assertContainsInOrder(result, listOf(
            "L7:",
            "; EXIT ForStatement",
            "br label %L9",
            "L9:",
            "br label %L3",
            "L4:",
        ))
    }

    @Test
    fun shouldStartNewBlockAfterExit() {
        symbolTable.addGlobal(IDENT_I64_I, "0")
        symbolTable.addGlobal(IDENT_I64_J, "0")

        val body = listOf(EXIT_FOR, AssignStatement(INE_I64_J, IL_1))
        val result = generate(codeGenerator, ForStatement(INE_I64_I, IL_1, IL_5, null, body))

        assertContainsInOrder(result, listOf(
            "br label %L4",
            "L5:",
            "store i64 1, ptr @_j",
            "br label %L3",
            "L3:",
        ))
    }

    @Test
    fun shouldRejectExitOutsideLoop() {
        assertThrows<IllegalStateException> { generate(codeGenerator, EXIT_FOR) }
    }

    @Test
    fun shouldRejectExitAfterLoop() {
        symbolTable.addGlobal(IDENT_I64_I, "0")
        val loop = ForStatement(INE_I64_I, IL_1, IL_5, null, listOf())
        assertThrows<IllegalStateException> { generate(codeGenerator, loop, EXIT_FOR) }
    }

    @Test
    fun shouldRejectExitOutsideLoopOfKind() {
        assertThrows<IllegalStateException> { generate(codeGenerator, WhileStatement(TRUE, listOf(EXIT_FOR))) }
    }

    companion object {
        private val EXIT_FOR = LoopExitStatement(ForStatement::class.java)
        private val EXIT_WHILE = LoopExitStatement(WhileStatement::class.java)
    }
}
