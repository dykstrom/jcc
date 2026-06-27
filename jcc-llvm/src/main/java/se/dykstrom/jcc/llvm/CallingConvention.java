/*
 * Copyright (C) 2026 Johan Dykstrom
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

package se.dykstrom.jcc.llvm;

import se.dykstrom.jcc.common.functions.Function;
import se.dykstrom.jcc.common.functions.ReferenceFunction;
import se.dykstrom.jcc.common.functions.UserDefinedFunction;

/**
 * The LLVM calling convention used for a function definition or call site. User-defined functions
 * use {@code tailcc}, the convention built for guaranteed tail calls, so that {@code musttail}
 * calls are valid even across mismatched prototypes (cross-overload and mutual recursion). The
 * synthesized {@code main} (called by the C runtime) and all external/built-in functions use the
 * default C convention, which is omitted from the IR.
 *
 * <p>The convention must match between a function's definition and every call site that targets
 * it; deriving it from the {@link Function} guarantees that automatically.
 */
public enum CallingConvention {

    C(""),
    TAIL("tailcc ");

    private final String text;

    CallingConvention(final String text) {
        this.text = text;
    }

    /**
     * Returns the IR text for this convention, including a trailing space, or the empty string for
     * the default C convention.
     */
    public String toText() {
        return text;
    }

    public static CallingConvention of(final Function function) {
        // User-defined functions use tailcc (except the C-runtime entry point main). A reference
        // function is a function-typed value; in the LLVM backend these only ever point to
        // user-defined functions, so an indirect call must use tailcc too to match the callee.
        if (function instanceof ReferenceFunction) {
            return TAIL;
        }
        if (function instanceof UserDefinedFunction && !"main".equals(function.getName())) {
            return TAIL;
        }
        return C;
    }
}
