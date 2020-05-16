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

package se.dykstrom.jcc.basic.compiler

import org.junit.Test
import se.dykstrom.jcc.basic.ast.PrintStatement
import se.dykstrom.jcc.common.ast.ArrayAccessExpression
import se.dykstrom.jcc.common.ast.IdentifierDerefExpression
import se.dykstrom.jcc.common.types.F64
import se.dykstrom.jcc.common.types.I64
import se.dykstrom.jcc.common.types.Str
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests class `BasicSemanticsParser`, especially functionality related to arrays.
 *
 * @author Johan Dykstrom
 * @see BasicSemanticsParser
 */
class BasicSemanticsParserArrayTests : AbstractBasicSemanticsParserTests() {

    @Test
    fun shouldParseSingleDimensionStaticDim() {
        parse("dim a(10) as integer")
        parse("dim foo(1) as boolean")
        parse("dim bar(999) as string")
        parse("dim axe(9) as double")
    }

    @Test
    fun shouldParseDimWithTypeSpecifier() {
        parse("dim a%(10) as integer")
        parse("dim bar$(999) as string")
        parse("dim foo#(99) as double")
    }

    @Test
    fun shouldParseMultiDimensionStaticDim() {
        parse("dim two(3, 4) as string")
        parse("dim three(10, 10, 10) as integer")
        parse("dim ten(1, 2, 3, 4, 5, 6, 7, 8, 9, 10) as boolean")
    }

    @Test
    fun arraysAndScalarsShouldHaveDifferentNameSpaces() {
        parse("dim a(10) as integer : dim a as string")
        parse("dim foo(10) as integer, foo as boolean")
    }

    @Test
    fun shouldParseSingleDimensionArrayAccess() {
        val program = parse("dim a%(10) as integer : print a%(1)")
        val printStatement = program.statements[1] as PrintStatement
        val arrayAccessExpression = printStatement.expressions[0] as ArrayAccessExpression
        assertEquals("a%", arrayAccessExpression.identifier.name)
        assertEquals(I64.INSTANCE, arrayAccessExpression.type)
        assertEquals(IL_1, arrayAccessExpression.subscripts[0])
    }

    @Test
    fun shouldParseStringArrayAccess() {
        val program = parse("dim foo(100) as string : print foo(0)")
        val printStatement = program.statements[1] as PrintStatement
        val arrayAccessExpression = printStatement.expressions[0] as ArrayAccessExpression
        assertEquals("foo", arrayAccessExpression.identifier.name)
        assertEquals(Str.INSTANCE, arrayAccessExpression.type)
        assertEquals(IL_0, arrayAccessExpression.subscripts[0])
    }

    @Test
    fun shouldParseMultiDimensionArrayAccess() {
        val program = parse("dim foo(10, 10) as string : print foo(1, 2)")
        val printStatement = program.statements[1] as PrintStatement
        val arrayAccessExpression = printStatement.expressions[0] as ArrayAccessExpression
        assertEquals("foo", arrayAccessExpression.identifier.name)
        assertEquals(Str.INSTANCE, arrayAccessExpression.type)
        assertEquals(2, arrayAccessExpression.subscripts.size)
        assertEquals(IL_1, arrayAccessExpression.subscripts[0])
        assertEquals(IL_2, arrayAccessExpression.subscripts[1])
    }

    @Test
    fun shouldParseArrayAccessWithExpressionSubscripts() {
        val program = parse("dim index as integer : dim array(10, 10) as double : print array(index - 3, 0 * 8 + 1)")
        val printStatement = program.statements[2] as PrintStatement
        val arrayAccessExpression = printStatement.expressions[0] as ArrayAccessExpression
        assertEquals("array", arrayAccessExpression.identifier.name)
        assertEquals(F64.INSTANCE, arrayAccessExpression.type)
        assertEquals(2, arrayAccessExpression.subscripts.size)
        assertEquals(I64.INSTANCE, semanticsParser.typeManager().getType(arrayAccessExpression.subscripts[0]))
        assertEquals(I64.INSTANCE, semanticsParser.typeManager().getType(arrayAccessExpression.subscripts[1]))
    }

    @Test
    fun shouldParseNestedArrayAccess() {
        val program = parse("dim index as integer : dim values(10) as integer : print values(values(index))")
        val printStatement = program.statements[2] as PrintStatement
        val arrayAccessExpression = printStatement.expressions[0] as ArrayAccessExpression
        assertEquals("values", arrayAccessExpression.identifier.name)
        assertEquals(I64.INSTANCE, arrayAccessExpression.type)
        assertEquals(1, arrayAccessExpression.subscripts.size)
        val nestedExpression = arrayAccessExpression.subscripts[0] as ArrayAccessExpression
        assertEquals("values", nestedExpression.identifier.name)
        assertEquals(I64.INSTANCE, nestedExpression.type)
        assertEquals(1, nestedExpression.subscripts.size)
        assertTrue(nestedExpression.subscripts[0] is IdentifierDerefExpression)
    }

    @Test
    fun shouldNotParseDimWithInvalidType() {
        parseAndExpectException("dim a$(1) as integer", "type specifier string and type integer")
        parseAndExpectException("dim a%(1) as double", "type specifier integer and type double")
        parseAndExpectException("dim a#(1) as string", "type specifier double and type string")
    }

    @Test
    fun shouldNotParseRedefinitionOfArray() {
        parseAndExpectException("dim a(1) as integer : dim a(1) as integer", "variable 'a' is already defined")
    }

    @Test
    fun shouldNotParseNonIntegerSubscripts() {
        parseAndExpectException("dim a(1.7) as integer", "array 'a' has non-integer")
        parseAndExpectException("dim b(true) as integer", "array 'b' has non-integer")
        parseAndExpectException("dim c(\"foo\") as integer", "array 'c' has non-integer")
        parseAndExpectException("dim d(1 / 7) as integer", "array 'd' has non-integer")
    }

    /**
     * $DYNAMIC arrays are not implemented yet.
     */
    @Test
    fun shouldNotAcceptDynamicArrays() {
        parseAndExpectException("dim a as integer : dim foo(a) as integer", "\$DYNAMIC arrays not supported yet")
        parseAndExpectException("dim b as integer : dim foo(7, 1 + b) as integer", "\$DYNAMIC arrays not supported yet")
    }
}
