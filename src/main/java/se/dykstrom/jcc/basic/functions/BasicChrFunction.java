/*
 * Copyright (C) 2018 Johan Dykstrom
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

import se.dykstrom.jcc.common.intermediate.CodeContainer;
import se.dykstrom.jcc.common.assembly.base.Label;
import se.dykstrom.jcc.common.intermediate.Line;
import se.dykstrom.jcc.common.assembly.instruction.*;
import se.dykstrom.jcc.common.assembly.other.Snippets;
import se.dykstrom.jcc.common.functions.AssemblyFunction;
import se.dykstrom.jcc.common.types.Constant;
import se.dykstrom.jcc.common.types.I64;
import se.dykstrom.jcc.common.types.Identifier;
import se.dykstrom.jcc.common.types.Str;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.singletonList;
import static se.dykstrom.jcc.common.assembly.base.Register.*;
import static se.dykstrom.jcc.common.functions.BuiltInFunctions.*;
import static se.dykstrom.jcc.common.functions.FunctionUtils.LIB_LIBC;

/**
 * Implements the "chr$" function. This function returns a string of length one that contains
 * the character that corresponds to the given ASCII code. If the given number is less than 0
 * or greater than 255, an illegal function call occurs.
 *
 * The "chr$" function allocates memory for the returned string. This memory must be managed and
 * freed when not needed.
 * 
 * Signature: chr$(number : I64) : Str
 * 
 * @author Johan Dykstrom
 */
public class BasicChrFunction extends AssemblyFunction {

    public static final String NAME = "chr$";

    private static final String ASCII_NULL = "0h";
    private static final String NUMBER_OFFSET = "10h";
    private static final String STRING_SIZE = "2h";
    private static final Constant ERROR_MSG = new Constant(new Identifier("_err_function_chr", Str.INSTANCE), "\"Error: Illegal function call: chr$\",0");

    public BasicChrFunction() {
        super(NAME,
                singletonList(I64.INSTANCE),
                Str.INSTANCE,
                Map.of(LIB_LIBC, Set.of(FUN_EXIT, FUN_MALLOC, FUN_PRINTF)),
                Set.of(ERROR_MSG));
    }

    @Override
    public List<Line> lines() {
        CodeContainer codeContainer = new CodeContainer();

        // Create jump labels
        Label errorLabel = new Label("_chr$_error");
        Label doneLabel = new Label("_chr$_done");

        // Save arguments in home locations
        {
            codeContainer.add(new PushReg(RBP));
            codeContainer.add(new MoveRegToReg(RSP, RBP));
            codeContainer.add(new MoveRegToMem(RCX, RBP, NUMBER_OFFSET));
        }

        // Check bounds
        {
            codeContainer.add(new CmpRegWithImm(RCX, "0"));
            codeContainer.add(new Jl(errorLabel));
            codeContainer.add(new CmpRegWithImm(RCX, "255"));
            codeContainer.add(new Jg(errorLabel));
        }

        // Allocate memory for string
        codeContainer.addAll(Snippets.malloc(STRING_SIZE));

        // Fill string
        {
            // Move ascii code to first position in string
            codeContainer.add(new MoveMemToReg(RBP, NUMBER_OFFSET, RCX));
            codeContainer.add(new MoveRegToMem(RCX.asLowRegister8(), RAX));  // mov [rax], cl
            // Move null character to second position in string
            codeContainer.add(new MoveByteImmToMem(ASCII_NULL, RAX, "1h"));
        }
        codeContainer.add(new Jmp(doneLabel));

        // ERROR
        codeContainer.add(errorLabel);

        // Print error message and exit
        codeContainer.addAll(Snippets.printf(ERROR_MSG.getIdentifier().getMappedName()));
        codeContainer.addAll(Snippets.exit("1h"));

        // DONE
        codeContainer.add(doneLabel);

        codeContainer.add(new PopReg(RBP));
        codeContainer.add(new Ret());
        
        return codeContainer.lines();
    }
}
