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

import org.junit.Assert.assertEquals
import org.junit.Test
import se.dykstrom.jcc.common.assembly.other.DataDefinition
import se.dykstrom.jcc.common.ast.ArrayDeclaration
import se.dykstrom.jcc.common.ast.VariableDeclarationStatement
import se.dykstrom.jcc.common.types.Arr
import se.dykstrom.jcc.common.types.I64

/**
 * Tests features related to arrays in code generation.
 *
 * @author Johan Dykstrom
 */
class BasicCodeGeneratorArrayTests : AbstractBasicCodeGeneratorTest() {

    @Test
    fun shouldDefineIntegerArray() {
        // dim a%(3) as integer
        val declarations = listOf(ArrayDeclaration(0, 0, IDENT_I64_A.name, Arr.from(1, I64.INSTANCE),  listOf(IL_3)))
        val statement = VariableDeclarationStatement(0, 0, declarations)

        val result = assembleProgram(listOf(statement))
        val codes = result.codes()

        codes.forEach { println(it.toAsm()) }

        // Variable a% should be defined and be an array of integers
        assertEquals(1, codes
                .filterIsInstance(DataDefinition::class.java)
                .map { it.identifier }
                .count { it.mappedName == IDENT_I64_A.mappedName && it.type == I64.INSTANCE })
    }
}
