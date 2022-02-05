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
import se.dykstrom.jcc.assembunny.ast.AssembunnyRegister;
import se.dykstrom.jcc.assembunny.ast.IncStatement;
import se.dykstrom.jcc.assembunny.ast.JnzStatement;
import se.dykstrom.jcc.assembunny.ast.RegisterExpression;
import se.dykstrom.jcc.assembunny.compiler.AssembunnyParser.ProgramContext;
import se.dykstrom.jcc.common.ast.LabelledStatement;
import se.dykstrom.jcc.common.ast.Program;
import se.dykstrom.jcc.common.error.SemanticsErrorListener;
import se.dykstrom.jcc.common.utils.ParseUtils;

import static org.junit.Assert.assertEquals;

public class AssembunnySemanticsParserTest {

    private static final RegisterExpression RE_A = new RegisterExpression(0, 0, AssembunnyRegister.A);

    @Test
    public void shouldParseInc() {
        parse("inc a");
    }

    @Test
    public void shouldParseCorrectJnz() {
        // Given
        IncStatement is = new IncStatement(0, 0, AssembunnyRegister.A);
        JnzStatement js = new JnzStatement(0, 0, RE_A, "0");
        
        // When
        Program program = parse("inc a jnz a -1");
        
        // Then
        assertEquals(2, program.getStatements().size());
        assertEquals(new LabelledStatement("0", is), program.getStatements().get(0));
        assertEquals(new LabelledStatement("1", js), program.getStatements().get(1));
    }

    @Test
    public void shouldParseInvalidJnz() {
        // Given
        IncStatement is = new IncStatement(0, 0, AssembunnyRegister.A);
        JnzStatement js = new JnzStatement(0, 0, RE_A, AssembunnyUtils.END_JUMP_TARGET);
        
        // When
        Program program = parse("inc a jnz a 5");
        
        // Then
        assertEquals(2, program.getStatements().size());
        assertEquals(new LabelledStatement("0", is), program.getStatements().get(0));
        assertEquals(new LabelledStatement("1", js), program.getStatements().get(1));
    }

    private Program parse(String text) {
        AssembunnyLexer lexer = new AssembunnyLexer(CharStreams.fromString(text));
        lexer.addErrorListener(SYNTAX_ERROR_LISTENER);

        AssembunnyParser syntaxParser = new AssembunnyParser(new CommonTokenStream(lexer));
        syntaxParser.addErrorListener(SYNTAX_ERROR_LISTENER);

        ProgramContext ctx = syntaxParser.program();
        ParseUtils.checkParsingComplete(syntaxParser);

        AssembunnySyntaxVisitor visitor = new AssembunnySyntaxVisitor();
        Program program = (Program) visitor.visitProgram(ctx);

        AssembunnySemanticsParser semanticsParser = new AssembunnySemanticsParser();
        semanticsParser.addErrorListener(SEMANTICS_ERROR_LISTENER);
        return semanticsParser.program(program);
    }

    private static final SemanticsErrorListener SEMANTICS_ERROR_LISTENER = (line, column, msg, exception) -> {
        throw new IllegalStateException("Semantics error at " + line + ":" + column + ": " + msg, exception);
    };

    private static final BaseErrorListener SYNTAX_ERROR_LISTENER = new BaseErrorListener() {
        @Override
        public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
            throw new IllegalStateException("Syntax error at " + line + ":" + charPositionInLine + ": " + msg, e);
        }
    };
}
