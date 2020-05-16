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
import se.dykstrom.jcc.common.assembly.base.Code
import se.dykstrom.jcc.common.assembly.other.DataDefinition
import se.dykstrom.jcc.common.ast.ArrayAccessExpression
import se.dykstrom.jcc.common.ast.ArrayDeclaration
import se.dykstrom.jcc.common.ast.VariableDeclarationStatement
import se.dykstrom.jcc.common.types.*
import kotlin.test.assertEquals

/**
 * Tests features related to arrays in code generation.
 *
 * An array declared with a subscript of N can hold N + 1 elements, ranging from 0 to N.
 *
 * @author Johan Dykstrom
 */
class BasicCodeGeneratorArrayTests : AbstractBasicCodeGeneratorTest() {

    @Test
    fun shouldDefineSimpleIntegerArray() {
        // dim a%(3) as integer
        val declarations = listOf(ArrayDeclaration(0, 0, IDENT_I64_A.name, Arr.from(1, I64.INSTANCE),  listOf(IL_3)))
        val statement = VariableDeclarationStatement(0, 0, declarations)

        val result = assembleProgram(listOf(statement))
        val codes = result.codes()

        // Variable a% should be defined and be an array of integers
        assertEquals(1, codes
                .asSequence()
                .filterIsInstance(DataDefinition::class.java)
                .filter { it.identifier.type == I64.INSTANCE }
                .filter { it.identifier.mappedName == IDENT_I64_A.mappedName + "_arr" }
                .filter { it.value == "4 dup " + I64.INSTANCE.defaultValue }
                .count())
    }

    @Test
    fun shouldDefineMultiDimensionalIntegerArray() {
        // dim a%(2, 4) as integer
        val declarations = listOf(ArrayDeclaration(0, 0, IDENT_I64_A.name, Arr.from(2, I64.INSTANCE),  listOf(IL_2, IL_4)))
        val statement = VariableDeclarationStatement(0, 0, declarations)

        val result = assembleProgram(listOf(statement))
        val codes = result.codes()

        // Variable a% should be defined and be a two dimensional array of integers
        assertEquals(1, codes
                .asSequence()
                .filterIsInstance(DataDefinition::class.java)
                .filter { it.identifier.type == I64.INSTANCE }
                .filter { it.identifier.mappedName == IDENT_I64_A.mappedName + "_arr" }
                .filter { it.value == "15 dup " + I64.INSTANCE.defaultValue }
                .count())
        // There should be two dimensions
        assertEquals(2, getValueOfDataDefinitionAsInt(codes, IDENT_I64_A.mappedName + "_num_dims"))
        // Of size two
        assertEquals(2, getValueOfDataDefinitionAsInt(codes, IDENT_I64_A.mappedName + "_dim_0"))
        // And four
        assertEquals(4, getValueOfDataDefinitionAsInt(codes, IDENT_I64_A.mappedName + "_dim_1"))
    }

    @Test
    fun shouldDefineSimpleFloatArray() {
        // dim f(2) as double
        val declarations = listOf(ArrayDeclaration(0, 0, IDENT_F64_F.name, Arr.from(1, F64.INSTANCE),  listOf(IL_2)))
        val statement = VariableDeclarationStatement(0, 0, declarations)

        val result = assembleProgram(listOf(statement))
        val codes = result.codes()

        // Variable f should be defined and be an array of floats
        assertEquals(1, codes
                .asSequence()
                .filterIsInstance(DataDefinition::class.java)
                .filter { it.identifier.type == F64.INSTANCE }
                .filter { it.identifier.mappedName == IDENT_F64_F.mappedName + "_arr" }
                .filter { it.value == "3 dup " + F64.INSTANCE.defaultValue }
                .count())
        // There should be one dimension
        assertEquals(1, getValueOfDataDefinitionAsInt(codes, IDENT_F64_F.mappedName + "_num_dims"))
        // Of size two
        assertEquals(2, getValueOfDataDefinitionAsInt(codes, IDENT_F64_F.mappedName + "_dim_0"))
    }

