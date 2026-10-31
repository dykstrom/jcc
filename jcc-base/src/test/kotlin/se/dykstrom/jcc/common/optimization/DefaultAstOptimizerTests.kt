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
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import se.dykstrom.jcc.common.BaseTests.Companion.FUN_I64_TO_I64
import se.dykstrom.jcc.common.BaseTests.Companion.IDENT_I64_A
import se.dykstrom.jcc.common.BaseTests.Companion.IDE_I64_A
import se.dykstrom.jcc.common.BaseTests.Companion.IDE_I64_B
import se.dykstrom.jcc.common.BaseTests.Companion.IL_1
import se.dykstrom.jcc.common.BaseTests.Companion.IL_2
import se.dykstrom.jcc.common.BaseTests.Companion.IL_3
import se.dykstrom.jcc.common.BaseTests.Companion.INE_I64_A
import se.dykstrom.jcc.common.BaseTests.Companion.INE_I64_B
import se.dykstrom.jcc.common.ast.*
import se.dykstrom.jcc.common.ast.IntegerLiteral.ZERO
import se.dykstrom.jcc.common.compiler.DefaultTypeManager
import se.dykstrom.jcc.common.optimization.DefaultAstOptimizer.StatementComponent
import se.dykstrom.jcc.common.symbols.SymbolTable
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
    private val expressionOptimizer = DefaultAstExpressionOptimizer(DefaultTypeManager())
    private val optimizer = DefaultAstOptimizer(expressionOptimizer, symbolTable)

    @BeforeEach
    fun init() {
        OptimizationOptions.INSTANCE.level = 1
    }

    @AfterEach
    fun tearDown() {
        OptimizationOptions.INSTANCE.level = 0
    }

    @Test
    fun shouldNotOptimize() {
        OptimizationOptions.INSTANCE.level = 0

        // Given
        val addExpression = AddExpression(0, 0, IL_1, IL_2)
        val assignStatement = AssignStatement(0, 0, INE_I64_A, addExpression)
        val program = AstProgram(0, 0, listOf(assignStatement))

        // When
        val optimizedProgram = optimizer.program(program)
        val optimizedStatements = optimizedProgram.statements

        // Then: the literals are not folded, since optimization is disabled
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
        val program = AstProgram(0, 0, listOf(constDeclarationStatement))

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
        // b is an integer variable, while a is an integer constant
        val addExpression = AddExpression(0, 0, IDE_I64_B, IDE_I64_A)
        val assignStatement = AssignStatement(0, 0, INE_I64_B, addExpression)
        val program = AstProgram(
            0,
            0,
            listOf(constDeclarationStatement, assignStatement)
        )

        // Since a is a constant with value 1, "b = b + a" becomes "b = b + 1"
        val expectedStatement = AssignStatement(0, 0, INE_I64_B, AddExpression(0, 0, IDE_I64_B, IL_1))

        // When
        val optimizedProgram = optimizer.program(program)
        val optimizedStatements = optimizedProgram.statements

        // Then
        assertEquals(2, optimizedStatements.size)
        assertEquals(constDeclarationStatement, optimizedStatements[0])
        assertEquals(expectedStatement, optimizedStatements[1])
    }

    @Test
    fun shouldReplaceAddLiteralsWithOneLiteralInExpressionFunction() {
        // Given
        val ident = Identifier("FNbar%", FUN_I64_TO_I64)
        val declarations = listOf(Declaration(0, 0, "x", I64.INSTANCE))
        val expression = AddExpression(0, 0, IL_1, IL_2)
        val originalFds = FunctionDefinitionStatement(0, 0, ident, declarations, expression)
        val optimizedFds = FunctionDefinitionStatement(0, 0, ident, declarations, IL_3)
        val program = AstProgram(0, 0, listOf(originalFds))

        // When
        val optimizedProgram = optimizer.program(program)
        val optimizedStatements = optimizedProgram.statements

        // Then
        assertEquals(1, optimizedStatements.size)
        assertEquals(optimizedFds, optimizedStatements[0])
    }

    @Test
    fun shouldOptimizeStatementsInWhile() {
        // Given
        val subExpression = SubExpression(0, 0, IL_3, IL_1)
        val assignStatement = AssignStatement(0, 0, INE_I64_A, subExpression)
        val whileStatement = WhileStatement(0, 0, IL_2, listOf(assignStatement))
        val program = AstProgram(0, 0, listOf(whileStatement))

        val expectedStatement = AssignStatement(0, 0, INE_I64_A, IL_2)

        // When
        val optimizedProgram = optimizer.program(program)
        val optimizedStatements = optimizedProgram.statements

        // Then
        assertEquals(1, optimizedStatements.size)
        val optimizedWhileStatement = optimizedStatements[0] as WhileStatement
        assertEquals(1, optimizedWhileStatement.statements.size)
        assertEquals(expectedStatement, optimizedWhileStatement.statements[0])
    }

    @Test
    fun shouldKeepExprNotEqualToZeroInWhile() {
        // The rewrite to just 'x' relies on integer conditions, which only BASIC has
        val expression = NotEqualExpression(0, 0, ZERO, IDE_I64_B)
        val whileStatement = WhileStatement(0, 0, expression, listOf(AssignStatement(0, 0, IDE_I64_A, IL_1)))

        val optimizedProgram = optimizer.program(AstProgram(0, 0, listOf(whileStatement)))

        assertEquals(expression, (optimizedProgram.statements[0] as WhileStatement).expression)
    }

    @Test
    fun shouldApplyConditionStrategyAfterOptimizingCondition() {
        // The strategy sees the folded condition, 3, and replaces it
        val strategy = { e: Expression -> if (e == IL_3) IDE_I64_B else e }
        val conditionOptimizer = DefaultAstOptimizer(expressionOptimizer, strategy, listOf(), symbolTable)
        val whileStatement = WhileStatement(0, 0, AddExpression(0, 0, IL_1, IL_2), listOf(AssignStatement(0, 0, IDE_I64_A, IL_1)))

        val optimizedProgram = conditionOptimizer.program(AstProgram(0, 0, listOf(whileStatement)))

        assertEquals(IDE_I64_B, (optimizedProgram.statements[0] as WhileStatement).expression)
    }

    @Test
    fun shouldUseLanguageComponentBeforeBuiltInOne() {
        // The language's component replaces the built-in one for assignments
        val component = StatementComponent(AssignStatement::class.java) { s, _ -> s.withRhsExpression(IL_3) }
        val languageOptimizer = DefaultAstOptimizer(expressionOptimizer, { it }, listOf(component), symbolTable)
        val program = AstProgram(0, 0, listOf(AssignStatement(0, 0, INE_I64_A, AddExpression(0, 0, IL_1, IL_1))))

        val optimizedProgram = languageOptimizer.program(program)

        assertEquals(AssignStatement(0, 0, INE_I64_A, IL_3), optimizedProgram.statements[0])
    }

    @Test
    fun shouldMatchComponentToSubtypeOfItsStatementType() {
        // A subclass of AssignStatement, such as Assembunny's CpyStatement, goes to the assignment component
        class Copy(rhs: Expression) : AssignStatement(0, 0, INE_I64_A, rhs) {
            override fun withRhsExpression(expression: Expression) = Copy(expression)
        }
        val program = AstProgram(0, 0, listOf(Copy(AddExpression(0, 0, IL_1, IL_1))))

        val optimizedProgram = optimizer.program(program)

        val optimizedStatement = optimizedProgram.statements[0]
        assertTrue(optimizedStatement is Copy)
        assertEquals(IL_2, (optimizedStatement as Copy).rhsExpression)
    }

    @Test
    fun shouldOptimizeExpressionsAndStatementsInFor() {
        // Given
        val assignStatement = AssignStatement(0, 0, INE_I64_B, SubExpression(0, 0, IL_3, IL_1))
        val forStatement = ForStatement(
            INE_I64_A,
            SubExpression(0, 0, IL_2, IL_1),
            AddExpression(0, 0, IL_1, IL_2),
            AddExpression(0, 0, IL_1, IL_1),
            listOf(assignStatement)
        )
        val program = AstProgram(0, 0, listOf(forStatement))

        val expectedStatement = ForStatement(INE_I64_A, IL_1, IL_3, IL_2, listOf(AssignStatement(0, 0, INE_I64_B, IL_2)))

        // When
        val optimizedProgram = optimizer.program(program)
        val optimizedStatements = optimizedProgram.statements

        // Then
        assertEquals(listOf(expectedStatement), optimizedStatements)
    }

    @Test
    fun shouldOptimizeForWithoutStep() {
        // Given
        val forStatement = ForStatement(INE_I64_A, IL_1, AddExpression(0, 0, IL_1, IL_2), null, listOf())
        val program = AstProgram(0, 0, listOf(forStatement))

        val expectedStatement = ForStatement(INE_I64_A, IL_1, IL_3, null, listOf())

        // When
        val optimizedProgram = optimizer.program(program)

        // Then
        assertEquals(listOf(expectedStatement), optimizedProgram.statements)
    }

    @Test
    fun shouldOptimizeLabelledStatement() {
        // Given
        val subExpression = SubExpression(IL_2, IL_1)
        val assignStatement = AssignStatement(0, 0, INE_I64_A, subExpression)
        val labelledStatement = LabelledStatement("label", assignStatement)
        val program = AstProgram(0, 0, listOf(labelledStatement))

        val expectedStatement = LabelledStatement("label", AssignStatement(0, 0, INE_I64_A, IL_1))

        // When
        val optimizedProgram = optimizer.program(program)
        val optimizedStatements = optimizedProgram.statements

        // Then
        assertEquals(1, optimizedStatements.size)
        assertEquals(expectedStatement, optimizedStatements[0])
    }
}
