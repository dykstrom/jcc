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

package se.dykstrom.jcc.common.compiler;

import se.dykstrom.jcc.common.assembly.base.Comment;
import se.dykstrom.jcc.common.assembly.instruction.Call;
import se.dykstrom.jcc.common.ast.Expression;
import se.dykstrom.jcc.common.functions.Function;
import se.dykstrom.jcc.common.storage.StorageLocation;

import java.util.List;

/**
 * Interface to be implemented by the different function call helper classes. The
 * purpose of a function call helper is to help generating code for function calls.
 *
 * @author Johan Dykstrom
 */
public interface FunctionCallHelper {

    /**
     * Generates code for making the given {@code functionCall}. The list of expressions is evaluated, and the
     * values are stored in the function call registers (RCX, RDX, R8, and R9 for integer and pointer arguments,
     * or XMM0, XMM1, XMM2, and XMM3 for floating point arguments) and on the stack if needed.
     *
     * Shadow space is also allocated and cleaned up if needed. The already allocated {@link StorageLocation}
     * given to this method is used as the first storage location when evaluating the function argument
     * expressions. If more storage locations are required, they are allocated and de-allocated inside the
     * method.
     *
     * @param function The function to call.
     * @param functionCall The function call to make.
     * @param functionComment A function call comment to insert before calling the function.
     * @param args The arguments to the function.
     * @param firstLocation An already allocated storage location to use when evaluating expressions.
     */
    void addFunctionCall(Function function, Call functionCall, Comment functionComment, List<Expression> args, StorageLocation firstLocation);
}
