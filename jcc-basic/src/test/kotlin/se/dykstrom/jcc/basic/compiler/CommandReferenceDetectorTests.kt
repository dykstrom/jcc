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

package se.dykstrom.jcc.basic.compiler

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import se.dykstrom.jcc.basic.BasicTests.Companion.IL_5
import se.dykstrom.jcc.basic.BasicTests.Companion.INE_STR_S
import se.dykstrom.jcc.basic.BasicTests.Companion.SL_FOO
import se.dykstrom.jcc.basic.ast.statement.PrintStatement
import se.dykstrom.jcc.basic.ast.statement.RandomizeStatement
import se.dykstrom.jcc.basic.compiler.BasicSymbols.BF_COMMAND
import se.dykstrom.jcc.basic.compiler.CommandReferenceDetector.referencesCommand
import se.dykstrom.jcc.common.ast.AddExpression
import se.dykstrom.jcc.common.ast.ArrayAccessExpression
import se.dykstrom.jcc.common.ast.AssignStatement
import se.dykstrom.jcc.common.ast.Declaration
import se.dykstrom.jcc.common.ast.Expression
import se.dykstrom.jcc.common.ast.FunctionCallExpression
import se.dykstrom.jcc.common.ast.FunctionDefinitionStatement
import se.dykstrom.jcc.common.ast.IfStatement
import se.dykstrom.jcc.common.ast.LabelledStatement
import se.dykstrom.jcc.common.ast.NegateExpression
import se.dykstrom.jcc.common.ast.Statement
import se.dykstrom.jcc.common.ast.WhileStatement
import se.dykstrom.jcc.common.types.Arr
import se.dykstrom.jcc.common.types.I64
import se.dykstrom.jcc.common.types.Identifier
import se.dykstrom.jcc.common.types.Str

internal class CommandReferenceDetectorTests {

    // A call to the command$ built-in, and a call to some other function for the negative cases
    private val commandCall = FunctionCallExpression(BF_COMMAND.identifier, listOf<Expression>())
    private val otherCall = FunctionCallExpression(Identifier("other$", Str.INSTANCE), listOf<Expression>())

    @Test
    fun emptyProgramDoesNotReferenceCommand() {
        assertFalse(referencesCommand(listOf<Statement>()))
    }

    @Test
    fun printLiteralDoesNotReferenceCommand() {
        assertFalse(referencesCommand(listOf(PrintStatement(listOf(IL_5)))))
    }

    @Test
    fun printCommandReferencesCommand() {
        assertTrue(referencesCommand(listOf(PrintStatement(listOf(commandCall)))))
    }

    @Test
    fun assignCommandToRhsReferencesCommand() {
        assertTrue(referencesCommand(listOf(AssignStatement(INE_STR_S, commandCall))))
    }

    @Test
    fun commandInBinaryExpressionReferencesCommand() {
        assertTrue(referencesCommand(listOf(PrintStatement(listOf(AddExpression(SL_FOO, commandCall))))))
    }

    @Test
    fun commandInUnaryExpressionReferencesCommand() {
        assertTrue(referencesCommand(listOf(PrintStatement(listOf(NegateExpression(commandCall))))))
    }

    @Test
    fun commandAsFunctionArgumentReferencesCommand() {
        val outer = FunctionCallExpression(Identifier("len", Str.INSTANCE), listOf<Expression>(commandCall))
        assertTrue(referencesCommand(listOf(PrintStatement(listOf(outer)))))
    }

    @Test
    fun commandInArraySubscriptReferencesCommand() {
        val arrayIdentifier = Identifier("a%", Arr.from(1, I64.INSTANCE))
        val access = ArrayAccessExpression(0, 0, arrayIdentifier, listOf<Expression>(commandCall))
        assertTrue(referencesCommand(listOf(PrintStatement(listOf(access)))))
    }

    @Test
    fun commandInIfConditionReferencesCommand() {
        val statement = IfStatement.builder(commandCall, PrintStatement(listOf(IL_5))).build()
        assertTrue(referencesCommand(listOf(statement)))
    }

    @Test
    fun commandInThenBranchReferencesCommand() {
        val statement = IfStatement.builder(IL_5, PrintStatement(listOf(commandCall))).build()
        assertTrue(referencesCommand(listOf(statement)))
    }

    @Test
    fun commandInElseBranchReferencesCommand() {
        val statement = IfStatement.builder(IL_5, PrintStatement(listOf(IL_5)))
            .elseStatements(listOf(PrintStatement(listOf(commandCall))))
            .build()
        assertTrue(referencesCommand(listOf(statement)))
    }

    @Test
    fun ifWithoutCommandDoesNotReferenceCommand() {
        val statement = IfStatement.builder(IL_5, PrintStatement(listOf(otherCall))).build()
        assertFalse(referencesCommand(listOf(statement)))
    }

    @Test
    fun commandInWhileConditionReferencesCommand() {
        assertTrue(referencesCommand(listOf(WhileStatement(commandCall, listOf()))))
    }

    @Test
    fun commandInWhileBodyReferencesCommand() {
        assertTrue(referencesCommand(listOf(WhileStatement(IL_5, listOf(PrintStatement(listOf(commandCall)))))))
    }

    @Test
    fun commandInLabelledStatementReferencesCommand() {
        assertTrue(referencesCommand(listOf(LabelledStatement("line10", PrintStatement(listOf(commandCall))))))
    }

    @Test
    fun commandInRandomizeReferencesCommand() {
        assertTrue(referencesCommand(listOf(RandomizeStatement(0, 0, commandCall))))
    }

    @Test
    fun randomizeWithoutExpressionDoesNotReferenceCommand() {
        assertFalse(referencesCommand(listOf(RandomizeStatement(0, 0))))
    }

    @Test
    fun commandInFunctionDefinitionReferencesCommand() {
        val function = FunctionDefinitionStatement(0, 0, Identifier("FNfoo", Str.INSTANCE), listOf<Declaration>(), commandCall)
        assertTrue(referencesCommand(listOf(function)))
    }

    @Test
    fun functionDefinitionWithoutCommandDoesNotReferenceCommand() {
        val function = FunctionDefinitionStatement(0, 0, Identifier("FNfoo", Str.INSTANCE), listOf<Declaration>(), otherCall)
        assertFalse(referencesCommand(listOf(function)))
    }
}
