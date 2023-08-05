/*
 * Copyright (C) 2021 Johan Dykstrom
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

package se.dykstrom.jcc.common.storage

import org.junit.Assert.assertTrue
import org.junit.Test
import se.dykstrom.jcc.common.intermediate.CodeContainer
import se.dykstrom.jcc.common.assembly.instruction.CmpRegWithImm
import se.dykstrom.jcc.common.assembly.instruction.CmpRegWithReg
import se.dykstrom.jcc.common.assembly.instruction.MoveImmToReg
import se.dykstrom.jcc.common.assembly.instruction.MoveMemToReg

/**
 * Tests class `RegisterStorageLocation`.
 *
 * @author Johan Dykstrom
 */
class MemoryStorageLocationTests {

    companion object {
        private const val MEMORY_ADDRESS = "memory"

        private const val SMALL_NUMBER = "17"
        private const val LARGE_NUMBER = "${Integer.MAX_VALUE + 10_000L}"
    }

    private val memoryManager = MemoryManager()
    private val registerManager = RegisterManager()
    private val codeContainer = CodeContainer()

    private val testee = MemoryStorageLocation(MEMORY_ADDRESS, memoryManager, registerManager)

    @Test
    fun shouldGenerateCmpThisWithSmallImm() {
        testee.compareThisWithImm(SMALL_NUMBER, codeContainer)
        assertTrue(codeContainer.lines()[0] is MoveMemToReg)
        assertTrue(codeContainer.lines()[1] is CmpRegWithImm)
    }

    @Test
    fun shouldGenerateCmpThisWithLargeImm() {
        testee.compareThisWithImm(LARGE_NUMBER, codeContainer)
        assertTrue(codeContainer.lines()[0] is MoveMemToReg)
        assertTrue(codeContainer.lines()[1] is MoveImmToReg)
        assertTrue(codeContainer.lines()[2] is CmpRegWithReg)
    }
}
