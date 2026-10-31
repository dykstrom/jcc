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

package se.dykstrom.jcc.llvm.operation

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import se.dykstrom.jcc.common.types.Bool
import se.dykstrom.jcc.common.types.F32
import se.dykstrom.jcc.common.types.F64
import se.dykstrom.jcc.common.types.I32
import se.dykstrom.jcc.common.types.I64
import se.dykstrom.jcc.common.types.Identifier
import se.dykstrom.jcc.common.types.Opaque
import se.dykstrom.jcc.common.types.Str
import se.dykstrom.jcc.common.types.Type

class ConstOperationTests {

    @Test
    fun shouldWriteIntegerConstants() {
        assertEquals("@_a = private constant i64 17", text(I64.INSTANCE, "17"))
        assertEquals("@_a = private constant i32 -5", text(I32.INSTANCE, "-5"))
    }

    @Test
    fun shouldWriteFloatConstants() {
        assertEquals("@_a = private constant double 1.5", text(F64.INSTANCE, "1.5"))
        assertEquals("@_a = private constant float 0.10000000149011612", text(F32.INSTANCE, "0.1"))
        assertEquals("@_a = private constant double 0x7FF0000000000000", text(F64.INSTANCE, "Infinity"))
    }

    @Test
    fun shouldWriteBooleanConstants() {
        // A boolean literal holds -1 for true
        assertEquals("@_a = private constant i1 1", text(Bool.INSTANCE, "-1"))
        assertEquals("@_a = private constant i1 0", text(Bool.INSTANCE, "0"))
    }

    @Test
    fun shouldWriteStringConstant() {
        assertEquals("@_a = private constant [3 x i8] c\"hi\\00\"", text(Str.INSTANCE, "hi"))
    }

    @Test
    fun shouldWriteOpaqueConstantsAsUnderlyingType() {
        assertEquals("@_a = private constant double 9.5", text(Opaque("Meters", F64.INSTANCE), "9.5"))
        assertEquals("@_a = private constant float 0.10000000149011612", text(Opaque("Ratio", F32.INSTANCE), "0.1"))
        assertEquals("@_a = private constant [4 x i8] c\"Bob\\00\"", text(Opaque("Name", Str.INSTANCE), "Bob"))
    }

    private fun text(type: Type, value: String) = ConstOperation(Identifier("a", type), value).toText()
}
