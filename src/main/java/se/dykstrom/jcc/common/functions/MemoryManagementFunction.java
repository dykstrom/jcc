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

import se.dykstrom.jcc.common.types.Constant;
import se.dykstrom.jcc.common.types.I64;
import se.dykstrom.jcc.common.types.Identifier;
import se.dykstrom.jcc.common.utils.GcOptions;

/**
 * An interface that defines common constants for memory management functions.
 *
 * @author Johan Dykstrom
 */
public interface MemoryManagementFunction {

    String SHADOW_SPACE = "20h";

    String PTR_SIZE = "8h";
    String NODE_SIZE = Integer.toString(3 * 8, 16) + "h";

    String NODE_DATA_OFFSET = "8h";
    String NODE_TYPE_OFFSET = "10h";

    String NOT_MANAGED = "0h";
    String MARKED = "1";
    String UNMARKED = "0";

    Constant ALLOCATION_LIST = new Constant(new Identifier("_gc_allocation_list", I64.INSTANCE), "0");
    Constant ALLOCATION_COUNT = new Constant(new Identifier("_gc_allocation_count", I64.INSTANCE), "0");

    // The initial GC threshold can be configured on the command line, so we get the value from there
    Constant ALLOCATION_LIMIT = new Constant(new Identifier("_gc_allocation_limit", I64.INSTANCE),
            () -> Integer.toString(GcOptions.INSTANCE.getInitialGcThreshold()));

    /**
     * Runs the given debug code to print GC debug information if command line flag -print-gc is enabled.
     */
    default void debug(Runnable codeForDebugging) {
        if (GcOptions.INSTANCE.isPrintGc()) {
            codeForDebugging.run();
        }
    }
}
