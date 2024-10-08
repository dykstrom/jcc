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

package se.dykstrom.jcc.common.storage

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import se.dykstrom.jcc.common.assembly.base.Register.R12
import se.dykstrom.jcc.common.assembly.base.Register.RBX
import se.dykstrom.jcc.common.assembly.instruction.*
import se.dykstrom.jcc.common.code.CodeContainer
import se.dykstrom.jcc.common.code.Line
import java.util.*
import kotlin.reflect.KClass

/**
 * Tests class `RegisterStorageLocation`.
 *
 * @author Johan Dykstrom
 */
class RegisterStorageLocationTests {

    companion object {
        private const val MEMORY_ADDRESS = "memory"

        private const val SMALL_NUMBER = "17"
        private const val LARGE_NUMBER = "${Integer.MAX_VALUE + 10_000L}"

        private val THIS_REGISTER = RBX
        private val THAT_REGISTER = R12
    }

    private val memoryManager = MemoryManager()
    private val registerManager = RegisterManager()
    private val codeContainer = CodeContainer()
    private val registerLocation = RegisterStorageLocation(THAT_REGISTER, registerManager, null)

    private val memoryLocation = MemoryStorageLocation(MEMORY_ADDRESS, memoryManager, registerManager)

    private val testee = RegisterStorageLocation(THIS_REGISTER, registerManager, null)

    @Test
    fun shouldGetRegister() {
        assertEquals(THIS_REGISTER, testee.register)
    }

    @Test
    fun shouldCloseAndFreeRegister() {
        testee.close()
    }

    @Test
    fun shouldGenerateMoveThisToMem() {
        testee.moveThisToMem(MEMORY_ADDRESS, codeContainer)
        assertTrue(codeContainer.lines()[0] is MoveRegToMem)
    }

    @Test
    fun shouldGenerateMoveRegisterLocToThis() {
        testee.moveLocToThis(registerLocation, codeContainer)
        assertTrue(codeContainer.lines()[0] is MoveRegToReg)
    }

    @Test
    fun shouldGenerateMoveMemoryLocToThis() {
        testee.moveLocToThis(memoryLocation, codeContainer)
        assertTrue(codeContainer.lines()[0] is MoveMemToReg)
    }

    @Test
    fun shouldGenerateMoveMemoryAddressToThis() {
        testee.moveMemToThis(MEMORY_ADDRESS, 4, registerLocation.register, codeContainer)
        val instruction = codeContainer.lines()[0] as MoveMemToReg
        assertEquals(testee.register.name.lowercase(Locale.getDefault()), instruction.destination)
        assertEquals("[memory+4*r12]", instruction.source)
    }

    @Test
    fun shouldGenerateAddRegisterLocToThis() {
        testee.addLocToThis(registerLocation, codeContainer)
        assertTrue(codeContainer.lines()[0] is AddRegToReg)
    }

    @Test
    fun shouldGenerateAddMemoryLocToThis() {
        testee.addLocToThis(memoryLocation, codeContainer)
        assertTrue(codeContainer.lines()[0] is AddMemToReg)
    }

    @Test
    fun shouldGenerateIDivThisWithRegisterLoc() {
        testee.idivThisWithLoc(registerLocation, codeContainer)
        assertCodeClasses(codeContainer.lines(), MoveRegToReg::class, Cqo::class, IDivWithReg::class, MoveRegToReg::class)
    }

    @Test
    fun shouldGenerateIDivThisWithMemoryLoc() {
        testee.idivThisWithLoc(memoryLocation, codeContainer)
        assertCodeClasses(codeContainer.lines(), MoveRegToReg::class, Cqo::class, IDivWithMem::class, MoveRegToReg::class)
    }

    @Test
    fun shouldNotGenerateDivThisWithRegisterLoc() {
        assertThrows<UnsupportedOperationException> { testee.divideThisWithLoc(registerLocation, codeContainer) }
    }

    @Test
    fun shouldNotGenerateDivThisWithMemoryLoc() {
        assertThrows<UnsupportedOperationException> { testee.divideThisWithLoc(memoryLocation, codeContainer) }
    }

    @Test
    fun shouldGenerateIMulRegisterLocWithThis() {
        testee.multiplyLocWithThis(registerLocation, codeContainer)
        assertTrue(codeContainer.lines()[0] is IMulRegWithReg)
    }

    @Test
    fun shouldGenerateIMulMemoryLocWithThis() {
        testee.multiplyLocWithThis(memoryLocation, codeContainer)
        assertTrue(codeContainer.lines()[0] is IMulMemWithReg)
    }

    @Test
    fun shouldGenerateSubRegisterLocFromThis() {
        testee.subtractLocFromThis(registerLocation, codeContainer)
        assertTrue(codeContainer.lines()[0] is SubRegFromReg)
    }

    @Test
    fun shouldGenerateSubMemoryLocFromThis() {
        testee.subtractLocFromThis(memoryLocation, codeContainer)
        assertTrue(codeContainer.lines()[0] is SubMemFromReg)
    }

    @Test
    fun shouldGenerateCmpThisWithRegisterLoc() {
        testee.compareThisWithLoc(registerLocation, codeContainer)
        assertTrue(codeContainer.lines()[0] is CmpRegWithReg)
    }

    @Test
    fun shouldGenerateCmpThisWithMemoryLoc() {
        testee.compareThisWithLoc(memoryLocation, codeContainer)
        assertTrue(codeContainer.lines()[0] is CmpRegWithMem)
    }

    @Test
    fun shouldGenerateCmpThisWithSmallImm() {
        testee.compareThisWithImm(SMALL_NUMBER, codeContainer)
        assertTrue(codeContainer.lines()[0] is CmpRegWithImm)
    }

    @Test
    fun shouldGenerateCmpThisWithLargeImm() {
        testee.compareThisWithImm(LARGE_NUMBER, codeContainer)
        assertTrue(codeContainer.lines()[0] is MoveImmToReg)
        assertEquals(LARGE_NUMBER, (codeContainer.lines()[0] as MoveImmToReg).immediate)
        assertTrue(codeContainer.lines()[1] is CmpRegWithReg)
        assertTrue(codeContainer.lines()[1].toText().startsWith("cmp " + THIS_REGISTER.name.lowercase(Locale.getDefault())))
    }

    @Test
    fun shouldGenerateAndRegisterLocWithThis() {
        testee.andLocWithThis(registerLocation, codeContainer)
        assertTrue(codeContainer.lines()[0] is AndRegWithReg)
    }

    @Test
    fun shouldGenerateAndMemoryLocWithThis() {
        testee.andLocWithThis(memoryLocation, codeContainer)
        assertTrue(codeContainer.lines()[0] is AndMemWithReg)
    }

    @Test
    fun shouldGenerateOrRegisterLocWithThis() {
        testee.orLocWithThis(registerLocation, codeContainer)
        assertTrue(codeContainer.lines()[0] is OrRegWithReg)
    }

    @Test
    fun shouldGenerateOrMemoryLocWithThis() {
        testee.orLocWithThis(memoryLocation, codeContainer)
        assertTrue(codeContainer.lines()[0] is OrMemWithReg)
    }

    /**
     * Asserts that the classes of the code lines in `lines` match the classes in `expectedClasses`.
     */
    private fun assertCodeClasses(lines: List<Line>, vararg expectedClasses: KClass<out Line>) {
        val actualClasses = lines.map { it::class }.toTypedArray()
        assertArrayEquals(expectedClasses, actualClasses)
    }
}
