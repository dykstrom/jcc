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

import static se.dykstrom.jcc.common.assembly.base.Register.RAX;
import static se.dykstrom.jcc.common.assembly.base.Register.RDX;

import se.dykstrom.jcc.common.assembly.base.CodeContainer;
import se.dykstrom.jcc.common.assembly.base.Comment;
import se.dykstrom.jcc.common.assembly.base.Register;
import se.dykstrom.jcc.common.assembly.instruction.*;
import se.dykstrom.jcc.common.symbols.Identifier;

/**
 * Represents a storage location that stores data in a register.
 *
 * @author Johan Dykstrom
 */
class RegisterStorageLocation extends StorageLocation {

    private final Register register;

    private final RegisterManager registerManager;

    RegisterStorageLocation(Register register, RegisterManager registerManager) {
        this.register = register;
        this.registerManager = registerManager;
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
    public void moveThisToMem(Identifier memory, CodeContainer codeContainer) {
        codeContainer.add(new MoveRegToMem(register, memory));
    }

    @Override
    public void moveImmToThis(String immediate, CodeContainer codeContainer) {
        codeContainer.add(new MoveImmToReg(immediate, register));
    }

    @Override
    public void moveImmToThis(Identifier immediate, CodeContainer codeContainer) {
        codeContainer.add(new MoveImmToReg(immediate, register));
    }

    @Override
    public void moveMemToThis(Identifier memory, CodeContainer codeContainer) {
        codeContainer.add(new MoveMemToReg(memory, register));
    }

    @Override
    public void moveLocToThis(StorageLocation location, CodeContainer codeContainer) {
        if (location instanceof RegisterStorageLocation) {
            Register source = ((RegisterStorageLocation) location).getRegister();
            moveRegToRegIfNeeded(source, register, codeContainer);
        } else {
            throw new IllegalArgumentException("move from location of type " + location.getClass().getSimpleName() + " not supported");
        }
    }

    @Override
    public void pushThis(CodeContainer codeContainer) {
        codeContainer.add(new Push(register));
    }

    @Override
    public void addLocToThis(StorageLocation location, CodeContainer codeContainer) {
        if (location instanceof RegisterStorageLocation) {
            codeContainer.add(new AddRegToReg(((RegisterStorageLocation) location).getRegister(), register));
        } else {
            throw new IllegalArgumentException("add with location of type " + location.getClass().getSimpleName() + " not supported");
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
        Register rax = null;
        Register rdx = null;
        try {
            rax = registerManager.allocate(RAX);
            rdx = registerManager.allocate(RDX);
            if (rax == null || rdx == null) {
                throw new IllegalStateException("registers rax and rdx not available for division/modulo");
            }

            if (location instanceof RegisterStorageLocation) {
                // Move dividend (this) to rax
                moveRegToRegIfNeeded(register, rax, codeContainer);
                // Sign extend rax into rdx
                codeContainer.add(new Cqo());
                // Divide
                codeContainer.add(new IDivWithReg(((RegisterStorageLocation) location).getRegister()));
                // Move the result we are interested in from the "result register" to this
                moveRegToRegIfNeeded(resultRegister, register, codeContainer);
            } else {
                throw new IllegalArgumentException("idiv/mod with location of type " + location.getClass().getSimpleName() + " not supported");
            }
        } finally {
            if (rdx != null) {
                registerManager.free(rdx);
            }
            if (rax != null) {
                registerManager.free(rax);
            }
        }
    }

    @Override
    public void mulThisWithLoc(StorageLocation location, CodeContainer codeContainer) {
        if (location instanceof RegisterStorageLocation) {
            codeContainer.add(new IMulRegWithReg(((RegisterStorageLocation) location).getRegister(), register));
        } else {
            throw new IllegalArgumentException("mul with location of type " + location.getClass().getSimpleName() + " not supported");
        }
    }

    @Override
    public void subtractLocFromThis(StorageLocation location, CodeContainer codeContainer) {
        if (location instanceof RegisterStorageLocation) {
            codeContainer.add(new SubRegFromReg(((RegisterStorageLocation) location).getRegister(), register));
        } else {
            throw new IllegalArgumentException("sub with location of type " + location.getClass().getSimpleName() + " not supported");
        }
    }

    @Override
    public void compareThisWithLoc(StorageLocation location, CodeContainer codeContainer) {
        if (location instanceof RegisterStorageLocation) {
            codeContainer.add(new CmpRegWithReg(register, ((RegisterStorageLocation) location).getRegister()));
        } else {
            throw new IllegalArgumentException("cmp with location of type " + location.getClass().getSimpleName() + " not supported");
        }
    }

    @Override
    public void compareThisWithImm(String immediate, CodeContainer codeContainer) {
        codeContainer.add(new CmpRegWithImm(register, immediate));
    }

    @Override
    public void andThisWithLoc(StorageLocation location, CodeContainer codeContainer) {
        if (location instanceof RegisterStorageLocation) {
            codeContainer.add(new AndRegWithReg(((RegisterStorageLocation) location).getRegister(), register));
        } else {
            throw new IllegalArgumentException("and with location of type " + location.getClass().getSimpleName() + " not supported");
        }
    }

    @Override
    public void orThisWithLoc(StorageLocation location, CodeContainer codeContainer) {
        if (location instanceof RegisterStorageLocation) {
            codeContainer.add(new OrRegWithReg(((RegisterStorageLocation) location).getRegister(), register));
        } else {
            throw new IllegalArgumentException("or with location of type " + location.getClass().getSimpleName() + " not supported");
        }
    }

    /**
     * Generates code to move the contents of the source register to the destination register if they are not the same.
     */
    private static void moveRegToRegIfNeeded(Register source, Register destination, CodeContainer codeContainer) {
        if (!source.equals(destination)) {
            codeContainer.add(new MoveRegToReg(source, destination));
        } else {
            codeContainer.add(new Comment("mov " + destination + ", " + source + " removed"));
        }
    }
}
