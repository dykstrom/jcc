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

package se.dykstrom.jcc.common.storage;

import org.junit.Test;
import se.dykstrom.jcc.common.assembly.base.CodeContainer;
import se.dykstrom.jcc.common.assembly.base.Comment;
import se.dykstrom.jcc.common.assembly.base.FloatRegister;
import se.dykstrom.jcc.common.assembly.base.Register;
import se.dykstrom.jcc.common.assembly.instruction.MoveImmToReg;
import se.dykstrom.jcc.common.assembly.instruction.MoveRegToMem;
import se.dykstrom.jcc.common.assembly.instruction.floating.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static se.dykstrom.jcc.common.assembly.base.FloatRegister.XMM0;
import static se.dykstrom.jcc.common.assembly.base.FloatRegister.XMM1;

/**
 * Tests class {@code FloatRegisterStorageLocation}.
 * 
 * @author Johan Dykstrom
 */
public class FloatRegisterStorageLocationTest {

    private static final String MEMORY_ADDRESS = "memory";
    private static final String FLOAT_LITERAL = "2.75E+3";

    private static final FloatRegister THIS_REGISTER = XMM0;
    private static final FloatRegister THAT_REGISTER = XMM1;

    private final MemoryManager memoryManager = new MemoryManager();
    private final RegisterManager registerManager = new RegisterManager();
    private final FloatRegisterManager floatRegisterManager = new FloatRegisterManager();

    private final StorageLocation floatRegisterLocation = new FloatRegisterStorageLocation(THAT_REGISTER, floatRegisterManager, registerManager, memoryManager);
    private final StorageLocation registerLocation = new RegisterStorageLocation(Register.RCX, registerManager);
    private final StorageLocation memoryLocation = new MemoryStorageLocation(MEMORY_ADDRESS, memoryManager, registerManager);

    private final CodeContainer codeContainer = new CodeContainer();

    private final FloatRegisterStorageLocation testee = new FloatRegisterStorageLocation(THIS_REGISTER, floatRegisterManager, registerManager, memoryManager);

    @Test
    public void shouldGetRegister() {
        assertEquals(THIS_REGISTER, testee.getRegister());
    }
    
    @Test
    public void shouldCloseAndFreeRegister() {
        testee.close();
    }
    
    @Test
    public void shouldGenerateMoveThisToMem() {
        testee.moveThisToMem(MEMORY_ADDRESS, codeContainer);
        assertTrue(codeContainer.codes().get(0) instanceof MoveFloatRegToMem);
    }

    @Test
    public void shouldGenerateMoveImmediateToThis() {
        testee.moveImmToThis(FLOAT_LITERAL, codeContainer);
        assertTrue(codeContainer.codes().get(0) instanceof Comment);
        assertTrue(codeContainer.codes().get(1) instanceof MoveImmToReg);
        assertTrue(codeContainer.codes().get(2) instanceof MoveRegToMem);
        assertTrue(codeContainer.codes().get(3) instanceof MoveMemToFloatReg);
    }

    @Test
    public void shouldGenerateMoveMemToThis() {
        testee.moveMemToThis(MEMORY_ADDRESS, codeContainer);
        assertTrue(codeContainer.codes().get(0) instanceof MoveMemToFloatReg);
    }

    @Test
    public void shouldGenerateAddFloatRegLocToThis() {
        testee.addLocToThis(floatRegisterLocation, codeContainer);
        assertTrue(codeContainer.codes().get(0) instanceof AddFloatRegToFloatReg);
    }

    @Test
    public void shouldGenerateAddMemoryLocToThis() {
        testee.addLocToThis(memoryLocation, codeContainer);
        assertTrue(codeContainer.codes().get(0) instanceof ConvertIntMemToFloatReg);
        assertTrue(codeContainer.codes().get(1) instanceof AddFloatRegToFloatReg);
    }

    @Test
    public void shouldGenerateAddRegLocToThis() {
        testee.addLocToThis(registerLocation, codeContainer);
        assertTrue(codeContainer.codes().get(0) instanceof ConvertIntRegToFloatReg);
        assertTrue(codeContainer.codes().get(1) instanceof AddFloatRegToFloatReg);
    }
}
