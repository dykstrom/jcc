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
import se.dykstrom.jcc.basic.functions.BasicBuiltInFunctions.FUN_LBOUND
import se.dykstrom.jcc.common.ast.*
import se.dykstrom.jcc.common.error.SemanticsException
import se.dykstrom.jcc.common.functions.BuiltInFunctions.FUN_FMOD
import se.dykstrom.jcc.common.functions.ExternalFunction
import se.dykstrom.jcc.common.functions.LibraryFunction
import se.dykstrom.jcc.common.symbols.SymbolTable
import se.dykstrom.jcc.common.types.*
import java.util.Collections.emptyList

class BasicTypeManagerTests {

    private val symbols = SymbolTable()

    private val typeManager = BasicTypeManager()

    @Before
    fun setUp() {
        // Define some functions for testing
        symbols.addFunction(FUN_ABS)
        symbols.addFunction(FUN_FMOD)
        symbols.addFunction(FUN_COMMAND)
        symbols.addFunction(FUN_SIN)
        symbols.addFunction(FUN_THREE)
        // Function 'sum' is overloaded with different number of (and types of) arguments
        symbols.addFunction(FUN_SUM_F)
        symbols.addFunction(FUN_SUM_1)
        symbols.addFunction(FUN_SUM_2)
        symbols.addFunction(FUN_SUM_3)
        // Function 'foo' is overloaded with different types of arguments
        symbols.addFunction(FUN_FOO_DI)
        symbols.addFunction(FUN_FOO_ID)
        // Function 'lbound' takes a generic array as argument
        symbols.addFunction(FUN_LBOUND)
    }

    @Test
    fun shouldBeAssignableFrom() {
        // You can assign any basic type to itself
        assertTrue(typeManager.isAssignableFrom(F64.INSTANCE, F64.INSTANCE))
        assertTrue(typeManager.isAssignableFrom(I64.INSTANCE, I64.INSTANCE))
        assertTrue(typeManager.isAssignableFrom(Str.INSTANCE, Str.INSTANCE))

        // You can assign numeric values even if they are not exactly the same type
        assertTrue(typeManager.isAssignableFrom(F64.INSTANCE, I64.INSTANCE))
        assertTrue(typeManager.isAssignableFrom(I64.INSTANCE, F64.INSTANCE))

        // You can assign any array to the generic array type
        assertTrue(typeManager.isAssignableFrom(Arr.INSTANCE, Arr.from(1, I64.INSTANCE)))
        assertTrue(typeManager.isAssignableFrom(Arr.INSTANCE, Arr.from(5, Str.INSTANCE)))
    }

    @Test
    fun shouldNotBeAssignableFrom() {
        assertFalse(typeManager.isAssignableFrom(F64.INSTANCE, ID_FUN_INTEGER.type()))
        assertFalse(typeManager.isAssignableFrom(F64.INSTANCE, Str.INSTANCE))
        assertFalse(typeManager.isAssignableFrom(I64.INSTANCE, ID_FUN_INTEGER.type()))
        assertFalse(typeManager.isAssignableFrom(I64.INSTANCE, Str.INSTANCE))
        assertFalse(typeManager.isAssignableFrom(Str.INSTANCE, F64.INSTANCE))
        assertFalse(typeManager.isAssignableFrom(Str.INSTANCE, ID_FUN_STRING.type()))
        assertFalse(typeManager.isAssignableFrom(Str.INSTANCE, I64.INSTANCE))

        // You cannot assign the generic array type to any type of array
        assertFalse(typeManager.isAssignableFrom(Arr.from(5, Str.INSTANCE), Arr.INSTANCE))
        assertFalse(typeManager.isAssignableFrom(Arr.from(2, F64.INSTANCE), Arr.INSTANCE))

        // You cannot assign arrays of different types
        assertFalse(typeManager.isAssignableFrom(Arr.from(2, F64.INSTANCE), Arr.from(1, F64.INSTANCE)))
        assertFalse(typeManager.isAssignableFrom(Arr.from(2, F64.INSTANCE), Arr.from(2, I64.INSTANCE)))
    }

