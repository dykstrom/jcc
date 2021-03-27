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

package se.dykstrom.jcc.common.optimization

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import se.dykstrom.jcc.common.ast.*
import se.dykstrom.jcc.common.compiler.DefaultTypeManager
import se.dykstrom.jcc.common.types.F64
import se.dykstrom.jcc.common.types.I64
import se.dykstrom.jcc.common.types.Identifier
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
        val assignStatement = AssignStatement(0, 0, NAME_I64_A, addExpression)
        val program = Program(0, 0, listOf(assignStatement))

        // When
        val optimizedProgram = statementOptimizer.program(program)
        val optimizedStatements = optimizedProgram.statements

        // Then
        assertEquals(1, optimizedStatements.size)
        assertEquals(assignStatement, optimizedStatements[0])
    }

    @Test
    fun shouldNotReplaceAddWithAddAssignWhenVariablesDiffer() {
        // Given
        val addExpression = AddExpression(0, 0, IDE_I64_A, IL_1)
        val assignStatement = AssignStatement(0, 0, NAME_I64_B, addExpression)
        val program = Program(0, 0, listOf(assignStatement))

        // When
        val optimizedProgram = statementOptimizer.program(program)
        val optimizedStatements = optimizedProgram.statements

        // Then
        assertEquals(1, optimizedStatements.size)
        assertEquals(assignStatement, optimizedStatements[0])
    }

    @Test
    fun shouldNotReplaceAddWithAddAssignForFloatExpressions() {
        // Given
        val addExpression = AddExpression(0, 0, IDE_F64_F, FL_3_14)
        val assignStatement = AssignStatement(0, 0, NAME_F64_F, addExpression)
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
        val assignStatement = AssignStatement(0, 0, NAME_I64_A, addExpression)
        val program = Program(0, 0, listOf(assignStatement))

        val expectedStatement = IncStatement(0, 0, NAME_I64_A)

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
        val assignStatement = AssignStatement(0, 0, NAME_I64_A, subExpression)
        val program = Program(0, 0, listOf(assignStatement))

        val expectedStatement = DecStatement(0, 0, NAME_I64_A)

        // When
        val optimizedProgram = statementOptimizer.program(program)
        val optimizedStatements = optimizedProgram.statements

        // Then
        assertEquals(1, optimizedStatements.size)
        assertEquals(expectedStatement, optimizedStatements[0])
    }

    @Test
    fun shouldReplaceComplexSubOneWithDec() {
        // Given
        val addExpression = AddExpression(0, 0, IL_1, IL_2)
        val iDivExpression = IDivExpression(0, 0, addExpression, addExpression)
        val subExpression = SubExpression(0, 0, IDE_I64_A, iDivExpression)
        val assignStatement = AssignStatement(0, 0, NAME_I64_A, subExpression)
        val program = Program(0, 0, listOf(assignStatement))

        val expectedStatement = DecStatement(0, 0, NAME_I64_A)

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
        val assignStatement = AssignStatement(0, 0, NAME_I64_A, addExpression)
        val program = Program(0, 0, listOf(assignStatement))

        val expectedStatement = AddAssignStatement(0, 0, NAME_I64_A, IL_2)

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
        val assignStatement = AssignStatement(0, 0, NAME_I64_A, subExpression)
        val program = Program(0, 0, listOf(assignStatement))

        val expectedStatement = SubAssignStatement(0, 0, NAME_I64_A, IL_2)

        // When
        val optimizedProgram = statementOptimizer.program(program)
        val optimizedStatements = optimizedProgram.statements

        // Then
        assertEquals(1, optimizedStatements.size)
        assertEquals(expectedStatement, optimizedStatements[0])
    }

    companion object {
        private val FL_3_14 = FloatLiteral(0, 0, "3.14")
        private val IL_1 = IntegerLiteral(0, 0, "1")
        private val IL_2 = IntegerLiteral(0, 0, "2")

        private val IDENT_F64_F = Identifier("f", F64.INSTANCE)
        private val IDENT_I64_A = Identifier("a%", I64.INSTANCE)
        private val IDENT_I64_B = Identifier("b%", I64.INSTANCE)

        private val IDE_F64_F = IdentifierDerefExpression(0, 0, IDENT_F64_F)
        private val IDE_I64_A = IdentifierDerefExpression(0, 0, IDENT_I64_A)

        private val NAME_I64_A = IdentifierNameExpression(0, 0, IDENT_I64_A)
        private val NAME_I64_B = IdentifierNameExpression(0, 0, IDENT_I64_B)
        private val NAME_F64_F = IdentifierNameExpression(0, 0, IDENT_F64_F)

        // We have to use the default type manager here, since we don't have access to any other.
        // If this becomes a problem for the tests, we will have to make the default type manager
        // more advanced.
        private val statementOptimizer = DefaultAstOptimizer(DefaultTypeManager())
    }
}
