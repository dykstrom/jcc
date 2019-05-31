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
import static se.dykstrom.jcc.common.functions.BuiltInFunctions.FUN_MALLOC;
import static se.dykstrom.jcc.common.functions.BuiltInFunctions.FUN_MEMSET;
import static se.dykstrom.jcc.common.functions.FunctionUtils.LIB_LIBC;

/**
 * Implements the "string$(I64, I64)" function. This function returns a string of characters
 * whose ASCII code is specified by the second integer. The size of the string is specified by
 * the first integer.
 *
 * If the specified size is less than 0, or if the asciiCode is not in the range 0-255, an
 * illegal function call occurs.
 *
 * The "string$" function allocates memory for the returned string. This memory must be managed
 * and freed when not needed.
 * 
 * Signature: string$(size : I64, asciiCode: I64) : Str
 * 
 * @author Johan Dykstrom
 */
public class BasicStringIntFunction extends AssemblyFunction {

    public static final String NAME = "string$";

    private static final String ASCII_NULL = "0h";
    private static final String SIZE_OFFSET = "10h";
    private static final String CODE_OFFSET = "18h";

    private static final Constant ERROR_MSG = new Constant(new Identifier("_err_function_string$", Str.INSTANCE), "\"Error: Illegal function call: string$\",0");

    BasicStringIntFunction() {
        super(NAME, asList(I64.INSTANCE, I64.INSTANCE), Str.INSTANCE, MapUtils.of(LIB_LIBC, SetUtils.of(FUN_MALLOC, FUN_MEMSET)), SetUtils.of(ERROR_MSG));
    }

    @Override
    public List<Code> codes() {
        return new InternalCodeContainer().codes();
    }

    private static class InternalCodeContainer extends CodeContainer {
        private InternalCodeContainer() {
            // Create jump labels
            Label errorLabel = new Label("_string_int$_error");
            Label doneLabel = new Label("_string_int$_done");

            // Save arguments in home locations
            addAll(Snippets.enter(2));

            // Check bounds
            {
                add(new CmpRegWithImm(RCX, "0h"));
                add(new Jl(errorLabel));
                add(new CmpRegWithImm(RDX, "0h"));
                add(new Jl(errorLabel));
                add(new CmpRegWithImm(RDX, "255"));
                add(new Jg(errorLabel));
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
                add(new MoveMemToReg(RBP, CODE_OFFSET, RDX));
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

            add(new PopReg(RBP));
            add(new Ret());
        }
    }
}
