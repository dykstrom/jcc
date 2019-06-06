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

import se.dykstrom.jcc.common.assembly.base.Code;
import se.dykstrom.jcc.common.assembly.base.CodeContainer;
import se.dykstrom.jcc.common.assembly.base.Label;
import se.dykstrom.jcc.common.assembly.instruction.*;
import se.dykstrom.jcc.common.assembly.other.Snippets;
import se.dykstrom.jcc.common.functions.AssemblyFunction;
import se.dykstrom.jcc.common.types.Constant;
import se.dykstrom.jcc.common.types.I64;
import se.dykstrom.jcc.common.types.Identifier;
import se.dykstrom.jcc.common.types.Str;
import se.dykstrom.jcc.common.utils.MapUtils;
import se.dykstrom.jcc.common.utils.SetUtils;

import java.util.List;

import static java.util.Arrays.asList;
import static se.dykstrom.jcc.common.assembly.base.Register.*;
import static se.dykstrom.jcc.common.functions.BuiltInFunctions.*;
import static se.dykstrom.jcc.common.functions.FunctionUtils.LIB_LIBC;

/**
 * Implements the {@code left$(string, number)} function. This function returns the {@code number}
 * leftmost characters of {@code string}. If {@code number} is greater than the length of {@code string}
 * the whole string is returned.
 * <p/>
 * If the specified number is less than 0, an illegal function call occurs.
 * <p/>
 * The {@code left$} function allocates memory for the returned string. This memory must be managed
 * and freed when not needed.
 * <p/>
 * Signature: {@code left$(string : Str, number : I64) : Str}
 *
 * @author Johan Dykstrom
 */
public class BasicLeftFunction extends AssemblyFunction {

    public static final String NAME = "left$";

    private static final String ASCII_NULL = "0h";
    private static final String STRING_OFFSET = "10h";
    private static final String NUMBER_OFFSET = "18h";

    private static final Constant ERROR_MSG = new Constant(new Identifier("_err_function_left$", Str.INSTANCE), "\"Error: Illegal function call: left$\",0");

    BasicLeftFunction() {
        super(NAME, asList(Str.INSTANCE, I64.INSTANCE), Str.INSTANCE, MapUtils.of(LIB_LIBC, SetUtils.of(FUN_MALLOC, FUN_STRLEN, FUN_STRNCPY)), SetUtils.of(ERROR_MSG));
    }

    @Override
    public List<Code> codes() {
        return new BasicLeftFunction.InternalCodeContainer().codes();
    }

    private static class InternalCodeContainer extends CodeContainer {
        private InternalCodeContainer() {
            // Create jump labels
            Label doneLabel = new Label("_left$_done");
            Label allocateLabel = new Label("_left$_allocate");
            Label errorLabel = new Label("_left$_error");

            // Save arguments in home locations
            addAll(Snippets.enter(2));

            // Check bounds
            {
                add(new CmpRegWithImm(RDX, "0h"));
                add(new Jl(errorLabel));
            }

            // Find length of string
            addAll(Snippets.strlen(RCX)); // Length of string in RAX

            {
                add(new MoveMemToReg(RBP, NUMBER_OFFSET, RDX));
                add(new CmpRegWithReg(RAX, RDX));
                // If length is greater than number, copy number characters
                add(new Jge(allocateLabel));
                // Otherwise, copy length characters
                add(new MoveRegToReg(RAX, RDX));
                add(new MoveRegToMem(RDX, RBP, NUMBER_OFFSET));
            }

            // Allocate memory for new string
            {
                // ALLOCATE
                add(allocateLabel);
                add(new IncReg(RDX));
                addAll(Snippets.malloc(RDX)); // Pointer to new string now in RAX
            }

            // Copy string
            {
                add(new MoveMemToReg(RBP, STRING_OFFSET, RDX));
                add(new MoveMemToReg(RBP, NUMBER_OFFSET, R8));
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
            add(new PopReg(RBP));
            add(new Ret());
        }
    }
}
