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
import se.dykstrom.jcc.common.assembly.instruction.*;
import se.dykstrom.jcc.common.functions.AssemblyFunction;
import se.dykstrom.jcc.common.types.I64;
import se.dykstrom.jcc.common.types.Str;
import se.dykstrom.jcc.common.utils.MapUtils;
import se.dykstrom.jcc.common.utils.SetUtils;

import java.util.List;

import static java.util.Collections.singletonList;
import static se.dykstrom.jcc.common.assembly.base.Register.*;
import static se.dykstrom.jcc.common.functions.BuiltInFunctions.FUN_MALLOC;
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

    public BasicChrFunction() {
        super(NAME, singletonList(I64.INSTANCE), Str.INSTANCE, MapUtils.of(LIB_LIBC, SetUtils.of(FUN_MALLOC.getName())));
    }

    @Override
    public List<Code> codes() {
        CodeContainer codeContainer = new CodeContainer();

        // Save arguments in home locations
        {
            codeContainer.add(new PushReg(RBP));
            codeContainer.add(new MoveRegToReg(RSP, RBP));
            codeContainer.add(new MoveRegToMem(RCX, RBP, NUMBER_OFFSET));
        }

        // Allocate memory for string
        {
            // Size of string goes in RCX
            codeContainer.add(new MoveImmToReg(STRING_SIZE, RCX));

            // RAX = malloc(size)
            codeContainer.add(new SubImmFromReg("20h", RSP));
            codeContainer.add(new CallIndirect(new FixedLabel(FUN_MALLOC.getMappedName())));
            codeContainer.add(new AddImmToReg("20h", RSP));
        }

        // Fill string
        {
            // Move ascii code to first position in string
            codeContainer.add(new MoveMemToReg(RBP, NUMBER_OFFSET, RCX));
            codeContainer.add(new MoveRegToMem(RCX.asLowRegister8(), RAX));  // mov [rax], cl
            // Move null character to second position in string
            codeContainer.add(new MoveByteImmToMem(ASCII_NULL, RAX, "1h"));
        }

        codeContainer.add(new PopReg(RBP));
        codeContainer.add(new Ret());
        
        return codeContainer.codes();
    }
}
