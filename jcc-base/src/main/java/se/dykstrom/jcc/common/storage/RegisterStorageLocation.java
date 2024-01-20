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

import se.dykstrom.jcc.common.intermediate.CodeContainer;
import se.dykstrom.jcc.common.assembly.base.Register;
import se.dykstrom.jcc.common.assembly.instruction.*;
import se.dykstrom.jcc.common.assembly.instruction.floating.MoveFloatRegToMem;
import se.dykstrom.jcc.common.assembly.instruction.floating.RoundFloatRegToIntReg;
import se.dykstrom.jcc.common.types.F64;
import se.dykstrom.jcc.common.types.Type;

import static se.dykstrom.jcc.common.assembly.base.Register.*;

/**
 * Represents a storage location that stores data in a general purpose register.
 *
 * @author Johan Dykstrom
 */
public class RegisterStorageLocation implements StorageLocation {

    private final Register register;
    private final RegisterManager registerManager;
    private final MemoryManager memoryManager;

    RegisterStorageLocation(Register register, RegisterManager registerManager, MemoryManager memoryManager) {
        this.register = register;
        this.registerManager = registerManager;
        this.memoryManager = memoryManager;
    }

    /**
     * Returns the register of this storage location.
     */
    public Register getRegister() {
        return register;
    }

    @Override
    public String toString() {
        return register.toString();
    }

    @Override
    public void close() {
        registerManager.free(register);
    }

    @Override
    public boolean stores(Type type) {
        return !(type instanceof F64);
    }

    @Override
    public void moveThisToMem(String destinationAddress, CodeContainer codeContainer) {
        codeContainer.add(new MoveRegToMem(register, destinationAddress));
    }

    @Override
    public void moveThisToMem(String destinationAddress, int scale, Register offset, CodeContainer codeContainer) {
        codeContainer.add(new MoveRegToMem(register, destinationAddress, scale, offset));
    }

    @Override
    public void moveImmToThis(String immediate, CodeContainer codeContainer) {
        codeContainer.add(new MoveImmToReg(immediate, register));
    }

    @Override
    public void moveAddressToThis(String address, CodeContainer codeContainer) {
        codeContainer.add(new Lea(address, register));
    }

    @Override
    public void moveRegToThis(Register sourceRegister, CodeContainer codeContainer) {
        moveRegToRegIfNeeded(sourceRegister, register, codeContainer);
    }

    @Override
    public void moveMemToThis(String sourceAddress, CodeContainer codeContainer) {
        codeContainer.add(new MoveMemToReg(sourceAddress, register));
    }

    @Override
    public void moveMemToThis(String sourceAddress, int scale, Register offset, CodeContainer codeContainer) {
        codeContainer.add(new MoveMemToReg(sourceAddress, scale, offset, register));
    }

    @Override
    public void moveLocToThis(StorageLocation location, CodeContainer codeContainer) {
        if (location instanceof RegisterStorageLocation) {
            moveRegToThis(((RegisterStorageLocation) location).getRegister(), codeContainer);
        } else if (location instanceof MemoryStorageLocation) {
            moveMemToThis(((MemoryStorageLocation) location).getMemory(), codeContainer);
        } else {
            memoryManager.withTemporaryMemory(m -> {
                codeContainer.add(new MoveFloatRegToMem(((FloatRegisterStorageLocation) location).getRegister(), m));
                codeContainer.add(new MoveMemToReg(m, register));
            });
        }
    }

    @Override
    public void convertAndMoveLocToThis(StorageLocation location, CodeContainer codeContainer) {
        if (location instanceof RegisterStorageLocation) {
            // No conversion needed
            moveRegToThis(((RegisterStorageLocation) location).getRegister(), codeContainer);
        } else if (location instanceof MemoryStorageLocation) {
            // No conversion needed
            moveMemToThis(((MemoryStorageLocation) location).getMemory(), codeContainer);
        } else {
            // Convert float to integer
            codeContainer.add(new RoundFloatRegToIntReg(((FloatRegisterStorageLocation) location).getRegister(), register));
        }
    }

    @Override
    public void pushThis(CodeContainer codeContainer) {
        codeContainer.add(new PushReg(register));
    }

    @Override
    public void addLocToThis(StorageLocation location, CodeContainer codeContainer) {
        if (location instanceof RegisterStorageLocation) {
            codeContainer.add(new AddRegToReg(((RegisterStorageLocation) location).getRegister(), register));
        } else {
            codeContainer.add(new AddMemToReg(((MemoryStorageLocation) location).getMemory(), register));
        }
    }

    @Override
    public void addImmToMem(String immediate, String destinationAddress, CodeContainer codeContainer) {
        long value = Long.parseLong(immediate);
        if (value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE) {
            codeContainer.add(new AddImmToMem(immediate, destinationAddress));
        } else {
            codeContainer.add(new MoveImmToReg(immediate, register));
            codeContainer.add(new AddRegToMem(register, destinationAddress));
        }
    }

    @Override
    public void divideThisWithLoc(StorageLocation location, CodeContainer codeContainer) {
        throw new UnsupportedOperationException("DIV is not supported on general purpose registers");
    }

    @Override
    public void idivThisWithLoc(StorageLocation location, CodeContainer codeContainer) {
        performDivMod(location, RAX, codeContainer);
    }

    @Override
    public void modThisWithLoc(StorageLocation location, CodeContainer codeContainer) {
        performDivMod(location, RDX, codeContainer);
    }

