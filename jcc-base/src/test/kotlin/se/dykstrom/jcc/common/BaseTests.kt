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

package se.dykstrom.jcc.common

import se.dykstrom.jcc.common.ast.*
import se.dykstrom.jcc.common.types.*

class BaseTests {

    companion object {

        // Literals
        val IL_0 = IntegerLiteral(0, 0, "0")
        val IL_1 = IntegerLiteral(0, 0, "1")
        val IL_2 = IntegerLiteral(0, 0, "2")
        val IL_3 = IntegerLiteral(0, 0, "3")
        val IL_5 = IntegerLiteral(0, 0, "5")
        val IL_6 = IntegerLiteral(0, 0, "6")
        val IL_7 = IntegerLiteral(0, 0, "7")
        val IL_8 = IntegerLiteral(0, 0, "8")
        val IL_9 = IntegerLiteral(0, 0, "9")
        val IL_14 = IntegerLiteral(0, 0, "14")
        val IL_17 = IntegerLiteral(0, 0, "17")
        val IL_M1 = IntegerLiteral(0, 0, "-1")
        val IL_1_I32 = IntegerLiteral(0, 0, "1", I32.INSTANCE)
        val IL_2_I32 = IntegerLiteral(0, 0, "2", I32.INSTANCE)

        val FL_0_0 = FloatLiteral(0, 0, "0.0")
        val FL_1_0 = FloatLiteral(0, 0, "1.0")
        val FL_2_0 = FloatLiteral(0, 0, "2.0")
        val FL_2_25 = FloatLiteral(0, 0, "2.25")
        val FL_3_14 = FloatLiteral(0, 0, "3.14")
        val FL_4_14 = FloatLiteral(0, 0, "4.14")
        val FL_5_0 = FloatLiteral(0, 0, "5.0")
        val FL_M1_0 = FloatLiteral(0, 0, "-1.0")
        val FL_M3_14 = FloatLiteral(0, 0, "-3.14")
        val FL_1_0_F32 = FloatLiteral(0, 0, "1.0", F32.INSTANCE)
        val FL_2_0_F32 = FloatLiteral(0, 0, "2.0", F32.INSTANCE)

        val SL_A = StringLiteral(0, 0, "a")
        val SL_B = StringLiteral(0, 0, "b")
        val SL_ONE = StringLiteral(0, 0, "One")
        val SL_TWO = StringLiteral(0, 0, "Two")
        val SL_ONE_TWO = StringLiteral(0, 0, "OneTwo")

        // Identifiers
        val IDENT_F64_F = Identifier("f", F64.INSTANCE)
        val IDENT_I64_A = Identifier("a", I64.INSTANCE)
        val IDENT_I64_B = Identifier("b", I64.INSTANCE)
        val IDENT_STR_S = Identifier("s", Str.INSTANCE)

        // Identifier references
        val IDE_F64_F = IdentifierDerefExpression(IDENT_F64_F)
        val IDE_I64_A = IdentifierDerefExpression(IDENT_I64_A)
        val IDE_I64_B = IdentifierDerefExpression(IDENT_I64_B)
        val IDE_STR_S = IdentifierDerefExpression(IDENT_STR_S)

        val INE_I64_A = IdentifierNameExpression(0, 0, IDENT_I64_A)
        val INE_I64_B = IdentifierNameExpression(0, 0, IDENT_I64_B)

        // Function types
        val FUN_I64_TO_I64: Fun = Fun.from(listOf(I64.INSTANCE), I64.INSTANCE)

        // Function calls
        val FCE_FOO = FunctionCallExpression(Identifier("foo", FUN_I64_TO_I64), listOf(IL_1))
    }
}
