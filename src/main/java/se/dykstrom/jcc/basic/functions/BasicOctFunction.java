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
import se.dykstrom.jcc.common.assembly.other.Snippets;
import se.dykstrom.jcc.common.functions.AssemblyFunction;
import se.dykstrom.jcc.common.types.Constant;
import se.dykstrom.jcc.common.types.I64;
import se.dykstrom.jcc.common.types.Identifier;
import se.dykstrom.jcc.common.types.Str;
import se.dykstrom.jcc.common.utils.MapUtils;
import se.dykstrom.jcc.common.utils.SetUtils;

import java.util.List;

import static java.util.Collections.singletonList;
import static se.dykstrom.jcc.common.assembly.base.Register.*;
import static se.dykstrom.jcc.common.functions.BuiltInFunctions.FUN_MALLOC;
import static se.dykstrom.jcc.common.functions.BuiltInFunctions.FUN_SPRINTF;
import static se.dykstrom.jcc.common.functions.FunctionUtils.LIB_LIBC;

/**
 * Implements the "oct$" function. This function converts a numeric expression to a octal string.
 * The "oct$" function allocates memory for the returned string. This memory must be managed and
 * freed when not needed.
 * 
 * Signature: oct$(number : I64) : Str
 * 
 * @author Johan Dykstrom
 */
public class BasicOctFunction extends AssemblyFunction {

    public static final String NAME = "oct$";

    private static final String NUMBER_OFFSET = "10h";
    private static final String ADDRESS_OFFSET = "18h";
    private static final String STRING_SIZE = "30";
    private static final Constant FORMAT_STRING = new Constant(new Identifier("_fmt_function_oct", Str.INSTANCE), "\"%llo\",0");

    public BasicOctFunction() {
        super(NAME, singletonList(I64.INSTANCE), Str.INSTANCE, MapUtils.of(LIB_LIBC, SetUtils.of(FUN_MALLOC, FUN_SPRINTF)), SetUtils.of(FORMAT_STRING));
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
        codeContainer.addAll(Snippets.malloc(STRING_SIZE));
        // Save address to string
        codeContainer.add(new MoveRegToMem(RAX, RBP, ADDRESS_OFFSET));

        // Write number in octal format to string
        {
            // Address to string goes in RCX, format string in RDX, and number in R8
            codeContainer.add(new MoveRegToReg(RAX, RCX));
            codeContainer.add(new MoveImmToReg(FORMAT_STRING.getIdentifier().getMappedName(), RDX));
            codeContainer.add(new MoveMemToReg(RBP, NUMBER_OFFSET, R8));

            // sprintf(str, format, number)
            codeContainer.add(new SubImmFromReg("20h", RSP));
            codeContainer.add(new CallIndirect(new FixedLabel(FUN_SPRINTF.getMappedName())));
            codeContainer.add(new AddImmToReg("20h", RSP));
        }

        // Store address to string in RAX
        codeContainer.add(new MoveMemToReg(RBP, ADDRESS_OFFSET, RAX));

        codeContainer.add(new PopReg(RBP));
        codeContainer.add(new Ret());
        
        return codeContainer.codes();
    }
}
