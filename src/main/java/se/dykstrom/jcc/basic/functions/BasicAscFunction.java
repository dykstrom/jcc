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

import se.dykstrom.jcc.common.assembly.base.Line;
import se.dykstrom.jcc.common.assembly.base.CodeContainer;
import se.dykstrom.jcc.common.assembly.instruction.MoveByteMemToReg;
import se.dykstrom.jcc.common.assembly.instruction.Ret;
import se.dykstrom.jcc.common.functions.AssemblyFunction;
import se.dykstrom.jcc.common.types.I64;
import se.dykstrom.jcc.common.types.Str;

import java.util.List;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static se.dykstrom.jcc.common.assembly.base.Register.RAX;
import static se.dykstrom.jcc.common.assembly.base.Register.RCX;

/**
 * Implements the "asc" function. This function returns the ASCII value of the first character of
 * the given string. For an empty string, it returns 0.
 * 
 * Signature: asc(string : Str) : I64
 * 
 * @author Johan Dykstrom
 */
public class BasicAscFunction extends AssemblyFunction {

    public static final String NAME = "asc";

    public BasicAscFunction() {
        super(NAME, singletonList(Str.INSTANCE), I64.INSTANCE, emptyMap());
    }

    @Override
    public List<Line> lines() {
        CodeContainer codeContainer = new CodeContainer();

        // RCX contains the address to the (first character of the) string
        codeContainer.add(new MoveByteMemToReg(RCX, RAX));
        codeContainer.add(new Ret());
        
        return codeContainer.lines();
    }
}
