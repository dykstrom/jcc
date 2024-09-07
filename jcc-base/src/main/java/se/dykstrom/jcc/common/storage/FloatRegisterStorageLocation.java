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
import se.dykstrom.jcc.common.assembly.instruction.*;
import se.dykstrom.jcc.common.assembly.instruction.floating.*;
import se.dykstrom.jcc.common.code.CodeContainer;
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
    public void moveThisToMem(String destinationAddress, int scale, Register offset, CodeContainer codeContainer) {
        codeContainer.add(new MoveFloatRegToMem(register, destinationAddress, scale, offset));
    }

    @Override
    public void moveImmToThis(String immediate, CodeContainer codeContainer) {
        registerManager.withTemporaryRegister(r ->
            memoryManager.withTemporaryMemory(m -> {
                codeContainer.add(new AssemblyComment("Move float literal to float register via gp register and memory"));
                // Move immediate to temporary register
                codeContainer.add(new MoveImmToReg(immediate, r));
                // Move temporary register to temporary memory
                codeContainer.add(new MoveRegToMem(r, m));
                // Move temporary memory to this register
                codeContainer.add(new MoveMemToFloatReg(m, register));
            })
        );
    }

    @Override
    public void moveAddressToThis(String address, CodeContainer codeContainer) {
        throw new UnsupportedOperationException("Moving a memory address to a float register is not supported");
    }

    @Override
    public void moveRegToThis(Register sourceRegister, CodeContainer codeContainer) {
        memoryManager.withTemporaryMemory(m -> {
            codeContainer.add(new AssemblyComment("Move gp register to float register via memory"));
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
    public void moveMemToThis(String sourceAddress, int scale, Register offset, CodeContainer codeContainer) {
        codeContainer.add(new MoveMemToFloatReg(sourceAddress, scale, offset, register));
    }

    @Override
    public void moveLocToThis(StorageLocation location, CodeContainer codeContainer) {
        if (location instanceof FloatRegisterStorageLocation fl) {
            codeContainer.add(new MoveFloatRegToFloatReg(fl.getRegister(), register));
        } else if (location instanceof MemoryStorageLocation ml) {
            moveMemToThis(ml.getMemory(), codeContainer);
        } else if (location instanceof RegisterStorageLocation rl) {
            moveRegToThis(rl.getRegister(), codeContainer);
        } else {
            throw new IllegalArgumentException("unhandled location of type: " + location.getClass());
        }
    }

    @Override
    public void convertAndMoveLocToThis(StorageLocation location, CodeContainer codeContainer) {
        if (location instanceof FloatRegisterStorageLocation fl) {
            // No conversion needed
            codeContainer.add(new MoveFloatRegToFloatReg(fl.getRegister(), register));
        } else if (location instanceof MemoryStorageLocation ml) {
            // Convert integer to float
            codeContainer.add(new ConvertIntMemToFloatReg(ml.getMemory(), register));
        } else if (location instanceof RegisterStorageLocation rl) {
            // Convert integer to float
            codeContainer.add(new ConvertIntRegToFloatReg(rl.getRegister(), register));
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
    public void multiplyImmWithThis(final String immediate, final CodeContainer codeContainer) {
        floatRegisterManager.withTemporaryFloatRegister(fr ->
            registerManager.withTemporaryRegister(r ->
                memoryManager.withTemporaryMemory(m -> {
                    codeContainer.add(new AssemblyComment("Move float literal to float register via gp register and memory"));
                    // Move immediate to temporary gp register
                    codeContainer.add(new MoveImmToReg(immediate, r));
                    // Move temporary gp register to temporary memory
                    codeContainer.add(new MoveRegToMem(r, m));
                    // Move temporary memory to temporary fp register
                    codeContainer.add(new MoveMemToFloatReg(m, fr));
                    // Multiply and store result in this fp register
                    codeContainer.add(new MulFloatRegWithFloatReg(fr, register));
                })
            )
        );

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

        if (location instanceof FloatRegisterStorageLocation fl) {
            codeContainer.add(invokeConstructorOrFail(constructor, fl.getRegister(), register));
        } else if (location instanceof MemoryStorageLocation ml) {
            floatRegisterManager.withTemporaryFloatRegister(fr -> {
                // Convert integer to float
                codeContainer.add(new ConvertIntMemToFloatReg(ml.getMemory(), fr));
                codeContainer.add(invokeConstructorOrFail(constructor, fr, register));
            });
        } else if (location instanceof RegisterStorageLocation rl) {
            floatRegisterManager.withTemporaryFloatRegister(fr -> {
                // Convert integer to float
                codeContainer.add(new ConvertIntRegToFloatReg(rl.getRegister(), fr));
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
        if (location instanceof FloatRegisterStorageLocation fl) {
            codeContainer.add(new CompareFloatRegWithFloatReg(register, fl.getRegister()));
        } else if (location instanceof MemoryStorageLocation ml) {
            floatRegisterManager.withTemporaryFloatRegister(fr -> {
                // Convert integer to float
                codeContainer.add(new ConvertIntMemToFloatReg(ml.getMemory(), fr));
                codeContainer.add(new CompareFloatRegWithFloatReg(register, fr));
            });
        } else if (location instanceof RegisterStorageLocation rl) {
            floatRegisterManager.withTemporaryFloatRegister(fr -> {
                // Convert integer to float
                codeContainer.add(new ConvertIntRegToFloatReg(rl.getRegister(), fr));
                codeContainer.add(new CompareFloatRegWithFloatReg(register, fr));
            });
        } else {
            throw new IllegalArgumentException("unhandled location of type: " + location.getClass());
        }
    }

    @Override
    public void compareThisWithImm(String immediate, CodeContainer codeContainer) {
        registerManager.withTemporaryRegister(r ->
                memoryManager.withTemporaryMemory(m -> {
                codeContainer.add(new AssemblyComment("Compare float register to literal loaded into temporary memory"));
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
    public void xorLocWithThis(final StorageLocation location, final CodeContainer codeContainer) {
        if (location instanceof FloatRegisterStorageLocation fl) {
            codeContainer.add(new XorFloatRegWithFloatReg(fl.getRegister(), register));
        } else if (location instanceof MemoryStorageLocation ml) {
            floatRegisterManager.withTemporaryFloatRegister(fr -> {
                // Convert integer to float
                codeContainer.add(new ConvertIntMemToFloatReg(ml.getMemory(), fr));
                codeContainer.add(new XorFloatRegWithFloatReg(fr, register));
            });
        } else if (location instanceof RegisterStorageLocation rl) {
            floatRegisterManager.withTemporaryFloatRegister(fr -> {
                // Convert integer to float
                codeContainer.add(new ConvertIntRegToFloatReg(rl.getRegister(), fr));
                codeContainer.add(new XorFloatRegWithFloatReg(fr, register));
            });
        }
    }

    @Override
    public void notThis(final CodeContainer codeContainer) {
        throw new UnsupportedOperationException("NOT is not supported on floating point values");
    }

    @Override
    public void negateThis(final CodeContainer codeContainer) {
        throw new UnsupportedOperationException("NEG is not supported on floating point values");
    }

    @Override
    public void shiftThisLeftByLoc(StorageLocation location, CodeContainer codeContainer) {
        throw new UnsupportedOperationException("SAL is not supported on floating point values");
    }
}
