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
import se.dykstrom.jcc.common.assembly.instruction.*;
import se.dykstrom.jcc.common.assembly.other.Snippets;
import se.dykstrom.jcc.common.code.CodeContainer;
import se.dykstrom.jcc.common.code.Label;
import se.dykstrom.jcc.common.code.Line;
import se.dykstrom.jcc.common.functions.AssemblyFunction;
import se.dykstrom.jcc.common.types.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static se.dykstrom.jcc.common.assembly.base.Register.*;
import static se.dykstrom.jcc.common.functions.FunctionUtils.LIB_LIBC;
import static se.dykstrom.jcc.common.functions.LibcBuiltIns.*;

/**
 * Implements the {@code right$(string, number)} function. This function returns the {@code number}
 * rightmost characters of {@code string}. If {@code number} is greater than the length of {@code string}
 * the whole string is returned.
 * <p/>
 * If the specified number is less than 0, an illegal function call occurs.
 * <p/>
 * The {@code right$} function allocates memory for the returned string. This memory must be managed
 * and freed when not needed.
 * <p/>
 * Signature: {@code right$(string : Str, number : I64) : Str}
 *
 * @author Johan Dykstrom
 */
public class BasicRightFunction extends AssemblyFunction {

    public static final String NAME = "right$";

    private static final String ASCII_NULL = "0h";
    private static final String STRING_OFFSET = "10h";
    private static final String NUMBER_OFFSET = "18h";
    private static final String LENGTH_OFFSET = "20h";

    private static final Constant ERROR_MSG = new Constant(new Identifier("_err_function_right$", Str.INSTANCE), "\"Error: Illegal function call: right$\",0");
    private static final List<Type> ARG_TYPES = List.of(Str.INSTANCE, I64.INSTANCE);

    BasicRightFunction() {
        super(NAME, ARG_TYPES, Str.INSTANCE, Map.of(LIB_LIBC, Set.of(FUN_MALLOC, FUN_STRLEN, FUN_STRNCPY)), Set.of(ERROR_MSG));
    }

    @Override
    public List<Line> lines() {
        return new BasicRightFunction.InternalCodeContainer().lines();
    }

    private static class InternalCodeContainer extends CodeContainer {
        private InternalCodeContainer() {
            // Create jump labels
            Label doneLabel = new Label("_right$_done");
            Label allocateLabel = new Label("_right$_allocate");
            Label errorLabel = new Label("_right$_error");

            add(new AssemblyComment("Save base pointer"));
            add(new PushReg(RBP));
            add(new MoveRegToReg(RSP, RBP));

            // Save arguments in home locations
            addAll(Snippets.enter(ARG_TYPES));

            // Check bounds
            {
                add(new CmpRegWithImm(RDX, "0h"));
                add(new Jl(errorLabel));
            }

            // Find length of string
            {
                addAll(Snippets.strlen(RCX)); // Length of string in RAX
                add(new MoveRegToMem(RAX, RBP, LENGTH_OFFSET));
            }

            // Find out how many characters to copy
            {
                add(new MoveMemToReg(RBP, NUMBER_OFFSET, RCX));
                add(new CmpRegWithReg(RAX, RCX));
                // If length is greater than number, copy number characters
                add(new Jge(allocateLabel));
                // Otherwise, copy length characters
                add(new MoveRegToReg(RAX, RCX));
                add(new MoveRegToMem(RCX, RBP, NUMBER_OFFSET));
            }

            // Allocate memory for new string
            {
                // ALLOCATE
                add(allocateLabel);
                add(new IncReg(RCX));
                addAll(Snippets.malloc(RCX)); // Pointer to new string now in RAX
            }

            // Calculate start index
            {
                add(new MoveMemToReg(RBP, LENGTH_OFFSET, RCX));
                add(new MoveMemToReg(RBP, STRING_OFFSET, RDX));
                add(new MoveMemToReg(RBP, NUMBER_OFFSET, R8));
                add(new AddRegToReg(RCX, RDX));
                add(new SubRegFromReg(R8, RDX));
            }

            // Copy string
            {
                addAll(Snippets.strncpy(RAX, RDX, R8)); // Pointer to new string still in RAX
            }

            // Add null character at the end
            {
                add(new MoveRegToReg(RAX, R11));
                add(new AddMemToReg(RBP, NUMBER_OFFSET, R11));
                add(new MoveByteImmToMem(ASCII_NULL, R11));
            }
            add(new Jmp(doneLabel));

            // ERROR
            add(errorLabel);
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
