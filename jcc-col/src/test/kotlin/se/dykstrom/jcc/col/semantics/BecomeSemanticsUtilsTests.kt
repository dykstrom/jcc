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

package se.dykstrom.jcc.col.semantics

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import se.dykstrom.jcc.col.ColTests.Companion.IL_5
import se.dykstrom.jcc.col.ast.expression.BecomeExpression
import se.dykstrom.jcc.col.ast.statement.FunCallStatement
import se.dykstrom.jcc.col.ast.statement.ValDeclarationStatement
import se.dykstrom.jcc.common.ast.AddExpression
import se.dykstrom.jcc.common.ast.DeclarationAssignment
import se.dykstrom.jcc.common.ast.Expression
import se.dykstrom.jcc.common.ast.FunctionCallExpression
import se.dykstrom.jcc.common.ast.FunctionDefinitionStatement
import se.dykstrom.jcc.common.ast.Node
import se.dykstrom.jcc.common.ast.Statement
import se.dykstrom.jcc.common.types.Fun
import se.dykstrom.jcc.common.types.I64
import se.dykstrom.jcc.common.types.Identifier

class BecomeSemanticsUtilsTests {

    private val reportedNodes = mutableListOf<Node>()
    private val reportedMessages = mutableListOf<String>()

    private fun check(vararg statements: Statement) =
        BecomeSemanticsUtils.checkNoTopLevelBecome(statements.toList()) { node, msg ->
            reportedNodes.add(node)
            reportedMessages.add(msg)
        }

    private fun become(): BecomeExpression {
        val identifier = Identifier("f", Fun.from(listOf(I64.INSTANCE), I64.INSTANCE))
        return BecomeExpression(FunctionCallExpression(identifier, listOf(IL_5)))
    }

    private fun valOf(expression: Expression?): ValDeclarationStatement =
        ValDeclarationStatement(DeclarationAssignment("x", I64.INSTANCE, expression))

    private fun callOf(vararg args: Expression): FunCallStatement =
        FunCallStatement(FunctionCallExpression(Identifier("println", Fun.from(listOf(I64.INSTANCE), I64.INSTANCE)), args.toList()))

    @Test
    fun shouldReportBecomeInValInitializer() {
        val become = become()
        check(valOf(become))
        assertEquals(1, reportedNodes.size)
        assertSame(become, reportedNodes[0])
        assertTrue(reportedMessages[0].contains("become is only allowed inside a function body"))
    }

    @Test
    fun shouldReportBecomeInCallArgument() {
        val become = become()
        check(callOf(become))
        assertEquals(1, reportedNodes.size)
        assertSame(become, reportedNodes[0])
    }

    @Test
    fun shouldReportBecomeNestedInExpression() {
        // become buried inside a larger expression is still found
        val become = become()
        check(valOf(AddExpression(IL_5, become)))
        assertEquals(1, reportedNodes.size)
        assertSame(become, reportedNodes[0])
    }

    @Test
    fun shouldReportEveryTopLevelBecome() {
        check(valOf(become()), callOf(become()))
        assertEquals(2, reportedNodes.size)
    }

    @Test
    fun shouldNotReportWhenNoBecomePresent() {
        check(valOf(IL_5), callOf(IL_5))
        assertTrue(reportedNodes.isEmpty())
    }

    @Test
    fun shouldNotReportForValWithoutInitializer() {
        // A val with no initializer must not cause a NullPointerException
        check(valOf(null))
        assertTrue(reportedNodes.isEmpty())
    }

    @Test
    fun shouldIgnoreBecomeInFunctionDefinitionBody() {
        // become inside a function body is legal and is checked elsewhere, not by this method
        val identifier = Identifier("g", Fun.from(listOf(), I64.INSTANCE))
        val fds = FunctionDefinitionStatement(0, 0, identifier, listOf(), become())
        check(fds)
        assertTrue(reportedNodes.isEmpty())
    }
}
