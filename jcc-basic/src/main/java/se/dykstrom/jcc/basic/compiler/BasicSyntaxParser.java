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

package se.dykstrom.jcc.basic.compiler;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CommonTokenStream;
import se.dykstrom.jcc.antlr4.Antlr4Utils;
import se.dykstrom.jcc.common.ast.AstProgram;
import se.dykstrom.jcc.common.compiler.SyntaxParser;
import se.dykstrom.jcc.common.error.CompilationErrorListener;
import se.dykstrom.jcc.common.error.SyntaxException;

import java.io.InputStream;

import static java.util.Objects.requireNonNull;
import static se.dykstrom.jcc.common.utils.VerboseLogger.log;

/**
 * A syntax parser for the BASIC language.
 */
public class BasicSyntaxParser implements SyntaxParser {

    private final BasicTypeManager typeManager;
    private final BaseErrorListener errorListener;

    public BasicSyntaxParser(final BasicTypeManager typeManager, final CompilationErrorListener errorListener) {
        this.typeManager = requireNonNull(typeManager);
        this.errorListener = Antlr4Utils.asBaseErrorListener(requireNonNull(errorListener));
    }

    @Override
    public AstProgram parse(final InputStream inputStream) throws SyntaxException {
        BasicLexer lexer = new BasicLexer(Antlr4Utils.toCharStream(inputStream));
        lexer.addErrorListener(errorListener);

        BasicParser parser = new BasicParser(new CommonTokenStream(lexer));
        parser.addErrorListener(errorListener);

        BasicParser.ProgramContext ctx = parser.program();
        Antlr4Utils.checkParsingComplete(parser);

        // If we discovered syntax errors, we stop here
        if (parser.getNumberOfSyntaxErrors() > 0) {
            throw new SyntaxException("Syntax error");
        }

        log("  Building AST");
        BasicSyntaxVisitor visitor = new BasicSyntaxVisitor(typeManager);
        return (AstProgram) visitor.visitProgram(ctx);
    }
}
