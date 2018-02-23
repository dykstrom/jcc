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

package se.dykstrom.jcc.basic.compiler;

import org.junit.Test;
import se.dykstrom.jcc.common.assembly.AsmProgram;
import se.dykstrom.jcc.common.assembly.base.Code;
import se.dykstrom.jcc.common.assembly.instruction.MoveImmToReg;
import se.dykstrom.jcc.common.assembly.instruction.MoveRegToMem;
import se.dykstrom.jcc.common.assembly.instruction.floating.AddFloatRegToFloatReg;
import se.dykstrom.jcc.common.assembly.instruction.floating.ConvertIntRegToFloatReg;
import se.dykstrom.jcc.common.assembly.instruction.floating.MoveFloatRegToMem;
import se.dykstrom.jcc.common.assembly.instruction.floating.MoveMemToFloatReg;
import se.dykstrom.jcc.common.ast.AddExpression;
import se.dykstrom.jcc.common.ast.AssignStatement;
import se.dykstrom.jcc.common.ast.Expression;
import se.dykstrom.jcc.common.ast.Statement;

import java.util.List;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;

public class BasicCodeGeneratorFloatTest extends AbstractBasicCodeGeneratorTest {

    @Test
    public void shouldAssignFloatLiteral() {
        Statement as = new AssignStatement(0, 0, IDENT_F64_F, FL_3_14);

        AsmProgram result = assembleProgram(singletonList(as));

        List<Code> codes = result.codes();
        // Exit code, and evaluating the float literal
        assertEquals(2, countInstances(MoveImmToReg.class, codes));
        assertEquals(1, countInstances(MoveRegToMem.class, codes));
        assertEquals(1, countInstances(MoveMemToFloatReg.class, codes));
        // Storing the evaluated float literal
        assertEquals(1, countInstances(MoveFloatRegToMem.class, codes));
    }

    @Test
    public void shouldAssignAddFloatFloatExpression() {
        Expression ae = new AddExpression(0, 0, FL_3_14, FL_17_E4);
        Statement as = new AssignStatement(0, 0, IDENT_F64_F, ae);

        AsmProgram result = assembleProgram(singletonList(as));

        List<Code> codes = result.codes();
        assertEquals(1, countInstances(AddFloatRegToFloatReg.class, codes));
        assertEquals(1, codes
                .stream()
                .filter(code -> code instanceof MoveFloatRegToMem)
                .map(code -> ((MoveFloatRegToMem) code).getDestination())
                .filter(name -> name.equals("[" + IDENT_F64_F.getMappedName() + "]"))
                .count());
    }

    @Test
    public void shouldAssignAddFloatIntegerExpression() {
        Expression ae = new AddExpression(0, 0, FL_3_14, IL_3);
        Statement as = new AssignStatement(0, 0, IDENT_F64_F, ae);

        AsmProgram result = assembleProgram(singletonList(as));
        System.out.println(result.toAsm());

        List<Code> codes = result.codes();
        assertEquals(1, countInstances(AddFloatRegToFloatReg.class, codes));
        assertEquals(1, countInstances(ConvertIntRegToFloatReg.class, codes));
        assertEquals(1, codes
                .stream()
                .filter(code -> code instanceof MoveFloatRegToMem)
                .map(code -> ((MoveFloatRegToMem) code).getDestination())
                .filter(name -> name.equals("[" + IDENT_F64_F.getMappedName() + "]"))
                .count());
    }

    @Test
    public void shouldAssignAddIntegerFloatExpression() {
        Expression ae = new AddExpression(0, 0, IL_3, FL_3_14);
        Statement as = new AssignStatement(0, 0, IDENT_F64_F, ae);

        AsmProgram result = assembleProgram(singletonList(as));
        System.out.println(result.toAsm());

        List<Code> codes = result.codes();
        assertEquals(1, countInstances(AddFloatRegToFloatReg.class, codes));
        assertEquals(1, countInstances(ConvertIntRegToFloatReg.class, codes));
        assertEquals(1, countInstances(MoveFloatRegToMem.class, codes));
        assertEquals(1, codes
                .stream()
                .filter(code -> code instanceof MoveFloatRegToMem)
                .map(code -> ((MoveFloatRegToMem) code).getDestination())
                .filter(name -> name.equals("[" + IDENT_F64_F.getMappedName() + "]"))
                .count());
    }
}
