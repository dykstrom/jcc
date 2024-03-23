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

package se.dykstrom.jcc.basic

import org.antlr.v4.runtime.BaseErrorListener
import org.antlr.v4.runtime.RecognitionException
import org.antlr.v4.runtime.Recognizer
import se.dykstrom.jcc.common.assembly.instruction.CallDirect
import se.dykstrom.jcc.common.assembly.instruction.CallIndirect
import se.dykstrom.jcc.common.ast.*
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
        val FL_0_3 = FloatLiteral(0, 0, "0.3")
        val FL_1_0 = FloatLiteral(0, 0, 1.0)
        val FL_0_5 = FloatLiteral(0, 0, 0.5)
        val FL_1_2 = FloatLiteral(0, 0, "1.2")
        val FL_2_0 = FloatLiteral(0, 0, "2.0")
        val FL_3_14 = FloatLiteral(0, 0, "3.14")
        val FL_7_5_E10 = FloatLiteral(0, 0, "7.5e+10")
        val FL_17_E4 = FloatLiteral(0, 0, "17E+4")
        val FL_M_3_14 = FloatLiteral(0, 0, "-3.14")

        val IL_0 = IntegerLiteral(0, 0, "0")
        val IL_1 = IntegerLiteral(0, 0, "1")
        val IL_2 = IntegerLiteral(0, 0, "2")
        val IL_3 = IntegerLiteral(0, 0, "3")
        val IL_4 = IntegerLiteral(0, 0, "4")
        val IL_5 = IntegerLiteral(0, 0, "5")
        val IL_10 = IntegerLiteral(0, 0, "10")
        val IL_53 = IntegerLiteral(0, 0, 53)
        val IL_255 = IntegerLiteral(0, 0, "255")
        val IL_M1 = IntegerLiteral(0, 0, "-1")
        val IL_M3 = IntegerLiteral(0, 0, "-3")

        val SL_A = StringLiteral(0, 0, "A")
        val SL_B = StringLiteral(0, 0, "B")
        val SL_C = StringLiteral(0, 0, "C")
        val SL_FOO = StringLiteral(0, 0, "foo")
        val SL_BAR = StringLiteral(0, 0, "bar")
        val SL_ONE = StringLiteral(0, 0, "One")
        val SL_TWO = StringLiteral(0, 0, "Two")

        // Identifiers
        val IDENT_F64_F = Identifier("f#", F64.INSTANCE)
        val IDENT_F64_G = Identifier("g#", F64.INSTANCE)
        val IDENT_F64_X = Identifier("x", F64.INSTANCE)
        val IDENT_I64_A = Identifier("a%", I64.INSTANCE)
        val IDENT_I64_B = Identifier("b%", I64.INSTANCE)
        val IDENT_I64_H = Identifier("h%", I64.INSTANCE)
        val IDENT_I64_FOO = Identifier("foo", I64.INSTANCE)
        val IDENT_STR_B = Identifier("b$", Str.INSTANCE)
        val IDENT_STR_S = Identifier("s$", Str.INSTANCE)
        val IDENT_STR_X = Identifier("x", Str.INSTANCE)
        val PARAM_I64_PA = Parameter("pa", I64.INSTANCE, "hl_pa")
        val PARAM_I64_PB = Parameter("pb", I64.INSTANCE, "hl_pb")

        // Identifier references
        val INE_F64_F = IdentifierNameExpression(0, 0, IDENT_F64_F)
        val INE_F64_G = IdentifierNameExpression(0, 0, IDENT_F64_G)
        val INE_I64_A = IdentifierNameExpression(0, 0, IDENT_I64_A)
        val INE_I64_B = IdentifierNameExpression(0, 0, IDENT_I64_B)
        val INE_I64_H = IdentifierNameExpression(0, 0, IDENT_I64_H)
        val INE_STR_B = IdentifierNameExpression(0, 0, IDENT_STR_B)
        val INE_STR_S = IdentifierNameExpression(0, 0, IDENT_STR_S)

        val IDE_I64_A = IdentifierDerefExpression(0, 0, IDENT_I64_A)
        val IDE_I64_B = IdentifierDerefExpression(0, 0, IDENT_I64_B)
        val IDE_I64_H = IdentifierDerefExpression(0, 0, IDENT_I64_H)
        val IDE_F64_F = IdentifierDerefExpression(0, 0, IDENT_F64_F)
        val IDE_F64_X = IdentifierDerefExpression(0, 0, IDENT_F64_X)
        val IDE_STR_B = IdentifierDerefExpression(0, 0, IDENT_STR_B)
        val IDE_STR_S = IdentifierDerefExpression(0, 0, IDENT_STR_S)
        val IDE_STR_X = IdentifierDerefExpression(0, 0, IDENT_STR_X)
        val IDE_I64_PA = IdentifierDerefExpression(0, 0, PARAM_I64_PA)
        val IDE_I64_PB = IdentifierDerefExpression(0, 0, PARAM_I64_PB)

        // Function types
        val FUN_F64_TO_F64: Fun = Fun.from(listOf(F64.INSTANCE), F64.INSTANCE)
        val FUN_F64_TO_I64: Fun = Fun.from(listOf(F64.INSTANCE), I64.INSTANCE)
        val FUN_I64_F64_I64_F64_I64_F64_TO_F64: Fun = Fun.from(listOf(I64.INSTANCE, F64.INSTANCE, I64.INSTANCE, F64.INSTANCE, I64.INSTANCE, F64.INSTANCE), F64.INSTANCE)
        val FUN_I64_F64_TO_F64: Fun = Fun.from(listOf(I64.INSTANCE, F64.INSTANCE), F64.INSTANCE)
        val FUN_I64_F64_I64_TO_F64: Fun = Fun.from(listOf(I64.INSTANCE, F64.INSTANCE, I64.INSTANCE), F64.INSTANCE)
        val FUN_I64_TO_I64: Fun = Fun.from(listOf(I64.INSTANCE), I64.INSTANCE)
        val FUN_I64_TO_STR: Fun = Fun.from(listOf(I64.INSTANCE), Str.INSTANCE)
        val FUN_STR_TO_STR: Fun = Fun.from(listOf(Str.INSTANCE), Str.INSTANCE)
        val FUN_TO_F64: Fun = Fun.from(listOf(), F64.INSTANCE)
        val FUN_TO_I64: Fun = Fun.from(listOf(), I64.INSTANCE)
        val FUN_TO_STR: Fun = Fun.from(listOf(), Str.INSTANCE)

        val FUN_FOO = LibraryFunction("foo", listOf(I64.INSTANCE, I64.INSTANCE, I64.INSTANCE), I64.INSTANCE, LIB_LIBC, ExternalFunction("fooo"))
        val FUN_FLO = LibraryFunction("flo", listOf(F64.INSTANCE, F64.INSTANCE, F64.INSTANCE), F64.INSTANCE, LIB_LIBC, ExternalFunction("floo"))

        // Function identifiers
        val IDENT_FUN_FOO: Identifier = FUN_FOO.identifier
        val IDENT_FUN_FLO: Identifier = FUN_FLO.identifier
        val IDENT_FUN_BAR_I64 = Identifier("bar%", FUN_TO_I64) // bar%(...) : I64
        val IDENT_FUN_FOO_F64 = Identifier("foo", FUN_TO_F64) // foo(...) : F64
        val IDENT_FUN_FNFOO_F64 = Identifier("FNfoo", FUN_F64_TO_F64) // FNfoo(F64) : F64
        val IDENT_FUN_COMMAND_STR = Identifier("command$", FUN_TO_STR) // command$(...) : Str

        val ERROR_LISTENER = object : BaseErrorListener() {
            override fun syntaxError(recognizer: Recognizer<*, *>, offendingSymbol: Any?, line: Int, charPositionInLine: Int, msg: String, e: RecognitionException?) {
                throw IllegalStateException("Syntax error at $line:$charPositionInLine: $msg", e)
            }
        }
    }
}
