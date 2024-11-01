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

package se.dykstrom.jcc.col.types

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import se.dykstrom.jcc.col.compiler.ColTests.Companion.FL_1_0
import se.dykstrom.jcc.col.compiler.ColTests.Companion.FUN_ABS
import se.dykstrom.jcc.col.compiler.ColTests.Companion.FUN_F64_TO_I64
import se.dykstrom.jcc.col.compiler.ColTests.Companion.FUN_I64_TO_I64
import se.dykstrom.jcc.col.compiler.ColTests.Companion.FUN_TO_I64
import se.dykstrom.jcc.col.compiler.ColTests.Companion.IL_17
import se.dykstrom.jcc.col.compiler.ColTests.Companion.IL_18
import se.dykstrom.jcc.col.compiler.ColTests.Companion.IL_5
import se.dykstrom.jcc.common.ast.FunctionCallExpression
import se.dykstrom.jcc.common.ast.IdentifierDerefExpression
import se.dykstrom.jcc.common.ast.LessExpression
import se.dykstrom.jcc.common.error.SemanticsException
import se.dykstrom.jcc.common.functions.ReferenceFunction
import se.dykstrom.jcc.common.functions.UserDefinedFunction
import se.dykstrom.jcc.common.symbols.SymbolTable
import se.dykstrom.jcc.common.types.*

class ColTypeManagerTests {

    private val symbols = SymbolTable()

    private val typeManager = ColTypeManager()

    private val funFoo1 = UserDefinedFunction("foo", listOf("f"), listOf(FUN_I64_TO_I64), I64.INSTANCE)
    private val funFoo2 = UserDefinedFunction("foo", listOf("f"), listOf(I64.INSTANCE), I64.INSTANCE)
    private val funFoo3 = UserDefinedFunction("foo", listOf("f"), listOf(FUN_F64_TO_I64), I64.INSTANCE)

    @Test
    fun shouldGetTypeNameOfScalarTypes() {
        assertEquals("bool", typeManager.getTypeName(Bool.INSTANCE))
        assertEquals("f64", typeManager.getTypeName(F64.INSTANCE))
        assertEquals("i64", typeManager.getTypeName(I64.INSTANCE))
        assertEquals("string", typeManager.getTypeName(Str.INSTANCE))
    }

    @Test
    fun shouldFindPredefinedTypes() {
        assertEquals(Bool.INSTANCE, typeManager.getTypeFromName("bool").get())
        assertEquals(F64.INSTANCE, typeManager.getTypeFromName("f64").get())
        assertEquals(I64.INSTANCE, typeManager.getTypeFromName("i64").get())
        assertEquals(Str.INSTANCE, typeManager.getTypeFromName("string").get())
    }

    @Test
    fun shouldGetBoolFromIntegerRelational() {
        assertEquals(Bool.INSTANCE, typeManager.getType(LessExpression(0, 0, IL_5, IL_5)))
    }

    @Test
    fun shouldDefineType() {
        // Given
        assertTrue { typeManager.getTypeFromName("foo").isEmpty }

        // When
        typeManager.defineTypeName("foo", I64.INSTANCE)

        // Then
        assertEquals(I64.INSTANCE, typeManager.getTypeFromName("foo").get())
    }

    @Test
    fun shouldResolveFunctionWithFunctionArg() {
        // Given
        symbols.addFunction(funFoo1)
        symbols.addFunction(funFoo2)

        // When
        val resolvedFunction = typeManager.resolveFunction(funFoo1.name, funFoo1.argTypes, symbols)

        // Then
        assertEquals(funFoo1, resolvedFunction)
    }

    @Test
    fun shouldResolveFunctionWithFunctionArgUsingTypeInference() {
        // Given
        symbols.addFunction(funFoo1)
        symbols.addFunction(funFoo2)
        val argTypes = listOf(AmbiguousType(setOf(FUN_F64_TO_I64, FUN_I64_TO_I64)))

        // When
        val resolvedFunction = typeManager.resolveFunction(funFoo1.name, argTypes, symbols)

        // Then
        assertEquals(funFoo1, resolvedFunction)
    }

    @Test
    fun shouldNotResolveFunctionWithAmbiguousFunctionTypes() {
        // Given
        symbols.addFunction(funFoo1) // Possible match: foo(f as (i64) -> i64) -> i64
        symbols.addFunction(funFoo2)
        symbols.addFunction(funFoo3) // Possible match: foo(f as (f64) -> i64) -> i64
        val argTypes = listOf(AmbiguousType(setOf(FUN_F64_TO_I64, FUN_I64_TO_I64)))

        // When
        val exception = assertThrows<SemanticsException> {
            typeManager.resolveFunction(funFoo1.name, argTypes, symbols)
        }

        // Then
        assertTrue(exception.message?.contains("ambiguous function call") ?: false)
    }

    @Test
    fun shouldResolveFunctionStoredInParameter() {
        // Given
        val name = "foo"
        val funFoo4 = ReferenceFunction(name, FUN_I64_TO_I64.argTypes, FUN_I64_TO_I64.returnType)
        val varFoo = Identifier(name, FUN_I64_TO_I64)
        symbols.addFunction(funFoo1)
        // Do not add funFoo4 itself, just a parameter that matches with it
        symbols.addParameter(varFoo)

        // When
        val resolvedFunFoo = typeManager.resolveFunction(name, funFoo1.argTypes, symbols)
        val resolvedVarFoo = typeManager.resolveFunction(name, funFoo4.argTypes, symbols)

        // Then
        assertEquals(funFoo1, resolvedFunFoo)
        assertEquals(funFoo4, resolvedVarFoo)
    }

    @Test
    fun shouldResolveArguments() {
        // Given
        val funCallAbs = FunctionCallExpression(0, 0, FUN_ABS.identifier, listOf(IL_18))
        val ideAbs = IdentifierDerefExpression(0, 0, FUN_ABS.identifier)
        val atSum = AmbiguousType(setOf(FUN_TO_I64, FUN_I64_TO_I64))
        val ideAmbiguousSum = IdentifierDerefExpression(0, 0, Identifier("sum", atSum))
        val ideResolvedSum = IdentifierDerefExpression(0, 0, Identifier("sum", FUN_TO_I64))

        val originalArgs = listOf(IL_17, FL_1_0, funCallAbs, ideAbs, ideAmbiguousSum)
        val formalArgTypes = listOf(I64.INSTANCE, F64.INSTANCE, I64.INSTANCE, FUN_I64_TO_I64, FUN_TO_I64)
        val expectedArgs = listOf(IL_17, FL_1_0, funCallAbs, ideAbs, ideResolvedSum)

        // When
        val resolvedArgs = typeManager.resolveArgs(originalArgs, formalArgTypes)

        // Then
        assertEquals(expectedArgs, resolvedArgs)
    }
}
