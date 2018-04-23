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
import se.dykstrom.jcc.common.assembly.instruction.Ret;
import se.dykstrom.jcc.common.assembly.instruction.floating.TruncateFloatRegToIntReg;
import se.dykstrom.jcc.common.functions.AssemblyFunction;
import se.dykstrom.jcc.common.types.F64;
import se.dykstrom.jcc.common.types.I64;

import java.util.List;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static se.dykstrom.jcc.common.assembly.base.FloatRegister.XMM0;
import static se.dykstrom.jcc.common.assembly.base.Register.RAX;

/**
 * Implements the "fix" function. This function converts the given argument, a numeric expression,
 * to an integer by truncating the decimals.
 * 
 * Signature: fix(expression : F64) : I64
 * 
 * @author Johan Dykstrom
 */
public class BasicFixFunction extends AssemblyFunction {

    public static final String NAME = "fix";

    public BasicFixFunction() {
        super(NAME, singletonList(F64.INSTANCE), I64.INSTANCE, emptyMap());
    }

    @Override
    public List<Code> codes() {
        CodeContainer codeContainer = new CodeContainer();

        // XMM0 contains the floating point value, RAX should contain the result
        codeContainer.add(new TruncateFloatRegToIntReg(XMM0, RAX));
        codeContainer.add(new Ret());
        
        return codeContainer.codes();
    }
}
