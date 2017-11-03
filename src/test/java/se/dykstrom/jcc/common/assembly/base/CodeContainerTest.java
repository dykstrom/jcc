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

package se.dykstrom.jcc.common.assembly.base;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static se.dykstrom.jcc.common.assembly.base.Register.RAX;
import static se.dykstrom.jcc.common.assembly.base.Register.RDX;

import java.util.List;

import org.junit.Test;

import se.dykstrom.jcc.common.assembly.instruction.AddImmToReg;
import se.dykstrom.jcc.common.assembly.instruction.MoveRegToReg;
import se.dykstrom.jcc.common.assembly.instruction.Ret;

public class CodeContainerTest {

    private static final Code ADD = new AddImmToReg("0", RDX);
    private static final Code MOVE = new MoveRegToReg(RDX, RAX);
    private static final Code RET = new Ret();
    
    private final CodeContainer codeContainer = new CodeContainer();
    
    @Test
    public void shouldAddAll() {
        codeContainer.add(ADD).addAll(asList(MOVE, RET));
        List<Code> expectedCodes = asList(ADD, MOVE, RET);
        assertEquals(expectedCodes, codeContainer.codes());
    }
    
    @Test
    public void shouldGetLastInstruction() {
        codeContainer.add(ADD).add(MOVE).add(RET);
        assertEquals(RET, codeContainer.lastInstruction());
    }
}
