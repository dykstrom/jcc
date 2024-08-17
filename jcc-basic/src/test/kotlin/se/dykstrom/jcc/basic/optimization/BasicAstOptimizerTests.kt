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

package se.dykstrom.jcc.basic.optimization

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import se.dykstrom.jcc.basic.BasicTests.Companion.IDE_I64_A
import se.dykstrom.jcc.basic.BasicTests.Companion.IL_0
import se.dykstrom.jcc.basic.BasicTests.Companion.IL_1
import se.dykstrom.jcc.basic.BasicTests.Companion.IL_2
import se.dykstrom.jcc.basic.ast.PrintStatement
import se.dykstrom.jcc.basic.ast.RandomizeStatement
import se.dykstrom.jcc.basic.compiler.BasicSymbols
import se.dykstrom.jcc.basic.compiler.BasicTypeManager
import se.dykstrom.jcc.common.ast.AddExpression
import se.dykstrom.jcc.common.ast.MulExpression
import se.dykstrom.jcc.common.ast.AstProgram
import se.dykstrom.jcc.common.utils.OptimizationOptions

/**
 * Tests class `BasicAstOptimizer`.
 *
 * @author Johan Dykstrom
 * @see BasicAstOptimizer
 */
class BasicAstOptimizerTests {

    private val symbolTable = BasicSymbols()
    private val typeManager = BasicTypeManager()

    private val optimizer = BasicAstOptimizer(typeManager, symbolTable)

    @BeforeEach
    fun init() {
        OptimizationOptions.INSTANCE.level = 1
    }

    @Test
    fun shouldOptimizePrintStatement() {
        // Given
        val addExpression = AddExpression(0, 0, IL_1, IL_1)
        val mulExpression = MulExpression(0, 0, IDE_I64_A, IL_0)
        val printStatement = PrintStatement(0, 0, listOf(addExpression, mulExpression))
        val program = AstProgram(0, 0, listOf(printStatement))

        val expectedStatement = PrintStatement(0, 0, listOf(IL_2, IL_0))

        // When
        val optimizedProgram = optimizer.program(program)
        val optimizedStatements = optimizedProgram.statements

        // Then
        assertEquals(1, optimizedStatements.size)
        assertEquals(expectedStatement, optimizedStatements[0])
    }

    @Test
    fun shouldOptimizeRandomizeStatement() {
        // Given
        val addExpression = AddExpression(0, 0, IL_1, IL_1)
        val randomizeStatement = RandomizeStatement(0, 0, addExpression)
        val program = AstProgram(0, 0, listOf(randomizeStatement))

        val expectedStatement = RandomizeStatement(0, 0, IL_2)

        // When
        val optimizedProgram = optimizer.program(program)
        val optimizedStatements = optimizedProgram.statements

        // Then
        assertEquals(1, optimizedStatements.size)
        assertEquals(expectedStatement, optimizedStatements[0])
    }
}
