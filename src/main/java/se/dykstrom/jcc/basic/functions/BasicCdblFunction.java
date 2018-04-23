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
import se.dykstrom.jcc.common.functions.AssemblyFunction;
import se.dykstrom.jcc.common.types.F64;

import java.util.List;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;

/**
 * Implements the "cdbl" function. This function converts the given argument, a numeric expression,
 * to a double. If the argument is a double already, the value is just returned. Note that the type
 * conversion rules will automatically cast integer arguments to doubles.
 * 
 * Signature: cdbl(expression : F64) : F64
 * 
 * @author Johan Dykstrom
 */
public class BasicCdblFunction extends AssemblyFunction {

    public static final String NAME = "cdbl";

    public BasicCdblFunction() {
        super(NAME, singletonList(F64.INSTANCE), F64.INSTANCE, emptyMap());
    }

    @Override
    public List<Code> codes() {
        CodeContainer codeContainer = new CodeContainer();

        // XMM0 already contains the value converted to a double, so we can just return
        codeContainer.add(new Ret());
        
        return codeContainer.codes();
    }
}
