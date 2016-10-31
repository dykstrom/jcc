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

import org.junit.Test;
import se.dykstrom.jcc.basic.ast.EndStatement;
import se.dykstrom.jcc.basic.ast.GotoStatement;
import se.dykstrom.jcc.basic.ast.PrintStatement;
import se.dykstrom.jcc.basic.ast.RemStatement;
import se.dykstrom.jcc.common.assembly.AsmProgram;
import se.dykstrom.jcc.common.assembly.base.Code;
import se.dykstrom.jcc.common.assembly.base.Label;
import se.dykstrom.jcc.common.assembly.instruction.*;
import se.dykstrom.jcc.common.assembly.other.Import;
import se.dykstrom.jcc.common.assembly.other.Library;
import se.dykstrom.jcc.common.ast.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BasicCodeGeneratorTest {

    private static final String FILENAME = "file.bas";
    private static final IntegerLiteral IL_1 = new IntegerLiteral(0, 0, "1");
    private static final IntegerLiteral IL_2 = new IntegerLiteral(0, 0, "2");
    private static final IntegerLiteral IL_3 = new IntegerLiteral(0, 0, "3");
    private static final IntegerLiteral IL_4 = new IntegerLiteral(0, 0, "4");
    private static final StringLiteral SL_FOO = new StringLiteral(0, 0, "foo");
    private static final StringLiteral SL_ONE = new StringLiteral(0, 0, "One");
    private static final StringLiteral SL_TWO = new StringLiteral(0, 0, "Two");

    private final BasicCodeGenerator testee = new BasicCodeGenerator();

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
        Statement rs = new RemStatement(0, 0, "10");

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
        assertEquals(1, codes.stream().filter(code -> code instanceof Jmp).count());
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
        assertEquals(2, codes.stream().filter(code -> code instanceof Jmp).count());
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
        assertCodes(codes, 1, 1, 2, 2);
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
        assertCodes(codes, 1, 1, 2, 2);
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
        assertCodes(codes, 1, 1, 2, 3);
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
        assertCodes(codes, 1, 1, 3, 3);
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
        assertCodes(codes, 1, 1, 2, 2);
        assertEquals(7, codes.stream().filter(code -> code instanceof Push).count());
    }

    @Test
    public void testOnePrintAdd() {
        Expression expression = new AddExpression(0, 0, IL_1, IL_2);
        Statement statement = new PrintStatement(0, 0, singletonList(expression), "10");

        AsmProgram result = assembleProgram(singletonList(statement));

        List<Code> codes = result.codes();
        assertEquals(4, codes.stream().filter(code -> code instanceof MoveImmToReg).count());
        assertEquals(1, codes.stream().filter(code -> code instanceof AddRegToReg).count());
    }

    @Test
    public void testOnePrintSub() {
        Expression expression = new SubExpression(0, 0, IL_1, IL_2);
        Statement statement = new PrintStatement(0, 0, singletonList(expression), "10");

        AsmProgram result = assembleProgram(singletonList(statement));

        List<Code> codes = result.codes();
        assertEquals(4, codes.stream().filter(code -> code instanceof MoveImmToReg).count());
        assertEquals(1, codes.stream().filter(code -> code instanceof SubRegFromReg).count());
    }

    @Test
    public void testOnePrintMul() {
        Expression expression = new MulExpression(0, 0, IL_1, IL_2);
        Statement statement = new PrintStatement(0, 0, singletonList(expression), "10");

        AsmProgram result = assembleProgram(singletonList(statement));

        List<Code> codes = result.codes();
        assertEquals(4, codes.stream().filter(code -> code instanceof MoveImmToReg).count());
        assertEquals(1, codes.stream().filter(code -> code instanceof IMulRegWithReg).count());
    }

    @Test
    public void testOnePrintDiv() {
        Expression expression = new DivExpression(0, 0, IL_1, IL_2);
        Statement statement = new PrintStatement(0, 0, singletonList(expression), "10");

        AsmProgram result = assembleProgram(singletonList(statement));

        List<Code> codes = result.codes();
        assertEquals(4, codes.stream().filter(code -> code instanceof MoveImmToReg).count());
        assertEquals(1, codes.stream().filter(code -> code instanceof Cqo).count());
        assertEquals(1, codes.stream().filter(code -> code instanceof IDivWithReg).count());
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
        assertEquals(6, codes.stream().filter(code -> code instanceof MoveImmToReg).count());
        assertEquals(3, codes.stream().filter(code -> code instanceof MoveRegToReg).count());
        assertEquals(2, codes.stream().filter(code -> code instanceof IMulRegWithReg).count());
        assertEquals(1, codes.stream().filter(code -> code instanceof AddRegToReg).count());
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
        assertCodes(codes, 1, 1, 4, 2);
        assertEquals(3, codes.stream().filter(code -> code instanceof Push).count());
        assertEquals(1, codes.stream().filter(code -> code instanceof Jmp).count());
    }

    private AsmProgram assembleProgram(List<Statement> statements) {
        Program program = new Program(0, 0, statements);
        program.setSourceFilename(FILENAME);
        return testee.program(program);
    }

    private static void assertCodes(List<Code> codes, int libraries, int imports, int labels, int calls) {
        assertEquals(libraries, codes.stream().filter(code -> code instanceof Library).count());
        assertEquals(imports, codes.stream().filter(code -> code instanceof Import).count());
        assertEquals(labels, codes.stream().filter(code -> code instanceof Label).count());
        assertEquals(calls, codes.stream().filter(code -> code instanceof Call).count());
    }
}
