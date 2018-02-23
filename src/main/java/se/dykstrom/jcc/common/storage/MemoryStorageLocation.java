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

import se.dykstrom.jcc.common.assembly.base.CodeContainer;
import se.dykstrom.jcc.common.assembly.base.Register;
import se.dykstrom.jcc.common.assembly.instruction.*;
import se.dykstrom.jcc.common.types.F64;
import se.dykstrom.jcc.common.types.Type;

import static se.dykstrom.jcc.common.assembly.base.Register.*;

/**
 * Represents a storage location that stores data in a memory location.
 * 
 * It is not possible to move a 64-bit value between two memory locations,
 * or to move a 64-bit immediate to a memory location, so we have to use a
 * volatile register as a temporary landing point
 *
 * @author Johan Dykstrom
 */
class MemoryStorageLocation extends AbstractStorageLocation {

    private final String memoryAddress;

    MemoryStorageLocation(String memory, MemoryManager memoryManager, RegisterManager registerManager) {
        super(registerManager, null, memoryManager);
        this.memoryAddress = memory;
    }

    /**
     * Returns the memory address of this storage location.
     */
    public String getMemory() {
        return memoryAddress;
    }

    @Override
    public String toString() {
        return memoryAddress;
    }

    @Override
    public void close() {
        memoryManager.free(memoryAddress);
    }

    @Override
    public boolean stores(Type type) {
        return !(type instanceof F64);
    }

    @Override
    public void moveThisToMem(String destinationAddress, CodeContainer codeContainer) {
        withTemporaryRegister(r -> {
            codeContainer.add(new MoveMemToReg(memoryAddress, r));
            codeContainer.add(new MoveRegToMem(r, destinationAddress));
        });
    }

    @Override
    public void moveImmToThis(String immediate, CodeContainer codeContainer) {
        withTemporaryRegister(R10, () -> {
            codeContainer.add(new MoveImmToReg(immediate, R10));
            codeContainer.add(new MoveRegToMem(R10, memoryAddress));
        });
    }

    @Override
    public void moveMemToThis(String sourceAddress, CodeContainer codeContainer) {
        withTemporaryRegister(R10, () -> {
            codeContainer.add(new MoveMemToReg(sourceAddress, R10));
            codeContainer.add(new MoveRegToMem(R10, memoryAddress));
        });
    }

    @Override
    public void moveLocToThis(StorageLocation location, CodeContainer codeContainer) {
        if (location instanceof RegisterStorageLocation) {
            codeContainer.add(new MoveRegToMem(((RegisterStorageLocation) location).getRegister(), memoryAddress));
        } else {
            moveMemToThis(((MemoryStorageLocation) location).getMemory(), codeContainer);
        }
    }

    @Override
    public void pushThis(CodeContainer codeContainer) {
        codeContainer.add(new PushMem(memoryAddress));
    }

    @Override
    public void addLocToThis(StorageLocation location, CodeContainer codeContainer) {
        if (location instanceof RegisterStorageLocation) {
            codeContainer.add(new AddRegToMem(((RegisterStorageLocation) location).getRegister(), memoryAddress));
        } else {
            withTemporaryRegister(R10, () -> {
                codeContainer.add(new MoveMemToReg(((MemoryStorageLocation) location).getMemory(), R10));
                codeContainer.add(new AddRegToMem(R10, memoryAddress));
            });
        }
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
        withTemporaryRegisters(RAX, RDX, () -> {
            // Move dividend (this) to rax
            codeContainer.add(new MoveMemToReg(memoryAddress, RAX));
            // Sign extend rax into rdx
            codeContainer.add(new Cqo());
            // Divide
            if (location instanceof RegisterStorageLocation) {
                codeContainer.add(new IDivWithReg(((RegisterStorageLocation) location).getRegister()));
            } else {
                codeContainer.add(new IDivWithMem(((MemoryStorageLocation) location).getMemory()));
            }
            // Move the result we are interested in from the "result register" to this
            codeContainer.add(new MoveRegToMem(resultRegister, memoryAddress));
        });
    }
    
