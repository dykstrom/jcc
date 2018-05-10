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

package se.dykstrom.jcc.tiny.compiler;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import se.dykstrom.jcc.common.assembly.AsmProgram;
import se.dykstrom.jcc.common.assembly.base.Code;
import se.dykstrom.jcc.common.assembly.base.Label;
import se.dykstrom.jcc.common.assembly.instruction.*;
import se.dykstrom.jcc.common.assembly.other.Import;
import se.dykstrom.jcc.common.assembly.other.Library;
import se.dykstrom.jcc.common.ast.*;
import se.dykstrom.jcc.common.types.Identifier;
import se.dykstrom.jcc.common.types.I64;
import se.dykstrom.jcc.tiny.ast.ReadStatement;
import se.dykstrom.jcc.tiny.ast.WriteStatement;

public class TinyCodeGeneratorTest {

    private static final String FILENAME = "file.tiny";

    private static final Identifier IDENT_A = new Identifier("a", I64.INSTANCE);
    private static final Identifier IDENT_B = new Identifier("b", I64.INSTANCE);

    private final TinyCodeGenerator testee = new TinyCodeGenerator();

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
    public void testSingleReadSingleIdentifier() {
        Statement statement = new ReadStatement(0, 0, singletonList(IDENT_A));

        AsmProgram result = assembleProgram(singletonList(statement));

        Map<String, Set<String>> dependencies = result.getDependencies();
        assertEquals(1, dependencies.size());
        String library = dependencies.keySet().iterator().next();
        assertTrue(dependencies.get(library).contains("exit"));
        assertTrue(dependencies.get(library).contains("scanf"));

        assertCodes(result.codes(), 1, 1, 1, 2);
    }

    @Test
    public void testSingleReadMultipleIdentifiers() {
        Statement statement = new ReadStatement(0, 0, asList(IDENT_A, IDENT_B));

        AsmProgram result = assembleProgram(singletonList(statement));

        Map<String, Set<String>> dependencies = result.getDependencies();
        assertEquals(1, dependencies.size());
        String library = dependencies.keySet().iterator().next();
        assertTrue(dependencies.get(library).contains("exit"));
        assertTrue(dependencies.get(library).contains("scanf"));

        assertCodes(result.codes(), 1, 1, 1, 3);
    }

    @Test
    public void testMultipleReadsSingleIdentifier() {
        Statement statement1 = new ReadStatement(1, 0, singletonList(IDENT_A));
        Statement statement2 = new ReadStatement(2, 0, singletonList(IDENT_B));

        AsmProgram result = assembleProgram(asList(statement1, statement2));

        Map<String, Set<String>> dependencies = result.getDependencies();
        assertEquals(1, dependencies.size());
        String library = dependencies.keySet().iterator().next();
        assertTrue(dependencies.get(library).contains("exit"));
        assertTrue(dependencies.get(library).contains("scanf"));

        assertCodes(result.codes(), 1, 1, 1, 3);
    }

    @Test
    public void testSingleAssignmentLiteralExpression() {
        Statement statement = new AssignStatement(0, 0, IDENT_A, new IntegerLiteral(0, 0, "5"));

        AsmProgram result = assembleProgram(singletonList(statement));

        List<Code> codes = result.codes();
        assertEquals(2, codes.stream().filter(code -> code instanceof MoveImmToReg).count());
        assertEquals(1, codes.stream().filter(code -> code instanceof MoveRegToMem).count());
    }

    @Test
    public void testSingleAssignmentIdentifierExpression() {
        Statement statement = new AssignStatement(0, 0, IDENT_A, new IdentifierDerefExpression(0, 0, IDENT_B));

        AsmProgram result = assembleProgram(singletonList(statement));

        List<Code> codes = result.codes();
        assertEquals(1, codes.stream().filter(code -> code instanceof MoveImmToReg).count());
        assertEquals(1, codes
                .stream()
                .filter(code -> code instanceof MoveMemToReg)
                .map(code -> ((MoveMemToReg) code).getSource())
                .filter(name -> name.equals("[" + IDENT_B.getMappedName() + "]"))
                .count());
        assertEquals(1, codes
                .stream()
                .filter(code -> code instanceof MoveRegToMem)
                .map(code -> ((MoveRegToMem) code).getDestination())
                .filter(name -> name.equals("[" + IDENT_A.getMappedName() + "]"))
                .count());
    }

