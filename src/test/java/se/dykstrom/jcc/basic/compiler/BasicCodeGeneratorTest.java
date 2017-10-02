/*
 * Copyright (C) 2016 Johan Dykstrom
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

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import se.dykstrom.jcc.basic.ast.EndStatement;
import se.dykstrom.jcc.basic.ast.PrintStatement;
import se.dykstrom.jcc.common.assembly.AsmProgram;
import se.dykstrom.jcc.common.assembly.base.Code;
import se.dykstrom.jcc.common.assembly.instruction.*;
import se.dykstrom.jcc.common.ast.*;

public class BasicCodeGeneratorTest extends AbstractBasicCodeGeneratorTest {

    @Test
    public void testEmptyProgram() {
        AsmProgram result = assembleProgram(emptyList());

        Map<String, Set<String>> dependencies = result.getDependencies();
        assertEquals(1, dependencies.size());
        String library = dependencies.keySet().iterator().next();
        assertTrue(dependencies.get(library).contains("exit"));

        assertCodes(result.codes(), 1, 1, 1, 1);
    }

    @Test
    public void testOneEnd() {
        Statement es = new EndStatement(0, 0, "10");

        AsmProgram result = assembleProgram(singletonList(es));

        Map<String, Set<String>> dependencies = result.getDependencies();
        assertEquals(1, dependencies.size());
        String library = dependencies.keySet().iterator().next();
        assertTrue(dependencies.get(library).contains("exit"));

        List<Code> codes = result.codes();
        assertCodes(codes, 1, 1, 2, 1);
    }

    @Test
    public void testOneRem() {
        Statement rs = new CommentStatement(0, 0, "10");

        AsmProgram result = assembleProgram(singletonList(rs));

        Map<String, Set<String>> dependencies = result.getDependencies();
        assertEquals(1, dependencies.size());
        String library = dependencies.keySet().iterator().next();
        assertTrue(dependencies.get(library).contains("exit"));

        List<Code> codes = result.codes();
        assertCodes(codes, 1, 1, 2, 1);
    }

    @Test
    public void testOneGoto() {
        Statement gs = new GotoStatement(0, 0, "10", "10");

        AsmProgram result = assembleProgram(singletonList(gs));

        Map<String, Set<String>> dependencies = result.getDependencies();
        assertEquals(1, dependencies.size());
        String library = dependencies.keySet().iterator().next();
        assertTrue(dependencies.get(library).contains("exit"));

        List<Code> codes = result.codes();
        assertCodes(codes, 1, 1, 2, 1);
        assertEquals(1, countInstances(codes, Jmp.class));
    }

    @Test
    public void testTwoGotos() {
        Statement gs10 = new GotoStatement(0, 0, "20", "10");
        Statement gs20 = new GotoStatement(0, 0, "10", "20");

        AsmProgram result = assembleProgram(asList(gs10, gs20));

        Map<String, Set<String>> dependencies = result.getDependencies();
        assertEquals(1, dependencies.size());
        String library = dependencies.keySet().iterator().next();
        assertTrue(dependencies.get(library).contains("exit"));

        List<Code> codes = result.codes();
        assertCodes(codes, 1, 1, 3, 1);
        assertEquals(2, countInstances(codes, Jmp.class));
    }

    @Test
    public void testOnePrint() {
        Statement ps = new PrintStatement(0, 0, singletonList(SL_FOO), "100");

        AsmProgram result = assembleProgram(singletonList(ps));

        Map<String, Set<String>> dependencies = result.getDependencies();
        assertEquals(1, dependencies.size());
        String library = dependencies.keySet().iterator().next();
        assertTrue(dependencies.get(library).contains("exit"));
        assertTrue(dependencies.get(library).contains("printf"));

        List<Code> codes = result.codes();
        assertCodes(codes, 1, 2, 2, 2);
    }

    @Test
    public void testOnePrintTwoStrings() {
        Statement ps = new PrintStatement(0, 0, asList(new StringLiteral(0, 0, "Hello, "), new StringLiteral(0, 0, "world!")), "100");

        AsmProgram result = assembleProgram(singletonList(ps));

        Map<String, Set<String>> dependencies = result.getDependencies();
        assertEquals(1, dependencies.size());
        String library = dependencies.keySet().iterator().next();
        assertTrue(dependencies.get(library).contains("exit"));
        assertTrue(dependencies.get(library).contains("printf"));

        List<Code> codes = result.codes();
        assertCodes(codes, 1, 2, 2, 2);
    }

    @Test
    public void testTwoPrintsOneLine() {
        Statement ps100a = new PrintStatement(0, 0, singletonList(SL_ONE), "100");
        Statement ps100b = new PrintStatement(0, 0, singletonList(SL_TWO));

        AsmProgram result = assembleProgram(asList(ps100a, ps100b));

        Map<String, Set<String>> dependencies = result.getDependencies();
        assertEquals(1, dependencies.size());
        String library = dependencies.keySet().iterator().next();
        assertTrue(dependencies.get(library).contains("exit"));
        assertTrue(dependencies.get(library).contains("printf"));

        List<Code> codes = result.codes();
        assertCodes(codes, 1, 2, 2, 3);
    }

    @Test
    public void testTwoPrintsTwoLines() {
        Statement ps100 = new PrintStatement(1, 0, singletonList(SL_ONE), "100");
        Statement ps110 = new PrintStatement(2, 0, singletonList(SL_TWO), "110");

        AsmProgram result = assembleProgram(asList(ps100, ps110));

        Map<String, Set<String>> dependencies = result.getDependencies();
        assertEquals(1, dependencies.size());
        String library = dependencies.keySet().iterator().next();
        assertTrue(dependencies.get(library).contains("exit"));
        assertTrue(dependencies.get(library).contains("printf"));

        List<Code> codes = result.codes();
        assertCodes(codes, 1, 2, 3, 3);
    }

    @Test
    public void testOnePrintFiveStrings() {
        StringLiteral s1 = new StringLiteral(1, 10, "<1>");
        StringLiteral s2 = new StringLiteral(1, 20, "<2>");
        StringLiteral s3 = new StringLiteral(1, 30, "<3>");
        StringLiteral s4 = new StringLiteral(1, 40, "<4>");
        StringLiteral s5 = new StringLiteral(1, 50, "<5>");
        Statement ps = new PrintStatement(1, 0, asList(s1, s2, s3, s4, s5), "100");

        AsmProgram result = assembleProgram(singletonList(ps));

        Map<String, Set<String>> dependencies = result.getDependencies();
        assertEquals(1, dependencies.size());
        String library = dependencies.keySet().iterator().next();
        assertTrue(dependencies.get(library).contains("exit"));
        assertTrue(dependencies.get(library).contains("printf"));

        List<Code> codes = result.codes();
        assertCodes(codes, 1, 2, 2, 2);
        assertEquals(7, countInstances(codes, Push.class));
    }

    @Test
    public void testOnePrintAdd() {
        Expression expression = new AddExpression(0, 0, IL_1, IL_2);
        Statement statement = new PrintStatement(0, 0, singletonList(expression), "10");

        AsmProgram result = assembleProgram(singletonList(statement));

        List<Code> codes = result.codes();
        assertEquals(4, countInstances(codes, MoveImmToReg.class));
        assertEquals(1, countInstances(codes, AddRegToReg.class));
    }

    @Test
    public void testOnePrintSub() {
        Expression expression = new SubExpression(0, 0, IL_1, IL_2);
        Statement statement = new PrintStatement(0, 0, singletonList(expression), "10");

        AsmProgram result = assembleProgram(singletonList(statement));

        List<Code> codes = result.codes();
        assertEquals(4, countInstances(codes, MoveImmToReg.class));
        assertEquals(1, countInstances(codes, SubRegFromReg.class));
    }

    @Test
    public void testOnePrintMul() {
        Expression expression = new MulExpression(0, 0, IL_1, IL_2);
        Statement statement = new PrintStatement(0, 0, singletonList(expression), "10");

        AsmProgram result = assembleProgram(singletonList(statement));

        List<Code> codes = result.codes();
        assertEquals(4, countInstances(codes, MoveImmToReg.class));
        assertEquals(1, countInstances(codes, IMulRegWithReg.class));
    }

    @Test
    public void testOnePrintDiv() {
        Expression expression = new DivExpression(0, 0, IL_1, IL_2);
        Statement statement = new PrintStatement(0, 0, singletonList(expression), "10");

        AsmProgram result = assembleProgram(singletonList(statement));

        List<Code> codes = result.codes();
        assertEquals(4, countInstances(codes, MoveImmToReg.class));
        assertEquals(1, countInstances(codes, IDivWithReg.class));
        assertEquals(1, countInstances(codes, Cqo.class));
    }

    @Test
    public void testOnePrintIDiv() {
        Expression expression = new IDivExpression(0, 0, IL_1, IL_2);
        Statement statement = new PrintStatement(0, 0, singletonList(expression), "10");

        AsmProgram result = assembleProgram(singletonList(statement));
        
        List<Code> codes = result.codes();
        assertEquals(4, countInstances(codes, MoveImmToReg.class));
        assertEquals(1, countInstances(codes, IDivWithReg.class));
        assertEquals(1, countInstances(codes, Cqo.class));
    }

    @Test
    public void testOnePrintMod() {
        Expression expression = new ModExpression(0, 0, IL_1, IL_2);
        Statement statement = new PrintStatement(0, 0, singletonList(expression), "10");

        AsmProgram result = assembleProgram(singletonList(statement));
        
        List<Code> codes = result.codes();
        assertEquals(4, countInstances(codes, MoveImmToReg.class));
        assertEquals(1, countInstances(codes, IDivWithReg.class));
        assertEquals(1, countInstances(codes, Cqo.class));
    }

    @Test
    public void testOnePrintMulAddMul() {
        // 4 * 2 + 3 * 1
        Expression ms1 = new MulExpression(0, 0, IL_4, IL_2);
        Expression ms2 = new MulExpression(0, 0, IL_3, IL_1);
        Expression as = new AddExpression(0, 0, ms1, ms2);
        Statement statement = new PrintStatement(0, 0, singletonList(as), "10");

        AsmProgram result = assembleProgram(singletonList(statement));

        List<Code> codes = result.codes();
        assertEquals(6, countInstances(codes, MoveImmToReg.class));
        assertEquals(3, countInstances(codes, MoveRegToReg.class));
        assertEquals(2, countInstances(codes, IMulRegWithReg.class));
        assertEquals(1, countInstances(codes, AddRegToReg.class));
    }

    @Test
    public void testGotoPrintAndEnd() {
        Statement gs100 = new GotoStatement(1, 0, "110", "100");
        Statement ps110 = new PrintStatement(2, 0, singletonList(SL_FOO), "110");
        Statement es120 = new EndStatement(3, 0, "120");

        AsmProgram result = assembleProgram(asList(gs100, ps110, es120));

        Map<String, Set<String>> dependencies = result.getDependencies();
        assertEquals(1, dependencies.size());
        String library = dependencies.keySet().iterator().next();
        assertTrue(dependencies.get(library).contains("exit"));
        assertTrue(dependencies.get(library).contains("printf"));

        List<Code> codes = result.codes();
        assertCodes(codes, 1, 2, 4, 2);
        assertEquals(3, countInstances(codes, Push.class));
        assertEquals(1, countInstances(codes, Jmp.class));
    }

    @Test
    public void testOneAssignmentIntegerLiteral() {
        Statement as = new AssignStatement(0, 0, IDENT_I64_A, IL_4);

        AsmProgram result = assembleProgram(singletonList(as));

        List<Code> codes = result.codes();
        // Exit code, and evaluating the integer literal
        assertEquals(2, countInstances(codes, MoveImmToReg.class));
        // Storing the evaluated integer literal
        assertEquals(1, countInstances(codes, MoveRegToMem.class));
    }

    @Test
    public void testOneAssignmentStringLiteral() {
        Statement as = new AssignStatement(0, 0, IDENT_STR_B, SL_FOO);

        AsmProgram result = assembleProgram(singletonList(as));

        List<Code> codes = result.codes();
        // Exit code, and evaluating the string literal
        assertEquals(2, countInstances(codes, MoveImmToReg.class));
        // Storing the evaluated string literal
        assertEquals(1, countInstances(codes, MoveRegToMem.class));
    }

    @Test
    public void testOneAssignmentBooleanLiteral() {
        Statement as = new AssignStatement(0, 0, IDENT_BOOL_C, BL_TRUE);

        AsmProgram result = assembleProgram(singletonList(as));

        List<Code> codes = result.codes();
        // Exit code, and evaluating the boolean literal
        assertEquals(2, countInstances(codes, MoveImmToReg.class));
        // Find move that stores the literal value in register while evaluating
        assertEquals(1, codes.stream()
                .filter(code -> code instanceof MoveImmToReg)
                .map(code -> ((MoveImmToReg) code).getImmediate())
                .filter(immediate -> immediate.equals(BL_TRUE.getValue()))
                .count());
        // Storing the evaluated literal in memory
        assertEquals(1, countInstances(codes, MoveRegToMem.class));
    }

    @Test
    public void testOneAssignmentAddExpression() {
        Expression ae = new AddExpression(0, 0, IL_1, IL_2);
        Statement as = new AssignStatement(0, 0, IDENT_I64_A, ae);

        AsmProgram result = assembleProgram(singletonList(as));

        List<Code> codes = result.codes();
        assertEquals(1, countInstances(codes, AddRegToReg.class));
        assertEquals(1, codes
                .stream()
                .filter(code -> code instanceof MoveRegToMem)
                .map(code -> ((MoveRegToMem) code).getMemory())
                .filter(name -> name.equals(IDENT_I64_A.getMappedName()))
                .count());
    }

    @Test
    public void testOneAssignmentIdentifierExpression() {
        Statement statement = new AssignStatement(0, 0, IDENT_I64_A, IDE_I64_H);

        AsmProgram result = assembleProgram(singletonList(statement));

        List<Code> codes = result.codes();
        assertEquals(1, countInstances(codes, MoveImmToReg.class));
        assertEquals(1, codes
                .stream()
                .filter(code -> code instanceof MoveMemToReg)
                .map(code -> ((MoveMemToReg) code).getMemory())
                .filter(name -> name.equals(IDENT_I64_H.getMappedName()))
                .count());
        assertEquals(1, codes
                .stream()
                .filter(code -> code instanceof MoveRegToMem)
                .map(code -> ((MoveRegToMem) code).getMemory())
                .filter(name -> name.equals(IDENT_I64_A.getMappedName()))
                .count());
    }

    @Test
    public void testPrintTwoIdentifierExpressions() {
        Statement statement = new PrintStatement(0, 0, asList(IDE_I64_A, IDE_I64_H));

        AsmProgram result = assembleProgram(singletonList(statement));

        List<Code> codes = result.codes();
        assertEquals(2, countInstances(codes, MoveImmToReg.class));
        assertEquals(1, codes
                .stream()
                .filter(code -> code instanceof MoveMemToReg)
                .map(code -> ((MoveMemToReg) code).getMemory())
                .filter(name -> name.equals(IDENT_I64_H.getMappedName()))
                .count());
        assertEquals(1, codes
                .stream()
                .filter(code -> code instanceof MoveMemToReg)
                .map(code -> ((MoveMemToReg) code).getMemory())
                .filter(name -> name.equals(IDENT_I64_A.getMappedName()))
                .count());
    }

    @Test
    public void shouldGenerateEqualExpressionIntegers() {
        assertRelationalExpressionIntegers(new EqualExpression(0, 0, IL_3, IL_4), Je.class);
    }

    @Test
    public void shouldGenerateNotEqualExpressionIntegers() {
        assertRelationalExpressionIntegers(new NotEqualExpression(0, 0, IL_3, IL_4), Jne.class);
    }

    @Test
    public void shouldGenerateGreaterExpressionIntegers() {
        assertRelationalExpressionIntegers(new GreaterExpression(0, 0, IL_3, IL_4), Jg.class);
    }

    @Test
    public void shouldGenerateGreaterOrEqualExpressionIntegers() {
        assertRelationalExpressionIntegers(new GreaterOrEqualExpression(0, 0, IL_3, IL_4), Jge.class);
    }

    @Test
    public void shouldGenerateLessExpressionIntegers() {
        assertRelationalExpressionIntegers(new LessExpression(0, 0, IL_3, IL_4), Jl.class);
    }

    @Test
    public void shouldGenerateLessOrEqualExpressionIntegers() {
        assertRelationalExpressionIntegers(new LessOrEqualExpression(0, 0, IL_3, IL_4), Jle.class);
    }

    @Test
    public void shouldGenerateEqualExpressionStrings() {
        assertRelationalExpressionStrings(new EqualExpression(0, 0, SL_ONE, SL_TWO), Je.class);
    }

    @Test
    public void shouldGenerateNotEqualExpressionStrings() {
        assertRelationalExpressionStrings(new NotEqualExpression(0, 0, SL_ONE, SL_TWO), Jne.class);
    }

    @Test
    public void shouldGenerateGreaterExpressionStrings() {
        assertRelationalExpressionStrings(new GreaterExpression(0, 0, SL_ONE, SL_TWO), Jg.class);
    }

    @Test
    public void shouldGenerateGreaterOrEqualExpressionStrings() {
        assertRelationalExpressionStrings(new GreaterOrEqualExpression(0, 0, SL_ONE, SL_TWO), Jge.class);
    }

    @Test
    public void shouldGenerateLessExpressionStrings() {
        assertRelationalExpressionStrings(new LessExpression(0, 0, SL_ONE, SL_TWO), Jl.class);
    }

    @Test
    public void shouldGenerateLessOrEqualExpressionStrings() {
        assertRelationalExpressionStrings(new LessOrEqualExpression(0, 0, SL_ONE, SL_TWO), Jle.class);
    }

    private void assertRelationalExpressionIntegers(Expression expression, Class<? extends Jump> conditionalJump) {
        AsmProgram result = assembleProgram(singletonList(new AssignStatement(0, 0, IDENT_BOOL_C, expression)));
        List<Code> codes = result.codes();

        // One for the exit code, two for the integer subexpressions, and two for the boolean results
        assertEquals(5, countInstances(codes, MoveImmToReg.class));
        // One for comparing the integer subexpressions
        assertEquals(1, countInstances(codes, Cmp.class));
        // One for the conditional jump
        assertEquals(1, countInstances(codes, conditionalJump));
        // One for the unconditional jump
        assertEquals(1, countInstances(codes, Jmp.class));
        // Storing the boolean result in memory
        assertEquals(1, countInstances(codes, MoveRegToMem.class));
    }
    
    private void assertRelationalExpressionStrings(Expression expression, Class<? extends Jump> conditionalJump) {
        AsmProgram result = assembleProgram(singletonList(new AssignStatement(0, 0, IDENT_BOOL_C, expression)));
        List<Code> codes = result.codes();
        
        // Libraries: msvcrt
        // Imports: strcmp, exit
        // Labels: main, @@, after_cmp
        // Calls: strcmp, exit
        assertCodes(codes, 1, 2, 3, 2);
        
        // One for the exit code, two for the integer subexpressions, and two for the boolean results
        assertEquals(5, countInstances(codes, MoveImmToReg.class));
        // One for comparing the integer subexpressions
        assertEquals(1, countInstances(codes, Cmp.class));
        // One for the conditional jump
        assertEquals(1, countInstances(codes, conditionalJump));
        // One for the unconditional jump
        assertEquals(1, countInstances(codes, Jmp.class));
        // Storing the boolean result in memory
        assertEquals(1, countInstances(codes, MoveRegToMem.class));
    }

    @Test
    public void testOneAssignmentWithOneComplexEqualExpression() {
        Expression ae = new AddExpression(0, 0, IDE_I64_A, IL_1);
        Expression se = new SubExpression(0, 0, IL_2, IDE_I64_H);
        Expression expression = new EqualExpression(0, 0, ae, se);
        
        Statement as = new AssignStatement(0, 0, IDENT_BOOL_C, expression);
        AsmProgram result = assembleProgram(singletonList(as));
        List<Code> codes = result.codes();

        // One for the exit code, two for the integer subexpressions, and two for the boolean results
        assertEquals(5, countInstances(codes, MoveImmToReg.class));
        // Two for the ident subexpressions
        assertEquals(2, countInstances(codes, MoveMemToReg.class));
        // One for comparing the integer subexpressions
        assertEquals(1, countInstances(codes, Cmp.class));
        // One for the conditional jump
        assertEquals(1, countInstances(codes, Je.class));
        // One for the unconditional jump
        assertEquals(1, countInstances(codes, Jmp.class));
        // Storing the boolean result in memory
        assertEquals(1, countInstances(codes, MoveRegToMem.class));
        // Find assignment to memory location
        assertEquals(1, codes
                .stream()
                .filter(code -> code instanceof MoveRegToMem)
                .map(code -> ((MoveRegToMem) code).getMemory())
                .filter(name -> name.equals(IDENT_BOOL_C.getMappedName()))
                .count());
    }

    @Test
    public void testOneAssignmentWithOneAnd() {
        Expression ee = new EqualExpression(0, 0, IL_3, IL_4);
        Expression expression = new AndExpression(0, 0, BL_FALSE, ee);
        
        Statement as = new AssignStatement(0, 0, IDENT_BOOL_C, expression);
        AsmProgram result = assembleProgram(singletonList(as));
        List<Code> codes = result.codes();

        // One for the exit code, one for the boolean subexpression, 
        // two for the integer subexpressions, and two for the boolean results
        assertEquals(6, countInstances(codes, MoveImmToReg.class));
        // One for comparing the integer subexpressions
        assertEquals(1, countInstances(codes, Cmp.class));
        // One for the conditional jump
        assertEquals(1, countInstances(codes, Je.class));
        // One for the unconditional jump
        assertEquals(1, countInstances(codes, Jmp.class));
        // One for the and:ing of booleans
        assertEquals(1, countInstances(codes, AndRegWithReg.class));
        // Storing the boolean result in memory
        assertEquals(1, countInstances(codes, MoveRegToMem.class));
    }

    @Test
    public void testOneAssignmentWithOneOr() {
        Expression expression = new OrExpression(0, 0, BL_FALSE, BL_TRUE);
        
        Statement as = new AssignStatement(0, 0, IDENT_BOOL_C, expression);
        AsmProgram result = assembleProgram(singletonList(as));
        List<Code> codes = result.codes();

        // One for the exit code, two for the boolean subexpressions
        assertEquals(3, countInstances(codes, MoveImmToReg.class));
        // One for the or:ing of booleans
        assertEquals(1, countInstances(codes, OrRegWithReg.class));
        // Storing the boolean result in memory
        assertEquals(1, countInstances(codes, MoveRegToMem.class));
    }

    @Test
    public void testComplexBooleanExpression() {
        Expression ee = new EqualExpression(0, 0, IL_3, IL_4);
        Expression ge = new GreaterExpression(0, 0, IL_2, IDE_I64_A);
        Expression ae1 = new AndExpression(0, 0, BL_FALSE, ee);
        Expression ae2 = new AndExpression(0, 0, ge, BL_TRUE);
        Expression oe = new OrExpression(0, 0, ae1, ae2);

        Statement as = new AssignStatement(0, 0, IDENT_BOOL_C, oe, "10");
        AsmProgram result = assembleProgram(singletonList(as));
        List<Code> codes = result.codes();

        // One for the exit code, two for the boolean literals,
        // three for the integer literals, and four for the boolean results
        assertEquals(10, countInstances(codes, MoveImmToReg.class));
        // Two for comparing two integer subexpressions
        assertEquals(2, countInstances(codes, Cmp.class));
        // One for the conditional jump
        assertEquals(1, countInstances(codes, Je.class));
        // One for the other conditional jump
        assertEquals(1, countInstances(codes, Jg.class));
        // Two for the unconditional jumps
        assertEquals(2, countInstances(codes, Jmp.class));
        // Two for the and:ing of booleans
        assertEquals(2, countInstances(codes, AndRegWithReg.class));
        // One for the or:ing of booleans
        assertEquals(1, countInstances(codes, OrRegWithReg.class));
        // Storing the boolean result in memory
        assertEquals(1, countInstances(codes, MoveRegToMem.class));
    }
}
