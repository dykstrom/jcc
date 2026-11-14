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

package se.dykstrom.jcc.common.semantics.expression

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import se.dykstrom.jcc.common.ast.AstProgram
import se.dykstrom.jcc.common.ast.Expression
import se.dykstrom.jcc.common.ast.IntegerLiteral
import se.dykstrom.jcc.common.compiler.AbstractSemanticsParser
import se.dykstrom.jcc.common.compiler.AbstractTypeManager
import se.dykstrom.jcc.common.compiler.TypeManager
import se.dykstrom.jcc.common.error.CompilationErrorListener
import se.dykstrom.jcc.common.symbols.SymbolTable
import se.dykstrom.jcc.common.types.I128
import se.dykstrom.jcc.common.types.I32
import se.dykstrom.jcc.common.types.I64
import se.dykstrom.jcc.common.types.Type

/**
 * Tests the range check of [IntegerSemanticsParser], which depends on the width of the literal's type.
 */
class IntegerSemanticsParserTests {

    private val errorListener = CompilationErrorListener()

    private val parser = object : AbstractSemanticsParser<TypeManager>(errorListener, SymbolTable(), TYPE_MANAGER) {
        override fun parse(program: AstProgram) = program

        override fun expression(expression: Expression) = expression
    }

    @Test
    fun shouldAcceptLimitsOfEachWidth() {
        listOf(
            I32.INSTANCE to "2147483647",
            I32.INSTANCE to "-2147483648",
            I64.INSTANCE to "9223372036854775807",
            I64.INSTANCE to "-9223372036854775808",
            I128.INSTANCE to I128_MAX,
            I128.INSTANCE to I128_MIN,
        ).forEach { (type, value) ->
            val literal = IntegerLiteral(0, 0, value, type)
            assertSame(literal, IntegerSemanticsParser(parser).parse(literal))
        }
        assertEquals(listOf<String>(), errorListener.errors.map { it.msg })
    }

    @Test
    fun shouldReportValueOutsideWidth() {
        listOf(
            I32.INSTANCE to "2147483648",
            I32.INSTANCE to "-2147483649",
            I64.INSTANCE to "9223372036854775808",
            I64.INSTANCE to "-9223372036854775809",
            I128.INSTANCE to "170141183460469231731687303715884105728",
            I128.INSTANCE to "-170141183460469231731687303715884105729",
        ).forEach { (type, value) ->
            errorListener.errors.clear()
            IntegerSemanticsParser(parser).parse(IntegerLiteral(0, 0, value, type))
            assertEquals(listOf("integer out of range: $value"), errorListener.errors.map { it.msg })
        }
    }

    companion object {
        private const val I128_MAX = "170141183460469231731687303715884105727"
        private const val I128_MIN = "-170141183460469231731687303715884105728"

        private val TYPE_MANAGER = object : AbstractTypeManager() {
            override fun getTypeName(type: Type): String = type.javaClass.simpleName

            override fun isKnownAssignableFrom(thisType: Type, thatType: Type): Boolean = thisType == thatType
        }
    }
}
