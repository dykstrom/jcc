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

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import se.dykstrom.jcc.common.ast.ArrayDeclaration
import se.dykstrom.jcc.common.ast.IntegerLiteral
import se.dykstrom.jcc.common.functions.UserDefinedFunction
import se.dykstrom.jcc.common.types.*

class SymbolTableTests {

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
    fun shouldAddVariableWithInitialValue() {
        val valueOfA = "7"
        val valueOfB = "foo"

        symbolTable.addVariable(IDENT_I64_A, valueOfA)
        assertEquals(1, symbolTable.size())
        assertTrue(symbolTable.contains(IDENT_I64_A.name()))
        assertEquals(I64.INSTANCE, symbolTable.getType(IDENT_I64_A.name()))
        assertEquals(valueOfA, symbolTable.getValue(IDENT_I64_A.name()))
        assertFalse(symbolTable.isConstant(IDENT_I64_A.name()))
        assertFalse(symbolTable.contains(IDENT_STR_B.name()))

        symbolTable.addVariable(IDENT_STR_B, valueOfB)
        assertEquals(2, symbolTable.size())
        assertTrue(symbolTable.contains(IDENT_I64_A.name()))
        assertTrue(symbolTable.contains(IDENT_STR_B.name()))
        assertEquals(valueOfB, symbolTable.getValue(IDENT_STR_B.name()))
        assertFalse(symbolTable.isConstant(IDENT_STR_B.name()))
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
        symbolTable.addFunction(FUN_I64_TO_I64)
        symbolTable.addFunction(FUN_STR_TO_I64)
        symbolTable.addFunction(FUN_STR_TO_I64) // Add twice to verify that only one instance is saved

        assertEquals(1, symbolTable.size())
        assertTrue(symbolTable.containsFunction(NAME_FOO))
        assertEquals(2, symbolTable.functionIdentifiers().size)
        assertEquals(setOf(IDENT_FUN_INT, IDENT_FUN_STR), symbolTable.functionIdentifiers())
        assertEquals(2, symbolTable.getFunctions(NAME_FOO).size)
        assertEquals(setOf(FUN_I64_TO_I64, FUN_STR_TO_I64), symbolTable.getFunctions(NAME_FOO))
        assertTrue(symbolTable.containsFunction(NAME_FOO, FUN_I64_TO_I64.argTypes))
        assertEquals(FUN_I64_TO_I64, symbolTable.getFunction(NAME_FOO, FUN_I64_TO_I64.argTypes))
    }

    @Test
    fun shouldPushAndPopSymbolTable() {
        val childTable = SymbolTable(symbolTable)
        val parentTable = childTable.pop()
        assertSame(symbolTable, parentTable)
    }

    @Test
    fun shouldFindVariableInParentTable() {
        // Add a variable
        symbolTable.addVariable(IDENT_I64_A)

        // Create a child table
        val childTable = SymbolTable(symbolTable)

        // Find variable in parent table via child table
        assertTrue { childTable.contains(NAME_A) }
        assertEquals(IDENT_I64_A, childTable.getIdentifier(NAME_A))
    }

    @Test
    fun shouldFindVariableInParentParentTable() {
        // Add a variable
        symbolTable.addVariable(IDENT_I64_A)

        // Create a child table
        val childTable = SymbolTable(symbolTable)
        // Create a grandchild table
        val grandchildTable = SymbolTable(childTable)

        // Find variable in parent table via grandchild table
        assertTrue { grandchildTable.contains(NAME_A) }
        assertEquals(IDENT_I64_A, grandchildTable.getIdentifier(NAME_A))
    }

    @Test
    fun shouldFindVariableInChildTableBeforeParentTable() {
        // Add a variable
        symbolTable.addVariable(IDENT_I64_A)

        // Create a child table
        val childTable = SymbolTable(symbolTable)

        // Add a local variable with the same name
        val identStrA = Identifier(NAME_A, Str.INSTANCE)
        childTable.addVariable(identStrA)

        // Find variable in child table
        assertTrue { childTable.contains(NAME_A) }
        assertEquals(identStrA, childTable.getIdentifier(NAME_A))
    }

