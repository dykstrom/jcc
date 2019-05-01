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

package se.dykstrom.jcc.common.compiler

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import se.dykstrom.jcc.common.ast.*
import se.dykstrom.jcc.common.types.F64
import se.dykstrom.jcc.common.types.I64
import se.dykstrom.jcc.common.types.Identifier
import se.dykstrom.jcc.common.types.Str
import se.dykstrom.jcc.common.utils.OptimizationOptions

/**
 * Tests class `DefaultAstOptimizer`.
 *
 * @author Johan Dykstrom
 * @see DefaultAstOptimizer
 */
class DefaultAstOptimizerTests {

    @Before
    fun init() {
        OptimizationOptions.INSTANCE.level = 1
    }

    @Test
    fun shouldNotOptimize() {
        OptimizationOptions.INSTANCE.level = 0

        // Given
        val addExpression = AddExpression(0, 0, IDE_I64_A, IL_1)
        val assignStatement = AssignStatement(0, 0, IDENT_I64_A, addExpression)
        val program = Program(0, 0, listOf(assignStatement))

        // When
        val optimizedProgram = statementOptimizer.program(program)
        val optimizedStatements = optimizedProgram.statements

        // Then
        assertEquals(1, optimizedStatements.size)
        assertEquals(assignStatement, optimizedStatements[0])
    }

    @Test
    fun shouldNotReplaceWhenVariablesDiffer() {
        // Given
        val addExpression = AddExpression(0, 0, IDE_I64_A, IL_1)
        val assignStatement = AssignStatement(0, 0, IDENT_I64_B, addExpression)
        val program = Program(0, 0, listOf(assignStatement))

        // When
        val optimizedProgram = statementOptimizer.program(program)
        val optimizedStatements = optimizedProgram.statements

        // Then
        assertEquals(1, optimizedStatements.size)
        assertEquals(assignStatement, optimizedStatements[0])
    }

    @Test
    fun shouldNotReplaceFloatExpressions() {
        // Given
        val addExpression = AddExpression(0, 0, IDE_F64_F, FL_3_14)
        val assignStatement = AssignStatement(0, 0, IDENT_F64_F, addExpression)
        val program = Program(0, 0, listOf(assignStatement))

        // When
        val optimizedProgram = statementOptimizer.program(program)
        val optimizedStatements = optimizedProgram.statements

        // Then
        assertEquals(1, optimizedStatements.size)
        assertEquals(assignStatement, optimizedStatements[0])
    }

    @Test
    fun shouldReplaceAddOneWithInc() {
        // Given
        val addExpression = AddExpression(0, 0, IDE_I64_A, IL_1)
        val assignStatement = AssignStatement(0, 0, IDENT_I64_A, addExpression)
        val program = Program(0, 0, listOf(assignStatement))

        val expectedStatement = IncStatement(0, 0, IDENT_I64_A)

        // When
        val optimizedProgram = statementOptimizer.program(program)
        val optimizedStatements = optimizedProgram.statements

        // Then
        assertEquals(1, optimizedStatements.size)
        assertEquals(expectedStatement, optimizedStatements[0])
    }

    @Test
    fun shouldReplaceSubOneWithDec() {
        // Given
        val subExpression = SubExpression(0, 0, IDE_I64_A, IL_1)
        val assignStatement = AssignStatement(0, 0, IDENT_I64_A, subExpression)
        val program = Program(0, 0, listOf(assignStatement))

        val expectedStatement = DecStatement(0, 0, IDENT_I64_A)

        // When
        val optimizedProgram = statementOptimizer.program(program)
        val optimizedStatements = optimizedProgram.statements

        // Then
        assertEquals(1, optimizedStatements.size)
        assertEquals(expectedStatement, optimizedStatements[0])
    }

    @Test
    fun shouldReplaceAddTwoWithAddAssign() {
        // Given
        val addExpression = AddExpression(0, 0, IDE_I64_A, IL_2)
        val assignStatement = AssignStatement(0, 0, IDENT_I64_A, addExpression)
        val program = Program(0, 0, listOf(assignStatement))

        val expectedStatement = AddAssignStatement(0, 0, IDENT_I64_A, IL_2)

        // When
        val optimizedProgram = statementOptimizer.program(program)
        val optimizedStatements = optimizedProgram.statements

        // Then
        assertEquals(1, optimizedStatements.size)
        assertEquals(expectedStatement, optimizedStatements[0])
    }

