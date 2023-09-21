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

import se.dykstrom.jcc.common.ast.FloatLiteral
import se.dykstrom.jcc.common.ast.IntegerLiteral
import se.dykstrom.jcc.common.ast.Program
import se.dykstrom.jcc.common.ast.Statement
import se.dykstrom.jcc.common.functions.ExternalFunction
import se.dykstrom.jcc.common.functions.LibraryFunction
import se.dykstrom.jcc.common.types.I64
import java.nio.file.Path
import kotlin.test.assertEquals

class ColTests {

    companion object {

        fun verify(program: Program, vararg statements: Statement) {
            assertEquals(statements.size, program.statements.size)
            for ((index, statement) in statements.withIndex()) {
                assertEquals(statement, program.statements[index])
            }
        }

        val IL_5 = IntegerLiteral(0, 0, 5)
        val IL_17 = IntegerLiteral(0, 0, 17)
        val IL_18 = IntegerLiteral(0, 0, 18)
        val IL_1_000 = IntegerLiteral(0, 0, 1_000)
        val IL_M_1 = IntegerLiteral(0, 0, -1)

        val FL_1_0 = FloatLiteral(0, 0, "1.0")

        private val EXT_FUN_ABS64 = ExternalFunction("_abs64")
        private val EXT_FUN_SUM = ExternalFunction("sum")

        val FUN_ABS = LibraryFunction("abs", listOf(I64.INSTANCE), I64.INSTANCE, "msvcrt.dll", EXT_FUN_ABS64)
        val FUN_SUM0 = LibraryFunction("sum", listOf(), I64.INSTANCE, "lib.dll", EXT_FUN_SUM)
        val FUN_SUM1 = LibraryFunction("sum", listOf(I64.INSTANCE), I64.INSTANCE, "lib.dll", EXT_FUN_SUM)
        val FUN_SUM2 = LibraryFunction("sum", listOf(I64.INSTANCE, I64.INSTANCE), I64.INSTANCE, "lib.dll", EXT_FUN_SUM)

        val SOURCE_PATH: Path = Path.of("file.col")
    }
}
