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
