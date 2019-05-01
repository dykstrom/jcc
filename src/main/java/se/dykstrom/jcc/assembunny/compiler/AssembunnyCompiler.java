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

import org.antlr.v4.runtime.CommonTokenStream;
import se.dykstrom.jcc.assembunny.compiler.AssembunnyParser.ProgramContext;
import se.dykstrom.jcc.common.assembly.AsmProgram;
import se.dykstrom.jcc.common.ast.Program;
import se.dykstrom.jcc.common.compiler.AbstractCompiler;
import se.dykstrom.jcc.common.compiler.AstOptimizer;
import se.dykstrom.jcc.common.compiler.DefaultAstOptimizer;
import se.dykstrom.jcc.common.utils.ParseUtils;

import static se.dykstrom.jcc.common.utils.VerboseLogger.log;

/**
 * The compiler class for the Assembunny language. This class puts all the parts of the compiler together,
 * to parse and generate code for a Assembunny source file.
 *
 * @author Johan Dykstrom
 */
public class AssembunnyCompiler extends AbstractCompiler {

    @Override
    public AsmProgram compile() {
        AssembunnyLexer lexer = new AssembunnyLexer(getInputStream());
        lexer.addErrorListener(getErrorListener());

        AssembunnyParser parser = new AssembunnyParser(new CommonTokenStream(lexer));
        parser.addErrorListener(getErrorListener());

        log("  Parsing syntax");
        ProgramContext ctx = parser.program();
        ParseUtils.checkParsingComplete(parser);

        // If we discovered syntax errors, we stop here
        if (parser.getNumberOfSyntaxErrors() > 0) {
            return null;
        }
        
        log("  Building AST");
        AssembunnySyntaxVisitor visitor = new AssembunnySyntaxVisitor();
        Program program = (Program) visitor.visitProgram(ctx);

        log("  Parsing semantics");
        AssembunnySemanticsParser semanticsParser = new AssembunnySemanticsParser();
        semanticsParser.addErrorListener(getErrorListener());
        program = semanticsParser.program(program);
        
        // If we discovered semantics errors, we stop here
        if (getErrorListener().hasErrors()) {
            return null;
        }

        log("  Optimizing");
        AstOptimizer optimizer = new DefaultAstOptimizer();
        program = optimizer.program(program);

        log("  Generating assembly code");
        AssembunnyCodeGenerator codeGenerator = new AssembunnyCodeGenerator();
        program.setSourceFilename(getSourceFilename());
        return codeGenerator.program(program);
    }
}
