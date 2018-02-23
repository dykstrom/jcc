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

import se.dykstrom.jcc.common.assembly.base.CodeContainer;
import se.dykstrom.jcc.common.assembly.base.Comment;
import se.dykstrom.jcc.common.assembly.base.FloatRegister;
import se.dykstrom.jcc.common.assembly.instruction.MoveImmToReg;
import se.dykstrom.jcc.common.assembly.instruction.MoveRegToMem;
import se.dykstrom.jcc.common.assembly.instruction.PushMem;
import se.dykstrom.jcc.common.assembly.instruction.floating.*;
import se.dykstrom.jcc.common.types.F64;
import se.dykstrom.jcc.common.types.Type;

/**
 * Represents a storage location that stores data in a floating point register.
 *
 * @author Johan Dykstrom
 */
class FloatRegisterStorageLocation extends AbstractStorageLocation {

    private final FloatRegister register;

    FloatRegisterStorageLocation(FloatRegister register, FloatRegisterManager floatRegisterManager,
                                 RegisterManager registerManager, MemoryManager memoryManager) {
        super(registerManager, floatRegisterManager, memoryManager);
        this.register = register;
    }

    /**
     * Returns the register of this storage location.
     */
    public FloatRegister getRegister() {
        return register;
    }

    @Override
    public String toString() {
        return register.toString();
    }

    @Override
    public void close() {
        floatRegisterManager.free(register);
    }

    @Override
    public boolean stores(Type type) {
        return type instanceof F64;
    }

    @Override
    public void moveThisToMem(String destinationAddress, CodeContainer codeContainer) {
        codeContainer.add(new MoveFloatRegToMem(register, destinationAddress));
    }

    @Override
    public void moveImmToThis(String immediate, CodeContainer codeContainer) {
        withTemporaryRegister(r ->
            withTemporaryMemory(m -> {
                codeContainer.add(new Comment("Move float literal to float register via gp register and memory"));
                // Move immediate to temporary register
                codeContainer.add(new MoveImmToReg(immediate, r));
                // Move temporary register to temporary memory
                codeContainer.add(new MoveRegToMem(r, m));
                // Move temporary memory to this register
                codeContainer.add(new MoveMemToFloatReg(m, register));
            }
        ));
    }

    @Override
    public void moveMemToThis(String sourceAddress, CodeContainer codeContainer) {
        codeContainer.add(new MoveMemToFloatReg(sourceAddress, register));
    }

    @Override
    public void moveLocToThis(StorageLocation location, CodeContainer codeContainer) {
        if (location instanceof FloatRegisterStorageLocation) {
            codeContainer.add(new MoveFloatRegToFloatReg(((FloatRegisterStorageLocation) location).getRegister(), register));
        } else if (location instanceof MemoryStorageLocation) {
            // Assume that a MemoryStorageLocation contains an integer value that needs to be converted
            codeContainer.add(new ConvertIntMemToFloatReg(((MemoryStorageLocation) location).getMemory(), register));
        } else if (location instanceof RegisterStorageLocation) {
            codeContainer.add(new ConvertIntRegToFloatReg(((RegisterStorageLocation) location).getRegister(), register));
        } else {
            throw new IllegalArgumentException("unhandled location of type: " + location.getClass());
        }
    }

    @Override
    public void pushThis(CodeContainer codeContainer) {
        withTemporaryMemory(m -> {
            // Move this to temporary memory
            codeContainer.add(new MoveFloatRegToMem(register, m));
            // Push temporary memory
            codeContainer.add(new PushMem(m));
        });
    }

    @Override
    public void addLocToThis(StorageLocation location, CodeContainer codeContainer) {
        if (location instanceof FloatRegisterStorageLocation) {
            codeContainer.add(new AddFloatRegToFloatReg(((FloatRegisterStorageLocation) location).getRegister(), register));
        } else if (location instanceof MemoryStorageLocation) {
            withTemporaryFloatRegister(fr -> {
                // Assume that a MemoryStorageLocation contains an integer value that needs to be converted
                codeContainer.add(new ConvertIntMemToFloatReg(((MemoryStorageLocation) location).getMemory(), fr));
                codeContainer.add(new AddFloatRegToFloatReg(fr, register));
            });
        } else if (location instanceof RegisterStorageLocation) {
            withTemporaryFloatRegister(fr -> {
                codeContainer.add(new ConvertIntRegToFloatReg(((RegisterStorageLocation) location).getRegister(), fr));
                codeContainer.add(new AddFloatRegToFloatReg(fr, register));
            });
        } else {
            throw new IllegalArgumentException("unhandled location of type: " + location.getClass());
        }
    }

    @Override
    public void idivThisWithLoc(StorageLocation location, CodeContainer codeContainer) {
        throw new UnsupportedOperationException("IDIV is not supported on floating point registers");
    }

    @Override
    public void modThisWithLoc(StorageLocation location, CodeContainer codeContainer) {
        codeContainer.add(new Comment("modThisWithLoc not implemented yet"));
    }

    @Override
    public void imulLocWithThis(StorageLocation location, CodeContainer codeContainer) {
        codeContainer.add(new Comment("imulLocWithThis not implemented yet"));
    }

    @Override
    public void subtractLocFromThis(StorageLocation location, CodeContainer codeContainer) {
        codeContainer.add(new Comment("subtractLocFromThis not implemented yet"));
    }

    @Override
    public void incrementThis(CodeContainer codeContainer) {
        throw new UnsupportedOperationException("INC is not supported on floating point registers");
    }

    @Override
    public void decrementThis(CodeContainer codeContainer) {
        throw new UnsupportedOperationException("DEC is not supported on floating point registers");
    }

    @Override
    public void compareThisWithLoc(StorageLocation location, CodeContainer codeContainer) {
        codeContainer.add(new Comment("compareThisWithLoc not implemented yet"));
    }

    @Override
    public void compareThisWithImm(String immediate, CodeContainer codeContainer) {
        codeContainer.add(new Comment("compareThisWithImm not implemented yet"));
    }

    @Override
    public void andLocWithThis(StorageLocation location, CodeContainer codeContainer) {
        throw new UnsupportedOperationException("AND is not supported on floating point registers");
    }

    @Override
    public void orLocWithThis(StorageLocation location, CodeContainer codeContainer) {
        throw new UnsupportedOperationException("OR is not supported on floating point registers");
    }

    @Override
    public void xorLocWithThis(StorageLocation location, CodeContainer codeContainer) {
        throw new UnsupportedOperationException("XOR is not supported on floating point registers");
    }

    @Override
    public void notThis(CodeContainer codeContainer) {
        throw new UnsupportedOperationException("NOT is not supported on floating point registers");
    }
}