    @Test
    fun shouldReplaceSubTwoWithSubAssign() {
        // Given
        val subExpression = SubExpression(0, 0, IDE_I64_A, IL_2)
        val assignStatement = AssignStatement(0, 0, IDENT_I64_A, subExpression)
        val program = Program(0, 0, listOf(assignStatement))

        val expectedStatement = SubAssignStatement(0, 0, IDENT_I64_A, IL_2)

        // When
        val optimizedProgram = statementOptimizer.program(program)
        val optimizedStatements = optimizedProgram.statements

        // Then
        assertEquals(1, optimizedStatements.size)
        assertEquals(expectedStatement, optimizedStatements[0])
    }

    @Test
    fun shouldReplaceAddIntegerLiteralsWithOneLiteral() {
        // Given
        val addExpression = AddExpression(0, 0, IL_2, IL_1)
        val assignStatement = AssignStatement(0, 0, IDENT_I64_A, addExpression)
        val program = Program(0, 0, listOf(assignStatement))

        val expectedStatement = AssignStatement(0, 0, IDENT_I64_A, IL_3)

        // When
        val optimizedProgram = statementOptimizer.program(program)
        val optimizedStatements = optimizedProgram.statements

        // Then
        assertEquals(1, optimizedStatements.size)
        assertEquals(expectedStatement, optimizedStatements[0])
    }

    @Test
    fun shouldReplaceAddIntegerLiteralsWithOneLiteralDownTheTree() {
        // Given
        val addExpression1 = AddExpression(0, 0, IL_2, IL_1)
        val addExpression2 = AddExpression(0, 0, IDE_I64_A, addExpression1)
        val assignStatement = AssignStatement(0, 0, IDENT_I64_B, addExpression2)
        val program = Program(0, 0, listOf(assignStatement))

        val expectedStatement = AssignStatement(0, 0, IDENT_I64_B, AddExpression(0, 0, IDE_I64_A, IL_3))

        // When
        val optimizedProgram = statementOptimizer.program(program)
        val optimizedStatements = optimizedProgram.statements

        // Then
        assertEquals(1, optimizedStatements.size)
        assertEquals(expectedStatement, optimizedStatements[0])
    }

    @Test
    fun shouldReplaceAddStringLiteralsWithOneLiteral() {
        // Given
        val addExpression = AddExpression(0, 0, SL_ONE, SL_TWO)
        val assignStatement = AssignStatement(0, 0, IDENT_STR_S, addExpression)
        val program = Program(0, 0, listOf(assignStatement))

        val expectedStatement = AssignStatement(0, 0, IDENT_STR_S, SL_ONE_TWO)

        // When
        val optimizedProgram = statementOptimizer.program(program)
        val optimizedStatements = optimizedProgram.statements

        // Then
        assertEquals(1, optimizedStatements.size)
        assertEquals(expectedStatement, optimizedStatements[0])
    }

    @Test
    fun shouldReplaceAddFloatLiteralsWithOneLiteral() {
        // Given
        val addExpression = AddExpression(0, 0, FL_1_00, FL_3_14)
        val assignStatement = AssignStatement(0, 0, IDENT_F64_F, addExpression)
        val program = Program(0, 0, listOf(assignStatement))

        // When
        val optimizedProgram = statementOptimizer.program(program)
        val optimizedStatements = optimizedProgram.statements

        // Then
        assertEquals(1, optimizedStatements.size)
        val expectedValue = FL_4_14.asDouble()
        val optimizedValue = ((optimizedStatements[0] as AssignStatement).expression as FloatLiteral).asDouble()
        assertEquals(expectedValue, optimizedValue, 0.0001)
    }

    companion object {
        private val FL_1_00 = FloatLiteral(0, 0, "1.00")
        private val FL_3_14 = FloatLiteral(0, 0, "3.14")
        private val FL_4_14 = FloatLiteral(0, 0, "4.14")
        private val IL_1 = IntegerLiteral(0, 0, "1")
        private val IL_2 = IntegerLiteral(0, 0, "2")
        private val IL_3 = IntegerLiteral(0, 0, "3")
        private val SL_ONE = StringLiteral(0, 0, "One")
        private val SL_TWO = StringLiteral(0, 0, "Two")
        private val SL_ONE_TWO = StringLiteral(0, 0, "OneTwo")

        private val IDENT_F64_F = Identifier("f", F64.INSTANCE)
        private val IDENT_I64_A = Identifier("a%", I64.INSTANCE)
        private val IDENT_I64_B = Identifier("b%", I64.INSTANCE)
        private val IDENT_STR_S = Identifier("s$", Str.INSTANCE)

        private val IDE_F64_F: Expression = IdentifierDerefExpression(0, 0, IDENT_F64_F)
        private val IDE_I64_A: Expression = IdentifierDerefExpression(0, 0, IDENT_I64_A)

        private val statementOptimizer = DefaultAstOptimizer()
    }
}
