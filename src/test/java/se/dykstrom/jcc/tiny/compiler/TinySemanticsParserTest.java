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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static se.dykstrom.jcc.common.utils.FormatUtils.EOL;

import org.antlr.v4.runtime.*;
import org.junit.Test;

import se.dykstrom.jcc.common.ast.Program;
import se.dykstrom.jcc.common.error.InvalidException;
import se.dykstrom.jcc.common.error.UndefinedException;
import se.dykstrom.jcc.common.symbols.SymbolTable;
import se.dykstrom.jcc.common.utils.ParseUtils;
import se.dykstrom.jcc.tiny.compiler.TinyParser.ProgramContext;

public class TinySemanticsParserTest {

    private static final String NAME_A = "a";
    private static final String NAME_B = "b";
    private static final String NAME_C = "c";
    private static final String NAME_N = "n";
    private static final String NAME_UNDEFINED = "undefined";

    private final TinySemanticsParser testee = new TinySemanticsParser();

    @Test
    public void testWrite() {
        parse("BEGIN WRITE 17 END");

        SymbolTable symbols = testee.getSymbols();
        assertEquals(0, symbols.size());
    }

    @Test
    public void testReadWrite() {
        parse("BEGIN" + EOL + "READ n" + EOL + "WRITE n" + EOL + "END");

        SymbolTable symbols = testee.getSymbols();
        assertEquals(1, symbols.size());
        assertTrue(symbols.contains(NAME_N));
    }

    @Test
    public void testAssignment() {
        parse("BEGIN" + EOL + "a := 0" + EOL + "END");

        SymbolTable symbols = testee.getSymbols();
        assertEquals(1, symbols.size());
        assertTrue(symbols.contains(NAME_A));
    }

    @Test
    public void testReadAssignWrite() {
        parse("BEGIN" + EOL + "READ a" + EOL + "b := a + 1" + EOL + "WRITE b" + EOL + "END");

        SymbolTable symbols = testee.getSymbols();
        assertEquals(2, symbols.size());
        assertTrue(symbols.contains(NAME_A, NAME_B));
    }

    @Test
    public void testMultipleArgs() {
        parse("BEGIN" + EOL + "READ a, b" + EOL + "c := a + b" + EOL + "WRITE a, b, c" + EOL + "END");

        SymbolTable symbols = testee.getSymbols();
        assertEquals(3, symbols.size());
        assertTrue(symbols.contains(NAME_A, NAME_B, NAME_C));
    }

    @Test
    public void testMultipleAssignments() {
        parse("BEGIN" + EOL
                + "READ a" + EOL
                + "b := a + 1" + EOL
                + "c := b - 1" + EOL
                + "WRITE a, b, c" + EOL
                + "END");

        SymbolTable symbols = testee.getSymbols();
        assertEquals(3, symbols.size());
        assertTrue(symbols.contains(NAME_A, NAME_B, NAME_C));
    }

    @Test
    public void testMaxI64() {
        parse("BEGIN WRITE 9223372036854775807 END");

        SymbolTable symbols = testee.getSymbols();
        assertEquals(0, symbols.size());
    }

    /**
     * Invalid integer -> overflow.
     */
    @Test(expected = InvalidException.class)
    public void testOverflowI64() {
        String value = "9223372036854775808";
        try {
            parse("BEGIN WRITE " + value + " END");
        } catch (IllegalStateException ise) {
            InvalidException ie = (InvalidException) ise.getCause();
            assertEquals(value, ie.getValue());
            throw ie;
        }
    }

    /**
     * Undefined identifier in write statement.
     */
    @Test(expected = UndefinedException.class)
    public void testUndefinedInWrite() {
        try {
            parse("BEGIN WRITE undefined END");
        } catch (IllegalStateException ise) {
            UndefinedException ue = (UndefinedException) ise.getCause();
            assertEquals(NAME_UNDEFINED, ue.getName());
            throw ue;
        }
    }

    /**
     * Undefined identifier in assign statement.
     */
    @Test(expected = UndefinedException.class)
    public void testUndefinedInAssign() {
        try {
            parse("BEGIN" + EOL + "a := undefined" + EOL + "END");
        } catch (IllegalStateException ise) {
            UndefinedException ue = (UndefinedException) ise.getCause();
            assertEquals(NAME_UNDEFINED, ue.getName());
            throw ue;
        }
    }

    /**
     * Undefined identifier in complex expression.
     */
    @Test(expected = UndefinedException.class)
    public void testUndefinedInExpression() {
        try {
            parse("BEGIN" + EOL
                    + "WRITE 1 + undefined - 2" + EOL
                    + "END");
        } catch (IllegalStateException ise) {
            UndefinedException ue = (UndefinedException) ise.getCause();
            assertEquals(NAME_UNDEFINED, ue.getName());
            throw ue;
        }
    }

    /**
     * Undefined identifier in expression list.
     */
    @Test(expected = UndefinedException.class)
    public void testUndefinedInList() {
        try {
            parse("BEGIN" + EOL
                    + "WRITE 1, undefined, 3" + EOL
                    + "END");
        } catch (IllegalStateException ise) {
            UndefinedException ue = (UndefinedException) ise.getCause();
            assertEquals(NAME_UNDEFINED, ue.getName());
            throw ue;
        }
    }

    private void parse(String text) {
        TinyLexer lexer = new TinyLexer(CharStreams.fromString(text));
        lexer.addErrorListener(SYNTAX_ERROR_LISTENER);

        TinyParser parser = new TinyParser(new CommonTokenStream(lexer));
        parser.addErrorListener(SYNTAX_ERROR_LISTENER);

        ProgramContext ctx = parser.program();
        ParseUtils.checkParsingComplete(parser);

        TinySyntaxVisitor visitor = new TinySyntaxVisitor();
        Program program = (Program) visitor.visitProgram(ctx);

        testee.addErrorListener((line, column, msg, e) -> {
            throw new IllegalStateException("Semantics error at " + line + ":" + column + ": " + msg, e);
        });
        testee.program(program);
    }

    private static final BaseErrorListener SYNTAX_ERROR_LISTENER = new BaseErrorListener() {
        @Override
        public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
            throw new IllegalStateException("Syntax error at " + line + ":" + charPositionInLine + ": " + msg, e);
        }
    };
}
