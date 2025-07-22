/*
 * Copyright (C) 2025 Johan Dykstrom
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

package se.dykstrom.jcc.llvm.code;

import se.dykstrom.jcc.common.ast.Expression;
import se.dykstrom.jcc.common.functions.Function;

import java.util.List;
import java.util.Optional;

/**
 * This interface maps a built-in function definition to an expression
 * or a library function. The methods in this interface are called during
 * code generation. How the built-in functions are implemented does not
 * affect the semantic analysis.
 */
public interface LlvmFunctions {

    /**
     * Returns an optional expression that can be used to inline
     * a call to the given function.
     */
    Optional<Expression> getInlineExpression(final Function function, final List<Expression> args);

    /**
     * Returns the library function that implements the given built-in function.
     */
    Function getLibraryFunction(final Function function);
}
