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

package se.dykstrom.jcc.common.ast

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import se.dykstrom.jcc.common.types.F32
import se.dykstrom.jcc.common.types.F64
import se.dykstrom.jcc.common.types.I32
import se.dykstrom.jcc.common.types.I64

/**
 * Casts differing only in destination type are different nodes, and code generation lowers
 * them to different instructions, so the destination type must take part in equality.
 */
class CastExpressionTests {

    companion object {
        private val IL_5 = IntegerLiteral(0, 0, "5")
        private val FL_5 = FloatLiteral(0, 0, "5.0")
    }

    @Test
    fun castToIntShouldDependOnDestinationType() {
        assertEquals(CastToIntExpression(IL_5, I64.INSTANCE), CastToIntExpression(IL_5, I64.INSTANCE))
        assertNotEquals(CastToIntExpression(IL_5, I64.INSTANCE), CastToIntExpression(IL_5, I32.INSTANCE))
    }

    @Test
    fun castToFloatShouldDependOnDestinationType() {
        assertEquals(CastToFloatExpression(FL_5, F64.INSTANCE), CastToFloatExpression(FL_5, F64.INSTANCE))
        assertNotEquals(CastToFloatExpression(FL_5, F64.INSTANCE), CastToFloatExpression(FL_5, F32.INSTANCE))
    }

    @Test
    fun truncateShouldDependOnDestinationType() {
        assertEquals(TruncateExpression(0, 0, IL_5, I32.INSTANCE), TruncateExpression(0, 0, IL_5, I32.INSTANCE))
        assertNotEquals(TruncateExpression(0, 0, IL_5, I32.INSTANCE), TruncateExpression(0, 0, IL_5, I64.INSTANCE))
    }
}
