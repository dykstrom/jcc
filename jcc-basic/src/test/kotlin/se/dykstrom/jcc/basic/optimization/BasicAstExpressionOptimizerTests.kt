/*
 * Copyright (C) 2024 Johan Dykstrom
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
import se.dykstrom.jcc.basic.BasicTests.Companion.FL_1_0
import se.dykstrom.jcc.basic.BasicTests.Companion.FL_2_0
import se.dykstrom.jcc.basic.compiler.BasicSymbols
import se.dykstrom.jcc.basic.compiler.BasicTypeManager
import se.dykstrom.jcc.basic.functions.BasicBuiltInFunctions.FUN_SQR
import se.dykstrom.jcc.common.ast.AddExpression
import se.dykstrom.jcc.common.ast.FunctionCallExpression
import se.dykstrom.jcc.common.ast.SqrtExpression
import se.dykstrom.jcc.common.utils.OptimizationOptions

class BasicAstExpressionOptimizerTests {

    private val symbolTable = BasicSymbols()
    private val typeManager = BasicTypeManager()

    private val optimizer = BasicAstExpressionOptimizer(typeManager)

    @BeforeEach
    fun init() {
        OptimizationOptions.INSTANCE.level = 1
    }

    @Test
    fun shouldOptimizeSqrtFunctionCall() {
        // Given
        val addExpression = AddExpression(0, 0, FL_1_0, FL_1_0)
        val fce = FunctionCallExpression(0, 0, FUN_SQR.identifier, listOf(addExpression))
        val expectedExpression = SqrtExpression(0, 0, FL_2_0)

        // When
        val optimizedExpression = optimizer.expression(fce, symbolTable)

        // Then
        assertEquals(expectedExpression, optimizedExpression)
    }
}
