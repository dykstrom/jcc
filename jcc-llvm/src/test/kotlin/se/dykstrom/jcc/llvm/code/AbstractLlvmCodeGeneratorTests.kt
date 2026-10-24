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

package se.dykstrom.jcc.llvm.code

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import se.dykstrom.jcc.common.ast.AssignStatement
import se.dykstrom.jcc.common.ast.CommentStatement
import se.dykstrom.jcc.common.ast.GotoStatement
import se.dykstrom.jcc.common.ast.LabelledStatement
import se.dykstrom.jcc.common.symbols.SymbolTable
import se.dykstrom.jcc.llvm.LlvmTests.Companion.IDENT_I64_J
import se.dykstrom.jcc.llvm.LlvmTests.Companion.IL_1
import se.dykstrom.jcc.llvm.LlvmTests.Companion.INE_I64_J
import se.dykstrom.jcc.llvm.LlvmTests.Companion.assertContainsInOrder
import se.dykstrom.jcc.llvm.LlvmTests.Companion.generate

class AbstractLlvmCodeGeneratorTests {

    private val symbolTable = SymbolTable()
    private val codeGenerator = TestLlvmCodeGenerator(symbolTable)

    @Test
    fun shouldStartNewBlockForStatementAfterJump() {
        symbolTable.addGlobal(IDENT_I64_J, "0")

        val result = generate(codeGenerator, GotoStatement("out"), AssignStatement(INE_I64_J, IL_1), LABELLED_OUT)

        assertContainsInOrder(result, listOf(
            "br label %.out",
            "L0:",
            "store i64 1, ptr @_j",
            "br label %.out",
            ".out:",
        ))
    }

    @Test
    fun shouldNotStartNewBlockForLabelledStatementAfterJump() {
        val result = generate(codeGenerator, GotoStatement("out"), LABELLED_OUT)

        assertContainsInOrder(result, listOf(
            "br label %.out",
            "Suppress branch to out",
            ".out:",
        ))
        assertFalse(result.contains("L0:"))
    }

    @Test
    fun shouldStartNewBlockForReturnAfterJump() {
        val result = generate(codeGenerator, LABELLED_OUT, GotoStatement("out"))

        assertContainsInOrder(result, listOf(
            ".out:",
            "br label %.out",
            "L0:",
            "ret i32 0",
        ))
    }

    companion object {
        private val LABELLED_OUT = LabelledStatement("out", CommentStatement(0, 0, "out"))
    }
}
