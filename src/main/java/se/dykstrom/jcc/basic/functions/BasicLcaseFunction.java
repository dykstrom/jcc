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

import se.dykstrom.jcc.common.intermediate.CodeContainer;
import se.dykstrom.jcc.common.assembly.base.FixedLabel;
import se.dykstrom.jcc.common.assembly.base.Label;
import se.dykstrom.jcc.common.intermediate.Line;
import se.dykstrom.jcc.common.assembly.instruction.*;
import se.dykstrom.jcc.common.assembly.other.Snippets;
import se.dykstrom.jcc.common.functions.AssemblyFunction;
import se.dykstrom.jcc.common.types.Str;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.singletonList;
import static se.dykstrom.jcc.common.assembly.base.Register.*;
import static se.dykstrom.jcc.common.functions.BuiltInFunctions.*;
import static se.dykstrom.jcc.common.functions.FunctionUtils.LIB_LIBC;

/**
 * Implements the "lcase$" function. This function converts a string to lower case. The "lcase$" function
 * allocates memory for the returned string. This memory must be managed and freed when not needed.
 * <p>
 * Signature: lcase$(string : Str) : Str
 *
 * @author Johan Dykstrom
 */
public class BasicLcaseFunction extends AssemblyFunction {

    public static final String NAME = "lcase$";

    private static final String ASCII_NULL = "0h";
    private static final String SOURCE_OFFSET = "10h";
    private static final String DESTINATION_OFFSET = "18h";
    private static final String SHADOW_SPACE = "20h";

    public BasicLcaseFunction() {
        super(NAME, singletonList(Str.INSTANCE), Str.INSTANCE, Map.of(LIB_LIBC, Set.of(FUN_MALLOC, FUN_STRLEN, FUN_TOLOWER)));
    }

    @Override
    public List<Line> lines() {
        CodeContainer codeContainer = new CodeContainer();

        // Create jump labels
        Label doneLabel = new Label("_lcase$_done");
        Label loopLabel = new Label("_lcase$_loop");

        // Save arguments in home locations
        {
            codeContainer.add(new PushReg(RBP));
            codeContainer.add(new MoveRegToReg(RSP, RBP));
            codeContainer.add(new MoveRegToMem(RCX, RBP, SOURCE_OFFSET));
        }

        // Push registers used locally
        {
            codeContainer.add(new PushReg(RDI));
            codeContainer.add(new PushReg(RSI));
        }

        // Find out size of source string
        {
            codeContainer.add(new SubImmFromReg(SHADOW_SPACE, RSP));
            codeContainer.add(new CallIndirect(new FixedLabel(FUN_STRLEN.getMappedName())));
            codeContainer.add(new AddImmToReg(SHADOW_SPACE, RSP));
        }

        // Allocate memory for destination string
        {
            codeContainer.add(new IncReg(RAX));                                  // Make room for null character
            codeContainer.addAll(Snippets.malloc(RAX));                          // Allocate memory
            codeContainer.add(new MoveRegToMem(RAX, RBP, DESTINATION_OFFSET));   // Save address to destination string
        }

        // Fill string
        {
            codeContainer.add(new MoveMemToReg(RBP, SOURCE_OFFSET, RSI));        // Source address goes in RSI
            codeContainer.add(new MoveRegToReg(RAX, RDI));                       // Destination address goes in RDI

            // LOOP
            codeContainer.add(loopLabel);

            codeContainer.add(new CmpByteMemWithImm(RSI, ASCII_NULL));           // Loop until we find a null
            codeContainer.add(new Je(doneLabel));

            // Convert character to lower case
            {
                codeContainer.add(new MoveByteMemToReg(RSI, RCX));
                codeContainer.add(new SubImmFromReg(SHADOW_SPACE, RSP));
                codeContainer.add(new CallIndirect(new FixedLabel(FUN_TOLOWER.getMappedName())));
                codeContainer.add(new AddImmToReg(SHADOW_SPACE, RSP));
                codeContainer.add(new MoveRegToMem(RAX.asLowRegister8(), RDI));
            }

            codeContainer.add(new IncReg(RSI));                                  // Increase counters
            codeContainer.add(new IncReg(RDI));
            codeContainer.add(new Jmp(loopLabel));
        }

        // DONE
        codeContainer.add(doneLabel);

        codeContainer.add(new MoveByteImmToMem(ASCII_NULL, RDI));                // Add null character at the end
        codeContainer.add(new MoveMemToReg(RBP, DESTINATION_OFFSET, RAX));       // Return address to destination string

        // Restore registers used locally
        {
            codeContainer.add(new PopReg(RSI));
            codeContainer.add(new PopReg(RDI));
            codeContainer.add(new PopReg(RBP));
        }
        codeContainer.add(new Ret());

        return codeContainer.lines();
    }
}
