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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import se.dykstrom.jcc.common.ast.*
import se.dykstrom.jcc.common.compiler.DefaultTypeManager
import se.dykstrom.jcc.common.symbols.SymbolTable
import se.dykstrom.jcc.common.types.F64
import se.dykstrom.jcc.common.types.Fun
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

    private val symbolTable = SymbolTable()

    // We have to use the default type manager here, since we don't have access to any other.
    // If this becomes a problem for the tests, we will have to make the default type manager
    // more advanced.
    private val optimizer = DefaultAstOptimizer(DefaultTypeManager(), symbolTable)

    @BeforeEach
    fun init() {
        OptimizationOptions.INSTANCE.level = 1
    }

    @Test
    fun shouldNotOptimize() {
        OptimizationOptions.INSTANCE.level = 0

        // Given
        val addExpression = AddExpression(0, 0, IDE_I64_A, IL_1)
        val assignStatement = AssignStatement(0, 0, INE_I64_A, addExpression)
        val program = Program(0, 0, listOf(assignStatement))

        // When
        val optimizedProgram = optimizer.program(program)
        val optimizedStatements = optimizedProgram.statements

        // Then
        assertEquals(1, optimizedStatements.size)
        assertEquals(assignStatement, optimizedStatements[0])
    }

    @Test
    fun shouldNotReplaceAddWithAddAssignWhenVariablesDiffer() {
        // Given
        val addExpression = AddExpression(0, 0, IDE_I64_A, IL_1)
        val assignStatement = AssignStatement(0, 0, INE_I64_B, addExpression)
        val program = Program(0, 0, listOf(assignStatement))

        // When
        val optimizedProgram = optimizer.program(program)
        val optimizedStatements = optimizedProgram.statements

        // Then
        assertEquals(1, optimizedStatements.size)
        assertEquals(assignStatement, optimizedStatements[0])
    }

    @Test
    fun shouldNotReplaceAddWithAddAssignForFloatIdentifier() {
        // Given
        val addExpression = AddExpression(0, 0, IDE_F64_F, IL_3)
        val assignStatement = AssignStatement(0, 0, INE_F64_F, addExpression)
        val program = Program(0, 0, listOf(assignStatement))

        // When
        val optimizedProgram = optimizer.program(program)
        val optimizedStatements = optimizedProgram.statements

        // Then
        assertEquals(1, optimizedStatements.size)
        assertEquals(assignStatement, optimizedStatements[0])
    }

    @Test
    fun shouldNotReplaceAddWithAddAssignForFloatLiteral() {
        // Given
        val addExpression = AddExpression(0, 0, IDE_I64_A, FL_3_14)
        val assignStatement = AssignStatement(0, 0, INE_I64_A, addExpression)
        val program = Program(0, 0, listOf(assignStatement))

        // When
        val optimizedProgram = optimizer.program(program)
        val optimizedStatements = optimizedProgram.statements

        // Then
        assertEquals(1, optimizedStatements.size)
        assertEquals(assignStatement, optimizedStatements[0])
    }

    @Test
    fun shouldReplaceAddOneWithInc() {
        // Given
        val addExpression = AddExpression(0, 0, IDE_I64_A, IL_1)
        val assignStatement = AssignStatement(0, 0, INE_I64_A, addExpression)
        val program = Program(0, 0, listOf(assignStatement))

        val expectedStatement = IncStatement(0, 0, INE_I64_A)

        // When
        val optimizedProgram = optimizer.program(program)
        val optimizedStatements = optimizedProgram.statements

        // Then
        assertEquals(1, optimizedStatements.size)
        assertEquals(expectedStatement, optimizedStatements[0])
    }

    @Test
    fun shouldReplaceOneAddWithInc() {
        // Given
        val addExpression = AddExpression(0, 0, IL_1, IDE_I64_A)
        val assignStatement = AssignStatement(0, 0, INE_I64_A, addExpression)
        val program = Program(0, 0, listOf(assignStatement))

        val expectedStatement = IncStatement(0, 0, INE_I64_A)

        // When
        val optimizedProgram = optimizer.program(program)
        val optimizedStatements = optimizedProgram.statements

        // Then
        assertEquals(1, optimizedStatements.size)
        assertEquals(expectedStatement, optimizedStatements[0])
    }

    @Test
    fun shouldReplaceSubOneWithDec() {
        // Given
        val subExpression = SubExpression(0, 0, IDE_I64_A, IL_1)
        val assignStatement = AssignStatement(0, 0, INE_I64_A, subExpression)
        val program = Program(0, 0, listOf(assignStatement))

        val expectedStatement = DecStatement(0, 0, INE_I64_A)

        // When
        val optimizedProgram = optimizer.program(program)
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
        val assignStatement = AssignStatement(0, 0, INE_I64_A, subExpression)
        val program = Program(0, 0, listOf(assignStatement))

        val expectedStatement = DecStatement(0, 0, INE_I64_A)

        // When
        val optimizedProgram = optimizer.program(program)
        val optimizedStatements = optimizedProgram.statements

        // Then
        assertEquals(1, optimizedStatements.size)
        assertEquals(expectedStatement, optimizedStatements[0])
    }

    @Test
    fun shouldReplaceAddTwoWithAddAssign() {
        // Given
        val addExpression = AddExpression(0, 0, IDE_I64_A, IL_2)
        val assignStatement = AssignStatement(0, 0, INE_I64_A, addExpression)
        val program = Program(0, 0, listOf(assignStatement))

        val expectedStatement = AddAssignStatement(0, 0, INE_I64_A, IL_2)

        // When
        val optimizedProgram = optimizer.program(program)
        val optimizedStatements = optimizedProgram.statements

        // Then
        assertEquals(1, optimizedStatements.size)
        assertEquals(expectedStatement, optimizedStatements[0])
    }

    @Test
    fun addAssignShouldHandleLargeNumbers() {
        // Given
        val literal = IntegerLiteral(0, 0, Integer.MAX_VALUE + 10L)
        val addExpression = AddExpression(0, 0, IDE_I64_A, literal)
        val assignStatement = AssignStatement(0, 0, INE_I64_A, addExpression)
        val program = Program(0, 0, listOf(assignStatement))

        val expectedStatement = AddAssignStatement(0, 0, INE_I64_A, literal)

        // When
        val optimizedProgram = optimizer.program(program)
        val optimizedStatements = optimizedProgram.statements

        // Then
        assertEquals(1, optimizedStatements.size)
        assertEquals(expectedStatement, optimizedStatements[0])
    }

    @Test
    fun shouldReplaceMulThreeWithMulAssign() {
        // Given
        val mulExpression = MulExpression(0, 0, IDE_I64_A, IL_3)
        val assignStatement = AssignStatement(0, 0, INE_I64_A, mulExpression)
        val program = Program(0, 0, listOf(assignStatement))

        val expectedStatement = MulAssignStatement(0, 0, INE_I64_A, IL_3)

        // When
        val optimizedProgram = optimizer.program(program)
        val optimizedStatements = optimizedProgram.statements

        // Then
        assertEquals(1, optimizedStatements.size)
        assertEquals(expectedStatement, optimizedStatements[0])
    }

    @Test
    fun shouldReplaceThreeMulWithMulAssign() {
        // Given
        val mulExpression = MulExpression(0, 0, IL_3, IDE_I64_A)
        val assignStatement = AssignStatement(0, 0, INE_I64_A, mulExpression)
        val program = Program(0, 0, listOf(assignStatement))

        val expectedStatement = MulAssignStatement(0, 0, INE_I64_A, IL_3)

        // When
        val optimizedProgram = optimizer.program(program)
        val optimizedStatements = optimizedProgram.statements

        // Then
        assertEquals(1, optimizedStatements.size)
        assertEquals(expectedStatement, optimizedStatements[0])
    }

    @Test
    fun shouldReplaceIDivThreeWithIDivAssign() {
        // Given
        val iDivExpression = IDivExpression(0, 0, IDE_I64_A, IL_3)
        val assignStatement = AssignStatement(0, 0, INE_I64_A, iDivExpression)
        val program = Program(0, 0, listOf(assignStatement))

        val expectedStatement = IDivAssignStatement(0, 0, INE_I64_A, IL_3)

        // When
        val optimizedProgram = optimizer.program(program)
        val optimizedStatements = optimizedProgram.statements

        // Then
        assertEquals(1, optimizedStatements.size)
        assertEquals(expectedStatement, optimizedStatements[0])
    }

    @Test
    fun shouldReplaceSubTwoWithSubAssign() {
        // Given
        val subExpression = SubExpression(0, 0, IDE_I64_A, IL_2)
        val assignStatement = AssignStatement(0, 0, INE_I64_A, subExpression)
        val program = Program(0, 0, listOf(assignStatement))

        val expectedStatement = SubAssignStatement(0, 0, INE_I64_A, IL_2)

        // When
        val optimizedProgram = optimizer.program(program)
        val optimizedStatements = optimizedProgram.statements

        // Then
        assertEquals(1, optimizedStatements.size)
        assertEquals(expectedStatement, optimizedStatements[0])
    }

    @Test
    fun shouldNotReplaceReplaceTwoSubWithSubAssign() {
        // Given
        val subExpression = SubExpression(0, 0, IL_2, IDE_I64_A)
        val assignStatement = AssignStatement(0, 0, INE_I64_A, subExpression)
        val program = Program(0, 0, listOf(assignStatement))

        // When
        val optimizedProgram = optimizer.program(program)
        val optimizedStatements = optimizedProgram.statements

        // Then
        assertEquals(1, optimizedStatements.size)
        assertEquals(assignStatement, optimizedStatements[0])
    }

    @Test
    fun shouldOptimizeConstant() {
        // Given
        val name = IDENT_I64_A.name()
        val type = IDENT_I64_A.type()
        val addExpression = AddExpression(0, 0, IL_1, IL_1)
        val declarations = listOf(DeclarationAssignment(0, 0, name, type, addExpression))
        val constDeclarationStatement = ConstDeclarationStatement(0, 0, declarations)
        val program = Program(0, 0, listOf(constDeclarationStatement))

        val expectedDeclarations = listOf(DeclarationAssignment(0, 0, name, type, IL_2))
        val expectedStatement = ConstDeclarationStatement(0, 0, expectedDeclarations)

        // When
        val optimizedProgram = optimizer.program(program)
        val optimizedStatements = optimizedProgram.statements

        // Then
        assertEquals(1, optimizedStatements.size)
        assertEquals(expectedStatement, optimizedStatements[0])
        assertTrue(symbolTable.contains(name))
        assertTrue(symbolTable.isConstant(name))
    }

    @Test
    fun shouldOptimizeExpressionWithConstant() {
        // Given
        val declarations = listOf(DeclarationAssignment(0, 0, IDENT_I64_A.name(), IDENT_I64_A.type(), IL_1))
        val constDeclarationStatement = ConstDeclarationStatement(0, 0, declarations)
        // b% is an integer variable, while a% is an integer constant
        val addExpression = AddExpression(0, 0, IDE_I64_B, IDE_I64_A)
        val assignStatement = AssignStatement(0, 0, INE_I64_B, addExpression)
        val program = Program(0, 0, listOf(constDeclarationStatement, assignStatement))

        val incStatement = IncStatement(0, 0, INE_I64_B)

        // When
        val optimizedProgram = optimizer.program(program)
        val optimizedStatements = optimizedProgram.statements

        // Then
        assertEquals(2, optimizedStatements.size)
        assertEquals(constDeclarationStatement, optimizedStatements[0])
        // Since a% is a constant with value 1, "b% = b% + a%" can be optimized to b%++
        assertEquals(incStatement, optimizedStatements[1])
    }

    @Test
    fun shouldReplaceAddLiteralsWithOneLiteralInExpressionFunction() {
        // Given
        val ident = Identifier("FNbar%", FUN_I64_TO_I64)
        val declarations = listOf(Declaration(0, 0, "x", I64.INSTANCE))
        val expression = AddExpression(0, 0, IL_1, IL_2)
        val originalFds = FunctionDefinitionStatement(0, 0, ident, declarations, expression)
        val optimizedFds = FunctionDefinitionStatement(0, 0, ident, declarations, IL_3)
        val program = Program(0, 0, listOf(originalFds))

        // When
        val optimizedProgram = optimizer.program(program)
        val optimizedStatements = optimizedProgram.statements

        // Then
        assertEquals(1, optimizedStatements.size)
        assertEquals(optimizedFds, optimizedStatements[0])
    }

    companion object {
        private val FL_3_14 = FloatLiteral(0, 0, "3.14")
        private val IL_1 = IntegerLiteral(0, 0, "1")
        private val IL_2 = IntegerLiteral(0, 0, "2")
        private val IL_3 = IntegerLiteral(0, 0, "3")

        private val IDENT_F64_F = Identifier("f", F64.INSTANCE)
        private val IDENT_I64_A = Identifier("a%", I64.INSTANCE)
        private val IDENT_I64_B = Identifier("b%", I64.INSTANCE)

        private val IDE_F64_F = IdentifierDerefExpression(0, 0, IDENT_F64_F)
        private val IDE_I64_A = IdentifierDerefExpression(0, 0, IDENT_I64_A)
        private val IDE_I64_B = IdentifierDerefExpression(0, 0, IDENT_I64_B)

        private val INE_I64_A = IdentifierNameExpression(0, 0, IDENT_I64_A)
        private val INE_I64_B = IdentifierNameExpression(0, 0, IDENT_I64_B)
        private val INE_F64_F = IdentifierNameExpression(0, 0, IDENT_F64_F)

        private val FUN_I64_TO_I64: Fun = Fun.from(listOf(I64.INSTANCE), I64.INSTANCE)
    }
}
