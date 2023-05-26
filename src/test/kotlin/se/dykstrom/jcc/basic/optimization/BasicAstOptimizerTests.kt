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

import org.junit.Before
import org.junit.Test
import se.dykstrom.jcc.basic.ast.RandomizeStatement
import se.dykstrom.jcc.basic.compiler.BasicTypeManager
import se.dykstrom.jcc.common.ast.AddExpression
import se.dykstrom.jcc.common.ast.IntegerLiteral
import se.dykstrom.jcc.common.ast.Program
import se.dykstrom.jcc.common.utils.OptimizationOptions
import kotlin.test.assertEquals

/**
 * Tests class `BasicAstOptimizer`.
 *
 * @author Johan Dykstrom
 * @see BasicAstOptimizer
 */
class BasicAstOptimizerTests {

    @Before
    fun init() {
        OptimizationOptions.INSTANCE.level = 1
    }

    @Test
    fun shouldOptimizeRandomizeExpression() {
        // Given
        val addExpression = AddExpression(0, 0, IL_1, IL_1)
        val randomizeStatement = RandomizeStatement(0, 0, addExpression)
        val program = Program(0, 0, listOf(randomizeStatement))

        val expectedStatement = RandomizeStatement(0, 0, IL_2)

        // When
        val optimizedProgram = statementOptimizer.program(program)
        val optimizedStatements = optimizedProgram.statements

        // Then
        assertEquals(1, optimizedStatements.size)
        assertEquals(expectedStatement, optimizedStatements[0])
    }

    companion object {
        private val IL_1 = IntegerLiteral(0, 0, "1")
        private val IL_2 = IntegerLiteral(0, 0, "2")

        private val TYPE_MANAGER = BasicTypeManager()

        private val statementOptimizer = BasicAstOptimizer(TYPE_MANAGER)
    }
}
