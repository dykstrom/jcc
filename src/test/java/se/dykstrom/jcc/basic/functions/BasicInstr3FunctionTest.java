/*
 * Copyright (C) 2017 Johan Dykstrom
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

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import se.dykstrom.jcc.common.assembly.base.Code;
import se.dykstrom.jcc.common.assembly.instruction.Ret;

public class BasicInstr3FunctionTest {

    private static final Code RET = new Ret();
    
    @Test
    public void shouldEndWithRet() {
        BasicInstr3Function function = new BasicInstr3Function();
        List<Code> codeLines = function.codes();
        assertEquals(RET, codeLines.get(codeLines.size() - 1));
    }
}
