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

import se.dykstrom.jcc.common.assembly.base.*;
import se.dykstrom.jcc.common.assembly.instruction.MoveImmToReg;
import se.dykstrom.jcc.common.assembly.instruction.MoveRegToMem;
import se.dykstrom.jcc.common.assembly.instruction.PushMem;
import se.dykstrom.jcc.common.assembly.instruction.floating.*;
import se.dykstrom.jcc.common.types.F64;
import se.dykstrom.jcc.common.types.Type;

import java.lang.reflect.Constructor;

import static se.dykstrom.jcc.common.utils.ReflectionUtils.getConstructorOrFail;
import static se.dykstrom.jcc.common.utils.ReflectionUtils.invokeConstructorOrFail;

/**
 * Represents a storage location that stores data in a floating point register.
 *
 * @author Johan Dykstrom
 */
public class FloatRegisterStorageLocation implements StorageLocation {

    private final FloatRegister register;
    private final FloatRegisterManager floatRegisterManager;
    private final RegisterManager registerManager;
    private final MemoryManager memoryManager;

    FloatRegisterStorageLocation(FloatRegister register, FloatRegisterManager floatRegisterManager,
                                 RegisterManager registerManager, MemoryManager memoryManager) {
        this.register = register;
        this.floatRegisterManager = floatRegisterManager;
        this.registerManager = registerManager;
        this.memoryManager = memoryManager;
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
        registerManager.withTemporaryRegister(r ->
                memoryManager.withTemporaryMemory(m -> {
                codeContainer.add(new Comment("Move float literal to float register via gp register and memory"));

                // TODO: Can we use MOVQ here to save one instruction?

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
    public void moveRegToThis(Register sourceRegister, CodeContainer codeContainer) {
        memoryManager.withTemporaryMemory(m -> {
            codeContainer.add(new Comment("Move gp register to float register via memory"));
            // Move g.p. register to temporary memory
            codeContainer.add(new MoveRegToMem(sourceRegister, m));
            // Move temporary memory to this float register
            codeContainer.add(new MoveMemToFloatReg(m, register));
        });
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
            // Assume that a RegisterStorageLocation contains an integer value that needs to be converted
            codeContainer.add(new ConvertIntRegToFloatReg(((RegisterStorageLocation) location).getRegister(), register));
        } else {
            throw new IllegalArgumentException("unhandled location of type: " + location.getClass());
        }
    }

    @Override
    public void pushThis(CodeContainer codeContainer) {
        memoryManager.withTemporaryMemory(m -> {
            // Move this to temporary memory
            codeContainer.add(new MoveFloatRegToMem(register, m));
            // Push temporary memory
            codeContainer.add(new PushMem(m));
        });
    }

    @Override
    public void addLocToThis(StorageLocation location, CodeContainer codeContainer) {
        doOperationOnLocAndThis(location, codeContainer, AddFloatRegToFloatReg.class);
    }

    @Override
    public void addImmToMem(String immediate, String destinationAddress, CodeContainer codeContainer) {
        moveImmToThis(immediate, codeContainer);
        codeContainer.add(new AddMemToFloatReg(destinationAddress, register));
        codeContainer.add(new MoveFloatRegToMem(register, destinationAddress));
    }

    @Override
    public void divideThisWithLoc(StorageLocation location, CodeContainer codeContainer) {
        doOperationOnLocAndThis(location, codeContainer, DivFloatRegWithFloatReg.class);
    }

    @Override
    public void idivThisWithLoc(StorageLocation location, CodeContainer codeContainer) {
        throw new UnsupportedOperationException("IDIV is not supported on floating point values");
    }

    @Override
    public void modThisWithLoc(StorageLocation location, CodeContainer codeContainer) {
        throw new UnsupportedOperationException("MOD is not supported on floating point values, use function fmod");
    }

    @Override
    public void multiplyLocWithThis(StorageLocation location, CodeContainer codeContainer) {
        doOperationOnLocAndThis(location, codeContainer, MulFloatRegWithFloatReg.class);
    }

    @Override
    public void subtractLocFromThis(StorageLocation location, CodeContainer codeContainer) {
        doOperationOnLocAndThis(location, codeContainer, SubFloatRegFromFloatReg.class);
    }

    @Override
    public void subtractImmFromMem(String immediate, String destinationAddress, CodeContainer codeContainer) {
        moveImmToThis(immediate, codeContainer);
        codeContainer.add(new SubMemFromFloatReg(destinationAddress, register));
        codeContainer.add(new MoveFloatRegToMem(register, destinationAddress));
    }

    /**
     * Generates code for the operation specified by {@code instructionClass} on the given location and this float register.
     *
     * @param location The other storage location.
     * @param codeContainer The code container to generate code to.
     * @param instructionClass The assembly instruction class that specifies which operation to generate code for.
     */
    private void doOperationOnLocAndThis(StorageLocation location, CodeContainer codeContainer, Class<? extends Instruction> instructionClass) {
        // Find constructor of instruction class
        Constructor<? extends Instruction> constructor = getConstructorOrFail(instructionClass, FloatRegister.class, FloatRegister.class);

        if (location instanceof FloatRegisterStorageLocation) {
            codeContainer.add(invokeConstructorOrFail(constructor, ((FloatRegisterStorageLocation) location).getRegister(), register));
        } else if (location instanceof MemoryStorageLocation) {
            floatRegisterManager.withTemporaryFloatRegister(fr -> {
                // Assume that a MemoryStorageLocation contains an integer value that needs to be converted
                // TODO: Check this.
                codeContainer.add(new ConvertIntMemToFloatReg(((MemoryStorageLocation) location).getMemory(), fr));
                codeContainer.add(invokeConstructorOrFail(constructor, fr, register));
            });
        } else if (location instanceof RegisterStorageLocation) {
            floatRegisterManager.withTemporaryFloatRegister(fr -> {
                codeContainer.add(new ConvertIntRegToFloatReg(((RegisterStorageLocation) location).getRegister(), fr));
                codeContainer.add(invokeConstructorOrFail(constructor, fr, register));
            });
        } else {
            throw new IllegalArgumentException("unhandled location of type " + location.getClass() + " for instruction " + instructionClass);
        }
    }

    @Override
    public void incrementThis(CodeContainer codeContainer) {
        throw new UnsupportedOperationException("INC is not supported on floating point values");
    }

    @Override
    public void decrementThis(CodeContainer codeContainer) {
        throw new UnsupportedOperationException("DEC is not supported on floating point values");
    }

    @Override
    public void compareThisWithLoc(StorageLocation location, CodeContainer codeContainer) {
        if (location instanceof FloatRegisterStorageLocation) {
            codeContainer.add(new CompareFloatRegWithFloatReg(register, ((FloatRegisterStorageLocation) location).getRegister()));
        } else if (location instanceof MemoryStorageLocation) {
            // Assume that a MemoryStorageLocation contains an integer value that needs to be converted
            // TODO: Check this.
            floatRegisterManager.withTemporaryFloatRegister(r -> {
                codeContainer.add(new ConvertIntMemToFloatReg(((MemoryStorageLocation) location).getMemory(), r));
                codeContainer.add(new CompareFloatRegWithFloatReg(register, r));
            });
        } else if (location instanceof RegisterStorageLocation) {
            floatRegisterManager.withTemporaryFloatRegister(r -> {
                codeContainer.add(new ConvertIntRegToFloatReg(((RegisterStorageLocation) location).getRegister(), r));
                codeContainer.add(new CompareFloatRegWithFloatReg(register, r));
            });
        } else {
            throw new IllegalArgumentException("unhandled location of type: " + location.getClass());
        }
    }

    @Override
    public void compareThisWithImm(String immediate, CodeContainer codeContainer) {
        registerManager.withTemporaryRegister(r ->
                memoryManager.withTemporaryMemory(m -> {
                codeContainer.add(new Comment("Compare float register to literal loaded into temporary memory"));
                // Move immediate to temporary register
                codeContainer.add(new MoveImmToReg(immediate, r));
                // Move temporary register to temporary memory
                codeContainer.add(new MoveRegToMem(r, m));
                // Compare this register to temporary memory
                codeContainer.add(new CompareFloatRegWithMem(register, m));
            }
        ));
    }

    @Override
    public void andLocWithThis(StorageLocation location, CodeContainer codeContainer) {
        throw new UnsupportedOperationException("AND is not supported on floating point values");
    }

    @Override
    public void orLocWithThis(StorageLocation location, CodeContainer codeContainer) {
        throw new UnsupportedOperationException("OR is not supported on floating point values");
    }

    @Override
    public void xorLocWithThis(StorageLocation location, CodeContainer codeContainer) {
        throw new UnsupportedOperationException("XOR is not supported on floating point values");
    }

    @Override
    public void notThis(CodeContainer codeContainer) {
        throw new UnsupportedOperationException("NOT is not supported on floating point values");
    }
}
