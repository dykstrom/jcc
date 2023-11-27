/*
 * Copyright (C) 2023 Johan Dykstrom
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

package se.dykstrom.jcc.basic.compiler

import se.dykstrom.jcc.common.assembly.instruction.CallDirect
import se.dykstrom.jcc.common.assembly.instruction.CallIndirect
import se.dykstrom.jcc.common.ast.FloatLiteral
import se.dykstrom.jcc.common.ast.IntegerLiteral
import se.dykstrom.jcc.common.ast.StringLiteral
import se.dykstrom.jcc.common.functions.ExternalFunction
import se.dykstrom.jcc.common.functions.FunctionUtils.LIB_LIBC
import se.dykstrom.jcc.common.functions.LibraryFunction
import se.dykstrom.jcc.common.intermediate.Line
import se.dykstrom.jcc.common.types.*

class BasicTests {

    companion object {

        fun hasIndirectCallTo(lines: List<Line>, mappedName: String) =
            lines.filterIsInstance<CallIndirect>().any { it.target == "[$mappedName]" }

        fun hasDirectCallTo(lines: List<Line>, mappedName: String) =
            lines.filterIsInstance<CallDirect>().any { it.target == "_$mappedName" }

        // Literals
        val FL_2_0 = FloatLiteral(0, 0, "2.0")
        val FL_3_14 = FloatLiteral(0, 0, "3.14")
        val IL_0 = IntegerLiteral(0, 0, "0")
        val IL_1 = IntegerLiteral(0, 0, "1")
        val IL_2 = IntegerLiteral(0, 0, "2")
        val IL_M1 = IntegerLiteral(0, 0, "-1")
        val SL_A = StringLiteral(0, 0, "A")
        val SL_B = StringLiteral(0, 0, "B")

        // Function types
        val FUN_F64_TO_F64: Fun = Fun.from(listOf(F64.INSTANCE), F64.INSTANCE)
        val FUN_F64_TO_I64: Fun = Fun.from(listOf(F64.INSTANCE), I64.INSTANCE)
        val FUN_I64_F64_I64_F64_I64_F64_TO_F64: Fun = Fun.from(listOf(I64.INSTANCE, F64.INSTANCE, I64.INSTANCE, F64.INSTANCE, I64.INSTANCE, F64.INSTANCE), F64.INSTANCE)
        val FUN_I64_F64_TO_F64: Fun = Fun.from(listOf(I64.INSTANCE, F64.INSTANCE), F64.INSTANCE)
        val FUN_I64_TO_I64: Fun = Fun.from(listOf(I64.INSTANCE), I64.INSTANCE)
        val FUN_I64_TO_STR: Fun = Fun.from(listOf(I64.INSTANCE), Str.INSTANCE)
        val FUN_STR_TO_STR: Fun = Fun.from(listOf(Str.INSTANCE), Str.INSTANCE)
        val FUN_TO_F64: Fun = Fun.from(listOf(), F64.INSTANCE)
        val FUN_TO_I64: Fun = Fun.from(listOf(), I64.INSTANCE)
        val FUN_TO_STR: Fun = Fun.from(listOf(), Str.INSTANCE)

        val FUN_FOO = LibraryFunction("foo", listOf(I64.INSTANCE, I64.INSTANCE, I64.INSTANCE), I64.INSTANCE, LIB_LIBC, ExternalFunction("fooo"))
        val FUN_FLO = LibraryFunction("flo", listOf(F64.INSTANCE, F64.INSTANCE, F64.INSTANCE), F64.INSTANCE, LIB_LIBC, ExternalFunction("floo"))

        val IDENT_FUN_FOO: Identifier = FUN_FOO.identifier
        val IDENT_FUN_FLO: Identifier = FUN_FLO.identifier
    }
}