    // Type names:

    @Test
    fun shouldGetTypeNameOfScalarTypes() {
        assertEquals("double", typeManager.getTypeName(F64.INSTANCE))
        assertEquals("integer", typeManager.getTypeName(I64.INSTANCE))
        assertEquals("string", typeManager.getTypeName(Str.INSTANCE))
    }

    @Test
    fun shouldGetTypeNameOfFunctionTypes() {
        assertEquals("function(integer)->double", typeManager.getTypeName(ID_FUN_FLOAT.type()))
        assertEquals("function(string)->integer", typeManager.getTypeName(ID_FUN_INTEGER.type()))
        assertEquals("function(integer, double)->string", typeManager.getTypeName(ID_FUN_STRING.type()))
    }

    @Test
    fun shouldGetTypeNameOfArrayTypes() {
        assertEquals("double[]", typeManager.getTypeName(Arr.from(1, F64.INSTANCE)))
        assertEquals("integer[]", typeManager.getTypeName(Arr.from(1, I64.INSTANCE)))
        assertEquals("string[]", typeManager.getTypeName(Arr.from(1, Str.INSTANCE)))
        assertEquals("string[][]", typeManager.getTypeName(Arr.from(2, Str.INSTANCE)))
        assertEquals("integer[][][]", typeManager.getTypeName(Arr.from(3, I64.INSTANCE)))
        assertEquals("T[]", typeManager.getTypeName(Arr.INSTANCE))
    }

    // Literals:

    @Test
    fun shouldGetStringFromStringLiteral() {
        assertEquals(Str.INSTANCE, typeManager.getType(STRING_LITERAL))
    }

    @Test
    fun shouldGetFloatFromFloatLiteral() {
        assertEquals(F64.INSTANCE, typeManager.getType(FLOAT_LITERAL))
    }

    @Test
    fun shouldGetIntegerFromIntegerLiteral() {
        assertEquals(I64.INSTANCE, typeManager.getType(INTEGER_LITERAL))
    }

    // Identifiers:

    @Test
    fun shouldGetStringFromStringIdentExpr() {
        assertEquals(Str.INSTANCE, typeManager.getType(STRING_IDE))
    }

    @Test
    fun shouldGetFloatFromFloatIdentExpr() {
        assertEquals(F64.INSTANCE, typeManager.getType(FLOAT_IDE))
    }

    @Test
    fun shouldGetIntegerFromIntegerIdentExpr() {
        assertEquals(I64.INSTANCE, typeManager.getType(INTEGER_IDE))
    }

    // Expressions:

    @Test
    fun testAddStrings() {
        assertEquals(Str.INSTANCE, typeManager.getType(ADD_STRINGS))
    }

    @Test
    fun testAddFloats() {
        assertEquals(F64.INSTANCE, typeManager.getType(ADD_FLOATS))
    }

    @Test
    fun testAddIntegers() {
        assertEquals(I64.INSTANCE, typeManager.getType(ADD_INTEGERS))
    }

    @Test
    fun testAddIntegersComplex() {
        assertEquals(I64.INSTANCE, typeManager.getType(ADD_INTEGERS_COMPLEX))
    }

    @Test
    fun testSubIntegers() {
        assertEquals(I64.INSTANCE, typeManager.getType(SUB_INTEGERS))
    }

    @Test
    fun testSubIntegersComplex() {
        assertEquals(I64.INSTANCE, typeManager.getType(SUB_INTEGERS_COMPLEX))
    }

    @Test
    fun shouldGetFloatFromDivision() {
        assertEquals(F64.INSTANCE, typeManager.getType(DIV_INTEGERS))
    }

    @Test
    fun shouldGetIntegerFromIntegerDivision() {
        assertEquals(I64.INSTANCE, typeManager.getType(IDIV_INTEGERS))
    }

