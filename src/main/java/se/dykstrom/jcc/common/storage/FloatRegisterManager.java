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

import static java.util.Arrays.asList;
import static se.dykstrom.jcc.common.assembly.base.FloatRegister.*;

/**
 * Manages allocation and de-allocation of floating point registers.
 *
 * @author Johan Dykstrom
 */
public class FloatRegisterManager {

    private static final List<FloatRegister> REGISTERS =
            asList(XMM0, XMM1, XMM2, XMM3, XMM4, XMM5, XMM6, XMM7, XMM8, XMM9, XMM10, XMM11, XMM12, XMM13, XMM14, XMM15);

    private final Set<FloatRegister> freeRegisters = new HashSet<>(REGISTERS);

    /**
     * Allocates an arbitrary register for temporary storage. If there is no register available,
     * this method returns {@code null}.
     */
    FloatRegister allocate() {
        return allocateFirstPossible(REGISTERS);
    }

    /**
     * Allocates {@code register} to use for temporary storage if possible.
     * If {@code register} is not available, this method returns {@code null}.
     *
     * @param register The register to allocate.
     */
    FloatRegister allocate(FloatRegister register) {
        return allocateIfPossible(register);
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
        if (freeRegisters.contains(register)) {
            freeRegisters.remove(register);
            return register;
        }
        return null;
    }

    /**
     * Frees the given register, and makes it available to use again.
     */
    void free(FloatRegister register) {
        freeRegisters.add(register);
    }
}
