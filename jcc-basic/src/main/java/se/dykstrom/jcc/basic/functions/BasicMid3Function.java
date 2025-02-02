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
 * Implements the three-argument {@code mid$(string, start, number)} function. This function returns a
 * substring of {@code string} that contains {@code number} characters starting from {@code start} and
 * forward. If {@code start} is greater than the length of {@code string} or if {@code number} is 0,
 * an empty string is returned. If there are less than {@code number} characters following {@code start}
 * these characters are returned. The index of the first character in the string is 1.
 * <p/>
 * If {@code start} is less than 1 or {@code number} is less than 0, an illegal function call occurs.
 * <p/>
 * The {@code mid$} function allocates memory for the returned string. This memory must be managed
 * and freed when not needed.
 * <p/>
 * Signature: {@code mid$(string : Str, start : I64, number : I64) : Str}
 *
 * @author Johan Dykstrom
 */
public class BasicMid3Function extends AssemblyFunction {

    public static final String NAME = "mid$";

    private static final String ASCII_NULL = "0h";
    private static final String STRING_OFFSET = "10h";
    private static final String START_OFFSET = "18h";
    private static final String NUMBER_OFFSET = "20h";
    private static final String LENGTH_OFFSET = "28h";

    private static final Constant ERROR_MSG = new Constant(new Identifier("_err_function_mid$", Str.INSTANCE), "\"Error: Illegal function call: mid$\",0");
    private static final List<Type> ARG_TYPES = List.of(Str.INSTANCE, I64.INSTANCE, I64.INSTANCE);

    BasicMid3Function() {
        super(NAME, ARG_TYPES, Str.INSTANCE, Map.of(LIB_LIBC, Set.of(FUN_MALLOC, FUN_STRLEN, FUN_STRNCPY)), Set.of(ERROR_MSG));
    }

    @Override
    public List<Line> lines() {
        return new BasicMid3Function.InternalCodeContainer().lines();
    }

    private static class InternalCodeContainer extends CodeContainer {
        private InternalCodeContainer() {
            // Create jump labels
            Label allocateLabel = new Label("_mid3$_allocate");
            Label doneLabel = new Label("_mid3$_done");
            Label errorLabel = new Label("_mid3$_error");

            add(new AssemblyComment("Save base pointer"));
            add(new PushReg(RBP));
            add(new MoveRegToReg(RSP, RBP));

            // Save arguments in home locations
            addAll(Snippets.enter(ARG_TYPES));

            // Check bounds
            add(new CmpRegWithImm(RDX, "1h")); // Basic uses 1-based indices
            add(new Jl(errorLabel));
            add(new CmpRegWithImm(R8, "0h"));
            add(new Jl(errorLabel));

            // Find length of string
            addAll(Snippets.strlen(RCX)); // Length of string in RAX
            add(new MoveRegToMem(RAX, RBP, LENGTH_OFFSET));

            // Find out how many characters to copy
            add(new SubMemFromReg(RBP, START_OFFSET, RAX));
            add(new IncReg(RAX));
            add(new CmpRegWithImm(RAX, "0"));
            add(new Jge(allocateLabel));
            add(new XorRegWithReg(RAX, RAX)); // If number is negative, set it to 0
            add(new MoveRegToMem(RAX, RBP, NUMBER_OFFSET));

            // Allocate memory for new string
            {
                // ALLOCATE
                add(allocateLabel);
                add(new IncReg(RAX));
                addAll(Snippets.malloc(RAX)); // Pointer to new string now in RAX
            }

            // Copy string
            {
                // Start index in RDX
                add(new MoveMemToReg(RBP, STRING_OFFSET, RDX));
                add(new AddMemToReg(RBP, START_OFFSET, RDX));
                add(new DecReg(RDX)); // Convert 1-based to 0-based
                // Length in R8
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
            add(new AssemblyComment("Restore base pointer"));
            add(new PopReg(RBP));
            add(new Ret());
        }
    }
}
