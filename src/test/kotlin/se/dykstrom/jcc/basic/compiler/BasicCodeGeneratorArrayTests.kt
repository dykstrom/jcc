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
import se.dykstrom.jcc.basic.ast.SwapStatement
import se.dykstrom.jcc.common.assembly.base.Line
import se.dykstrom.jcc.common.assembly.instruction.*
import se.dykstrom.jcc.common.assembly.instruction.floating.ConvertIntRegToFloatReg
import se.dykstrom.jcc.common.assembly.instruction.floating.MoveFloatRegToMem
import se.dykstrom.jcc.common.assembly.instruction.floating.MoveMemToFloatReg
import se.dykstrom.jcc.common.assembly.instruction.floating.RoundFloatRegToIntReg
import se.dykstrom.jcc.common.assembly.other.DataDefinition
import se.dykstrom.jcc.common.ast.*
import se.dykstrom.jcc.common.types.Arr
import se.dykstrom.jcc.common.types.F64
import se.dykstrom.jcc.common.types.I64
import se.dykstrom.jcc.common.types.Str
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
        val declarations = listOf(ArrayDeclaration(0, 0, IDENT_ARR_I64_A.name, TYPE_ARR_I64_1, listOf(IL_3)))
        val statement = VariableDeclarationStatement(0, 0, declarations)

        val result = assembleProgram(listOf(statement))
        val lines = result.lines()

        // Variable a% should be defined and be an array of integers
        assertEquals(1, lines.asSequence()
            .filterIsInstance<DataDefinition>()
            .filter { it.identifier.type == I64.INSTANCE }
            .filter { it.identifier.mappedName == IDENT_ARR_I64_A.mappedName + "_arr" }
            .filter { it.value == "3 dup " + I64.INSTANCE.defaultValue }
            .count())
    }

    @Test
    fun shouldDefineMultiDimensionalIntegerArray() {
        // dim a%(2, 4) as integer
        val declarations = listOf(ArrayDeclaration(0, 0, IDENT_ARR_I64_A.name, TYPE_ARR_I64_2, listOf(IL_2, IL_4)))
        val statement = VariableDeclarationStatement(0, 0, declarations)

        val result = assembleProgram(listOf(statement))
        val lines = result.lines()

        // Variable a% should be defined and be a two dimensional array of integers
        assertEquals(1, lines.asSequence()
            .filterIsInstance<DataDefinition>()
            .filter { it.identifier.type == I64.INSTANCE }
            .filter { it.identifier.mappedName == IDENT_ARR_I64_A.mappedName + "_arr" }
            .filter { it.value == "8 dup " + I64.INSTANCE.defaultValue }
            .count())
        // There should be two dimensions
        assertEquals(2, getValueOfDataDefinitionAsInt(lines, IDENT_ARR_I64_A.mappedName + "_num_dims"))
        // Of size two
        assertEquals(2, getValueOfDataDefinitionAsInt(lines, IDENT_ARR_I64_A.mappedName + "_dim_0"))
        // And four
        assertEquals(4, getValueOfDataDefinitionAsInt(lines, IDENT_ARR_I64_A.mappedName + "_dim_1"))
    }

    @Test
    fun shouldDefineSimpleFloatArray() {
        // dim d(2) as double
        val declarations = listOf(ArrayDeclaration(0, 0, IDENT_ARR_F64_D.name, TYPE_ARR_F64_1, listOf(IL_2)))
        val statement = VariableDeclarationStatement(0, 0, declarations)

        val result = assembleProgram(listOf(statement))
        val lines = result.lines()

        // Variable d should be defined and be an array of floats
        assertEquals(1, lines.asSequence()
            .filterIsInstance<DataDefinition>()
            .filter { it.identifier.type == F64.INSTANCE }
            .filter { it.identifier.mappedName == IDENT_ARR_F64_D.mappedName + "_arr" }
            .filter { it.value == "2 dup " + F64.INSTANCE.defaultValue }
            .count())
        // There should be one dimension
        assertEquals(1, getValueOfDataDefinitionAsInt(lines, IDENT_ARR_F64_D.mappedName + "_num_dims"))
        // Of size two
        assertEquals(2, getValueOfDataDefinitionAsInt(lines, IDENT_ARR_F64_D.mappedName + "_dim_0"))
    }

    @Test
    fun shouldDefineSimpleStringArray() {
        // dim s$(1) as string
        val declarations = listOf(ArrayDeclaration(0, 0, IDENT_ARR_STR_S.name, TYPE_ARR_STR_1, listOf(IL_1)))
        val statement = VariableDeclarationStatement(0, 0, declarations)

        val result = assembleProgram(listOf(statement))
        val lines = result.lines()

        // Variable s$ should be defined and be an array of strings
        assertEquals(1, lines.asSequence()
            .filterIsInstance<DataDefinition>()
            .filter { it.identifier.type == Str.INSTANCE }
            .filter { it.identifier.mappedName == IDENT_STR_S.mappedName + "_arr" }
            .filter { it.value == "1 dup " + Str.INSTANCE.defaultValue }
            .count())
        // There should be one dimension
        assertEquals(1, getValueOfDataDefinitionAsInt(lines, IDENT_STR_S.mappedName + "_num_dims"))
        // Of size one
        assertEquals(1, getValueOfDataDefinitionAsInt(lines, IDENT_STR_S.mappedName + "_dim_0"))
    }

    @Test
    fun shouldDefineTwoArrays() {
        // dim s$(1) as string
        // dim a%(4, 4) as integer
        val declarations = listOf(
                ArrayDeclaration(0, 0, IDENT_ARR_STR_S.name, TYPE_ARR_STR_1, listOf(IL_1)),
                ArrayDeclaration(0, 0, IDENT_ARR_I64_A.name, TYPE_ARR_I64_2, listOf(IL_4, IL_4))
        )
        val statement = VariableDeclarationStatement(0, 0, declarations)

        val result = assembleProgram(listOf(statement))
        val lines = result.lines()

        // Variable s$ should be defined and be an array of strings
        assertEquals(1, lines.asSequence()
            .filterIsInstance<DataDefinition>()
            .filter { it.identifier.type == Str.INSTANCE }
            .filter { it.identifier.mappedName == IDENT_STR_S.mappedName + "_arr" }
            .filter { it.value == "1 dup " + Str.INSTANCE.defaultValue }
            .count())
        // There should be one dimension
        assertEquals(1, getValueOfDataDefinitionAsInt(lines, IDENT_STR_S.mappedName + "_num_dims"))
        // Of size one
        assertEquals(1, getValueOfDataDefinitionAsInt(lines, IDENT_STR_S.mappedName + "_dim_0"))

        // Variable a% should be defined and be an array of integers
        assertEquals(1, lines.asSequence()
            .filterIsInstance<DataDefinition>()
            .filter { it.identifier.type == I64.INSTANCE }
            .filter { it.identifier.mappedName == IDENT_ARR_I64_A.mappedName + "_arr" }
            .filter { it.value == "16 dup " + I64.INSTANCE.defaultValue }
            .count())
        // There should be two dimensions
        assertEquals(2, getValueOfDataDefinitionAsInt(lines, IDENT_ARR_I64_A.mappedName + "_num_dims"))
        // Of size four
        assertEquals(4, getValueOfDataDefinitionAsInt(lines, IDENT_ARR_I64_A.mappedName + "_dim_0"))
        // And four
        assertEquals(4, getValueOfDataDefinitionAsInt(lines, IDENT_ARR_I64_A.mappedName + "_dim_1"))
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
        val lines = result.lines()

        // Move literal value subscript
        assertEquals(1, lines.asSequence()
            .filterIsInstance(MoveImmToReg::class.java)
            .count { it.source == "2" })
        // Move array element
        assertEquals(1, lines.asSequence()
            .filterIsInstance(MoveMemToReg::class.java)
            .count { it.source.contains(IDENT_ARR_I64_A.mappedName + "_arr") })
    }

    @Test
    fun shouldAccessElementInTwoDimensionalArray() {
        // dim a%(3, 2) as integer
        val declarations = listOf(ArrayDeclaration(0, 0, IDENT_ARR_I64_B.name, Arr.from(2, I64.INSTANCE), listOf(IL_3, IL_2)))
        val declarationStatement = VariableDeclarationStatement(0, 0, declarations)

        // print a%(2, 0)
        val arrayAccessExpression = ArrayAccessExpression(0, 0, IDENT_ARR_I64_B, listOf(IL_2, IL_0))
        val printStatement = PrintStatement(0, 0, listOf(arrayAccessExpression))

        val result = assembleProgram(listOf(declarationStatement, printStatement))
        val lines = result.lines()

        // Move array dimension 1
        assertEquals(1, lines.asSequence()
            .filterIsInstance(MoveMemToReg::class.java)
            .count { it.source.contains(IDENT_ARR_I64_B.mappedName + "_dim_1") })
        // Multiply accumulator with dimension 1
        assertEquals(1, lines.asSequence()
            .filterIsInstance(IMulRegWithReg::class.java)
            .count())
        // Move array element
        assertEquals(1, lines.asSequence()
            .filterIsInstance(MoveMemToReg::class.java)
            .count { it.source.contains(IDENT_ARR_I64_B.mappedName + "_arr") })
    }

    @Test
    fun shouldAccessElementInThreeDimensionalArray() {
        // dim a%(4, 2, 3) as integer
        val declarations = listOf(ArrayDeclaration(0, 0, IDENT_ARR_I64_C.name, Arr.from(3, I64.INSTANCE), listOf(IL_4, IL_2, IL_3)))
        val declarationStatement = VariableDeclarationStatement(0, 0, declarations)

        // print a%(2, 0)
        val arrayAccessExpression = ArrayAccessExpression(0, 0, IDENT_ARR_I64_C, listOf(IL_2, IL_0, IL_4))
        val printStatement = PrintStatement(0, 0, listOf(arrayAccessExpression))

        val result = assembleProgram(listOf(declarationStatement, printStatement))
        val lines = result.lines()

        // Move array dimension 1
        assertEquals(1, lines.asSequence()
            .filterIsInstance(MoveMemToReg::class.java)
            .count { it.source.contains(IDENT_ARR_I64_C.mappedName + "_dim_1") })
        // Move array dimension 2
        assertEquals(1, lines.asSequence()
            .filterIsInstance(MoveMemToReg::class.java)
            .count { it.source.contains(IDENT_ARR_I64_C.mappedName + "_dim_2") })
        // Multiply accumulator with dimension 1 and 2
        assertEquals(2, lines.asSequence()
            .filterIsInstance(IMulRegWithReg::class.java)
            .count())
        // Move array element
        assertEquals(1, lines.asSequence()
            .filterIsInstance(MoveMemToReg::class.java)
            .count { it.source.contains(IDENT_ARR_I64_C.mappedName + "_arr") })
    }

    @Test
    fun shouldAccessElementInFloatArray() {
        // dim a%(4) as float
        val declarations = listOf(ArrayDeclaration(0, 0, IDENT_ARR_F64_D.name, TYPE_ARR_F64_1, listOf(IL_4)))
        val declarationStatement = VariableDeclarationStatement(0, 0, declarations)

        // print a%(2)
        val arrayAccessExpression = ArrayAccessExpression(0, 0, IDENT_ARR_F64_D, listOf(IL_2))
        val printStatement = PrintStatement(0, 0, listOf(arrayAccessExpression))

        val result = assembleProgram(listOf(declarationStatement, printStatement))
        val lines = result.lines()

        // Move literal value subscript
        assertEquals(1, lines.asSequence()
            .filterIsInstance(MoveImmToReg::class.java)
            .count { it.source == "2" })
        // Move array element
        assertEquals(1, lines.asSequence()
            .filterIsInstance(MoveMemToFloatReg::class.java)
            .count { it.source.contains(IDENT_ARR_F64_D.mappedName + "_arr") })
    }

    @Test
    fun shouldAccessElementWithSubscriptExpression() {
        // dim a%(4) as float
        val declarations = listOf(ArrayDeclaration(0, 0, IDENT_ARR_F64_D.name, TYPE_ARR_F64_1, listOf(IL_4)))
        val declarationStatement = VariableDeclarationStatement(0, 0, declarations)

        // print a%(1 + 2)
        val addExpression = AddExpression(0, 0, IL_1, IL_2)
        val arrayAccessExpression = ArrayAccessExpression(0, 0, IDENT_ARR_F64_D, listOf(addExpression))
        val printStatement = PrintStatement(0, 0, listOf(arrayAccessExpression))

        val result = assembleProgram(listOf(declarationStatement, printStatement))
        val lines = result.lines()

        // Add registers containing 1 and 2
        assertEquals(1, lines.asSequence()
            .filterIsInstance(AddRegToReg::class.java)
            .count())
        // Move array element
        assertEquals(1, lines.asSequence()
            .filterIsInstance(MoveMemToFloatReg::class.java)
            .count { it.source.contains(IDENT_ARR_F64_D.mappedName + "_arr") })
    }

    @Test
    fun shouldSetElementInOneDimensionalArray() {
        // dim a%(4) as integer
        val declarations = listOf(ArrayDeclaration(0, 0, IDENT_ARR_I64_A.name, TYPE_ARR_I64_1, listOf(IL_4)))
        val declarationStatement = VariableDeclarationStatement(0, 0, declarations)

        // a%(2) = 4
        val arrayAccessExpression = ArrayAccessExpression(0, 0, IDENT_ARR_I64_A, listOf(IL_2))
        val assignStatement = AssignStatement(0, 0, arrayAccessExpression, IL_4)

        val result = assembleProgram(listOf(declarationStatement, assignStatement))
        val lines = result.lines()

        // Move literal value subscript
        assertEquals(1, lines.asSequence()
            .filterIsInstance(MoveImmToReg::class.java)
            .count { it.source == "2" })
        // Move literal value to assign
        assertEquals(1, lines.asSequence()
            .filterIsInstance(MoveImmToReg::class.java)
            .count { it.source == "4" })
        // Move array element
        assertEquals(1, lines.asSequence()
            .filterIsInstance(MoveRegToMem::class.java)
            .count { it.destination.contains(IDENT_ARR_I64_A.mappedName + "_arr") })
    }

    @Test
    fun shouldSwapIntegerAndArrayElement() {
        // dim a%(4) as integer
        val declarations = listOf(ArrayDeclaration(0, 0, IDENT_ARR_I64_A.name, TYPE_ARR_I64_1, listOf(IL_4)))
        val declarationStatement = VariableDeclarationStatement(0, 0, declarations)

        val arrayAccessExpression = ArrayAccessExpression(0, 0, IDENT_ARR_I64_A, listOf(IL_2))
        val swapStatement = SwapStatement(0, 0, arrayAccessExpression, NAME_H)

        val result = assembleProgram(listOf(declarationStatement, swapStatement))
        val lines = result.lines()

        // Variable a% should be defined and be an array of integers
        assertEquals(1, lines.asSequence()
            .filterIsInstance<DataDefinition>()
            .map { it.identifier }
            .count { it.type == I64.INSTANCE && it.mappedName == IDENT_ARR_I64_A.mappedName + "_arr" })
        // Variable h% should be defined and be an integer
        assertEquals(1, lines.asSequence()
            .filterIsInstance<DataDefinition>()
            .map { it.identifier }
            .count { it.type == I64.INSTANCE && it.mappedName == IDENT_I64_H.mappedName })
        // Moving the variable contents to registers
        assertEquals(2, countInstances(MoveMemToReg::class.java, lines))
        // Moving the register contents to variables
        assertEquals(2, countInstances(MoveRegToMem::class.java, lines))
    }

    @Test
    fun shouldSwapStringAndArrayElement() {
        // dim a%(4) as integer
        val declarations = listOf(ArrayDeclaration(0, 0, IDENT_ARR_STR_S.name, TYPE_ARR_STR_1, listOf(IL_4)))
        val declarationStatement = VariableDeclarationStatement(0, 0, declarations)

        val arrayAccessExpression = ArrayAccessExpression(0, 0, IDENT_ARR_STR_S, listOf(IL_2))
        val swapStatement = SwapStatement(0, 0, arrayAccessExpression, NAME_B)

        val result = assembleProgram(listOf(declarationStatement, swapStatement))
        val lines = result.lines()

        // Variable s$ should be defined and be an array of strings
        assertEquals(1, lines.asSequence()
            .filterIsInstance<DataDefinition>()
            .map { it.identifier }
            .count { it.type == Str.INSTANCE && it.mappedName == IDENT_ARR_STR_S.mappedName + "_arr" })
        // Variable b$ should be defined and be a string
        assertEquals(1, lines.asSequence()
            .filterIsInstance<DataDefinition>()
            .map { it.identifier }
            .count { it.type == Str.INSTANCE && it.mappedName == IDENT_STR_B.mappedName })
        // Moving the variable contents (and variable type pointers) to registers
        assertEquals(4, countInstances(MoveMemToReg::class.java, lines))
        // Moving the register contents to variables (and variable type pointers)
        assertEquals(4, countInstances(MoveRegToMem::class.java, lines))
    }

    @Test
    fun shouldSwapIntegerArrayElementAndFloatArrayElement() {
        // dim a%(4) as integer
        // dim d#(2) as double
        val declarations = listOf(
            ArrayDeclaration(0, 0, IDENT_ARR_I64_A.name, TYPE_ARR_I64_1, listOf(IL_4)),
            ArrayDeclaration(0, 0, IDENT_ARR_F64_D.name, TYPE_ARR_F64_1, listOf(IL_2))
        )
        val declarationStatement = VariableDeclarationStatement(0, 0, declarations)

        val integerArrayAccess = ArrayAccessExpression(0, 0, IDENT_ARR_I64_A, listOf(IL_2))
        val floatArrayAccess = ArrayAccessExpression(0, 0, IDENT_ARR_F64_D, listOf(IL_0))
        val swapStatement = SwapStatement(0, 0, integerArrayAccess, floatArrayAccess)

        val result = assembleProgram(listOf(declarationStatement, swapStatement))
        val lines = result.lines()

        // Variable a% should be defined and be an array of integers
        assertEquals(1, lines.asSequence()
            .filterIsInstance<DataDefinition>()
            .map { it.identifier }
            .count { it.type == I64.INSTANCE && it.mappedName == IDENT_ARR_I64_A.mappedName + "_arr" })
        // Variable d_hash should be defined and be an array of floats
        assertEquals(1, lines.asSequence()
            .filterIsInstance<DataDefinition>()
            .map { it.identifier }
            .count { it.type == F64.INSTANCE && it.mappedName == IDENT_ARR_F64_D.mappedName + "_arr" })
        // Move contents of integer array element to register
        assertEquals(1, countInstances(MoveMemToReg::class.java, lines))
        // Move register contents to integer array element
        assertEquals(1, countInstances(MoveRegToMem::class.java, lines))
        // Move contents of float array element to register
        assertEquals(1, countInstances(MoveMemToFloatReg::class.java, lines))
        // Move register contents to float array element
        assertEquals(1, countInstances(MoveFloatRegToMem::class.java, lines))
        // Convert integer to float
        assertEquals(1, countInstances(ConvertIntRegToFloatReg::class.java, lines))
        // Convert float to integer
        assertEquals(1, countInstances(RoundFloatRegToIntReg::class.java, lines))
    }

    /**
     * Extracts the value of the data definition with the given mapped name as an Int.
     * This method expects to find exactly one such data definition, and that the value
     * is actually an integer.
     */
    private fun getValueOfDataDefinitionAsInt(lines: List<Line>, mappedName: String): Int {
        val values = lines
                .filterIsInstance<DataDefinition>()
                .filter { it.identifier.mappedName == mappedName }
                .map { it.value.toInt() }
        assertEquals(1, values.size)
        return values.first()
    }
}
