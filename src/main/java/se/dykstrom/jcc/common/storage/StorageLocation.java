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
import se.dykstrom.jcc.common.types.Type;

/**
 * Represents a storage location of some kind, for example a CPU register. This interface defines a number of
 * operations on the storage location, such as moving an immediate value to this storage location, or adding 
 * the contents of another storage location to this storage location. These operations must be implemented 
 * by the concrete classes that deal with a specific kind of storage location.
 *
 * @author Johan Dykstrom
 */
public interface StorageLocation extends AutoCloseable {
    
    @Override
    void close();

    /**
     * Returns {@code true} if this storage location can store values of the given {@code type}.
     *
     * @param type The value type.
     * @return True if this storage location can store values of {@code type}.
     */
    boolean stores(Type type);

    /**
     * Generate code for moving the value stored in this storage location to the given memory address.
     */
    void moveThisToMem(String destinationAddress, CodeContainer codeContainer);

    /**
     * Generate code for moving the given immediate value to this storage location.
     */
    void moveImmToThis(String immediate, CodeContainer codeContainer);

    /**
     * Generate code for moving the value stored in the given source register to this storage location.
     */
    void moveRegToThis(Register sourceRegister, CodeContainer codeContainer);

    /**
     * Generate code for moving the value stored in the given memory address to this storage location.
     */
    void moveMemToThis(String sourceAddress, CodeContainer codeContainer);

    /**
     * Generate code for moving the value stored in the given storage location to this storage location.
     */
    void moveLocToThis(StorageLocation location, CodeContainer codeContainer);

    /**
     * Generate code for pushing the value stored in this storage location to the stack.
     */
    void pushThis(CodeContainer codeContainer);

    /**
     * Generate code for adding the value stored in the given storage location to this storage location, 
     * storing the result in this storage location.
     */
    void addLocToThis(StorageLocation location, CodeContainer codeContainer);

    /**
     * Generates code for adding the given immediate value to the given memory address.
     */
    void addImmToMem(String immediate, String destinationAddress, CodeContainer codeContainer);

    /**
     * Generate code for dividing the value stored in this storage location by the value stored in
     * the given storage location, storing the result in this storage location. This method handles
     * floating point division only. The result will be a floating point value.
     */
    void divideThisWithLoc(StorageLocation location, CodeContainer codeContainer);

    /**
     * Generate code for dividing the value stored in this storage location by the value stored in
     * the given storage location, storing the result in this storage location. This method handles
     * integer division only. The result will be an integer.
     */
    void idivThisWithLoc(StorageLocation location, CodeContainer codeContainer);

    /**
     * Generate code for calculating the modulo of the value stored in this storage location and
     * the value stored in the given storage location, storing the result in this storage location.
     */
    void modThisWithLoc(StorageLocation location, CodeContainer codeContainer);

    /**
     * Generate code for multiplying the value stored in the given storage location with the value
     * stored in this storage location, storing the result in this storage location.
     */
    void multiplyLocWithThis(StorageLocation location, CodeContainer codeContainer);

    /**
     * Generate code for subtracting the value stored in the given storage location from this storage location, 
     * storing the result in this storage location.
     */
    void subtractLocFromThis(StorageLocation location, CodeContainer codeContainer);

    /**
     * Generates code for subtracting the given immediate value from the given memory address.
     */
    void subtractImmFromMem(String immediate, String destinationAddress, CodeContainer codeContainer);

    /**
     * Generate code for incrementing the value stored in this storage location.
     */
    void incrementThis(CodeContainer codeContainer);

    /**
     * Generate code for decrementing the value stored in this storage location.
     */
    void decrementThis(CodeContainer codeContainer);

    /**
     * Generate code for comparing the value stored in this storage location with the value stored in 
     * the given storage location. Neither value is changed by this operation.
     */
    void compareThisWithLoc(StorageLocation location, CodeContainer codeContainer);

    /**
     * Generate code for comparing the value stored in this storage location with the given immediate value.
     * Neither value is changed by this operation.
     */
    void compareThisWithImm(String immediate, CodeContainer codeContainer);

    /**
     * Generate code for doing bitwise and on the value stored in the given storage location and the value
     * stored in this storage location. The result is stored in this storage location.
     */
    void andLocWithThis(StorageLocation location, CodeContainer codeContainer);

    /**
     * Generate code for doing bitwise or on the value stored in the given storage location and the value
     * stored in this storage location. The result is stored in this storage location.
     */
    void orLocWithThis(StorageLocation location, CodeContainer codeContainer);

    /**
     * Generate code for doing bitwise xor on the value stored in the given storage location and the value
     * stored in this storage location. The result is stored in this storage location.
     */
    void xorLocWithThis(StorageLocation location, CodeContainer codeContainer);

    /**
     * Generate code for doing bitwise not on the value stored in this storage location.
     */
    void notThis(CodeContainer codeContainer);
}
