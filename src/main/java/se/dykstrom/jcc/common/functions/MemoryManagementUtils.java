/*
 * Copyright (C) 2019 Johan Dykstrom
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

package se.dykstrom.jcc.common.functions;

import se.dykstrom.jcc.common.ast.Expression;
import se.dykstrom.jcc.common.ast.IdentifierDerefExpression;
import se.dykstrom.jcc.common.ast.IdentifierNameExpression;
import se.dykstrom.jcc.common.ast.StringLiteral;
import se.dykstrom.jcc.common.types.*;
import se.dykstrom.jcc.common.utils.GcOptions;

/**
 * A utility class that defines common constants for memory management functions.
 *
 * @author Johan Dykstrom
 */
public final class MemoryManagementUtils {

    private MemoryManagementUtils() { }

    static final String SHADOW_SPACE = "20h";

    static final String PTR_SIZE = "8h";
    static final String NODE_SIZE = Integer.toString(3 * 8, 16) + "h";

    static final String NODE_DATA_OFFSET = "8h";
    static final String NODE_TYPE_OFFSET = "10h";

    public static final String NOT_MANAGED = "0h";
    static final String MARKED = "1";
    static final String UNMARKED = "0";

    static final Constant ALLOCATION_LIST = new Constant(new Identifier("_gc_allocation_list", I64.INSTANCE), "0");
    static final Constant ALLOCATION_COUNT = new Constant(new Identifier("_gc_allocation_count", I64.INSTANCE), "0");

    public static final Constant TYPE_POINTERS_START = new Constant(new Identifier("_gc_type_pointers_start", I64.INSTANCE), NOT_MANAGED);
    public static final Constant TYPE_POINTERS_STOP = new Constant(new Identifier("_gc_type_pointers_stop", I64.INSTANCE), NOT_MANAGED);

    // The initial GC threshold can be configured on the command line, so we get the value from there
    static final Constant ALLOCATION_LIMIT = new Constant(new Identifier("_gc_allocation_limit", I64.INSTANCE),
            () -> Integer.toString(GcOptions.INSTANCE.getInitialGcThreshold()));

    /**
     * Returns {@code true} if evaluating the given expression will allocate dynamic memory
     * that needs to be managed. Examples:
     *
     * - using the value of a string literal does not allocate memory
     * - de-referencing a string variable does not allocate memory
     * - adding two strings _does_ allocate memory
     * - calling a function that returns a string _does_ allocate memory
     *
     * @param expression The expression to check.
     * @param type The type of the expression.
     * @return True if {@code expression} allocates dynamic memory.
     */
    public static boolean allocatesDynamicMemory(Expression expression, Type type) {
        return (type instanceof Str) &&
                !(expression instanceof StringLiteral) &&
                !(expression instanceof IdentifierDerefExpression) &&
                !(expression instanceof IdentifierNameExpression);
    }

    /**
     * Runs the given debug code to print GC debug information if command line flag -print-gc is enabled.
     */
    static void debug(Runnable codeForDebugging) {
        if (GcOptions.INSTANCE.isPrintGc()) {
            codeForDebugging.run();
        }
    }
}