    @Test
    fun shouldGetIntegerFromModulo() {
        assertEquals(I64.INSTANCE, typeManager.getType(MOD_INTEGERS))
    }

    @Test
    fun shouldGetIntegerFromAnd() {
        assertEquals(I64.INSTANCE, typeManager.getType(AND_INTEGERS))
    }

    @Test
    fun shouldGetIntegerFromXor() {
        assertEquals(I64.INSTANCE, typeManager.getType(XOR_INTEGERS))
    }

    @Test
    fun shouldGetIntegerFromComplexAnd() {
        assertEquals(I64.INSTANCE, typeManager.getType(AND_INTEGERS_COMPLEX))
    }

    @Test
    fun shouldGetIntegerFromIntegerRelational() {
        assertEquals(I64.INSTANCE, typeManager.getType(REL_INTEGERS))
    }

    @Test
    fun shouldGetIntegerFromStringRelational() {
        assertEquals(I64.INSTANCE, typeManager.getType(REL_STRINGS))
    }

    @Test
    fun shouldGetIntegerFromComplexRelational() {
        assertEquals(I64.INSTANCE, typeManager.getType(REL_COMPLEX))
    }

    @Test
    fun shouldGetIntegerFromNegativeIntegerExpression() {
        assertEquals(I64.INSTANCE, typeManager.getType(NEG_INTEGER))
    }

    @Test
    fun shouldGetFloatFromNegativeFloatExpression() {
        assertEquals(F64.INSTANCE, typeManager.getType(NEG_FLOAT))
    }

    @Test
    fun shouldGetFloatFromFloatFunction() {
        assertEquals(F64.INSTANCE, typeManager.getType(FLOAT_FCE))
    }

    @Test
    fun shouldGetIntegerFromIntegerFunction() {
        assertEquals(I64.INSTANCE, typeManager.getType(INTEGER_FCE))
    }

    @Test
    fun shouldGetStringFromStringFunction() {
        assertEquals(Str.INSTANCE, typeManager.getType(STRING_FCE))
    }

    @Test
    fun shouldGetIntegerFromAddIntegerWithFunction() {
        assertEquals(I64.INSTANCE, typeManager.getType(ADD_INTEGERS_FUNCTION))
    }

    // Type conversion integer -> float

    @Test
    fun shouldGetFloatFromAddIntegerFloat() {
        assertEquals(F64.INSTANCE, typeManager.getType(ADD_INTEGER_FLOAT))
    }

    @Test
    fun shouldGetFloatFromAddFloatInteger() {
        assertEquals(F64.INSTANCE, typeManager.getType(ADD_FLOAT_INTEGER))
    }

    // Resolving functions

    @Test
    fun shouldResolveFunctionWithExactArgs() {
        assertEquals(FUN_ABS, typeManager.resolveFunction(FUN_ABS.name, FUN_ABS.argTypes, symbols))
        assertEquals(FUN_FMOD, typeManager.resolveFunction(FUN_FMOD.name, FUN_FMOD.argTypes, symbols))
        assertEquals(FUN_COMMAND, typeManager.resolveFunction(FUN_COMMAND.name, FUN_COMMAND.argTypes, symbols))
        assertEquals(FUN_SUM_F, typeManager.resolveFunction("sum", FUN_SUM_F.argTypes, symbols))
        assertEquals(FUN_SUM_1, typeManager.resolveFunction("sum", FUN_SUM_1.argTypes, symbols))
        assertEquals(FUN_SUM_2, typeManager.resolveFunction("sum", FUN_SUM_2.argTypes, symbols))
        assertEquals(FUN_SUM_3, typeManager.resolveFunction("sum", FUN_SUM_3.argTypes, symbols))
        assertEquals(FUN_FOO_DI, typeManager.resolveFunction("foo", FUN_FOO_DI.argTypes, symbols))
        assertEquals(FUN_FOO_ID, typeManager.resolveFunction("foo", FUN_FOO_ID.argTypes, symbols))
    }