    /**
     * Generates code for performing the actual division/modulo calculation, storing the result in this.
     * The result is taken from {@code resultRegister} which must be either RAX (quotient) or RDX (remainder).
     */
    private void performDivMod(StorageLocation location, Register resultRegister, CodeContainer codeContainer) {
        // Move dividend (this) to rax
        moveRegToRegIfNeeded(register, RAX, codeContainer);
        // Sign extend rax into rdx
        codeContainer.add(new Cqo());
        // Divide
        if (location instanceof RegisterStorageLocation) {
            codeContainer.add(new IDivWithReg(((RegisterStorageLocation) location).getRegister()));
        } else {
            codeContainer.add(new IDivWithMem(((MemoryStorageLocation) location).getMemory()));
        }
        // Move the result we are interested in from the "result register" to this
        moveRegToRegIfNeeded(resultRegister, register, codeContainer);
    }

    @Override
    public void multiplyLocWithThis(StorageLocation location, CodeContainer codeContainer) {
        if (location instanceof RegisterStorageLocation rsl) {
            codeContainer.add(new IMulRegWithReg(rsl.getRegister(), register));
        } else {
            codeContainer.add(new IMulMemWithReg(((MemoryStorageLocation) location).getMemory(), register));
        }
    }

    @Override
    public void multiplyImmWithThis(final String immediate, final CodeContainer codeContainer) {
        codeContainer.add(new IMulImmWithReg(immediate, register));
    }

    @Override
    public void subtractLocFromThis(StorageLocation location, CodeContainer codeContainer) {
        if (location instanceof RegisterStorageLocation) {
            codeContainer.add(new SubRegFromReg(((RegisterStorageLocation) location).getRegister(), register));
        } else {
            codeContainer.add(new SubMemFromReg(((MemoryStorageLocation) location).getMemory(), register));
        }
    }

    @Override
    public void subtractImmFromMem(String immediate, String destinationAddress, CodeContainer codeContainer) {
        long value = Long.parseLong(immediate);
        if (value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE) {
            codeContainer.add(new SubImmFromMem(immediate, destinationAddress));
        } else {
            codeContainer.add(new MoveImmToReg(immediate, register));
            codeContainer.add(new SubRegFromMem(register, destinationAddress));
        }
    }

    @Override
    public void incrementThis(CodeContainer codeContainer) {
        codeContainer.add(new IncReg(register));
    }

    @Override
    public void decrementThis(CodeContainer codeContainer) {
        codeContainer.add(new DecReg(register));
    }

    @Override
    public void compareThisWithLoc(StorageLocation location, CodeContainer codeContainer) {
        if (location instanceof RegisterStorageLocation) {
            codeContainer.add(new CmpRegWithReg(register, ((RegisterStorageLocation) location).getRegister()));
        } else {
            codeContainer.add(new CmpRegWithMem(register, ((MemoryStorageLocation) location).getMemory()));
        }
    }

    @Override
    public void compareThisWithImm(String immediate, CodeContainer codeContainer) {
        long value = Long.parseLong(immediate);
        if (value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE) {
            codeContainer.add(new CmpRegWithImm(register, immediate));
        } else {
            registerManager.withTemporaryRegister(r -> {
                codeContainer.add(new MoveImmToReg(immediate, r));
                codeContainer.add(new CmpRegWithReg(register, r));
            });
        }
    }

    @Override
    public void andLocWithThis(StorageLocation location, CodeContainer codeContainer) {
        if (location instanceof RegisterStorageLocation) {
            codeContainer.add(new AndRegWithReg(((RegisterStorageLocation) location).getRegister(), register));
        } else {
            codeContainer.add(new AndMemWithReg(((MemoryStorageLocation) location).getMemory(), register));
        }
    }

    @Override
    public void orLocWithThis(StorageLocation location, CodeContainer codeContainer) {
        if (location instanceof RegisterStorageLocation) {
            codeContainer.add(new OrRegWithReg(((RegisterStorageLocation) location).getRegister(), register));
        } else {
            codeContainer.add(new OrMemWithReg(((MemoryStorageLocation) location).getMemory(), register));
        }
    }

    @Override
    public void xorLocWithThis(StorageLocation location, CodeContainer codeContainer) {
        if (location instanceof RegisterStorageLocation) {
            codeContainer.add(new XorRegWithReg(((RegisterStorageLocation) location).getRegister(), register));
        } else {
            codeContainer.add(new XorMemWithReg(((MemoryStorageLocation) location).getMemory(), register));
        }
    }

    @Override
    public void notThis(final CodeContainer codeContainer) {
        codeContainer.add(new NotReg(register));
    }

    @Override
    public void negateThis(final CodeContainer codeContainer) {
        codeContainer.add(new NegReg(register));
    }

    @Override
    public void shiftThisLeftByLoc(StorageLocation location, CodeContainer codeContainer) {
        if (location instanceof RegisterStorageLocation rsl) {
            moveRegToRegIfNeeded(rsl.getRegister(), RCX, codeContainer);
        } else if (location instanceof MemoryStorageLocation msl) {
            codeContainer.add(new MoveMemToReg(msl.getMemory(), RCX));
        } else {
            throw new IllegalArgumentException("invalid shift value: " + location);
        }
        codeContainer.add(new SalRegWithCL(register));
    }
}
