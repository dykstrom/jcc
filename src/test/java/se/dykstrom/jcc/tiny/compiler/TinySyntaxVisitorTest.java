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

package se.dykstrom.jcc.tiny.compiler;

import org.antlr.v4.runtime.*;
import org.junit.Test;
import se.dykstrom.jcc.common.ast.*;
import se.dykstrom.jcc.common.symbols.Identifier;
import se.dykstrom.jcc.common.types.I64;
import se.dykstrom.jcc.common.utils.ParseUtils;
import se.dykstrom.jcc.tiny.ast.ReadStatement;
import se.dykstrom.jcc.tiny.ast.WriteStatement;
import se.dykstrom.jcc.tiny.compiler.TinyParser.ProgramContext;

import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;

public class TinySyntaxVisitorTest {

    private static final Identifier IDENT_A = new Identifier("a", I64.INSTANCE);
    private static final Identifier IDENT_B = new Identifier("b", I64.INSTANCE);
    private static final Identifier IDENT_C = new Identifier("c", I64.INSTANCE);
    private static final Identifier IDENT_N = new Identifier("n", I64.INSTANCE);

    private static final IdentifierDerefExpression IDE_A = new IdentifierDerefExpression(0, 0, IDENT_A);
    private static final IdentifierDerefExpression IDE_B = new IdentifierDerefExpression(0, 0, IDENT_B);
    private static final IdentifierDerefExpression IDE_C = new IdentifierDerefExpression(0, 0, IDENT_C);
    private static final IdentifierDerefExpression IDE_N = new IdentifierDerefExpression(0, 0, IDENT_N);

    private static final IntegerLiteral IL_M3 = new IntegerLiteral(0, 0, "-3");
    private static final IntegerLiteral IL_0 = new IntegerLiteral(0, 0, "0");
    private static final IntegerLiteral IL_1 = new IntegerLiteral(0, 0, "1");
    private static final IntegerLiteral IL_17 = new IntegerLiteral(0, 0, "17");

    @Test
    public void testWrite() {
        WriteStatement ws = new WriteStatement(0, 0, singletonList(IL_17));

        Program program = parse("BEGIN WRITE 17 END");

        List<Statement> statements = program.getStatements();
        assertEquals(1, statements.size());
        assertEquals(ws, statements.get(0));
    }

    @Test
    public void testReadWrite() {
        ReadStatement rs = new ReadStatement(0, 0, singletonList(IDENT_N));
        WriteStatement ws = new WriteStatement(0, 0, singletonList(IDE_N));

        Program program = parse("BEGIN READ n WRITE n END");

        List<Statement> statements = program.getStatements();
        assertEquals(2, statements.size());
        assertEquals(rs, statements.get(0));
        assertEquals(ws, statements.get(1));
    }

    @Test
    public void testAssignment() {
        AssignStatement as = new AssignStatement(0, 0, IDENT_A, IL_0);

        Program program = parse("BEGIN a := 0 END");

        List<Statement> statements = program.getStatements();
        assertEquals(1, statements.size());
        assertEquals(as, statements.get(0));
    }

    @Test
    public void testReadAssignWrite() {
        ReadStatement rs = new ReadStatement(0, 0, singletonList(IDENT_A));
        AddExpression ae = new AddExpression(0, 0, IDE_A, IL_1);
        AssignStatement as = new AssignStatement(0, 0, IDENT_B, ae);
        WriteStatement ws = new WriteStatement(0, 0, singletonList(IDE_B));

        Program program = parse("BEGIN READ a b := a + 1 WRITE b END");

        List<Statement> statements = program.getStatements();
        assertEquals(3, statements.size());
        assertEquals(rs, statements.get(0));
        assertEquals(as, statements.get(1));
        assertEquals(ws, statements.get(2));
    }

    @Test
    public void testMultipleArgs() {
        ReadStatement rs = new ReadStatement(0, 0, asList(IDENT_A, IDENT_B));
        AssignStatement as = new AssignStatement(0, 0, IDENT_C, new AddExpression(0, 0, IDE_A, IDE_B));
        WriteStatement ws = new WriteStatement(0, 0, asList(IDE_A, IDE_B, IDE_C));

        Program program = parse("BEGIN "
                + "READ a, b "
                + "c := a + b "
                + "WRITE a, b, c "
                + "END");

        List<Statement> statements = program.getStatements();
        assertEquals(3, statements.size());
        assertEquals(rs, statements.get(0));
        assertEquals(as, statements.get(1));
        assertEquals(ws, statements.get(2));
    }

    @Test
    public void testMultipleAssignments() {
        ReadStatement rs = new ReadStatement(0, 0, singletonList(IDENT_A));
        AssignStatement as1 = new AssignStatement(0, 0, IDENT_B, new AddExpression(0, 0, IDE_A, IL_1));
        AssignStatement as2 = new AssignStatement(0, 0, IDENT_C, new SubExpression(0, 0, IDE_B, IL_1));
        WriteStatement ws = new WriteStatement(0, 0, asList(IDE_A, IDE_B, IDE_C));

        Program program = parse("BEGIN "
                + "READ a "
                + "b := a + 1 "
                + "c := b - 1 "
                + "WRITE a, b, c "
                + "END");

        List<Statement> statements = program.getStatements();
        assertEquals(4, statements.size());
        assertEquals(rs, statements.get(0));
        assertEquals(as1, statements.get(1));
        assertEquals(as2, statements.get(2));
        assertEquals(ws, statements.get(3));
    }

    @Test
    public void testNegativeNumber() {
        AssignStatement as = new AssignStatement(0, 0, IDENT_A, IL_M3);
        WriteStatement ws = new WriteStatement(0, 0, singletonList(IDE_A));
        List<Statement> expectedStatements = asList(as, ws);

        Program program = parse("BEGIN a := -3 WRITE a END");

        List<Statement> actualStatements = program.getStatements();
        assertEquals(2, actualStatements.size());
        assertEquals(expectedStatements, actualStatements);
    }

    private Program parse(String text) {
        TinyLexer lexer = new TinyLexer(CharStreams.fromString(text));
        lexer.addErrorListener(ERROR_LISTENER);

        TinyParser parser = new TinyParser(new CommonTokenStream(lexer));
        parser.addErrorListener(ERROR_LISTENER);

        ProgramContext ctx = parser.program();
        ParseUtils.checkParsingComplete(parser);

        TinySyntaxVisitor visitor = new TinySyntaxVisitor();
        return (Program) visitor.visitProgram(ctx);
    }

    private static final BaseErrorListener ERROR_LISTENER = new BaseErrorListener() {
        @Override
        public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
            throw new IllegalStateException("Syntax error at " + line + ":" + charPositionInLine + ": " + msg, e);
        }
    };
}
