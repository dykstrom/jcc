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

package se.dykstrom.jcc.basic.functions;

import se.dykstrom.jcc.common.assembly.base.AssemblyComment;
import se.dykstrom.jcc.common.assembly.base.Label;
import se.dykstrom.jcc.common.assembly.instruction.*;
import se.dykstrom.jcc.common.assembly.other.Snippets;
import se.dykstrom.jcc.common.functions.AssemblyFunction;
import se.dykstrom.jcc.common.intermediate.CodeContainer;
import se.dykstrom.jcc.common.intermediate.Line;
import se.dykstrom.jcc.common.types.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static se.dykstrom.jcc.common.assembly.base.Register.*;
import static se.dykstrom.jcc.common.functions.BuiltInFunctions.FUN_MALLOC;
import static se.dykstrom.jcc.common.functions.BuiltInFunctions.FUN_MEMSET;
import static se.dykstrom.jcc.common.functions.FunctionUtils.LIB_LIBC;

/**
 * Implements the "string$(I64, Str)" function. This function returns a string of characters
 * of size occurrences of the first character of the string string.
 *
 * If the specified size is less than 0, or if string is empty, an illegal function call occurs.
 *
 * The "string$" function allocates memory for the returned string. This memory must be managed
 * and freed when not needed.
 * 
 * Signature: string$(size : I64, string: Str) : Str
 * 
 * @author Johan Dykstrom
 */
public class BasicStringStrFunction extends AssemblyFunction {

    public static final String NAME = "string$";

    private static final String ASCII_NULL = "0h";
    private static final String SIZE_OFFSET = "10h";
    private static final String CODE_OFFSET = "18h";

    private static final Constant ERROR_MSG = new Constant(new Identifier("_err_function_string$", Str.INSTANCE), "\"Error: Illegal function call: string$\",0");
    private static final List<Type> ARG_TYPES = List.of(I64.INSTANCE, Str.INSTANCE);

    BasicStringStrFunction() {
        super(NAME, ARG_TYPES, Str.INSTANCE, Map.of(LIB_LIBC, Set.of(FUN_MALLOC, FUN_MEMSET)), Set.of(ERROR_MSG));
    }

    @Override
    public List<Line> lines() {
        return new InternalCodeContainer().lines();
    }

    private static class InternalCodeContainer extends CodeContainer {
        private InternalCodeContainer() {
            // Create jump labels
            Label errorLabel = new Label("_string_str$_error");
            Label doneLabel = new Label("_string_str$_done");

            add(new AssemblyComment("Save base pointer"));
            add(new PushReg(RBP));
            add(new MoveRegToReg(RSP, RBP));

            // Save arguments in home locations
            addAll(Snippets.enter(ARG_TYPES));

            // Check bounds
            {
                add(new CmpRegWithImm(RCX, "0h"));
                add(new Jl(errorLabel));
                add(new CmpByteMemWithImm(RDX, "0h"));
                add(new Je(errorLabel));
            }

            // Allocate memory for string
            {
                // Make room for the null character
                add(new IncReg(RCX));
                addAll(Snippets.malloc(RCX));
            }

            // Fill allocated memory with specified character
            {
                // Memory address goes in RCX, character in RDX, and size in R8
                add(new MoveMemToReg(RBP, CODE_OFFSET, RDX));         // Load address to string in RDX
                add(new MoveByteMemToReg(RDX, RDX));                  // Load first character in RDX
                add(new MoveMemToReg(RBP, SIZE_OFFSET, R8));
                addAll(Snippets.memset(RAX, RDX, R8));
            }

            // Add null character at the end
            {
                // Address to last character goes in R11
                add(new MoveMemToReg(RBP, SIZE_OFFSET, R11));
                add(new AddRegToReg(RAX, R11));
                add(new MoveByteImmToMem(ASCII_NULL, R11));
            }
            add(new Jmp(doneLabel));

            // ERROR
            add(errorLabel);

            // Print error message and exit
            addAll(Snippets.printf(ERROR_MSG.getIdentifier().getMappedName()));
            addAll(Snippets.exit("1h"));

            // DONE
            add(doneLabel);
            add(new AssemblyComment("Restore base pointer"));
            add(new PopReg(RBP));
            add(new Ret());
        }
    }
}