    @Test
    fun shouldDefineSimpleStringArray() {
        // dim s$(1) as string
        val declarations = listOf(ArrayDeclaration(0, 0, IDENT_STR_S.name, Arr.from(1, Str.INSTANCE),  listOf(IL_1)))
        val statement = VariableDeclarationStatement(0, 0, declarations)

        val result = assembleProgram(listOf(statement))
        val codes = result.codes()

        // Variable s$ should be defined and be an array of strings
        assertEquals(1, codes
                .asSequence()
                .filterIsInstance(DataDefinition::class.java)
                .filter { it.identifier.type == Str.INSTANCE }
                .filter { it.identifier.mappedName == IDENT_STR_S.mappedName + "_arr" }
                .filter { it.value == "2 dup " + Str.INSTANCE.defaultValue }
                .count())
        // There should be one dimension
        assertEquals(1, getValueOfDataDefinitionAsInt(codes, IDENT_STR_S.mappedName + "_num_dims"))
        // Of size one
        assertEquals(1, getValueOfDataDefinitionAsInt(codes, IDENT_STR_S.mappedName + "_dim_0"))
    }

    @Test
    fun shouldDefineTwoArrays() {
        // dim s$(1) as string
        // dim a%(4, 4) as integer
        val declarations = listOf(
                ArrayDeclaration(0, 0, IDENT_STR_S.name, Arr.from(1, Str.INSTANCE),  listOf(IL_1)),
                ArrayDeclaration(0, 0, IDENT_I64_A.name, Arr.from(2, I64.INSTANCE),  listOf(IL_4, IL_4))
        )
        val statement = VariableDeclarationStatement(0, 0, declarations)

        val result = assembleProgram(listOf(statement))
        val codes = result.codes()

        // Variable s$ should be defined and be an array of strings
        assertEquals(1, codes
                .asSequence()
                .filterIsInstance(DataDefinition::class.java)
                .filter { it.identifier.type == Str.INSTANCE }
                .filter { it.identifier.mappedName == IDENT_STR_S.mappedName + "_arr" }
                .filter { it.value == "2 dup " + Str.INSTANCE.defaultValue }
                .count())
        // There should be one dimension
        assertEquals(1, getValueOfDataDefinitionAsInt(codes, IDENT_STR_S.mappedName + "_num_dims"))
        // Of size one
        assertEquals(1, getValueOfDataDefinitionAsInt(codes, IDENT_STR_S.mappedName + "_dim_0"))

        // Variable a% should be defined and be an array of integers
        assertEquals(1, codes
                .asSequence()
                .filterIsInstance(DataDefinition::class.java)
                .filter { it.identifier.type == I64.INSTANCE }
                .filter { it.identifier.mappedName == IDENT_I64_A.mappedName + "_arr" }
                .filter { it.value == "25 dup " + I64.INSTANCE.defaultValue }
                .count())
        // There should be two dimensions
        assertEquals(2, getValueOfDataDefinitionAsInt(codes, IDENT_I64_A.mappedName + "_num_dims"))
        // Of size four
        assertEquals(4, getValueOfDataDefinitionAsInt(codes, IDENT_I64_A.mappedName + "_dim_0"))
        // And four
        assertEquals(4, getValueOfDataDefinitionAsInt(codes, IDENT_I64_A.mappedName + "_dim_1"))
    }

    @Test
    fun shouldAccessElementInOneDimensionalArray() {
        // dim a%(4) as integer
        val declarations = listOf(ArrayDeclaration(0, 0, IDENT_ARR_I64_A.name, TYPE_ARR_I64_1, listOf(IL_4)))
        val declarationStatement = VariableDeclarationStatement(0, 0, declarations)

        // print a%(2)
        val arrayAccessExpression = ArrayAccessExpression(0, 0, IDENT_ARR_I64_A, listOf(IL_2))
        val printStatement = PrintStatement(0, 0, listOf(arrayAccessExpression))

        val result = assembleProgram(listOf(declarationStatement, printStatement))
        val codes = result.codes()

        result.codes().forEach { println(it.toAsm()) }

        // TODO: Assert that we calculate the offset into the array, and that there is a move instruction that reads from that offset.
    }

    /**
     * Extracts the value of the data definition with the given mapped name as an Int.
     * This method expects to find exactly one sunch data definition, and that the value
     * is actually an integer.
     */
    private fun getValueOfDataDefinitionAsInt(codes: List<Code>, mappedName: String): Int {
        val values = codes
                .filterIsInstance(DataDefinition::class.java)
                .filter { it.identifier.mappedName == mappedName }
                .map { it.value.toInt() }
        assertEquals(1, values.size)
        return values.first()
    }
}
