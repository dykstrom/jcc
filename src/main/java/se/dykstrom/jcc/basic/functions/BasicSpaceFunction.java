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

import se.dykstrom.jcc.common.assembly.base.Code;
import se.dykstrom.jcc.common.assembly.base.CodeContainer;
import se.dykstrom.jcc.common.assembly.base.FixedLabel;
import se.dykstrom.jcc.common.assembly.base.Label;
import se.dykstrom.jcc.common.assembly.instruction.*;
import se.dykstrom.jcc.common.assembly.other.Snippets;
import se.dykstrom.jcc.common.functions.AssemblyFunction;
import se.dykstrom.jcc.common.types.I64;
import se.dykstrom.jcc.common.types.Str;
import se.dykstrom.jcc.common.utils.MapUtils;
import se.dykstrom.jcc.common.utils.SetUtils;

import java.util.List;

import static java.util.Collections.singletonList;
import static se.dykstrom.jcc.common.assembly.base.Register.*;
import static se.dykstrom.jcc.common.functions.BuiltInFunctions.*;
import static se.dykstrom.jcc.common.functions.FunctionUtils.LIB_LIBC;

/**
 * Implements the "space$" function. This function returns a string of spaces of the specified size.
 * If the size is negative, this function returns an empty string. The "space$" function allocates
 * memory for the returned string. This memory must be managed and freed when not needed.
 * 
 * Signature: space$(size : I64) : Str
 * 
 * @author Johan Dykstrom
 */
public class BasicSpaceFunction extends AssemblyFunction {

    public static final String NAME = "space$";

    private static final String ASCII_NULL = "0h";
    private static final String ASCII_SPACE = "20h";
    private static final String SIZE_OFFSET = "10h";

    public BasicSpaceFunction() {
        super(NAME, singletonList(I64.INSTANCE), Str.INSTANCE, MapUtils.of(LIB_LIBC, SetUtils.of(FUN_MALLOC.getName(), FUN_MEMSET.getName())));
    }

    @Override
    public List<Code> codes() {
        CodeContainer codeContainer = new CodeContainer();

        // Create jump labels
        Label continueLabel = new Label("_space_continue");

        // If size (RCX) >= 0 continue, otherwise set size to 0 before continuing
        codeContainer.add(new CmpRegWithImm(RCX, "0h"));
        codeContainer.add(new Jge(continueLabel));

        // Setting the size to 0 gives an empty string
        codeContainer.add(new MoveImmToReg("0h", RCX));

        codeContainer.add(continueLabel);

        // Save arguments in home locations
        {
            codeContainer.add(new PushReg(RBP));
            codeContainer.add(new MoveRegToReg(RSP, RBP));
            codeContainer.add(new MoveRegToMem(RCX, RBP, SIZE_OFFSET));
        }

        // Allocate memory for string
        {
            // Make room for the null character
            codeContainer.add(new IncReg(RCX));
            codeContainer.addAll(Snippets.malloc(RCX));
        }

        // Fill allocated memory with spaces
        {
            // Memory address goes in RCX, character in RDX, and size in R8
            codeContainer.add(new MoveRegToReg(RAX, RCX));
            codeContainer.add(new MoveImmToReg(ASCII_SPACE, RDX));
            codeContainer.add(new MoveMemToReg(RBP, SIZE_OFFSET, R8));

            // RAX = memset(address, character, size)
            codeContainer.add(new SubImmFromReg("20h", RSP));
            codeContainer.add(new CallIndirect(new FixedLabel(FUN_MEMSET.getMappedName())));
            codeContainer.add(new AddImmToReg("20h", RSP));
        }

        // Add null character at the end
        {
            // Address to last character goes in R11
            codeContainer.add(new MoveMemToReg(RBP, SIZE_OFFSET, R11));
            codeContainer.add(new AddRegToReg(RAX, R11));
            codeContainer.add(new MoveByteImmToMem(ASCII_NULL, R11));
        }

        codeContainer.add(new PopReg(RBP));
        codeContainer.add(new Ret());
        
        return codeContainer.codes();
    }
}
