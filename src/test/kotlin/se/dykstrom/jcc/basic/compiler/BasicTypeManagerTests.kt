/*
 * Copyright (C) 2016 Johan Dykstrom
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

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import se.dykstrom.jcc.basic.functions.BasicBuiltInFunctions.FUN_ABS
import se.dykstrom.jcc.basic.functions.BasicBuiltInFunctions.FUN_FMOD
import se.dykstrom.jcc.common.ast.*
import se.dykstrom.jcc.common.error.SemanticsException
import se.dykstrom.jcc.common.functions.ExternalFunction
import se.dykstrom.jcc.common.functions.LibraryFunction
import se.dykstrom.jcc.common.symbols.SymbolTable
import se.dykstrom.jcc.common.types.*
import java.util.Collections.emptyList

class BasicTypeManagerTests {

    private val symbols = SymbolTable()

    private val testee = BasicTypeManager()

    @Before
    fun setUp() {
        // Define some functions for testing
        symbols.addFunction(FUN_ABS)
        symbols.addFunction(FUN_FMOD)
        symbols.addFunction(FUN_COMMAND)
        symbols.addFunction(FUN_SIN)
        symbols.addFunction(FUN_THREE)
        // Function 'sum' is overloaded with different number of arguments
        symbols.addFunction(FUN_SUM_1)
        symbols.addFunction(FUN_SUM_2)
        symbols.addFunction(FUN_SUM_3)
        // Function 'foo' is overloaded with different types of arguments
        symbols.addFunction(FUN_FOO_DI)
        symbols.addFunction(FUN_FOO_ID)
    }

    @Test
    fun shouldBeAssignableFrom() {
        // You can assign any basic type to itself
        assertTrue(testee.isAssignableFrom(F64.INSTANCE, F64.INSTANCE))
        assertTrue(testee.isAssignableFrom(I64.INSTANCE, I64.INSTANCE))
        assertTrue(testee.isAssignableFrom(Str.INSTANCE, Str.INSTANCE))
        assertTrue(testee.isAssignableFrom(Bool.INSTANCE, Bool.INSTANCE))

        // You can assign an integer to a float
        assertTrue(testee.isAssignableFrom(F64.INSTANCE, I64.INSTANCE))

        // You can assign any basic type to an unknown
        assertTrue(testee.isAssignableFrom(Unknown.INSTANCE, F64.INSTANCE))
        assertTrue(testee.isAssignableFrom(Unknown.INSTANCE, I64.INSTANCE))
        assertTrue(testee.isAssignableFrom(Unknown.INSTANCE, Str.INSTANCE))
        assertTrue(testee.isAssignableFrom(Unknown.INSTANCE, Bool.INSTANCE))
    }

    @Test
    fun shouldNotBeAssignableFrom() {
        assertFalse(testee.isAssignableFrom(Bool.INSTANCE, ID_FUN_BOOLEAN.type))
        assertFalse(testee.isAssignableFrom(Bool.INSTANCE, F64.INSTANCE))
        assertFalse(testee.isAssignableFrom(Bool.INSTANCE, I64.INSTANCE))
        assertFalse(testee.isAssignableFrom(Bool.INSTANCE, Str.INSTANCE))
        assertFalse(testee.isAssignableFrom(F64.INSTANCE, Bool.INSTANCE))
        assertFalse(testee.isAssignableFrom(F64.INSTANCE, ID_FUN_INTEGER.type))
        assertFalse(testee.isAssignableFrom(F64.INSTANCE, Str.INSTANCE))
        assertFalse(testee.isAssignableFrom(I64.INSTANCE, Bool.INSTANCE))
        assertFalse(testee.isAssignableFrom(I64.INSTANCE, F64.INSTANCE))
        assertFalse(testee.isAssignableFrom(I64.INSTANCE, ID_FUN_INTEGER.type))
        assertFalse(testee.isAssignableFrom(I64.INSTANCE, Str.INSTANCE))
        assertFalse(testee.isAssignableFrom(Str.INSTANCE, Bool.INSTANCE))
        assertFalse(testee.isAssignableFrom(Str.INSTANCE, F64.INSTANCE))
        assertFalse(testee.isAssignableFrom(Str.INSTANCE, ID_FUN_STRING.type))
        assertFalse(testee.isAssignableFrom(Str.INSTANCE, I64.INSTANCE))
        assertFalse(testee.isAssignableFrom(Unknown.INSTANCE, Unknown.INSTANCE))
        assertFalse(testee.isAssignableFrom(Unknown.INSTANCE, ID_FUN_INTEGER.type))
    }

    // Type names:

    @Test
    fun shouldGetTypeName() {
        assertEquals("boolean", testee.getTypeName(Bool.INSTANCE))
        assertEquals("double", testee.getTypeName(F64.INSTANCE))
        assertEquals("integer", testee.getTypeName(I64.INSTANCE))
        assertEquals("string", testee.getTypeName(Str.INSTANCE))
        // Functions
        assertEquals("function()->boolean", testee.getTypeName(ID_FUN_BOOLEAN.type))
        assertEquals("function(integer)->double", testee.getTypeName(ID_FUN_FLOAT.type))
        assertEquals("function(string)->integer", testee.getTypeName(ID_FUN_INTEGER.type))
        assertEquals("function(integer, boolean)->string", testee.getTypeName(ID_FUN_STRING.type))
    }

    // Literals:

    @Test
    fun shouldGetStringFromStringLiteral() {
        assertEquals(Str.INSTANCE, testee.getType(STRING_LITERAL))
    }

    @Test
    fun shouldGetFloatFromFloatLiteral() {
        assertEquals(F64.INSTANCE, testee.getType(FLOAT_LITERAL))
    }

    @Test
    fun shouldGetIntegerFromIntegerLiteral() {
        assertEquals(I64.INSTANCE, testee.getType(INTEGER_LITERAL))
    }

    @Test
    fun shouldGetBooleanFromBooleanLiteral() {
        assertEquals(Bool.INSTANCE, testee.getType(BOOLEAN_LITERAL))
    }

    // Identifiers:

    @Test
    fun shouldGetStringFromStringIdentExpr() {
        assertEquals(Str.INSTANCE, testee.getType(STRING_IDE))
    }

    @Test
    fun shouldGetFloatFromFloatIdentExpr() {
        assertEquals(F64.INSTANCE, testee.getType(FLOAT_IDE))
    }

    @Test
    fun shouldGetIntegerFromIntegerIdentExpr() {
        assertEquals(I64.INSTANCE, testee.getType(INTEGER_IDE))
    }

    @Test
    fun shouldGetBooleanFromBooleanIdentExpr() {
        assertEquals(Bool.INSTANCE, testee.getType(BOOLEAN_IDE))
    }

    // Expressions:

    @Test
    fun testAddStrings() {
        assertEquals(Str.INSTANCE, testee.getType(ADD_STRINGS))
    }

    @Test
    fun testAddFloats() {
        assertEquals(F64.INSTANCE, testee.getType(ADD_FLOATS))
    }

    @Test
    fun testAddIntegers() {
        assertEquals(I64.INSTANCE, testee.getType(ADD_INTEGERS))
    }

    @Test
    fun testAddIntegersComplex() {
        assertEquals(I64.INSTANCE, testee.getType(ADD_INTEGERS_COMPLEX))
    }

    @Test
    fun testSubIntegers() {
        assertEquals(I64.INSTANCE, testee.getType(SUB_INTEGERS))
    }

    @Test
    fun testSubIntegersComplex() {
        assertEquals(I64.INSTANCE, testee.getType(SUB_INTEGERS_COMPLEX))
    }

    @Test
    fun shouldGetFloatFromDivision() {
        assertEquals(F64.INSTANCE, testee.getType(DIV_INTEGERS))
    }

    @Test
    fun shouldGetIntegerFromIntegerDivision() {
        assertEquals(I64.INSTANCE, testee.getType(IDIV_INTEGERS))
    }

    @Test
    fun shouldGetIntegerFromModulo() {
        assertEquals(I64.INSTANCE, testee.getType(MOD_INTEGERS))
    }

    @Test
    fun shouldGetBooleanFromAnd() {
        assertEquals(Bool.INSTANCE, testee.getType(AND_BOOLEANS))
    }

    @Test
    fun shouldGetBooleanFromComplexAnd() {
        assertEquals(Bool.INSTANCE, testee.getType(AND_BOOLEANS_COMPLEX))
    }

    @Test
    fun shouldGetBooleanFromIntegerRelational() {
        assertEquals(Bool.INSTANCE, testee.getType(REL_INTEGERS))
    }

    @Test
    fun shouldGetBooleanFromStringRelational() {
        assertEquals(Bool.INSTANCE, testee.getType(REL_STRINGS))
    }

    @Test
    fun shouldGetBooleanFromComplexRelational() {
        assertEquals(Bool.INSTANCE, testee.getType(REL_COMPLEX))
    }

    @Test
    fun shouldGetBooleanFromBooleanFunction() {
        assertEquals(Bool.INSTANCE, testee.getType(BOOLEAN_FCE))
    }

    @Test
    fun shouldGetFloatFromFloatFunction() {
        assertEquals(F64.INSTANCE, testee.getType(FLOAT_FCE))
    }

    @Test
    fun shouldGetIntegerFromIntegerFunction() {
        assertEquals(I64.INSTANCE, testee.getType(INTEGER_FCE))
    }

    @Test
    fun shouldGetStringFromStringFunction() {
        assertEquals(Str.INSTANCE, testee.getType(STRING_FCE))
    }

    @Test
    fun shouldGetIntegerFromAddIntegerWithFunction() {
        assertEquals(I64.INSTANCE, testee.getType(ADD_INTEGERS_FUNCTION))
    }

    // Type conversion integer -> float

    @Test
    fun shouldGetFloatFromAddIntegerFloat() {
        assertEquals(F64.INSTANCE, testee.getType(ADD_INTEGER_FLOAT))
    }

    @Test
    fun shouldGetFloatFromAddFloatInteger() {
        assertEquals(F64.INSTANCE, testee.getType(ADD_FLOAT_INTEGER))
    }

    // Resolving functions

    @Test
    fun shouldResolveFunctionWithExactArgs() {
        assertEquals(FUN_ABS, testee.resolveFunction(FUN_ABS.name, FUN_ABS.argTypes, symbols))
        assertEquals(FUN_FMOD, testee.resolveFunction(FUN_FMOD.name, FUN_FMOD.argTypes, symbols))
        assertEquals(FUN_COMMAND, testee.resolveFunction(FUN_COMMAND.name, FUN_COMMAND.argTypes, symbols))
        assertEquals(FUN_SUM_1, testee.resolveFunction("sum", FUN_SUM_1.argTypes, symbols))
        assertEquals(FUN_SUM_2, testee.resolveFunction("sum", FUN_SUM_2.argTypes, symbols))
        assertEquals(FUN_SUM_3, testee.resolveFunction("sum", FUN_SUM_3.argTypes, symbols))
        assertEquals(FUN_FOO_DI, testee.resolveFunction("foo", FUN_FOO_DI.argTypes, symbols))
        assertEquals(FUN_FOO_ID, testee.resolveFunction("foo", FUN_FOO_ID.argTypes, symbols))
    }

    @Test
    fun shouldResolveFunctionWithOneCast() {
        assertEquals(FUN_SIN, testee.resolveFunction(FUN_SIN.name, listOf(I64.INSTANCE), symbols))
    }

    @Test
    fun shouldResolveFunctionWithTwoCasts() {
        assertEquals(FUN_FMOD, testee.resolveFunction(FUN_FMOD.name, listOf(I64.INSTANCE, I64.INSTANCE), symbols))
        assertEquals(FUN_THREE, testee.resolveFunction(FUN_THREE.name, listOf(I64.INSTANCE, I64.INSTANCE, I64.INSTANCE), symbols))
    }

    // Negative tests:

    @Test(expected = SemanticsException::class)
    fun shouldNotResolveIntFunctionWithFloat() {
        testee.resolveFunction(FUN_ABS.name, listOf(F64.INSTANCE), symbols)
    }

    @Test(expected = SemanticsException::class)
    fun shouldNotResolveFloatFloatFunctionWithFloatString() {
        testee.resolveFunction(FUN_FMOD.name, listOf(F64.INSTANCE, Str.INSTANCE), symbols)
    }

    @Test(expected = SemanticsException::class)
    fun shouldNotResolveFunctionWithAmbiguousOverload() {
        testee.resolveFunction("foo", listOf(I64.INSTANCE, I64.INSTANCE), symbols)
    }

    @Test(expected = SemanticsException::class)
    fun testAddStringFloat() {
        testee.getType(ADD_STRING_FLOAT)
    }

    @Test(expected = SemanticsException::class)
    fun testAddStringInteger() {
        testee.getType(ADD_STRING_INTEGER)
    }

    @Test(expected = SemanticsException::class)
    fun testAddIntegerString() {
        testee.getType(ADD_INTEGER_STRING)
    }

    @Test(expected = SemanticsException::class)
    fun testSubString() {
        testee.getType(SUB_STRINGS)
    }

    @Test(expected = SemanticsException::class)
    fun testSubStringInteger() {
        testee.getType(SUB_STRING_INTEGER)
    }

    @Test(expected = SemanticsException::class)
    fun testSubBooleanInteger() {
        testee.getType(SUB_BOOLEAN_INTEGER)
    }

    @Test(expected = SemanticsException::class)
    fun shouldGetExceptionFromIDivStringInteger() {
        testee.getType(IDIV_STRING_INTEGER)
    }

    @Test(expected = SemanticsException::class)
    fun shouldGetExceptionFromModStringInteger() {
        testee.getType(MOD_STRING_INTEGER)
    }

    companion object {

        private val ID_BOOLEAN = Identifier("boolean", Bool.INSTANCE)
        private val ID_INTEGER = Identifier("integer", I64.INSTANCE)
        private val ID_FLOAT = Identifier("float", F64.INSTANCE)
        private val ID_STRING = Identifier("string", Str.INSTANCE)

        private val FUN_COMMAND = LibraryFunction("command$", emptyList(), Str.INSTANCE, null, ExternalFunction(""))
        private val FUN_SIN = LibraryFunction("sin", listOf(F64.INSTANCE), F64.INSTANCE, null, ExternalFunction(""))
        private val FUN_SUM_1 = LibraryFunction("sum", listOf(I64.INSTANCE), I64.INSTANCE, null, ExternalFunction(""))
        private val FUN_SUM_2 = LibraryFunction("sum", listOf(I64.INSTANCE, I64.INSTANCE), I64.INSTANCE, null, ExternalFunction(""))
        private val FUN_SUM_3 = LibraryFunction("sum", listOf(I64.INSTANCE, I64.INSTANCE, I64.INSTANCE), I64.INSTANCE, null, ExternalFunction(""))
        private val FUN_FOO_DI = LibraryFunction("foo", listOf(F64.INSTANCE, I64.INSTANCE), I64.INSTANCE, null, ExternalFunction(""))
        private val FUN_FOO_ID = LibraryFunction("foo", listOf(I64.INSTANCE, F64.INSTANCE), I64.INSTANCE, null, ExternalFunction(""))
        private val FUN_THREE = LibraryFunction("three", listOf(F64.INSTANCE, I64.INSTANCE, F64.INSTANCE), I64.INSTANCE, null, ExternalFunction(""))

        private val ID_FUN_BOOLEAN = Identifier("booleanf", Fun.from(emptyList(), Bool.INSTANCE))
        private val ID_FUN_FLOAT = Identifier("floatf", Fun.from(listOf(I64.INSTANCE), F64.INSTANCE))
        private val ID_FUN_INTEGER = Identifier("integerf", Fun.from(listOf(Str.INSTANCE), I64.INSTANCE))
        private val ID_FUN_STRING = Identifier("stringf", Fun.from(listOf(I64.INSTANCE, Bool.INSTANCE), Str.INSTANCE))

        private val BOOLEAN_LITERAL = BooleanLiteral(0, 0, "true")
        private val FLOAT_LITERAL = FloatLiteral(0, 0, "5.7")
        private val INTEGER_LITERAL = IntegerLiteral(0, 0, "5")
        private val STRING_LITERAL = StringLiteral(0, 0, "value")

        private val BOOLEAN_IDE = IdentifierDerefExpression(0, 0, ID_BOOLEAN)
        private val FLOAT_IDE = IdentifierDerefExpression(0, 0, ID_FLOAT)
        private val INTEGER_IDE = IdentifierDerefExpression(0, 0, ID_INTEGER)
        private val STRING_IDE = IdentifierDerefExpression(0, 0, ID_STRING)

        private val BOOLEAN_FCE = FunctionCallExpression(0, 0, ID_FUN_BOOLEAN, emptyList())
        private val FLOAT_FCE = FunctionCallExpression(0, 0, ID_FUN_FLOAT, emptyList())
        private val INTEGER_FCE = FunctionCallExpression(0, 0, ID_FUN_INTEGER, emptyList())
        private val STRING_FCE = FunctionCallExpression(0, 0, ID_FUN_STRING, emptyList())

        private val ADD_FLOATS = AddExpression(0, 0, FLOAT_LITERAL, FLOAT_IDE)

        private val ADD_INTEGERS = AddExpression(0, 0, INTEGER_LITERAL, INTEGER_IDE)
        private val ADD_INTEGERS_COMPLEX = AddExpression(0, 0, INTEGER_LITERAL, AddExpression(0, 0, INTEGER_IDE, INTEGER_IDE))
        private val ADD_INTEGERS_FUNCTION = AddExpression(0, 0, INTEGER_FCE, AddExpression(0, 0, INTEGER_IDE, INTEGER_FCE))

        private val ADD_STRINGS = AddExpression(0, 0, STRING_LITERAL, STRING_IDE)

        private val ADD_FLOAT_INTEGER = AddExpression(0, 0, FLOAT_LITERAL, INTEGER_LITERAL)
        private val ADD_INTEGER_FLOAT = AddExpression(0, 0, INTEGER_LITERAL, FLOAT_LITERAL)
        private val ADD_STRING_INTEGER = AddExpression(0, 0, STRING_LITERAL, INTEGER_LITERAL)
        private val ADD_STRING_FLOAT = AddExpression(0, 0, STRING_LITERAL, FLOAT_LITERAL)
        private val ADD_INTEGER_STRING = AddExpression(0, 0, INTEGER_LITERAL, STRING_LITERAL)

        private val SUB_INTEGERS = SubExpression(0, 0, INTEGER_LITERAL, INTEGER_IDE)
        private val SUB_INTEGERS_COMPLEX = SubExpression(0, 0, INTEGER_LITERAL, SubExpression(0, 0, INTEGER_IDE, INTEGER_IDE))

        private val SUB_STRINGS = SubExpression(0, 0, STRING_IDE, STRING_LITERAL)
        private val SUB_STRING_INTEGER = SubExpression(0, 0, STRING_IDE, INTEGER_IDE)
        private val SUB_BOOLEAN_INTEGER = SubExpression(0, 0, BOOLEAN_IDE, INTEGER_IDE)

        private val DIV_INTEGERS = DivExpression(0, 0, INTEGER_LITERAL, INTEGER_IDE)

        private val IDIV_INTEGERS = IDivExpression(0, 0, INTEGER_LITERAL, INTEGER_IDE)
        private val IDIV_STRING_INTEGER = IDivExpression(0, 0, STRING_IDE, INTEGER_IDE)

        private val MOD_INTEGERS = ModExpression(0, 0, INTEGER_LITERAL, INTEGER_IDE)
        private val MOD_STRING_INTEGER = ModExpression(0, 0, STRING_IDE, INTEGER_IDE)

        private val AND_BOOLEANS = AndExpression(0, 0, BOOLEAN_LITERAL, BOOLEAN_IDE)
        private val AND_BOOLEANS_COMPLEX = AndExpression(0, 0, BOOLEAN_LITERAL, AndExpression(0, 0, BOOLEAN_IDE, BOOLEAN_IDE))

        private val REL_INTEGERS = EqualExpression(0, 0, INTEGER_IDE, INTEGER_LITERAL)
        private val REL_STRINGS = NotEqualExpression(0, 0, STRING_IDE, STRING_LITERAL)
        private val REL_COMPLEX = AndExpression(0, 0, EqualExpression(0, 0, INTEGER_IDE, INTEGER_LITERAL), BOOLEAN_IDE)
    }
}
