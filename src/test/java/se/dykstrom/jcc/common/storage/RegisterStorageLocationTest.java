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

package se.dykstrom.jcc.common.storage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static se.dykstrom.jcc.common.assembly.base.Register.R12;
import static se.dykstrom.jcc.common.assembly.base.Register.RBX;

import org.junit.Test;

import se.dykstrom.jcc.common.assembly.base.CodeContainer;
import se.dykstrom.jcc.common.assembly.base.Register;
import se.dykstrom.jcc.common.assembly.instruction.*;

/**
 * Tests class {@code RegisterStorageLocation}.
 * 
 * @author Johan Dykstrom
 */
public class RegisterStorageLocationTest {

    private static final String MEMORY_ADDRESS = "memory";
    
    private static final Register THIS_REGISTER = RBX;
    private static final Register THAT_REGISTER = R12;

    private final MemoryManager memoryManager = new MemoryManager();
    private final RegisterManager registerManager = new RegisterManager();
    private final CodeContainer codeContainer = new CodeContainer();

    private final StorageLocation registerLocation = new RegisterStorageLocation(THAT_REGISTER, registerManager);
    private final StorageLocation memoryLocation = new MemoryStorageLocation(MEMORY_ADDRESS, memoryManager, registerManager);
    
    private final RegisterStorageLocation testee = new RegisterStorageLocation(THIS_REGISTER, registerManager);
    
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
        assertTrue(codeContainer.codes().get(0) instanceof MoveRegToMem);
    }
    
    @Test
    public void shouldGenerateMoveRegisterLocToThis() {
        testee.moveLocToThis(registerLocation, codeContainer);
        assertTrue(codeContainer.codes().get(0) instanceof MoveRegToReg);
    }
    
    @Test
    public void shouldGenerateMoveMemoryLocToThis() {
        testee.moveLocToThis(memoryLocation, codeContainer);
        assertTrue(codeContainer.codes().get(0) instanceof MoveMemToReg);
    }
    
    @Test
    public void shouldGenerateAddRegisterLocToThis() {
        testee.addLocToThis(registerLocation, codeContainer);
        assertTrue(codeContainer.codes().get(0) instanceof AddRegToReg);
    }
    
    @Test
    public void shouldGenerateAddMemoryLocToThis() {
        testee.addLocToThis(memoryLocation, codeContainer);
        assertTrue(codeContainer.codes().get(0) instanceof AddMemToReg);
    }
    
    @Test
    public void shouldGenerateIDivThisWithRegisterLoc() {
        testee.idivThisWithLoc(registerLocation, codeContainer);
        assertTrue(codeContainer.codes().get(0) instanceof MoveRegToReg);
        assertTrue(codeContainer.codes().get(1) instanceof Cqo);
        assertTrue(codeContainer.codes().get(2) instanceof IDivWithReg);
        assertTrue(codeContainer.codes().get(3) instanceof MoveRegToReg);
    }
    
    @Test
    public void shouldGenerateIDivThisWithMemoryLoc() {
        testee.idivThisWithLoc(memoryLocation, codeContainer);
        assertTrue(codeContainer.codes().get(0) instanceof MoveRegToReg);
        assertTrue(codeContainer.codes().get(1) instanceof Cqo);
        assertTrue(codeContainer.codes().get(2) instanceof IDivWithMem);
        assertTrue(codeContainer.codes().get(3) instanceof MoveRegToReg);
    }
    
    @Test
    public void shouldGenerateIMulRegisterLocWithThis() {
        testee.imulLocWithThis(registerLocation, codeContainer);
        assertTrue(codeContainer.codes().get(0) instanceof IMulRegWithReg);
    }
    
    @Test
    public void shouldGenerateIMulMemoryLocWithThis() {
        testee.imulLocWithThis(memoryLocation, codeContainer);
        assertTrue(codeContainer.codes().get(0) instanceof IMulMemWithReg);
    }
    
    @Test
    public void shouldGenerateSubRegisterLocFromThis() {
        testee.subtractLocFromThis(registerLocation, codeContainer);
        assertTrue(codeContainer.codes().get(0) instanceof SubRegFromReg);
    }
    
    @Test
    public void shouldGenerateSubMemoryLocFromThis() {
        testee.subtractLocFromThis(memoryLocation, codeContainer);
        assertTrue(codeContainer.codes().get(0) instanceof SubMemFromReg);
    }
    
    @Test
    public void shouldGenerateCmpRegisterLocWithThis() {
        testee.compareThisWithLoc(registerLocation, codeContainer);
        assertTrue(codeContainer.codes().get(0) instanceof CmpRegWithReg);
    }
    
    @Test
    public void shouldGenerateCmpMemoryLocWithThis() {
        testee.compareThisWithLoc(memoryLocation, codeContainer);
        assertTrue(codeContainer.codes().get(0) instanceof CmpRegWithMem);
    }
    
    @Test
    public void shouldGenerateAndRegisterLocWithThis() {
        testee.andLocWithThis(registerLocation, codeContainer);
        assertTrue(codeContainer.codes().get(0) instanceof AndRegWithReg);
    }
    
    @Test
    public void shouldGenerateAndMemoryLocWithThis() {
        testee.andLocWithThis(memoryLocation, codeContainer);
        assertTrue(codeContainer.codes().get(0) instanceof AndMemWithReg);
    }
    
    @Test
    public void shouldGenerateOrRegisterLocWithThis() {
        testee.orLocWithThis(registerLocation, codeContainer);
        assertTrue(codeContainer.codes().get(0) instanceof OrRegWithReg);
    }
    
    @Test
    public void shouldGenerateOrMemoryLocWithThis() {
        testee.orLocWithThis(memoryLocation, codeContainer);
        assertTrue(codeContainer.codes().get(0) instanceof OrMemWithReg);
    }
}
