/*
 * Copyright (C) 2023 Johan Dykstrom
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

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import se.dykstrom.jcc.common.ast.Program;
import se.dykstrom.jcc.common.compiler.SyntaxParser;
import se.dykstrom.jcc.common.error.CompilationErrorListener;
import se.dykstrom.jcc.common.error.JccException;
import se.dykstrom.jcc.common.error.SyntaxException;
import se.dykstrom.jcc.common.utils.ParseUtils;

import java.io.IOException;
import java.io.InputStream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;
import static se.dykstrom.jcc.common.utils.VerboseLogger.log;

/**
 * A syntax parser for the Assembunny language.
 */
public class AssembunnySyntaxParser implements SyntaxParser {

    private final CompilationErrorListener errorListener;

    public AssembunnySyntaxParser(final CompilationErrorListener errorListener) {
        this.errorListener = requireNonNull(errorListener);
    }

    @Override
    public Program parse(final InputStream inputStream) throws SyntaxException {
        CharStream charStream;
        try {
            charStream = CharStreams.fromStream(inputStream, UTF_8);
        } catch (IOException e) {
            throw new JccException("Cannot read source file");
        }
        AssembunnyLexer lexer = new AssembunnyLexer(charStream);
        lexer.addErrorListener(errorListener);

        AssembunnyParser parser = new AssembunnyParser(new CommonTokenStream(lexer));
        parser.addErrorListener(errorListener);

        AssembunnyParser.ProgramContext ctx = parser.program();
        ParseUtils.checkParsingComplete(parser);

        // If we discovered syntax errors, we stop here
        if (parser.getNumberOfSyntaxErrors() > 0) {
            throw new SyntaxException("Syntax error");
        }

        log("  Building AST");
        AssembunnySyntaxVisitor visitor = new AssembunnySyntaxVisitor();
        return (Program) visitor.visitProgram(ctx);
    }
}
