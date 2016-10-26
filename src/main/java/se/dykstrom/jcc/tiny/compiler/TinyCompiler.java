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

import org.antlr.v4.runtime.CommonTokenStream;
import se.dykstrom.jcc.common.assembly.AsmProgram;
import se.dykstrom.jcc.common.ast.Program;
import se.dykstrom.jcc.common.compiler.AbstractCompiler;

import static se.dykstrom.jcc.common.utils.VerboseLogger.log;

/**
 * The compiler class for the Tiny language. This class puts all the parts of the Tiny compiler together,
 * to parse and generate code for a Tiny source file.
 *
 * @author Johan Dykstrom
 */
public class TinyCompiler extends AbstractCompiler {

    @Override
    public AsmProgram compile() {
        TinySyntaxListener listener = new TinySyntaxListener();

        TinyLexer lexer = new TinyLexer(getInputStream());
        lexer.addErrorListener(getErrorListener());

        TinyParser parser = new TinyParser(new CommonTokenStream(lexer));
        parser.addErrorListener(getErrorListener());
        parser.addParseListener(listener);

        log("  Parsing syntax");
        parser.program();
        Program program = listener.getProgram();

        log("  Parsing semantics");
        TinySemanticsParser semanticsParser = new TinySemanticsParser();
        semanticsParser.addErrorListener(getErrorListener());
        semanticsParser.program(program);

        log("  Generating assembly code");
        TinyCodeGenerator codeGenerator = new TinyCodeGenerator();
        program.setSourceFilename(getSourceFilename());
        return codeGenerator.program(program);
    }
}