    @Test
    public void testSingleAssignmentAddExpression() {
        Expression expression = new AddExpression(0, 0, new IntegerLiteral(0, 0, "1"), new IntegerLiteral(0, 0, "2"));
        Statement statement = new AssignStatement(0, 0, IDENT_A, expression);

        AsmProgram result = assembleProgram(singletonList(statement));

        List<Code> codes = result.codes();
        assertEquals(1, codes.stream().filter(code -> code instanceof AddRegToReg).count());
        assertEquals(1, codes
                .stream()
                .filter(code -> code instanceof MoveRegToMem)
                .map(code -> ((MoveRegToMem) code).getDestination())
                .filter(name -> name.equals("[" + IDENT_A.getMappedName() + "]"))
                .count());
    }

    @Test
    public void testSingleAssignmentSubExpression() {
        Expression expression = new SubExpression(0, 0, new IntegerLiteral(0, 0, "17"), new IntegerLiteral(0, 0, "5"));
        Statement statement = new AssignStatement(0, 0, IDENT_A, expression);

        AsmProgram result = assembleProgram(singletonList(statement));

        List<Code> codes = result.codes();
        assertEquals(1, codes.stream().filter(code -> code instanceof SubRegFromReg).count());
        assertEquals(1, codes
                .stream()
                .filter(code -> code instanceof MoveRegToMem)
                .map(code -> ((MoveRegToMem) code).getDestination())
                .filter(name -> name.equals("[" + IDENT_A.getMappedName() + "]"))
                .count());
    }

    @Test
    public void testMultipleAssignmentsLiteralExpression() {
        Statement statement0 = new AssignStatement(0, 0, IDENT_A, new IntegerLiteral(0, 0, "5"));
        Statement statement1 = new AssignStatement(1, 0, IDENT_B, new IntegerLiteral(0, 0, "23"));

        AsmProgram result = assembleProgram(asList(statement0, statement1));

        List<Code> codes = result.codes();
        assertEquals(3, codes.stream().filter(code -> code instanceof MoveImmToReg).count());
        assertEquals(2, codes.stream().filter(code -> code instanceof MoveRegToMem).count());
    }

    @Test
    public void testSingleWriteSingleExpression() {
        Statement statement = new WriteStatement(0, 0, singletonList(new IntegerLiteral(0, 0, "9")));

        AsmProgram result = assembleProgram(singletonList(statement));

        List<Code> codes = result.codes();
        assertEquals(3, codes.stream().filter(code -> code instanceof MoveImmToReg).count());
    }

    @Test
    public void testSingleWriteAddExpression() {
        Expression expression = new AddExpression(0, 0, new IntegerLiteral(0, 0, "1"), new IntegerLiteral(0, 0, "2"));
        Statement statement = new WriteStatement(0, 0, singletonList(expression));

        AsmProgram result = assembleProgram(singletonList(statement));

        List<Code> codes = result.codes();
        assertEquals(4, codes.stream().filter(code -> code instanceof MoveImmToReg).count());
        assertEquals(1, codes.stream().filter(code -> code instanceof AddRegToReg).count());
    }

    @Test
    public void testSingleWriteWithExpressionList() {
        Expression expression0 = new AddExpression(0, 0, new IntegerLiteral(0, 0, "1"), new IntegerLiteral(0, 0, "2"));
        Expression expression1 = new AddExpression(0, 0, new IntegerLiteral(0, 0, "3"), new IntegerLiteral(0, 0, "4"));
        Statement statement = new WriteStatement(0, 0, asList(expression0, expression1));

        AsmProgram result = assembleProgram(singletonList(statement));

        List<Code> codes = result.codes();
        assertEquals(7, codes.stream().filter(code -> code instanceof MoveImmToReg).count());
        assertEquals(2, codes.stream().filter(code -> code instanceof AddRegToReg).count());
    }

    @Test
    public void testReadAssignWrite() {
        Statement readStatement = new ReadStatement(1, 0, singletonList(IDENT_A));
        Expression assignExpression = new AddExpression(2, 0, new IdentifierDerefExpression(2, 0, IDENT_A), new IntegerLiteral(2, 0, "1"));
        Statement assignStatement = new AssignStatement(2, 0, IDENT_B, assignExpression);
        Expression writeExpression = new IdentifierDerefExpression(3, 0, IDENT_B);
        Statement writeStatement = new WriteStatement(3, 0, singletonList(writeExpression));

        AsmProgram result = assembleProgram(asList(readStatement, assignStatement, writeStatement));

        List<Code> codes = result.codes();
        assertCodes(codes, 1, 1, 1, 3);
        assertEquals(1, codes.stream().filter(code -> code instanceof AddRegToReg).count());
        assertEquals(5, codes.stream().filter(code -> code instanceof MoveImmToReg).count());
        assertEquals(2, codes.stream().filter(code -> code instanceof MoveMemToReg).count());
        assertEquals(1, codes.stream().filter(code -> code instanceof MoveRegToMem).count());
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
