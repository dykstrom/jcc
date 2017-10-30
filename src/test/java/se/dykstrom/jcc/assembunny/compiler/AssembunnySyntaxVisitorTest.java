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

package se.dykstrom.jcc.assembunny.compiler;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;

import java.util.List;

import org.antlr.v4.runtime.*;
import org.junit.Test;

import se.dykstrom.jcc.assembunny.ast.*;
import se.dykstrom.jcc.assembunny.compiler.AssembunnyParser.ProgramContext;
import se.dykstrom.jcc.common.ast.IntegerLiteral;
import se.dykstrom.jcc.common.ast.Program;
import se.dykstrom.jcc.common.ast.Statement;
import se.dykstrom.jcc.common.utils.ParseUtils;

/**
 * Tests class {@code AssembunnySyntaxVisitor}.
 * 
 * @author Johan Dykstrom
 * @see AssembunnySyntaxVisitor
 */
public class AssembunnySyntaxVisitorTest {

    private static final IntegerLiteral IL_1 = new IntegerLiteral(0, 0, "1");
    
    private static final RegisterExpression RE_A = new RegisterExpression(0, 0, AssembunnyRegister.A);
    private static final RegisterExpression RE_B = new RegisterExpression(0, 0, AssembunnyRegister.B);

    @Test
    public void shouldParseEmptyProgram() throws Exception {
        List<Statement> expectedStatements = emptyList();
        parseAndAssert("", expectedStatements);
    }

    @Test
    public void shouldParseInc() throws Exception {
        Statement is = new IncStatement(0, 0, AssembunnyRegister.A, "0");
        List<Statement> expectedStatements = singletonList(is);
        parseAndAssert("inc a", expectedStatements);
    }

    @Test
    public void shouldParseDec() throws Exception {
        Statement ds = new DecStatement(0, 0, AssembunnyRegister.B, "0");
        List<Statement> expectedStatements = singletonList(ds);
        parseAndAssert("dec b", expectedStatements);
    }

    @Test
    public void shouldParseCpyFromReg() throws Exception {
        Statement cs = new CpyStatement(0, 0, RE_A, AssembunnyRegister.B, "0");
        List<Statement> expectedStatements = singletonList(cs);
        parseAndAssert("cpy a b", expectedStatements);
    }

    @Test
    public void shouldParseCpyFromInt() throws Exception {
        Statement cs = new CpyStatement(0, 0, IL_1, AssembunnyRegister.C, "0");
        List<Statement> expectedStatements = singletonList(cs);
        parseAndAssert("cpy 1 c", expectedStatements);
    }

    @Test
    public void shouldParseJnzOnReg() throws Exception {
        Statement js = new JnzStatement(0, 0, RE_A, "3", "0");
        List<Statement> expectedStatements = singletonList(js);
        parseAndAssert("jnz a 3", expectedStatements);
    }

    @Test
    public void shouldParseJnzOnInt() throws Exception {
        Statement js = new JnzStatement(0, 0, IL_1, "-2", "0");
        List<Statement> expectedStatements = singletonList(js);
        parseAndAssert("jnz 1 -2", expectedStatements);
    }

    @Test
    public void shouldParseOutn() throws Exception {
        Statement os = new OutnStatement(0, 0, RE_B, "0");
        List<Statement> expectedStatements = singletonList(os);
        parseAndAssert("outn b", expectedStatements);
    }

    @Test
    public void shouldParseMultipleStatements() throws Exception {
        Statement is = new IncStatement(0, 0, AssembunnyRegister.A, "0");
        Statement ds1 = new DecStatement(0, 0, AssembunnyRegister.A, "1");
        Statement cs = new CpyStatement(0, 0, IL_1, AssembunnyRegister.B, "2");
        Statement ds2 = new DecStatement(0, 0, AssembunnyRegister.B, "3");
        // A relative jump of -1 from 4 is an absolute jump to 3
        Statement js = new JnzStatement(0, 0, RE_B, "3", "4");
        Statement os = new OutnStatement(0, 0, RE_A, "5");
        List<Statement> expectedStatements = asList(is, ds1, cs, ds2, js, os);
        parseAndAssert(
                "inc a " +
                "dec a " +
                "cpy 1 b " +
                "dec b " +
                "jnz b -1" + 
                "outn a", 
                expectedStatements);
    }

    /**
     * Parses the given program text, and asserts that the parsed text and the given statements are equal.
     * 
     * @param text The code in text form.
     * @param expectedStatements The code in AST form.
     */
    protected void parseAndAssert(String text, List<Statement> expectedStatements) {
        Program program = parse(text);
        List<Statement> actualStatements = program.getStatements();
        assertEquals(expectedStatements, actualStatements);
    }

    /**
     * Parses the given program text, and returns the AST for the parsed program.
     */
    protected Program parse(String text) {
        AssembunnyLexer lexer = new AssembunnyLexer(new ANTLRInputStream(text));
        lexer.addErrorListener(ERROR_LISTENER);

        AssembunnyParser parser = new AssembunnyParser(new CommonTokenStream(lexer));
        parser.addErrorListener(ERROR_LISTENER);

        ProgramContext ctx = parser.program();
        ParseUtils.checkParsingComplete(parser);

        AssembunnySyntaxVisitor visitor = new AssembunnySyntaxVisitor();
        return (Program) visitor.visitProgram(ctx);
    }

    private static final BaseErrorListener ERROR_LISTENER = new BaseErrorListener() {
        @Override
        public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
            throw new IllegalStateException("Syntax error at " + line + ":" + charPositionInLine + ": " + msg, e);
        }
    };
}