    @Test
    fun shouldFindConstantInParentTable() {
        // Create a child table
        val childTable = SymbolTable(symbolTable)
        
        // Add a constant to the child table
        childTable.addConstant(IDENT_I64_A, "17")

        // Find constant directly in parent table
        assertTrue { symbolTable.contains(NAME_A) }
        assertTrue { symbolTable.isConstant(NAME_A) }
        assertEquals(IDENT_I64_A, symbolTable.getIdentifier(NAME_A))
        assertEquals("17", symbolTable.getValue(NAME_A))

        // Find constant in parent table via child table
        assertTrue { childTable.contains(NAME_A) }
        assertTrue { childTable.isConstant(NAME_A) }
        assertEquals(IDENT_I64_A, childTable.getIdentifier(NAME_A))
        assertEquals("17", childTable.getValue(NAME_A))
    }

    @Test
    fun shouldFindConstantByTypeAndValueInParentTable() {
        // Create a child table
        val childTable = SymbolTable(symbolTable)

        // Add a constant
        childTable.addConstant(IDENT_I64_A, "17")

        // Find constant directly in parent table
        assertEquals(IDENT_I64_A, symbolTable.getConstantByTypeAndValue(I64.INSTANCE, "17").get())

        // Find constant in parent table via child table
        assertEquals(IDENT_I64_A, childTable.getConstantByTypeAndValue(I64.INSTANCE, "17").get())
    }

    @Test
    fun shouldGetIdentifiersAlsoFromParentTable() {
        // Add a variable
        symbolTable.addVariable(IDENT_I64_A)

        // Create a child table
        val childTable = SymbolTable(symbolTable)

        // Add another variable
        childTable.addVariable(IDENT_STR_B)

        // Find both from child table
        val expectedIdentifiers = setOf(IDENT_I64_A, IDENT_STR_B)
        val actualIdentifiers = childTable.identifiers()
        assertEquals(expectedIdentifiers, actualIdentifiers)

        // Find only original 'a' from original table
        assertEquals(setOf(IDENT_I64_A), symbolTable.identifiers())
    }

    @Test
    fun shouldGetIdentifiersFromChildTableBeforeParentTable() {
        // Add a variable
        symbolTable.addVariable(IDENT_I64_A)

        // Create a child table
        val childTable = SymbolTable(symbolTable)

        // Add another variable
        childTable.addVariable(IDENT_STR_B)
        // And one with the same name that was defined in the parent
        val identStrA = Identifier(NAME_A, Str.INSTANCE)
        childTable.addVariable(identStrA)

        // Find both from child table
        val expectedIdentifiers = setOf(identStrA, IDENT_STR_B)
        val actualIdentifiers = childTable.identifiers()
        assertEquals(expectedIdentifiers, actualIdentifiers)

        // Find only original 'a' from original table
        assertEquals(setOf(IDENT_I64_A), symbolTable.identifiers())
    }

    @Test
    fun shouldOnlyGetLocalIdentifiers() {
        // Add a variable
        symbolTable.addVariable(IDENT_I64_A)

        // Create a child table
        val childTable = SymbolTable(symbolTable)

        // Add another variable
        childTable.addVariable(IDENT_STR_B)

        // Find only 'b' from child table
        val expectedIdentifiers = setOf(IDENT_STR_B)
        val actualIdentifiers = childTable.localIdentifiers()
        assertEquals(expectedIdentifiers, actualIdentifiers)

        // Find only original 'a' from original table
        assertEquals(setOf(IDENT_I64_A), symbolTable.identifiers())
    }

    @Test
    fun shouldFindArrayInParentTable() {
        // Add an array
        val arrayDeclaration = ArrayDeclaration(0, 0, NAME_X, TYPE_ARR_1_I64, listOf(IntegerLiteral.ONE))
        symbolTable.addArray(IDENT_ARR_I64_X, arrayDeclaration)

        // Create a child table
        val childTable = SymbolTable(symbolTable)

        // Find array variable in parent table via child table
        assertTrue { childTable.containsArray(NAME_X) }
        assertEquals(IDENT_ARR_I64_X, childTable.getArrayIdentifier(NAME_X))
    }

