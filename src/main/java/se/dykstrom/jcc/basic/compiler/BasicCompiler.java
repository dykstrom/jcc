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

package se.dykstrom.jcc.basic.compiler;

import org.antlr.v4.runtime.CommonTokenStream;
import se.dykstrom.jcc.basic.compiler.BasicParser.ProgramContext;
import se.dykstrom.jcc.basic.functions.BasicBuiltInFunctions;
import se.dykstrom.jcc.common.assembly.AsmProgram;
import se.dykstrom.jcc.common.ast.Program;
import se.dykstrom.jcc.common.compiler.AbstractCompiler;
import se.dykstrom.jcc.common.symbols.SymbolTable;
import se.dykstrom.jcc.common.utils.ParseUtils;

import static se.dykstrom.jcc.common.utils.VerboseLogger.log;

/**
 * The compiler class for the Basic language. This class puts all the parts of the Basic compiler together,
 * to parse and generate code for a Basic source file.
 *
 * @author Johan Dykstrom
 */
public class BasicCompiler extends AbstractCompiler {

    @Override
    public AsmProgram compile() {
        BasicLexer lexer = new BasicLexer(getInputStream());
        lexer.addErrorListener(getErrorListener());

        BasicParser parser = new BasicParser(new CommonTokenStream(lexer));
        parser.addErrorListener(getErrorListener());

        log("  Parsing syntax");
        ProgramContext ctx = parser.program();
        ParseUtils.checkParsingComplete(parser);

        // If we discovered syntax errors, we stop here
        if (parser.getNumberOfSyntaxErrors() > 0) {
            return null;
        }
        
        log("  Building AST");
        BasicSyntaxVisitor visitor = new BasicSyntaxVisitor();
        Program program = (Program) visitor.visitProgram(ctx);

        log("  Parsing semantics");
        BasicSemanticsParser semanticsParser = new BasicSemanticsParser();
        setupBuiltInFunctions(semanticsParser.getSymbols());
        semanticsParser.addErrorListener(getErrorListener());
        program = semanticsParser.program(program);
        
        // If we discovered semantics errors, we stop here
        if (getErrorListener().hasErrors()) {
            return null;
        }

        log("  Generating assembly code");
        BasicCodeGenerator codeGenerator = new BasicCodeGenerator();
        setupBuiltInFunctions(codeGenerator.getSymbols());
        program.setSourceFilename(getSourceFilename());
        return codeGenerator.program(program);
    }

    /**
     * Adds all built-in functions to the symbol table.
     */
    private void setupBuiltInFunctions(SymbolTable symbols) {
        symbols.addFunction(BasicBuiltInFunctions.FUN_ABS);
        symbols.addFunction(BasicBuiltInFunctions.FUN_ASC);
        symbols.addFunction(BasicBuiltInFunctions.FUN_ATN);
        symbols.addFunction(BasicBuiltInFunctions.FUN_CDBL);
        symbols.addFunction(BasicBuiltInFunctions.FUN_CINT);
        symbols.addFunction(BasicBuiltInFunctions.FUN_CHR);
        symbols.addFunction(BasicBuiltInFunctions.FUN_COS);
        symbols.addFunction(BasicBuiltInFunctions.FUN_EXP);
        symbols.addFunction(BasicBuiltInFunctions.FUN_FABS);
        symbols.addFunction(BasicBuiltInFunctions.FUN_FIX);
        symbols.addFunction(BasicBuiltInFunctions.FUN_FMOD); // Used internally
        symbols.addFunction(BasicBuiltInFunctions.FUN_HEX);
        symbols.addFunction(BasicBuiltInFunctions.FUN_INSTR2);
        symbols.addFunction(BasicBuiltInFunctions.FUN_INSTR3);
        symbols.addFunction(BasicBuiltInFunctions.FUN_INT);
        symbols.addFunction(BasicBuiltInFunctions.FUN_LCASE);
        symbols.addFunction(BasicBuiltInFunctions.FUN_LEN);
        symbols.addFunction(BasicBuiltInFunctions.FUN_LOG);
        symbols.addFunction(BasicBuiltInFunctions.FUN_OCT);
        symbols.addFunction(BasicBuiltInFunctions.FUN_SGN);
        symbols.addFunction(BasicBuiltInFunctions.FUN_SIN);
        symbols.addFunction(BasicBuiltInFunctions.FUN_SQR);
        symbols.addFunction(BasicBuiltInFunctions.FUN_SPACE);
        symbols.addFunction(BasicBuiltInFunctions.FUN_TAN);
        symbols.addFunction(BasicBuiltInFunctions.FUN_UCASE);
        symbols.addFunction(BasicBuiltInFunctions.FUN_VAL);
    }
}
