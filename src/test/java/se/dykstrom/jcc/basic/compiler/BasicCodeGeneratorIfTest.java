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

import org.junit.Test;
import se.dykstrom.jcc.basic.ast.PrintStatement;
import se.dykstrom.jcc.common.assembly.AsmProgram;
import se.dykstrom.jcc.common.assembly.base.Line;
import se.dykstrom.jcc.common.assembly.instruction.Cmp;
import se.dykstrom.jcc.common.assembly.instruction.Je;
import se.dykstrom.jcc.common.assembly.instruction.Jmp;
import se.dykstrom.jcc.common.assembly.instruction.MoveImmToReg;
import se.dykstrom.jcc.common.ast.EqualExpression;
import se.dykstrom.jcc.common.ast.Expression;
import se.dykstrom.jcc.common.ast.IfStatement;
import se.dykstrom.jcc.common.ast.Statement;

import java.util.List;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;

public class BasicCodeGeneratorIfTest extends AbstractBasicCodeGeneratorTest {

    @Test
    public void shouldGenerateIfThen() {
        Expression expression = new EqualExpression(0, 0, IL_3, IL_4);
        Statement ps = new PrintStatement(0, 0, singletonList(IL_1));
        Statement is = IfStatement.builder(expression, ps).build();
        
        AsmProgram result = assembleProgram(singletonList(is));
        List<Line> lines = result.lines();
        
        // One for the exit code, two for the integer subexpressions, 
        // two for the boolean results, and two for the print statement
        assertEquals(7, countInstances(MoveImmToReg.class, lines));
        // One for comparing the integers, and one for the if statement
        assertEquals(2, countInstances(Cmp.class, lines));
        // One for comparing the integers, and one for the if statement
        assertEquals(2, countInstances(Je.class, lines));
        // One for comparing the integers
        assertEquals(1, countInstances(Jmp.class, lines));
    }

    @Test
    public void shouldGenerateIfThenElse() {
        Expression expression = new EqualExpression(0, 0, IL_3, IL_4);
        Statement ps1 = new PrintStatement(0, 0, singletonList(IL_1));
        Statement ps2 = new PrintStatement(0, 0, singletonList(IL_2));
        Statement is = IfStatement.builder(expression, ps1).elseStatements(ps2).build();

        AsmProgram result = assembleProgram(singletonList(is));
        List<Line> lines = result.lines();
        
        // One for the exit code, two for the integer subexpressions, 
        // two for the boolean results, and four for the print statements
        assertEquals(9, countInstances(MoveImmToReg.class, lines));
        // One for comparing the integers, and one for the if statement
        assertEquals(2, countInstances(Cmp.class, lines));
        // One for comparing the integers, and one for the if statement
        assertEquals(2, countInstances(Je.class, lines));
        // One for comparing the integers, and one for the if statement
        assertEquals(2, countInstances(Jmp.class, lines));
    }

    @Test
    public void shouldGenerateIfThenElseIfElse() {
        Expression secondExpr = new EqualExpression(0, 0, IL_3, IL_4);
        Statement ps1 = new PrintStatement(0, 0, singletonList(IL_1));
        Statement ps2 = new PrintStatement(0, 0, singletonList(IL_2));
        Statement secondIf = IfStatement.builder(secondExpr, ps1).elseStatements(ps2).build();
        Expression firstExpr = new EqualExpression(0, 0, IL_1, IL_2);
        Statement ps3 = new PrintStatement(0, 0, singletonList(IL_3));
        Statement firstIf = IfStatement.builder(firstExpr, ps3).elseStatements(secondIf).build();
        
        AsmProgram result = assembleProgram(singletonList(firstIf));
        List<Line> lines = result.lines();
        
        // One for the exit code, four for the integer subexpressions, 
        // four for the boolean results, and six for the print statements
        assertEquals(15, countInstances(MoveImmToReg.class, lines));
        // Two for comparing the integers, and two for the if statements
        assertEquals(4, countInstances(Cmp.class, lines));
        // Two for comparing the integers, and two for the if statements
        assertEquals(4, countInstances(Je.class, lines));
        // Two for comparing the integers, and two for the if statements
        assertEquals(4, countInstances(Jmp.class, lines));
    }
}
