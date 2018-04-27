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

import se.dykstrom.jcc.common.assembly.base.FloatRegister;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import static java.util.Arrays.asList;
import static se.dykstrom.jcc.common.assembly.base.FloatRegister.*;

/**
 * Manages allocation and de-allocation of floating point registers.
 *
 * @author Johan Dykstrom
 */
public class FloatRegisterManager {

    // Reserved registers:  XMM0, XMM1, XMM2, XMM3
    private static final List<FloatRegister> VOLATILE_REGISTERS = asList(XMM4, XMM5);
    private static final List<FloatRegister> NON_VOLATILE_REGISTERS = asList(XMM6, XMM7, XMM8, XMM9, XMM10, XMM11, XMM12, XMM13, XMM14, XMM15);

    private final Set<FloatRegister> freeVolatileRegisters = new HashSet<>(VOLATILE_REGISTERS);
    private final Set<FloatRegister> freeNonVolatileRegisters = new HashSet<>(NON_VOLATILE_REGISTERS);

    private final Set<FloatRegister> usedNonVolatileRegisters = new HashSet<>();

    /**
     * Allocates a temporary volatile floating point register, executes {@code consumer},
     * and safely de-allocates the register again.
     *
     * @param consumer The consumer represents the code to run with the temporary register being allocated.
     */
    protected void withTemporaryFloatRegister(Consumer<FloatRegister> consumer) {
        FloatRegister register = allocateVolatile();
        if (register == null) throw new IllegalStateException("no volatile floating point register available");
        try {
            consumer.accept(register);
        } finally {
            free(register);
        }
    }

    /**
     * Allocates a non-volatile register for temporary storage. If there is no non-volatile register available,
     * this method returns {@code null}.
     */
    FloatRegister allocateNonVolatile() {
        return allocateFirstPossible(NON_VOLATILE_REGISTERS);
    }

    /**
     * Allocates a volatile register for temporary storage. If there is no volatile register available,
     * this method returns {@code null}.
     */
    private FloatRegister allocateVolatile() {
        return allocateFirstPossible(VOLATILE_REGISTERS);
    }

    private FloatRegister allocateFirstPossible(List<FloatRegister> registers) {
        FloatRegister result = null;
        Iterator<FloatRegister> iterator = registers.iterator();
        while (result == null && iterator.hasNext()) {
            result = allocateIfPossible(iterator.next());
        }
        return result;
    }

    private FloatRegister allocateIfPossible(FloatRegister register) {
        if (register.isVolatile()) {
            return allocateVolatileIfPossible(register);
        } else {
            return allocateNonVolatileIfPossible(register);
        }
    }

    private FloatRegister allocateVolatileIfPossible(FloatRegister register) {
        if (freeVolatileRegisters.contains(register)) {
            freeVolatileRegisters.remove(register);
            return register;
        }
        return null;
    }

    private FloatRegister allocateNonVolatileIfPossible(FloatRegister register) {
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
    void free(FloatRegister register) {
        if (register.isVolatile()) {
            freeVolatileRegisters.add(register);
        } else {
            freeNonVolatileRegisters.add(register);
        }
    }

    /**
     * Returns the set of used non-volatile registers, that need to be pushed on the stack.
     */
    public Set<FloatRegister> getUsedNonVolatileRegisters() {
        return usedNonVolatileRegisters;
    }
}
