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

package se.dykstrom.jcc.col.compiler

import org.junit.jupiter.api.Assertions.assertEquals
import se.dykstrom.jcc.col.types.NamedType
import se.dykstrom.jcc.common.ast.*
import se.dykstrom.jcc.common.functions.ExternalFunction
import se.dykstrom.jcc.common.functions.LibraryFunction
import se.dykstrom.jcc.common.types.F64
import se.dykstrom.jcc.common.types.Fun
import se.dykstrom.jcc.common.types.I64
import se.dykstrom.jcc.common.types.Identifier
import java.nio.file.Path

@Suppress("MemberVisibilityCanBePrivate")
class ColTests {

    companion object {

        fun verify(program: Program, vararg statements: Statement) {
            assertEquals(statements.size, program.statements.size)
            for ((index, statement) in statements.withIndex()) {
                assertEquals(statement, program.statements[index])
            }
        }

        // Literals
        val IL_5 = IntegerLiteral(0, 0, 5)
        val IL_17 = IntegerLiteral(0, 0, 17)
        val IL_18 = IntegerLiteral(0, 0, 18)
        val IL_1_000 = IntegerLiteral(0, 0, 1_000)
        val IL_M_1 = IntegerLiteral(0, 0, -1)

        val FL_1_0 = FloatLiteral(0, 0, "1.0")

        // Identifiers
        val IDENT_F64_F = Identifier("f", F64.INSTANCE)
        val IDENT_I64_A = Identifier("a", I64.INSTANCE)
        val IDENT_I64_B = Identifier("b", I64.INSTANCE)

        // Identifier references
        val IDE_F64_F = IdentifierDerefExpression(0, 0, IDENT_F64_F)
        val IDE_I64_A = IdentifierDerefExpression(0, 0, IDENT_I64_A)
        val IDE_I64_B = IdentifierDerefExpression(0, 0, IDENT_I64_B)
        val IDE_UNK_A = IdentifierDerefExpression(0, 0, Identifier("a", null))
        val IDE_UNK_B = IdentifierDerefExpression(0, 0, Identifier("b", null))

        // Types
        val NT_F64 = NamedType("f64")
        val NT_I64 = NamedType("i64")
        val NT_VOID = NamedType("void")

        // Function types
        val FUN_F64_TO_I64: Fun = Fun.from(listOf(F64.INSTANCE), I64.INSTANCE)
        val FUN_I64_TO_I64: Fun = Fun.from(listOf(I64.INSTANCE), I64.INSTANCE)
        val FUN_TO_F64: Fun = Fun.from(listOf(), F64.INSTANCE)
        val FUN_TO_I64: Fun = Fun.from(listOf(), I64.INSTANCE)

        // Functions
        val EXT_FUN_ABS64 = ExternalFunction("_abs64")
        val EXT_FUN_FOO = ExternalFunction("foo")
        val EXT_FUN_SUM = ExternalFunction("sum")

        val FUN_ABS = LibraryFunction("abs", listOf(I64.INSTANCE), I64.INSTANCE, "msvcrt.dll", EXT_FUN_ABS64)
        val FUN_SUM0 = LibraryFunction("sum", listOf(), I64.INSTANCE, "lib.dll", EXT_FUN_SUM)
        val FUN_SUM1 = LibraryFunction("sum", listOf(I64.INSTANCE), I64.INSTANCE, "lib.dll", EXT_FUN_SUM)
        val FUN_SUM2 = LibraryFunction("sum", listOf(I64.INSTANCE, I64.INSTANCE), I64.INSTANCE, "lib.dll", EXT_FUN_SUM)

        val SOURCE_PATH: Path = Path.of("file.col")
    }
}