    @Test
    fun shouldFindArrayInChildTableBeforeParentTable() {
        // Add an array
        symbolTable.addArray(IDENT_ARR_I64_X, DECL_ARR_1_I64_X)

        // Create a child table
        val childTable = SymbolTable(symbolTable)

        // Add a local array with the same name
        val childArrayDeclaration = ArrayDeclaration(0, 0, NAME_X, TYPE_ARR_1_F64, listOf(IL_7))
        val identArrF64X = Identifier(NAME_X, Arr.from(1, F64.INSTANCE))
        symbolTable.addArray(identArrF64X, childArrayDeclaration)

        // Find array variable in parent table via child table
        assertTrue { childTable.containsArray(NAME_X) }
        assertEquals(identArrF64X, childTable.getArrayIdentifier(NAME_X))
    }

    @Test
    fun shouldGetArrayIdentifiersFromChildTableBeforeParentTable() {
        // Add an array
        symbolTable.addArray(IDENT_ARR_I64_X, DECL_ARR_1_I64_X)

        // Create a child table
        val childTable = SymbolTable(symbolTable)

        // Add another variable
        childTable.addArray(IDENT_ARR_I64_A, DECL_ARR_1_I64_X)
        // And one with the same name that was defined in the parent
        val identArrF64X = Identifier(NAME_X, TYPE_ARR_1_F64)
        val childArrayDeclaration = ArrayDeclaration(0, 0, NAME_X, TYPE_ARR_1_I64, listOf(IL_7))
        childTable.addArray(identArrF64X, childArrayDeclaration)

        // Find both from child table
        val expectedIdentifiers = setOf(identArrF64X, IDENT_ARR_I64_A)
        val actualIdentifiers = childTable.arrayIdentifiers()
        assertEquals(expectedIdentifiers, actualIdentifiers)

        // Find only original 'x' from original table
        assertEquals(setOf(IDENT_ARR_I64_X), symbolTable.arrayIdentifiers())
    }

    @Test
    fun shouldFindFunctionInParentTableUsingNameAndType() {
        // Create a child table
        val childTable = SymbolTable(symbolTable)

        // Add a function to the child table
        childTable.addFunction(FUN_I64_TO_I64)

        // Find function directly in parent table
        assertTrue { symbolTable.containsFunction(NAME_FOO, FUN_I64_TO_I64.argTypes) }
        assertEquals(FUN_I64_TO_I64, symbolTable.getFunction(NAME_FOO, FUN_I64_TO_I64.argTypes))

        // Find function in parent table via child table
        assertTrue { childTable.containsFunction(NAME_FOO, FUN_I64_TO_I64.argTypes) }
        assertEquals(FUN_I64_TO_I64, childTable.getFunction(NAME_FOO, FUN_I64_TO_I64.argTypes))
    }

    @Test
    fun shouldFindFunctionInParentTableUsingOnlyName() {
        // Create a child table
        val childTable = SymbolTable(symbolTable)

        // Add a function to the child table
        childTable.addFunction(FUN_I64_TO_I64)

        // Find function directly in parent table
        assertTrue { symbolTable.containsFunction(NAME_FOO) }
        assertEquals(setOf(FUN_I64_TO_I64), symbolTable.getFunctions(NAME_FOO))

        // Find function in parent table via child table
        assertTrue { childTable.containsFunction(NAME_FOO) }
        assertEquals(setOf(FUN_I64_TO_I64), childTable.getFunctions(NAME_FOO))
    }

    @Test
    fun shouldFindAllFunctionsFromChildTable() {
        // Add a function
        symbolTable.addFunction(FUN_STR_TO_I64)

        // Create a child table
        val childTable = SymbolTable(symbolTable)

        // Add a function to the child table
        childTable.addFunction(FUN_I64_TO_I64)

        // Find functions directly in parent table
        assertEquals(setOf(FUN_I64_TO_I64, FUN_STR_TO_I64), symbolTable.getFunctions(NAME_FOO))

        // Find functions in parent table via child table
        assertEquals(setOf(FUN_I64_TO_I64, FUN_STR_TO_I64), childTable.getFunctions(NAME_FOO))
    }

