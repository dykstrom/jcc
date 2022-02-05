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

import org.antlr.v4.runtime.*;
import org.junit.Test;
import se.dykstrom.jcc.assembunny.ast.*;
import se.dykstrom.jcc.assembunny.compiler.AssembunnyParser.ProgramContext;
import se.dykstrom.jcc.common.ast.IntegerLiteral;
import se.dykstrom.jcc.common.ast.LabelledStatement;
import se.dykstrom.jcc.common.ast.Program;
import se.dykstrom.jcc.common.ast.Statement;
import se.dykstrom.jcc.common.utils.ParseUtils;

import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;

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
    public void shouldParseEmptyProgram() {
        List<Statement> expectedStatements = emptyList();
        parseAndAssert("", expectedStatements);
    }

    @Test
    public void shouldParseInc() {
        Statement is = new IncStatement(0, 0, AssembunnyRegister.A);
        List<Statement> expectedStatements = singletonList(new LabelledStatement("0", is));
        parseAndAssert("inc a", expectedStatements);
    }

    @Test
    public void shouldParseDec() {
        Statement ds = new DecStatement(0, 0, AssembunnyRegister.B);
        List<Statement> expectedStatements = singletonList(new LabelledStatement("0", ds));
        parseAndAssert("dec b", expectedStatements);
    }

    @Test
    public void shouldParseCpyFromReg() {
        Statement cs = new CpyStatement(0, 0, RE_A, AssembunnyRegister.B);
        List<Statement> expectedStatements = singletonList(new LabelledStatement("0", cs));
        parseAndAssert("cpy a b", expectedStatements);
    }

    @Test
    public void shouldParseCpyFromInt() {
        Statement cs = new CpyStatement(0, 0, IL_1, AssembunnyRegister.C);
        List<Statement> expectedStatements = singletonList(new LabelledStatement("0", cs));
        parseAndAssert("cpy 1 c", expectedStatements);
    }

    @Test
    public void shouldParseJnzOnReg() {
        Statement js = new JnzStatement(0, 0, RE_A, "3");
        List<Statement> expectedStatements = singletonList(new LabelledStatement("0", js));
        parseAndAssert("jnz a 3", expectedStatements);
    }

    @Test
    public void shouldParseJnzOnInt() {
        Statement js = new JnzStatement(0, 0, IL_1, "-2");
        List<Statement> expectedStatements = singletonList(new LabelledStatement("0", js));
        parseAndAssert("jnz 1 -2", expectedStatements);
    }

    @Test
    public void shouldParseOutn() {
        Statement os = new OutnStatement(0, 0, RE_B);
        List<Statement> expectedStatements = singletonList(new LabelledStatement("0", os));
        parseAndAssert("outn b", expectedStatements);
    }

    @Test
    public void shouldParseMultipleStatements() {
        Statement is = new IncStatement(0, 0, AssembunnyRegister.A);
        Statement ds1 = new DecStatement(0, 0, AssembunnyRegister.A);
        Statement cs = new CpyStatement(0, 0, IL_1, AssembunnyRegister.B);
        Statement ds2 = new DecStatement(0, 0, AssembunnyRegister.B);
        // A relative jump of -1 from 4 is an absolute jump to 3
        Statement js = new JnzStatement(0, 0, RE_B, "3");
        Statement os = new OutnStatement(0, 0, RE_A);

        List<Statement> expectedStatements = List.of(
                new LabelledStatement("0", is),
                new LabelledStatement("1", ds1),
                new LabelledStatement("2", cs),
                new LabelledStatement("3", ds2),
                new LabelledStatement("4", js),
                new LabelledStatement("5", os)
        );

        parseAndAssert("""
                inc a
                dec a
                cpy 1 b
                dec b
                jnz b -1
                outn a
                """, expectedStatements);
    }

    /**
     * Parses the given program text, and asserts that the parsed text and the given statements are equal.
     * 
     * @param text The code in text form.
     * @param expectedStatements The code in AST form.
     */
    private void parseAndAssert(String text, List<Statement> expectedStatements) {
        Program program = parse(text);
        List<Statement> actualStatements = program.getStatements();
        assertEquals(expectedStatements, actualStatements);
    }

    /**
     * Parses the given program text, and returns the AST for the parsed program.
     */
    private Program parse(String text) {
        AssembunnyLexer lexer = new AssembunnyLexer(CharStreams.fromString(text));
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
