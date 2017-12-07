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

package se.dykstrom.jcc.basic.compiler;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import se.dykstrom.jcc.basic.ast.PrintStatement;
import se.dykstrom.jcc.common.assembly.AsmProgram;
import se.dykstrom.jcc.common.assembly.base.Code;
import se.dykstrom.jcc.common.assembly.instruction.Cmp;
import se.dykstrom.jcc.common.assembly.instruction.Je;
import se.dykstrom.jcc.common.assembly.instruction.Jmp;
import se.dykstrom.jcc.common.assembly.instruction.MoveImmToReg;
import se.dykstrom.jcc.common.ast.EqualExpression;
import se.dykstrom.jcc.common.ast.Expression;
import se.dykstrom.jcc.common.ast.Statement;
import se.dykstrom.jcc.common.ast.WhileStatement;

public class BasicCodeGeneratorWhileTest extends AbstractBasicCodeGeneratorTest {

    @Test
    public void shouldGenerateEmptyWhile() {
        Expression expression = new EqualExpression(0, 0, IL_3, IL_4);
        Statement ws = new WhileStatement(0, 0, expression, emptyList());
        
        AsmProgram result = assembleProgram(singletonList(ws));
        List<Code> codes = result.codes();
        
        // One for the exit code, two for the integer expressions, two for the boolean results
        assertEquals(5, countInstances(MoveImmToReg.class, codes));
        // One for comparing the integers, and one for the while statement
        assertEquals(2, countInstances(Cmp.class, codes));
        // One for comparing the integers, and one for the while statement
        assertEquals(2, countInstances(Je.class, codes));
        // One for comparing the integers, and one for the while statement
        assertEquals(2, countInstances(Jmp.class, codes));
    }

    @Test
    public void shouldGenerateSimpleWhile() {
        Expression expression = new EqualExpression(0, 0, IL_3, IL_4);
        Statement ps = new PrintStatement(0, 0, singletonList(IL_1));
        Statement ws = new WhileStatement(0, 0, expression, singletonList(ps));
        
        AsmProgram result = assembleProgram(singletonList(ws));
        List<Code> codes = result.codes();
        
        // One for the exit code, two for the integer expressions, 
        // two for the boolean results, and two for the print statement
        assertEquals(7, countInstances(MoveImmToReg.class, codes));
        // One for comparing the integers, and one for the while statement
        assertEquals(2, countInstances(Cmp.class, codes));
        // One for comparing the integers, and one for the while statement
        assertEquals(2, countInstances(Je.class, codes));
        // One for comparing the integers, and one for the while statement
        assertEquals(2, countInstances(Jmp.class, codes));
    }

    @Test
    public void shouldGenerateNestedWhile() {
        Statement ps = new PrintStatement(0, 0, singletonList(BL_TRUE));
        Statement innerWhile = new WhileStatement(0, 0, IL_4, singletonList(ps));
        Statement outerWhile = new WhileStatement(0, 0, IL_2, singletonList(innerWhile));
        
        AsmProgram result = assembleProgram(singletonList(outerWhile));
        List<Code> codes = result.codes();
        
        // One for the exit code, two for the integer expressions, and two for the print statement
        assertEquals(5, countInstances(MoveImmToReg.class, codes));
        // Two for the while statements
        assertEquals(2, countInstances(Cmp.class, codes));
        // Two for the while statements
        assertEquals(2, countInstances(Je.class, codes));
        // Two for the while statements
        assertEquals(2, countInstances(Jmp.class, codes));
    }
}