    @Test
    fun shouldFindAllFunctionIdentifiersFromChildTable() {
        // Add a function
        symbolTable.addFunction(FUN_STR_TO_I64)

        // Create a child table
        val childTable = SymbolTable(symbolTable)

        // Add a function to the child table
        childTable.addFunction(FUN_I64_TO_I64)

        // Find function identifiers directly in parent table
        assertEquals(setOf(FUN_I64_TO_I64.identifier, FUN_STR_TO_I64.identifier), symbolTable.functionIdentifiers())

        // Find function identifiers in parent table via child table
        assertEquals(setOf(FUN_I64_TO_I64.identifier, FUN_STR_TO_I64.identifier), childTable.functionIdentifiers())
    }

    @Test
    fun shouldIncreaseTempNames() {
        assertEquals("%0", symbolTable.nextTempName())
        assertEquals("%1", symbolTable.nextTempName())
        assertEquals("%2", symbolTable.nextTempName())
    }

    @Test
    fun shouldResetTempNamesInChildSymbolTable() {
        assertEquals("%0", symbolTable.nextTempName())

        // Create a child table
        val childTable = SymbolTable(symbolTable)

        assertEquals("%0", childTable.nextTempName())
        assertEquals("%1", childTable.nextTempName())

        // Parent table has not changed
        assertEquals("%1", symbolTable.nextTempName())
    }

    @Test
    fun shouldNotFindUndefinedVariable() {
        assertThrows<IllegalArgumentException> { symbolTable.getIdentifier(NAME_A) }
    }

    @Test
    fun shouldNotFindUndefinedArray() {
        assertThrows<IllegalArgumentException> { symbolTable.getArrayIdentifier(NAME_A) }
    }

    @Test
    fun shouldNotFindFunctionWithWrongArgTypes() {
        assertThrows<IllegalArgumentException> { symbolTable.getFunction(NAME_FOO, listOf(F64.INSTANCE)) }
    }

    companion object {

        private const val NAME_A = "a"
        private const val NAME_B = "b"
        private const val NAME_X = "x"
        private const val NAME_FOO = "foo"

        private const val I64_VALUE = "17"
        private const val STR_VALUE = "hello"

        private val TYPE_ARR_1_I64 = Arr.from(1, I64.INSTANCE)
        private val TYPE_ARR_1_F64 = Arr.from(1, F64.INSTANCE)
        private val TYPE_FUN_I64_TO_I64 = Fun.from(listOf(I64.INSTANCE), I64.INSTANCE)
        private val TYPE_FUN_STR_TO_I64 = Fun.from(listOf(Str.INSTANCE), I64.INSTANCE)

        private val FUN_I64_TO_I64 = UserDefinedFunction(NAME_FOO, listOf("pa"), listOf(I64.INSTANCE), I64.INSTANCE)
        private val FUN_STR_TO_I64 = UserDefinedFunction(NAME_FOO, listOf("ps"), listOf(Str.INSTANCE), I64.INSTANCE)

        private val IDENT_FUN_INT = Identifier(FUN_I64_TO_I64.name, TYPE_FUN_I64_TO_I64)
        private val IDENT_FUN_STR = Identifier(FUN_STR_TO_I64.name, TYPE_FUN_STR_TO_I64)

        private val IDENT_I64_A = Identifier(NAME_A, I64.INSTANCE)
        private val IDENT_STR_B = Identifier(NAME_B, Str.INSTANCE)
        private val IDENT_ARR_I64_A = Identifier(NAME_A, TYPE_ARR_1_I64)
        private val IDENT_ARR_I64_X = Identifier(NAME_X, TYPE_ARR_1_I64)

        private val DECL_ARR_1_I64_X = ArrayDeclaration(0, 0, NAME_X, TYPE_ARR_1_I64, listOf(IntegerLiteral.ONE))

        private val IL_7 = IntegerLiteral(0, 0, "7")
    }
}