    @Test
    fun shouldResolveFunctionWithOneCast() {
        assertEquals(FUN_SIN, typeManager.resolveFunction(FUN_SIN.name, listOf(I64.INSTANCE), symbols))
        assertEquals(FUN_ABS, typeManager.resolveFunction(FUN_ABS.name, listOf(F64.INSTANCE), symbols))
        assertEquals(FUN_THREE, typeManager.resolveFunction(FUN_THREE.name, listOf(F64.INSTANCE, F64.INSTANCE, F64.INSTANCE), symbols))
    }

    @Test
    fun shouldResolveFunctionWithTwoCasts() {
        assertEquals(FUN_FMOD, typeManager.resolveFunction(FUN_FMOD.name, listOf(I64.INSTANCE, I64.INSTANCE), symbols))
        assertEquals(FUN_THREE, typeManager.resolveFunction(FUN_THREE.name, listOf(I64.INSTANCE, I64.INSTANCE, I64.INSTANCE), symbols))
        assertEquals(FUN_SUM_2, typeManager.resolveFunction(FUN_SUM_2.name, listOf(F64.INSTANCE, F64.INSTANCE), symbols))
    }

    @Test
    fun shouldResolveFunctionWithGenericArrayArg() {
        // Using the generic array type
        assertEquals(FUN_LBOUND, typeManager.resolveFunction(FUN_LBOUND.name, FUN_LBOUND.argTypes, symbols))
        // Using a specific array type
        assertEquals(FUN_LBOUND, typeManager.resolveFunction(FUN_LBOUND.name, listOf(Arr.from(1, F64.INSTANCE)), symbols))
    }

    // Negative tests:

    @Test(expected = SemanticsException::class)
    fun shouldNotResolveFloatFloatFunctionWithFloatString() {
        typeManager.resolveFunction(FUN_FMOD.name, listOf(F64.INSTANCE, Str.INSTANCE), symbols)
    }

    @Test(expected = SemanticsException::class)
    fun shouldNotResolveFunctionWithAmbiguousOverload() {
        typeManager.resolveFunction("foo", listOf(I64.INSTANCE, I64.INSTANCE), symbols)
    }

    @Test(expected = SemanticsException::class)
    fun testAddStringFloat() {
        typeManager.getType(ADD_STRING_FLOAT)
    }

    @Test(expected = SemanticsException::class)
    fun testAddStringInteger() {
        typeManager.getType(ADD_STRING_INTEGER)
    }

    @Test(expected = SemanticsException::class)
    fun testAddIntegerString() {
        typeManager.getType(ADD_INTEGER_STRING)
    }

    @Test(expected = SemanticsException::class)
    fun testSubString() {
        typeManager.getType(SUB_STRINGS)
    }

    @Test(expected = SemanticsException::class)
    fun testSubStringInteger() {
        typeManager.getType(SUB_STRING_INTEGER)
    }

    @Test(expected = SemanticsException::class)
    fun shouldGetExceptionFromIDivStringInteger() {
        typeManager.getType(IDIV_STRING_INTEGER)
    }

    @Test(expected = SemanticsException::class)
    fun shouldGetExceptionFromModStringInteger() {
        typeManager.getType(MOD_STRING_INTEGER)
    }

