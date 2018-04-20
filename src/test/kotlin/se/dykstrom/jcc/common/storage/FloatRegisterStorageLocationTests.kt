/*
 * Copyright (C) 2018 Johan Dykstrom
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

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import se.dykstrom.jcc.common.assembly.base.Code
import se.dykstrom.jcc.common.assembly.base.CodeContainer
import se.dykstrom.jcc.common.assembly.base.Comment
import se.dykstrom.jcc.common.assembly.base.FloatRegister.XMM6
import se.dykstrom.jcc.common.assembly.base.FloatRegister.XMM7
import se.dykstrom.jcc.common.assembly.base.Register
import se.dykstrom.jcc.common.assembly.instruction.MoveImmToReg
import se.dykstrom.jcc.common.assembly.instruction.MoveRegToMem
import se.dykstrom.jcc.common.assembly.instruction.floating.*
import kotlin.reflect.KClass

/**
 * Tests class `FloatRegisterStorageLocation`.
 *
 * @author Johan Dykstrom
 */
class FloatRegisterStorageLocationTests {

    companion object {
        private val MEMORY_ADDRESS = "memory"
        private val FLOAT_LITERAL = "2.75E+3"

        private val THIS_REGISTER = XMM6
        private val THAT_REGISTER = XMM7
    }

    private val memoryManager = MemoryManager()
    private val registerManager = RegisterManager()
    private val floatRegisterManager = FloatRegisterManager()

    private val floatRegisterLocation = FloatRegisterStorageLocation(THAT_REGISTER, floatRegisterManager, registerManager, memoryManager)
    private val registerLocation = RegisterStorageLocation(Register.RCX, registerManager, null)
    private val memoryLocation = MemoryStorageLocation(MEMORY_ADDRESS, memoryManager, registerManager)

    private val codeContainer = CodeContainer()

    private val testee = FloatRegisterStorageLocation(THIS_REGISTER, floatRegisterManager, registerManager, memoryManager)

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
        assertCodeClasses(codeContainer.codes(), MoveFloatRegToMem::class)
    }

    @Test
    fun shouldGenerateMoveImmediateToThis() {
        testee.moveImmToThis(FLOAT_LITERAL, codeContainer)
        assertCodeClasses(codeContainer.codes(), Comment::class, MoveImmToReg::class, MoveRegToMem::class, MoveMemToFloatReg::class)
    }

    @Test
    fun shouldGenerateMoveMemToThis() {
        testee.moveMemToThis(MEMORY_ADDRESS, codeContainer)
        assertCodeClasses(codeContainer.codes(), MoveMemToFloatReg::class)
    }

    @Test
    fun shouldGenerateAddFloatRegLocToThis() {
        testee.addLocToThis(floatRegisterLocation, codeContainer)
        assertCodeClasses(codeContainer.codes(), AddFloatRegToFloatReg::class)
    }

    @Test
    fun shouldGenerateAddMemoryLocToThis() {
        testee.addLocToThis(memoryLocation, codeContainer)
        assertCodeClasses(codeContainer.codes(), ConvertIntMemToFloatReg::class, AddFloatRegToFloatReg::class)
    }

    @Test
    fun shouldGenerateAddRegLocToThis() {
        testee.addLocToThis(registerLocation, codeContainer)
        assertCodeClasses(codeContainer.codes(), ConvertIntRegToFloatReg::class, AddFloatRegToFloatReg::class)
    }

    @Test
    fun shouldGenerateMulFloatRegLocWithThis() {
        testee.multiplyLocWithThis(floatRegisterLocation, codeContainer)
        assertCodeClasses(codeContainer.codes(), MulFloatRegWithFloatReg::class)
    }

    @Test
    fun shouldGenerateMulMemoryLocWithThis() {
        testee.multiplyLocWithThis(memoryLocation, codeContainer)
        assertCodeClasses(codeContainer.codes(), ConvertIntMemToFloatReg::class, MulFloatRegWithFloatReg::class)
    }

    @Test
    fun shouldGenerateMulRegLocWithThis() {
        testee.multiplyLocWithThis(registerLocation, codeContainer)
        assertCodeClasses(codeContainer.codes(), ConvertIntRegToFloatReg::class, MulFloatRegWithFloatReg::class)
    }

    @Test
    fun shouldGenerateDivThisWithFloatRegLoc() {
        testee.divideThisWithLoc(floatRegisterLocation, codeContainer)
        assertCodeClasses(codeContainer.codes(), DivFloatRegWithFloatReg::class)
    }

    @Test
    fun shouldGenerateDivThisWithMemoryLoc() {
        testee.divideThisWithLoc(memoryLocation, codeContainer)
        assertCodeClasses(codeContainer.codes(), ConvertIntMemToFloatReg::class, DivFloatRegWithFloatReg::class)
    }

    @Test
    fun shouldGenerateDivThisWithRegLoc() {
        testee.divideThisWithLoc(registerLocation, codeContainer)
        assertCodeClasses(codeContainer.codes(), ConvertIntRegToFloatReg::class, DivFloatRegWithFloatReg::class)
    }

    @Test(expected = UnsupportedOperationException::class)
    fun shouldNotGenerateIDivThisWithFloatRegLoc() {
        testee.idivThisWithLoc(floatRegisterLocation, codeContainer)
    }

    @Test(expected = UnsupportedOperationException::class)
    fun shouldNotGenerateModThisWithFloatRegLoc() {
        testee.modThisWithLoc(floatRegisterLocation, codeContainer)
    }

    @Test
    fun shouldGenerateCompareThisWithImm() {
        testee.compareThisWithImm(FLOAT_LITERAL, codeContainer)
        assertCodeClasses(codeContainer.codes(), Comment::class, MoveImmToReg::class, MoveRegToMem::class, CompareFloatRegWithMem::class)
    }

    @Test
    fun shouldGenerateCompareThisWithFloatRegLoc() {
        testee.compareThisWithLoc(floatRegisterLocation, codeContainer)
        assertCodeClasses(codeContainer.codes(), CompareFloatRegWithFloatReg::class)
    }

    @Test
    fun shouldGenerateCompareThisWithMemoryLoc() {
        testee.compareThisWithLoc(memoryLocation, codeContainer)
        assertCodeClasses(codeContainer.codes(), ConvertIntMemToFloatReg::class, CompareFloatRegWithFloatReg::class)
    }

    @Test
    fun shouldGenerateCompareThisWithRegLoc() {
        testee.compareThisWithLoc(registerLocation, codeContainer)
        assertCodeClasses(codeContainer.codes(), ConvertIntRegToFloatReg::class, CompareFloatRegWithFloatReg::class)
    }

    /**
     * Asserts that the classes of the codes in `codes` match the classes in `expectedClasses`.
     */
    private fun assertCodeClasses(codes: List<Code>, vararg expectedClasses: KClass<out Code>) {
        val actualClasses = codes.map { it::class }.toTypedArray()
        assertArrayEquals(expectedClasses, actualClasses)
    }
}
