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

import org.antlr.v4.runtime.*;
import se.dykstrom.jcc.basic.ast.PrintStatement;
import se.dykstrom.jcc.basic.compiler.BasicParser.ProgramContext;
import se.dykstrom.jcc.common.ast.*;
import se.dykstrom.jcc.common.types.*;
import se.dykstrom.jcc.antlr4.Antlr4Utils;

import java.util.List;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;

public abstract class AbstractBasicSyntaxVisitorTest {

    protected static final Identifier IDENT_FLOAT_F = new Identifier("f#", F64.INSTANCE);
    protected static final Identifier IDENT_FLOAT_G = new Identifier("g#", F64.INSTANCE);
    protected static final Identifier IDENT_FLOAT_FOO = new Identifier("foo", F64.INSTANCE);
    protected static final Identifier IDENT_INT_A = new Identifier("a%", I64.INSTANCE);
    protected static final Identifier IDENT_INT_B = new Identifier("b%", I64.INSTANCE);
    protected static final Identifier IDENT_INT_BAR = new Identifier("bar%", I64.INSTANCE);
    protected static final Identifier IDENT_STR_S = new Identifier("s$", Str.INSTANCE);
    protected static final Identifier IDENT_STR_COMMAND = new Identifier("command$", Str.INSTANCE);

    protected static final IdentifierExpression NAME_A = new IdentifierNameExpression(0, 0, IDENT_INT_A);
    protected static final IdentifierExpression NAME_B = new IdentifierNameExpression(0, 0, IDENT_INT_B);
    protected static final IdentifierExpression NAME_F = new IdentifierNameExpression(0, 0, IDENT_FLOAT_F);
    protected static final IdentifierExpression NAME_G = new IdentifierNameExpression(0, 0, IDENT_FLOAT_G);
    protected static final IdentifierExpression NAME_S = new IdentifierNameExpression(0, 0, IDENT_STR_S);

    protected static final Expression IDE_A = new IdentifierDerefExpression(0, 0, IDENT_INT_A);
    protected static final Expression IDE_B = new IdentifierDerefExpression(0, 0, IDENT_INT_B);
    protected static final Expression IDE_F = new IdentifierDerefExpression(0, 0, IDENT_FLOAT_F);
    protected static final Expression IDE_S = new IdentifierDerefExpression(0, 0, IDENT_STR_S);

    protected static final IntegerLiteral IL_0 = new IntegerLiteral(0, 0, "0");
    protected static final IntegerLiteral IL_1 = new IntegerLiteral(0, 0, "1");
    protected static final IntegerLiteral IL_2 = new IntegerLiteral(0, 0, "2");
    protected static final IntegerLiteral IL_3 = new IntegerLiteral(0, 0, "3");
    protected static final IntegerLiteral IL_4 = new IntegerLiteral(0, 0, "4");
    protected static final IntegerLiteral IL_5 = new IntegerLiteral(0, 0, "5");
    protected static final IntegerLiteral IL_10 = new IntegerLiteral(0, 0, "10");
    protected static final IntegerLiteral IL_255 = new IntegerLiteral(0, 0, "255");
    protected static final IntegerLiteral IL_M1 = new IntegerLiteral(0, 0, "-1");
    protected static final IntegerLiteral IL_M3 = new IntegerLiteral(0, 0, "-3");

    protected static final StringLiteral SL_A = new StringLiteral(0, 0, "A");
    protected static final StringLiteral SL_B = new StringLiteral(0, 0, "B");
    protected static final StringLiteral SL_C = new StringLiteral(0, 0, "C");

    protected static final FloatLiteral FL_1_2 = new FloatLiteral(0, 0, "1.2");
    protected static final FloatLiteral FL_0_3 = new FloatLiteral(0, 0, "0.3");
    protected static final FloatLiteral FL_7_5_EXP = new FloatLiteral(0, 0, "7.5e+10");
    protected static final FloatLiteral FL_M_3_14 = new FloatLiteral(0, 0, "-3.14");

    /**
     * Tests the generic case of parsing code for printing one expression,
     * asserting that the parsed expression and the given expression are equal.
     * 
     * @param text The expression in text form.
     * @param expectedExpression The expression in AST form.
     */
    protected void testPrintOneExpression(String text, Expression expectedExpression) {
        Statement ps = new PrintStatement(0, 0, singletonList(expectedExpression));
        List<Statement> expectedStatements = singletonList(ps);
        parseAndAssert("print " + text, expectedStatements);
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
     * Parses the given program text, and asserts that the parsed text and the given statement are equal.
     *
     * @param text The code in text form.
     * @param expectedStatement The code in AST form.
     */
    protected void parseAndAssert(String text, Statement expectedStatement) {
        parseAndAssert(text, List.of(expectedStatement));
    }

    /**
     * Parses the given program text, and returns the AST for the parsed program.
     */
    protected Program parse(String text) {
        BasicLexer lexer = new BasicLexer(CharStreams.fromString(text));
        lexer.addErrorListener(ERROR_LISTENER);

        BasicParser parser = new BasicParser(new CommonTokenStream(lexer));
        parser.addErrorListener(ERROR_LISTENER);

        ProgramContext ctx = parser.program();
        Antlr4Utils.checkParsingComplete(parser);

        BasicSyntaxVisitor visitor = new BasicSyntaxVisitor(new BasicTypeManager());
        return (Program) visitor.visitProgram(ctx);
    }

    private static final BaseErrorListener ERROR_LISTENER = new BaseErrorListener() {
        @Override
        public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
            throw new IllegalStateException("Syntax error at " + line + ":" + charPositionInLine + ": " + msg, e);
        }
    };
}