    companion object {

        private val ID_INTEGER = Identifier("integer", I64.INSTANCE)
        private val ID_FLOAT = Identifier("float", F64.INSTANCE)
        private val ID_STRING = Identifier("string", Str.INSTANCE)

        private val FUN_COMMAND = LibraryFunction("command$", emptyList(), Str.INSTANCE, "", ExternalFunction(""))
        private val FUN_SIN = LibraryFunction("sin", listOf(F64.INSTANCE), F64.INSTANCE, "", ExternalFunction(""))
        private val FUN_SUM_F = LibraryFunction("sum", listOf(F64.INSTANCE), F64.INSTANCE, "", ExternalFunction(""))
        private val FUN_SUM_1 = LibraryFunction("sum", listOf(I64.INSTANCE), I64.INSTANCE, "", ExternalFunction(""))
        private val FUN_SUM_2 = LibraryFunction("sum", listOf(I64.INSTANCE, I64.INSTANCE), I64.INSTANCE, "", ExternalFunction(""))
        private val FUN_SUM_3 = LibraryFunction("sum", listOf(I64.INSTANCE, I64.INSTANCE, I64.INSTANCE), I64.INSTANCE, "", ExternalFunction(""))
        private val FUN_FOO_DI = LibraryFunction("foo", listOf(F64.INSTANCE, I64.INSTANCE), I64.INSTANCE, "", ExternalFunction(""))
        private val FUN_FOO_ID = LibraryFunction("foo", listOf(I64.INSTANCE, F64.INSTANCE), I64.INSTANCE, "", ExternalFunction(""))
        private val FUN_THREE = LibraryFunction("three", listOf(F64.INSTANCE, I64.INSTANCE, F64.INSTANCE), I64.INSTANCE, "", ExternalFunction(""))

        private val ID_FUN_FLOAT = Identifier("floatf", Fun.from(listOf(I64.INSTANCE), F64.INSTANCE))
        private val ID_FUN_INTEGER = Identifier("integerf", Fun.from(listOf(Str.INSTANCE), I64.INSTANCE))
        private val ID_FUN_STRING = Identifier("stringf", Fun.from(listOf(I64.INSTANCE, F64.INSTANCE), Str.INSTANCE))

        private val FLOAT_LITERAL = FloatLiteral(0, 0, "5.7")
        private val INTEGER_LITERAL = IntegerLiteral(0, 0, "5")
        private val STRING_LITERAL = StringLiteral(0, 0, "value")

        private val FLOAT_IDE = IdentifierDerefExpression(0, 0, ID_FLOAT)
        private val INTEGER_IDE = IdentifierDerefExpression(0, 0, ID_INTEGER)
        private val STRING_IDE = IdentifierDerefExpression(0, 0, ID_STRING)

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

        private val DIV_INTEGERS = DivExpression(0, 0, INTEGER_LITERAL, INTEGER_IDE)

        private val IDIV_INTEGERS = IDivExpression(0, 0, INTEGER_LITERAL, INTEGER_IDE)
        private val IDIV_STRING_INTEGER = IDivExpression(0, 0, STRING_IDE, INTEGER_IDE)

        private val MOD_INTEGERS = ModExpression(0, 0, INTEGER_LITERAL, INTEGER_IDE)
        private val MOD_STRING_INTEGER = ModExpression(0, 0, STRING_IDE, INTEGER_IDE)

        private val AND_INTEGERS = AndExpression(0, 0, INTEGER_LITERAL, INTEGER_IDE)
        private val AND_INTEGERS_COMPLEX = AndExpression(0, 0, INTEGER_LITERAL, AndExpression(0, 0, INTEGER_IDE, INTEGER_IDE))
        private val XOR_INTEGERS = XorExpression(0, 0, INTEGER_LITERAL, INTEGER_IDE)

        private val REL_INTEGERS = EqualExpression(0, 0, INTEGER_IDE, INTEGER_LITERAL)
        private val REL_STRINGS = NotEqualExpression(0, 0, STRING_IDE, STRING_LITERAL)
        private val REL_COMPLEX = AndExpression(0, 0, EqualExpression(0, 0, INTEGER_IDE, INTEGER_LITERAL), INTEGER_IDE)

        private val NEG_INTEGER = NegateExpression(0, 0, AddExpression(0, 0, INTEGER_IDE, INTEGER_LITERAL))
        private val NEG_FLOAT = NegateExpression(0, 0, FLOAT_IDE)
    }
}
