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

import java.util.HashSet;
import java.util.Set;

/**
 * Manages allocation and de-allocation of memory addresses.
 *
 * @author Johan Dykstrom
 */
public class MemoryManager {

    private static final String PREFIX = "_tmp_location_";
    
    private final Set<String> freeMemoryAddresses = new HashSet<>();
    private final Set<String> usedMemoryAddresses = new HashSet<>();

    private int memoryIndex = 0;

    /**
     * Allocates a new memory address for temporary storage.
     */
    public String allocate() {
        String memory;
        
        if (!freeMemoryAddresses.isEmpty()) {
            // Reuse one of the already existing temporary memory addresses
            memory = freeMemoryAddresses.iterator().next();
            freeMemoryAddresses.remove(memory);
        } else {
            // Create a new temporary memory address
            memory = PREFIX + memoryIndex++;
            usedMemoryAddresses.add(memory);
        }
        return getMappedName(memory);
    }

    /**
     * Frees the given memory address, and makes it available to use again.
     */
    public void free(String memory) {
        freeMemoryAddresses.add(getUnmappedName(memory));
    }

    /**
     * Returns the set of used temporary memory addresses, that need to be created in the data section.
     */
    public Set<String> getUsedMemoryAddresses() {
        return usedMemoryAddresses;
    }

    /**
     * Returns the mapped name of the memory address, that is, the name that should be used in code generation
     * to avoid any clashes with the backend assembler reserved words.
     */
    private String getMappedName(String name) {
        return "_" + name;
    }

    private String getUnmappedName(String name) {
        return name.substring(1);
    }
}
