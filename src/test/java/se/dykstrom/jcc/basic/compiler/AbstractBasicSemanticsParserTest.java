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
import se.dykstrom.jcc.basic.compiler.BasicParser.ProgramContext;
import se.dykstrom.jcc.common.ast.Expression;
import se.dykstrom.jcc.common.ast.FloatLiteral;
import se.dykstrom.jcc.common.ast.IntegerLiteral;
import se.dykstrom.jcc.common.ast.Program;
import se.dykstrom.jcc.common.error.SemanticsErrorListener;
import se.dykstrom.jcc.common.functions.Function;
import se.dykstrom.jcc.common.symbols.Identifier;
import se.dykstrom.jcc.common.types.*;
import se.dykstrom.jcc.common.utils.ParseUtils;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

abstract class AbstractBasicSemanticsParserTest {

    static final Expression FL_3_14 = new FloatLiteral(0, 0, "3.14");
    static final Expression FL_2_0 = new FloatLiteral(0, 0, "2.0");
    static final Expression IL_1 = new IntegerLiteral(0, 0, "1");

    static final Identifier IDENT_BOOL_B = new Identifier("b", Bool.INSTANCE);
    static final Identifier IDENT_I64_A = new Identifier("a", I64.INSTANCE);
    static final Identifier IDENT_F64_F = new Identifier("f", F64.INSTANCE);
    static final Identifier IDENT_FUN_COMMAND = new Identifier("command$", Fun.from(emptyList(), Str.INSTANCE));
    static final Identifier IDENT_FUN_SUM = new Identifier("sum", Fun.from(asList(I64.INSTANCE, I64.INSTANCE, I64.INSTANCE), I64.INSTANCE));

    private final BasicSemanticsParser semanticsParser = new BasicSemanticsParser();

    /**
     * Defines a function in the current scope.
     */
    void defineFunction(Identifier identifier, Function function) {
        semanticsParser.getSymbols().addFunction(identifier, function);
    }
    
    void parseAndExpectException(String text, String message) {
        try {
            parse(text);
            fail("\nExpected: '" + message + "'\nActual:   ''");
        } catch (Exception e) {
            assertTrue("\nExpected: '" + message + "'\nActual:   '" + e.getMessage() + "'",
                    e.getMessage().contains(message));
        }
    }

    Program parse(String text) {
        BasicLexer lexer = new BasicLexer(CharStreams.fromString(text));
        lexer.addErrorListener(SYNTAX_ERROR_LISTENER);

        BasicParser syntaxParser = new BasicParser(new CommonTokenStream(lexer));
        syntaxParser.addErrorListener(SYNTAX_ERROR_LISTENER);

        ProgramContext ctx = syntaxParser.program();
        ParseUtils.checkParsingComplete(syntaxParser);

        BasicSyntaxVisitor visitor = new BasicSyntaxVisitor();
        Program program = (Program) visitor.visitProgram(ctx);

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
