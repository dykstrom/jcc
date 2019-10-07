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

package se.dykstrom.jcc.common.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test
import se.dykstrom.jcc.common.ast.*
import se.dykstrom.jcc.common.compiler.DefaultTypeManager
import se.dykstrom.jcc.common.types.F64
import se.dykstrom.jcc.common.types.I64
import se.dykstrom.jcc.common.types.Identifier
import se.dykstrom.jcc.common.utils.ExpressionUtils.areAllConstantExpressions
import se.dykstrom.jcc.common.utils.ExpressionUtils.areAllIntegerExpressions

class ExpressionUtilsTests {

    @Test
    fun allShouldBeIntegerExpressions() {
        val expressions = listOf(IL_1, IDE_I64_A, AddExpression(0, 0, IL_1, IDE_I64_A))
        assertTrue(areAllIntegerExpressions(expressions, DefaultTypeManager()))
    }

    @Ignore("need a more advanced default type manager")
    @Test
    fun allShouldNotBeIntegerExpressions() {
        assertFalse(areAllIntegerExpressions(listOf(SL_ONE), DefaultTypeManager()))
        assertFalse(areAllIntegerExpressions(listOf(IDE_F64_F), DefaultTypeManager()))
        assertFalse(areAllIntegerExpressions(listOf(AddExpression(0, 0, IL_1, FL_1_00)), DefaultTypeManager()))
    }

    @Test
    fun allShouldBeConstantExpressions() {
        val expressions = listOf(IL_1, AddExpression(0, 0, IL_1, IL_1), NotExpression(0, 0, BL_TRUE))
        assertTrue(areAllConstantExpressions(expressions))
    }

    @Test
    fun allShouldNotBeConstantExpressions() {
        assertFalse(areAllConstantExpressions(listOf(IDE_F64_F)))
        assertFalse(areAllConstantExpressions(listOf(AddExpression(0, 0, IL_1, IDE_I64_A))))
        assertFalse(areAllConstantExpressions(listOf(FunctionCallExpression(0, 0, Identifier("", I64.INSTANCE), listOf()))))
    }

    @Test
    fun shouldEvaluateConstantIntegerExpressions() {
        // TODO: Implement!
    }

    companion object {
        private val FL_1_00 = FloatLiteral(0, 0, "1.00")
        private val IL_1 = IntegerLiteral(0, 0, "1")
        private val SL_ONE = StringLiteral(0, 0, "One")
        private val BL_TRUE = BooleanLiteral(0, 0, "true")

        private val IDENT_F64_F = Identifier("f", F64.INSTANCE)
        private val IDENT_I64_A = Identifier("a%", I64.INSTANCE)

        private val IDE_F64_F: Expression = IdentifierDerefExpression(0, 0, IDENT_F64_F)
        private val IDE_I64_A: Expression = IdentifierDerefExpression(0, 0, IDENT_I64_A)
    }
}
