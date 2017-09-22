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
import se.dykstrom.jcc.common.symbols.Identifier;

/**
 * Represents a storage location of some kind, for example a register. This class defines a number of operations
 * on the storage location, such as moving an immediate value to this storage location. These operations must be
 * implemented by the concrete subclasses that deal with a specific kind of storage location.
 *
 * @author Johan Dykstrom
 */
public abstract class StorageLocation implements AutoCloseable {

    @Override
    public void close() { }

    /**
     * Generate code for moving the value stored in this storage location to the given memory location.
     */
    public abstract void moveThisToMem(Identifier memory, CodeContainer codeContainer);

    /**
     * Generate code for moving the given immediate value to this storage location.
     */
    public abstract void moveImmToThis(String immediate, CodeContainer codeContainer);

    /**
     * Generate code for moving the given immediate value to this storage location.
     * In this case, the immediate value is not the <i>value</i> of the identifier,
     * but its <i>name</i>.
     */
    public abstract void moveImmToThis(Identifier immediate, CodeContainer codeContainer);

    /**
     * Generate code for moving the value stored in the given memory location to this storage location.
     */
    public abstract void moveMemToThis(Identifier memory, CodeContainer codeContainer);

    /**
     * Generate code for moving the value stored in the given storage location to this storage location.
     */
    public abstract void moveLocToThis(StorageLocation location, CodeContainer codeContainer);

    /**
     * Generate code for pushing the value stored in this storage location to the stack.
     */
    public abstract void pushThis(CodeContainer codeContainer);

    /**
     * Generate code for adding the value stored in the given storage location to this storage location, 
     * storing the result in this storage location.
     */
    public abstract void addLocToThis(StorageLocation location, CodeContainer codeContainer);

    /**
     * Generate code for dividing the value stored in this storage location by the value stored in
     * the given storage location, storing the result in this storage location. This method handles
     * integer division only.
     */
    public abstract void idivThisWithLoc(StorageLocation location, CodeContainer codeContainer);

    /**
     * Generate code for calculating the modulo of the value stored in this storage location and
     * the value stored in the given storage location, storing the result in this storage location.
     */
    public abstract void modThisWithLoc(StorageLocation location, CodeContainer codeContainer);

    /**
     * Generate code for multiplying the value stored in this storage location with the value stored in
     * the given storage location, storing the result in this storage location.
     */
    public abstract void mulThisWithLoc(StorageLocation location, CodeContainer codeContainer);

    /**
     * Generate code for subtracting the value stored in the given storage location from this storage location, 
     * storing the result in this storage location.
     */
    public abstract void subtractLocFromThis(StorageLocation location, CodeContainer codeContainer);

    /**
     * Generate code for comparing the value stored in this storage location with the value stored 
     * in the given storage location. Neither value is changed by this operation.
     */
    public abstract void compareThisWithLoc(StorageLocation location, CodeContainer codeContainer);

    /**
     * Generate code for comparing the value stored in this storage location with the given immediate 
     * value. Neither value is changed by this operation.
     */
    public abstract void compareThisWithImm(String immediate, CodeContainer codeContainer);

    /**
     * Generate code for doing bitwise and on the value stored in this storage location and the value
     * stored in the given storage location. The result is stored in this storage location.
     */
    public abstract void andThisWithLoc(StorageLocation location, CodeContainer codeContainer);

    /**
     * Generate code for doing bitwise or on the value stored in this storage location and the value
     * stored in the given storage location. The result is stored in this storage location.
     */
    public abstract void orThisWithLoc(StorageLocation location, CodeContainer codeContainer);
}
