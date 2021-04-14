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

import se.dykstrom.jcc.common.assembly.base.CodeContainer;
import se.dykstrom.jcc.common.assembly.base.FixedLabel;
import se.dykstrom.jcc.common.assembly.base.Line;
import se.dykstrom.jcc.common.assembly.instruction.AddImmToReg;
import se.dykstrom.jcc.common.assembly.instruction.CallIndirect;
import se.dykstrom.jcc.common.assembly.instruction.Ret;
import se.dykstrom.jcc.common.assembly.instruction.SubImmFromReg;
import se.dykstrom.jcc.common.assembly.instruction.floating.RoundFloatRegToIntReg;
import se.dykstrom.jcc.common.functions.AssemblyFunction;
import se.dykstrom.jcc.common.types.F64;
import se.dykstrom.jcc.common.types.I64;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.singletonList;
import static se.dykstrom.jcc.common.assembly.base.FloatRegister.XMM0;
import static se.dykstrom.jcc.common.assembly.base.Register.RAX;
import static se.dykstrom.jcc.common.assembly.base.Register.RSP;
import static se.dykstrom.jcc.common.functions.BuiltInFunctions.FUN_FLOOR;
import static se.dykstrom.jcc.common.functions.FunctionUtils.LIB_LIBC;

/**
 * Implements the "int" function. This function converts the given argument, a numeric expression,
 * to an integer by rounding it down to the closest integer. For example, "int(2.5) = 2" and
 * "int(-2.5) = -3".
 * 
 * Signature: int(expression : F64) : I64
 * 
 * @author Johan Dykstrom
 */
public class BasicIntFunction extends AssemblyFunction {

    public static final String NAME = "int";

    public BasicIntFunction() {
        super(NAME, singletonList(F64.INSTANCE), I64.INSTANCE, Map.of(LIB_LIBC, Set.of(FUN_FLOOR)));
    }

    @Override
    public List<Line> lines() {
        CodeContainer codeContainer = new CodeContainer();

        // Allocate shadow space, call floor, and free shadow space
        codeContainer.add(new SubImmFromReg("20h", RSP));
        codeContainer.add(new CallIndirect(new FixedLabel(FUN_FLOOR.getMappedName())));
        codeContainer.add(new AddImmToReg("20h", RSP));

        // XMM0 now contains the floored floating point value, RAX should contain the result
        codeContainer.add(new RoundFloatRegToIntReg(XMM0, RAX));
        codeContainer.add(new Ret());
        
        return codeContainer.lines();
    }
}
