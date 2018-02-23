/*
 * Copyright (C) 2016 Johan Dykstrom
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

import se.dykstrom.jcc.common.assembly.base.Register;

import java.util.*;

import static java.util.Arrays.asList;
import static se.dykstrom.jcc.common.assembly.base.Register.*;

/**
 * Manages allocation and de-allocation of general purpose registers.
 *
 * @author Johan Dykstrom
 */
public class RegisterManager {

    private static final List<Register> VOLATILE_REGISTERS = asList(RAX, RCX, RDX, R8, R9, R10, R11);
    private static final List<Register> NON_VOLATILE_REGISTERS = asList(RBX, RDI, RSI, R12, R13, R14, R15); // RBP, RSP

    private final Set<Register> freeVolatileRegisters = new HashSet<>(VOLATILE_REGISTERS);
    private final Set<Register> freeNonVolatileRegisters = new HashSet<>(NON_VOLATILE_REGISTERS);

    private final Set<Register> usedNonVolatileRegisters = new HashSet<>();

    /**
     * Allocates a volatile register for temporary storage. If there is no volatile register available,
     * this method returns {@code null}.
     */
    Register allocateVolatile() {
        return allocateFirstPossible(VOLATILE_REGISTERS);
    }

    /**
     * Allocates a non-volatile register for temporary storage. If there is no non-volatile register available,
     * this method returns {@code null}.
     */
    Register allocateNonVolatile() {
        return allocateFirstPossible(NON_VOLATILE_REGISTERS);
    }

    /**
     * Allocates {@code register} to use for temporary storage if possible.
     * If {@code register} is not available, this method returns {@code null}.
     *
     * @param register The register to allocate.
     */
    Register allocate(Register register) {
        return allocateIfPossible(register);
    }

    private Register allocateFirstPossible(List<Register> registers) {
        Register result = null;
        Iterator<Register> iterator = registers.iterator();
        while (result == null && iterator.hasNext()) {
            result = allocateIfPossible(iterator.next());
        }
        return result;
    }

    private Register allocateIfPossible(Register register) {
        if (register.isVolatile()) {
            return allocateVolatileIfPossible(register);
        } else {
            return allocateNonVolatileIfPossible(register);
        }
    }

    private Register allocateVolatileIfPossible(Register register) {
        if (freeVolatileRegisters.contains(register)) {
            freeVolatileRegisters.remove(register);
            return register;
        }
        return null;
    }

    private Register allocateNonVolatileIfPossible(Register register) {
        if (freeNonVolatileRegisters.contains(register)) {
            freeNonVolatileRegisters.remove(register);
            usedNonVolatileRegisters.add(register);
            return register;
        }
        return null;
    }

    /**
     * Frees the given register, and makes it available to use again.
     */
    void free(Register register) {
        if (register.isVolatile()) {
            freeVolatileRegisters.add(register);
        } else {
            freeNonVolatileRegisters.add(register);
        }
    }

    /**
     * Returns the set of used non-volatile registers, that need to be pushed on the stack.
     */
    public Set<Register> getUsedNonVolatileRegisters() {
        return usedNonVolatileRegisters;
    }
}