    @Override
    public void imulLocWithThis(StorageLocation location, CodeContainer codeContainer) {
        withTemporaryRegister(R10, () -> {
            // Move source to temporary register
            if (location instanceof RegisterStorageLocation) {
                codeContainer.add(new MoveRegToReg(((RegisterStorageLocation) location).getRegister(), R10));
            } else {
                codeContainer.add(new MoveMemToReg(((MemoryStorageLocation) location).getMemory(), R10));
            }
            // Multiply this with register, storing result in register
            codeContainer.add(new IMulMemWithReg(memoryAddress, R10));
            // Move result to this
            codeContainer.add(new MoveRegToMem(R10, memoryAddress));
        });
    }

    @Override
    public void subtractLocFromThis(StorageLocation location, CodeContainer codeContainer) {
        if (location instanceof RegisterStorageLocation) {
            codeContainer.add(new SubRegFromMem(((RegisterStorageLocation) location).getRegister(), memoryAddress));
        } else {
            withTemporaryRegister(R10, () -> {
                codeContainer.add(new MoveMemToReg(((MemoryStorageLocation) location).getMemory(), R10));
                codeContainer.add(new SubRegFromMem(R10, memoryAddress));
            });
        }
    }

    @Override
    public void incrementThis(CodeContainer codeContainer) {
        codeContainer.add(new IncMem(memoryAddress));
    }

    @Override
    public void decrementThis(CodeContainer codeContainer) {
        codeContainer.add(new DecMem(memoryAddress));
    }

    @Override
    public void compareThisWithLoc(StorageLocation location, CodeContainer codeContainer) {
        if (location instanceof RegisterStorageLocation) {
            codeContainer.add(new CmpMemWithReg(memoryAddress, ((RegisterStorageLocation) location).getRegister()));
        } else {
            withTemporaryRegister(R10, () -> {
                codeContainer.add(new MoveMemToReg(((MemoryStorageLocation) location).getMemory(), R10));
                codeContainer.add(new CmpMemWithReg(memoryAddress, R10));
            });
        }
    }

    @Override
    public void compareThisWithImm(String immediate, CodeContainer codeContainer) {
        withTemporaryRegister(R10, () -> {
            codeContainer.add(new MoveMemToReg(memoryAddress, R10));
            // TODO: This operation does not support 64-bit immediate operands.
            codeContainer.add(new CmpRegWithImm(R10, immediate));
        });
    }

    @Override
    public void andLocWithThis(StorageLocation location, CodeContainer codeContainer) {
        if (location instanceof RegisterStorageLocation) {
            codeContainer.add(new AndRegWithMem(((RegisterStorageLocation) location).getRegister(), memoryAddress));
        } else {
            withTemporaryRegister(R10, () -> {
                codeContainer.add(new MoveMemToReg(((MemoryStorageLocation) location).getMemory(), R10));
                codeContainer.add(new AndRegWithMem(R10, memoryAddress));
            });
        }
    }

    @Override
    public void orLocWithThis(StorageLocation location, CodeContainer codeContainer) {
        if (location instanceof RegisterStorageLocation) {
            codeContainer.add(new OrRegWithMem(((RegisterStorageLocation) location).getRegister(), memoryAddress));
        } else {
            withTemporaryRegister(R10, () -> {
                codeContainer.add(new MoveMemToReg(((MemoryStorageLocation) location).getMemory(), R10));
                codeContainer.add(new OrRegWithMem(R10, memoryAddress));
            });
        }
    }

    @Override
    public void xorLocWithThis(StorageLocation location, CodeContainer codeContainer) {
        if (location instanceof RegisterStorageLocation) {
            codeContainer.add(new XorRegWithMem(((RegisterStorageLocation) location).getRegister(), memoryAddress));
        } else {
            withTemporaryRegister(R10, () -> {
                codeContainer.add(new MoveMemToReg(((MemoryStorageLocation) location).getMemory(), R10));
                codeContainer.add(new XorRegWithMem(R10, memoryAddress));
            });
        }
    }

    @Override
    public void notThis(CodeContainer codeContainer) {
        codeContainer.add(new NotMem(memoryAddress));
    }
}
