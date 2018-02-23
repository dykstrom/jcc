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
import se.dykstrom.jcc.common.assembly.base.FloatRegister;
import se.dykstrom.jcc.common.assembly.base.Register;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static se.dykstrom.jcc.common.assembly.base.FloatRegister.XMM0;

/**
 * Tests class {@code AbstractStorageLocation}.
 * 
 * @author Johan Dykstrom
 */
public class AbstractStorageLocationTest {

    private static final FloatRegister THIS_REGISTER = XMM0;

    private final MemoryManager memoryManager = new MemoryManager();
    private final RegisterManager registerManager = new RegisterManager();
    private final FloatRegisterManager floatRegisterManager = new FloatRegisterManager();

    private final FloatRegisterStorageLocation testee = new FloatRegisterStorageLocation(THIS_REGISTER, floatRegisterManager, registerManager, memoryManager);
    
    @Test
    public void shouldRunWithTemporaryRegister() {
        // Given
        Set<Register> registers = new HashSet<>();

        // When
        testee.withTemporaryRegister(registers::add);

        // Then
        assertEquals(1, registers.size());
        Register temporaryRegister = registers.iterator().next();
        Register allocatedRegister = registerManager.allocate(temporaryRegister);
        assertEquals(temporaryRegister, allocatedRegister);
    }

    @Test
    public void shouldRunWithTemporaryMemory() {
        // Given
        Set<String> memories = new HashSet<>();

        // When
        testee.withTemporaryMemory(memories::add);

        // Then
        assertEquals(1, memories.size());
        assertEquals(1, memoryManager.getUsedMemoryAddresses().size());
        String temporaryMemory = memories.iterator().next();
        assertTrue(memoryManager.getUsedMemoryAddresses().contains(getUnmappedName(temporaryMemory)));
        String allocatedMemory = memoryManager.allocate();
        assertEquals(temporaryMemory, allocatedMemory);
        assertEquals(1, memoryManager.getUsedMemoryAddresses().size());
    }

    private String getUnmappedName(String name) {
        return name.substring(1);
    }
}
