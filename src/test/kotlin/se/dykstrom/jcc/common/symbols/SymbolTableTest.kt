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

package se.dykstrom.jcc.common.symbols

import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.hasItems
import org.junit.Assert.*
import org.junit.Test
import se.dykstrom.jcc.common.functions.ExternalFunction
import se.dykstrom.jcc.common.functions.Function
import se.dykstrom.jcc.common.functions.LibraryFunction
import se.dykstrom.jcc.common.types.*

class SymbolTableTest {

    companion object {
        private const val NAME_FOO = "foo"

        private val FUN_INT = LibraryFunction(NAME_FOO, listOf(I64.INSTANCE), I64.INSTANCE, "", ExternalFunction("fooo"))
        private val FUN_STR = LibraryFunction(NAME_FOO, listOf(Str.INSTANCE), I64.INSTANCE, "", ExternalFunction("fooo"))

        private const val I64_VALUE = "17"
        private const val STR_VALUE = "hello"

        private val IDENT_FUN_INT = Identifier(FUN_INT.name, Fun.from(FUN_INT.argTypes, FUN_INT.returnType))
        private val IDENT_FUN_STR = Identifier(FUN_STR.name, Fun.from(FUN_STR.argTypes, FUN_STR.returnType))

        private val IDENT_I64_A = Identifier("a", I64.INSTANCE)
        private val IDENT_STR_B = Identifier("b", Str.INSTANCE)
    }

    private val testee = SymbolTable()

    @Test
    fun shouldVerifyEmptySymbolTable() {
        assertTrue(testee.isEmpty)
        assertEquals(0, testee.size())
    }

    @Test
    fun shouldAddVariable() {
        testee.addVariable(IDENT_I64_A)
        assertEquals(1, testee.size())
        assertTrue(testee.contains(IDENT_I64_A.name))
        assertEquals(I64.INSTANCE, testee.getType(IDENT_I64_A.name))
        assertEquals(I64.INSTANCE.defaultValue, testee.getValue(IDENT_I64_A.name))
        assertFalse(testee.isConstant(IDENT_I64_A.name))
        assertFalse(testee.contains(IDENT_STR_B.name))

        testee.addVariable(IDENT_STR_B)
        assertEquals(2, testee.size())
        assertTrue(testee.contains(IDENT_I64_A.name))
        assertTrue(testee.contains(IDENT_STR_B.name))
    }

    @Test
    fun shouldAddConstant() {
        testee.addConstant(IDENT_I64_A, I64_VALUE)
        testee.addConstant(IDENT_STR_B, STR_VALUE)

        assertEquals(2, testee.size())
        assertTrue(testee.contains(IDENT_I64_A.name))
        assertTrue(testee.contains(IDENT_STR_B.name))

        assertEquals(I64.INSTANCE, testee.getType(IDENT_I64_A.name))
        assertEquals(I64_VALUE, testee.getValue(IDENT_I64_A.name))
        assertTrue(testee.isConstant(IDENT_I64_A.name))

        assertEquals(Str.INSTANCE, testee.getType(IDENT_STR_B.name))
        assertEquals(STR_VALUE, testee.getValue(IDENT_STR_B.name))
        assertTrue(testee.isConstant(IDENT_STR_B.name))
    }

    @Test
    fun shouldGetConstantByTypeAndValue() {
        testee.addConstant(IDENT_I64_A, I64_VALUE)
        testee.addConstant(IDENT_STR_B, STR_VALUE)

        assertEquals(2, testee.size())
        assertTrue(testee.contains(IDENT_I64_A.name))
        assertTrue(testee.contains(IDENT_STR_B.name))

        assertEquals(IDENT_I64_A, testee.getConstantByTypeAndValue(I64.INSTANCE, I64_VALUE))
        assertEquals(IDENT_STR_B, testee.getConstantByTypeAndValue(Str.INSTANCE, STR_VALUE))

        // This combination does not exist
        assertNull(testee.getConstantByTypeAndValue(I64.INSTANCE, STR_VALUE))
    }

    @Test
    fun shouldNotReturnVariableAsConstant() {
        testee.addVariable(IDENT_I64_A)
        testee.addVariable(IDENT_STR_B)

        assertEquals(2, testee.size())
        assertTrue(testee.contains(IDENT_I64_A.name))
        assertTrue(testee.contains(IDENT_STR_B.name))

        // Should not return the variables when we ask for a constant
        assertNull(testee.getConstantByTypeAndValue(I64.INSTANCE, I64.INSTANCE.defaultValue))
        assertNull(testee.getConstantByTypeAndValue(Str.INSTANCE, Str.INSTANCE.defaultValue))
    }

    @Test
    fun shouldAddFunction() {
        testee.addFunction(FUN_INT)
        testee.addFunction(FUN_STR)
        testee.addFunction(FUN_STR) // Add twice to verify that only one instance is saved

        assertThat(testee.size(), equalTo(1))
        assertTrue(testee.containsFunction(NAME_FOO))
        assertThat(testee.functionIdentifiers().size, equalTo(2))
        assertThat(testee.functionIdentifiers(), hasItems(IDENT_FUN_INT, IDENT_FUN_STR))
        assertThat(testee.getFunctions(NAME_FOO).size, equalTo(2))
        assertThat(testee.getFunctions(NAME_FOO), hasItems<Function>(FUN_INT, FUN_STR))
        assertTrue(testee.containsFunction(NAME_FOO, FUN_INT.argTypes))
        assertThat(testee.getFunction(NAME_FOO, FUN_INT.argTypes), equalTo<Function>(FUN_INT))
    }

    @Test(expected = IllegalArgumentException::class)
    fun shouldNotFindFunctionWithWrongArgTypes() {
        testee.getFunction(NAME_FOO, listOf(Bool.INSTANCE))
    }
}
