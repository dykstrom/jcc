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
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assert.*
import org.junit.Test
import se.dykstrom.jcc.common.functions.ExternalFunction
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

    private val symbolTable = SymbolTable()

    @Test
    fun shouldVerifyEmptySymbolTable() {
        assertTrue(symbolTable.isEmpty)
        assertEquals(0, symbolTable.size())
    }

    @Test
    fun shouldAddVariable() {
        symbolTable.addVariable(IDENT_I64_A)
        assertEquals(1, symbolTable.size())
        assertTrue(symbolTable.contains(IDENT_I64_A.name()))
        assertEquals(I64.INSTANCE, symbolTable.getType(IDENT_I64_A.name()))
        assertEquals(I64.INSTANCE.defaultValue, symbolTable.getValue(IDENT_I64_A.name()))
        assertFalse(symbolTable.isConstant(IDENT_I64_A.name()))
        assertFalse(symbolTable.contains(IDENT_STR_B.name()))

        symbolTable.addVariable(IDENT_STR_B)
        assertEquals(2, symbolTable.size())
        assertTrue(symbolTable.contains(IDENT_I64_A.name()))
        assertTrue(symbolTable.contains(IDENT_STR_B.name()))
    }

    @Test
    fun shouldAddConstant() {
        symbolTable.addConstant(IDENT_I64_A, I64_VALUE)
        symbolTable.addConstant(IDENT_STR_B, STR_VALUE)

        assertEquals(2, symbolTable.size())
        assertTrue(symbolTable.contains(IDENT_I64_A.name()))
        assertTrue(symbolTable.contains(IDENT_STR_B.name()))

        assertEquals(I64.INSTANCE, symbolTable.getType(IDENT_I64_A.name()))
        assertEquals(I64_VALUE, symbolTable.getValue(IDENT_I64_A.name()))
        assertTrue(symbolTable.isConstant(IDENT_I64_A.name()))

        assertEquals(Str.INSTANCE, symbolTable.getType(IDENT_STR_B.name()))
        assertEquals(STR_VALUE, symbolTable.getValue(IDENT_STR_B.name()))
        assertTrue(symbolTable.isConstant(IDENT_STR_B.name()))
    }

    @Test
    fun shouldGetConstantByTypeAndValue() {
        symbolTable.addConstant(IDENT_I64_A, I64_VALUE)
        symbolTable.addConstant(IDENT_STR_B, STR_VALUE)

        assertEquals(2, symbolTable.size())
        assertTrue(symbolTable.contains(IDENT_I64_A.name()))
        assertTrue(symbolTable.contains(IDENT_STR_B.name()))

        assertEquals(IDENT_I64_A, symbolTable.getConstantByTypeAndValue(I64.INSTANCE, I64_VALUE).get())
        assertEquals(IDENT_STR_B, symbolTable.getConstantByTypeAndValue(Str.INSTANCE, STR_VALUE).get())

        // This combination does not exist
        assertTrue(symbolTable.getConstantByTypeAndValue(I64.INSTANCE, STR_VALUE).isEmpty)
    }

    @Test
    fun shouldNotReturnVariableAsConstant() {
        symbolTable.addVariable(IDENT_I64_A)
        symbolTable.addVariable(IDENT_STR_B)

        assertEquals(2, symbolTable.size())
        assertTrue(symbolTable.contains(IDENT_I64_A.name()))
        assertTrue(symbolTable.contains(IDENT_STR_B.name()))

        // Should not return the variables when we ask for a constant
        assertTrue(symbolTable.getConstantByTypeAndValue(I64.INSTANCE, I64.INSTANCE.defaultValue).isEmpty)
        assertTrue(symbolTable.getConstantByTypeAndValue(Str.INSTANCE, Str.INSTANCE.defaultValue).isEmpty)
    }

    @Test
    fun shouldAddFunction() {
        symbolTable.addFunction(FUN_INT)
        symbolTable.addFunction(FUN_STR)
        symbolTable.addFunction(FUN_STR) // Add twice to verify that only one instance is saved

        assertThat(symbolTable.size(), equalTo(1))
        assertTrue(symbolTable.containsFunction(NAME_FOO))
        assertThat(symbolTable.functionIdentifiers().size, equalTo(2))
        assertThat(symbolTable.functionIdentifiers(), hasItems(IDENT_FUN_INT, IDENT_FUN_STR))
        assertThat(symbolTable.getFunctions(NAME_FOO).size, equalTo(2))
        assertThat(symbolTable.getFunctions(NAME_FOO), hasItems(FUN_INT, FUN_STR))
        assertTrue(symbolTable.containsFunction(NAME_FOO, FUN_INT.argTypes))
        assertThat(symbolTable.getFunction(NAME_FOO, FUN_INT.argTypes), equalTo(FUN_INT))
    }

    @Test
    fun shouldNotFindFunctionWithWrongArgTypes() {
        assertThrows(IllegalArgumentException::class.java) { symbolTable.getFunction(NAME_FOO, listOf(F64.INSTANCE)) }
    }
}
