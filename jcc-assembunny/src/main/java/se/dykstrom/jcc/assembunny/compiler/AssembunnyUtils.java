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

package se.dykstrom.jcc.assembunny.compiler;

import se.dykstrom.jcc.assembunny.ast.AssembunnyRegister;
import se.dykstrom.jcc.common.ast.Expression;
import se.dykstrom.jcc.common.ast.IdentifierDerefExpression;
import se.dykstrom.jcc.common.storage.StorageFactory;
import se.dykstrom.jcc.common.storage.StorageLocation;
import se.dykstrom.jcc.common.types.I64;
import se.dykstrom.jcc.common.types.Identifier;

import java.util.HashMap;
import java.util.Map;

/**
 * Contains utility methods and constants for the Assembunny language.
 * 
 * @author Johan Dykstrom
 */
public class AssembunnyUtils {

    public static final Identifier IDENT_A = new Identifier(AssembunnyRegister.A.name(), I64.INSTANCE);
    public static final Expression IDE_A = new IdentifierDerefExpression(0, 0, IDENT_A);

    /** The end-of-program jump target where invalid jumps are redirected. */
    public static final String END_JUMP_TARGET = "end";

    /** Maps Assembunny register to CPU register. */
    private static final Map<String, StorageLocation> REGISTER_MAP = new HashMap<>();

    private AssembunnyUtils() { }

    /**
     * Allocates one CPU register for each Assembunny register.
     */
    public static void allocateCpuRegisters(final StorageFactory storageFactory) {
        for (AssembunnyRegister assembunnyRegister : AssembunnyRegister.values()) {
            REGISTER_MAP.put(assembunnyRegister.name(), storageFactory.allocateNonVolatile());
        }
    }

    /**
     * Returns the CPU register associated with the Assembunny register in the given expression.
     */
    public static StorageLocation getCpuRegister(final Identifier identifier) {
        return REGISTER_MAP.get(identifier.name());
    }

    public static String lineNumberLabel(final long lineNumber) {
        if (lineNumber < 0) {
            throw new IllegalArgumentException("negative line number");
        }
        return "line" + lineNumber;
    }
}
