/*
 * Copyright (C) 2017 Johan Dykstrom
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

package se.dykstrom.jcc.common.assembly.base

import org.junit.Assert.assertEquals
import org.junit.Test
import se.dykstrom.jcc.common.assembly.base.Register.RAX
import se.dykstrom.jcc.common.assembly.base.Register.RDX
import se.dykstrom.jcc.common.assembly.instruction.AddImmToReg
import se.dykstrom.jcc.common.assembly.instruction.MoveRegToReg
import se.dykstrom.jcc.common.assembly.instruction.Ret
import se.dykstrom.jcc.common.intermediate.CodeContainer

class CodeContainerTests {

    companion object {
        private val ADD = AddImmToReg("0", RDX)
        private val MOVE = MoveRegToReg(RDX, RAX)
        private val RET = Ret()
    }

    private val codeContainer = CodeContainer()

    @Test
    fun shouldAddAll() {
        codeContainer.add(ADD).addAll(listOf(MOVE, RET))
        val expectedLines = listOf(ADD, MOVE, RET)
        assertEquals(expectedLines, codeContainer.lines())
    }

    @Test
    fun shouldAddFirst() {
        codeContainer.addAll(listOf(MOVE, RET)).addFirst(ADD)
        val expectedLines = listOf(ADD, MOVE, RET)
        assertEquals(expectedLines, codeContainer.lines())
    }

    @Test
    fun shouldAddAllFirst() {
        codeContainer.add(RET).addAllFirst(listOf(ADD, MOVE))
        val expectedLines = listOf(ADD, MOVE, RET)
        assertEquals(expectedLines, codeContainer.lines())
    }
}
