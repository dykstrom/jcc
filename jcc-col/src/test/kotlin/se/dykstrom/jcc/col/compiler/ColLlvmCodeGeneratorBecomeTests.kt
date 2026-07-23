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

package se.dykstrom.jcc.col.compiler

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import se.dykstrom.jcc.col.ColTests.Companion.IDE_I64_A
import se.dykstrom.jcc.col.ColTests.Companion.IDE_I64_B
import se.dykstrom.jcc.col.ast.expression.BecomeExpression
import se.dykstrom.jcc.common.ast.*
import se.dykstrom.jcc.common.ast.IntegerLiteral.ONE
import se.dykstrom.jcc.common.ast.IntegerLiteral.ZERO
import se.dykstrom.jcc.common.code.TargetProgram
import se.dykstrom.jcc.common.functions.UserDefinedFunction
import se.dykstrom.jcc.common.types.Fun
import se.dykstrom.jcc.common.types.I64
import se.dykstrom.jcc.common.types.Identifier

internal class ColLlvmCodeGeneratorBecomeTests : AbstractColCodeGeneratorTests() {

    private val cg = ColLlvmCodeGenerator(typeManager, symbols, optimizer)

    @Test
    fun shouldGenerateMusttailCallImmediatelyFollowedByRet() {
        // Given: fun count(a, b) -> i64 := if a <= 0 then b else become count(a - 1, b + 1)
        val identifier = Identifier("count", Fun.from(listOf(I64.INSTANCE, I64.INSTANCE), I64.INSTANCE))
        val udf = UserDefinedFunction("count", listOf("a", "b"), listOf(I64.INSTANCE, I64.INSTANCE), I64.INSTANCE)
        val declarations = listOf(Declaration(0, 0, "a", I64.INSTANCE), Declaration(0, 0, "b", I64.INSTANCE))
        val tailCall = FunctionCallExpression(
            identifier,
            listOf(SubExpression(IDE_I64_A, ONE), AddExpression(IDE_I64_B, ONE)),
            udf
        )
        val body = IfExpression(LessOrEqualExpression(IDE_I64_A, ZERO), IDE_I64_B, BecomeExpression(tailCall))
        val fds = FunctionDefinitionStatement(0, 0, identifier, declarations, body)

        // When
        val result = assembleProgram(cg, listOf(fds))

        // Then
        assertContains(result, listOf(
            "define tailcc i64 @count_I64_I64(i64 %0, i64 %1)",
            "musttail call tailcc i64 @count_I64_I64",
        ))
        assertMusttailFollowedByRet(result)
        // The non-become branch returns its value directly, the become branch returns the tail call
        assertEquals(2, result.lines().map { it.toText() }.count { it.trim().startsWith("ret i64") })
    }

    @Test
    fun shouldGenerateNoMergeBlockWhenBothBranchesBecome() {
        // Given: fun choose(a) -> i64 := if a <= 0 then become choose(a - 1) else become choose(a + 1)
        val identifier = Identifier("choose", Fun.from(listOf(I64.INSTANCE), I64.INSTANCE))
        val udf = UserDefinedFunction("choose", listOf("a"), listOf(I64.INSTANCE), I64.INSTANCE)
        val declarations = listOf(Declaration(0, 0, "a", I64.INSTANCE))
        val thenCall = FunctionCallExpression(identifier, listOf(SubExpression(IDE_I64_A, ONE)), udf)
        val elseCall = FunctionCallExpression(identifier, listOf(AddExpression(IDE_I64_A, ONE)), udf)
        val body = IfExpression(
            LessOrEqualExpression(IDE_I64_A, ZERO),
            BecomeExpression(thenCall),
            BecomeExpression(elseCall)
        )
        val fds = FunctionDefinitionStatement(0, 0, identifier, declarations, body)

        // When
        val result = assembleProgram(cg, listOf(fds))

        // Then: both branches terminate with their own musttail + ret, so there is no phi/merge block
        val text = result.toText()
        assertTrue(!text.contains("phi"), "expected no phi when both branches become")
        assertEquals(2, result.lines().map { it.toText() }.count { it.contains("musttail call tailcc i64 @choose_I64") })
        assertMusttailFollowedByRet(result)
    }

    /** Every musttail call must be immediately followed by a ret of its result, as LLVM requires. */
    private fun assertMusttailFollowedByRet(program: TargetProgram) {
        val lines = program.lines().map { it.toText() }
        lines.forEachIndexed { index, line ->
            if (line.contains("musttail call")) {
                assertTrue(
                    // There is at least one more line, and the next line starts with "ret"
                    index + 1 < lines.size && lines[index + 1].trim().startsWith("ret "),
                    "musttail call must be immediately followed by ret, but next line was: " + (lines.getOrNull(index + 1) ?: "<none>")
                )
            }
        }
    }
}
