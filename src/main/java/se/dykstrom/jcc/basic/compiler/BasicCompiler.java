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
import se.dykstrom.jcc.basic.optimization.BasicAstOptimizer;
import se.dykstrom.jcc.common.assembly.AsmProgram;
import se.dykstrom.jcc.common.ast.Program;
import se.dykstrom.jcc.common.compiler.AbstractCompiler;
import se.dykstrom.jcc.common.optimization.AstOptimizer;
import se.dykstrom.jcc.common.symbols.SymbolTable;
import se.dykstrom.jcc.common.utils.ParseUtils;

import static se.dykstrom.jcc.basic.functions.BasicBuiltInFunctions.*;
import static se.dykstrom.jcc.common.functions.BuiltInFunctions.FUN_GETLINE;
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
        BasicTypeManager typeManager = new BasicTypeManager();
        BasicSyntaxVisitor visitor = new BasicSyntaxVisitor(typeManager);
        Program program = (Program) visitor.visitProgram(ctx);

        log("  Parsing semantics");
        BasicSemanticsParser semanticsParser = new BasicSemanticsParser(typeManager);
        setupBuiltInFunctions(semanticsParser.getSymbols());
        semanticsParser.addErrorListener(getErrorListener());
        program = semanticsParser.program(program);
        
        // If we discovered semantics errors, we stop here
        if (getErrorListener().hasErrors()) {
            return null;
        }

        log("  Optimizing");
        AstOptimizer optimizer = new BasicAstOptimizer(typeManager);
        program = optimizer.program(program);

        log("  Generating assembly code");
        BasicCodeGenerator codeGenerator = new BasicCodeGenerator(typeManager, optimizer);
        setupBuiltInFunctions(codeGenerator.symbols());
        program.setSourceFilename(getSourceFilename());
        return codeGenerator.program(program);
    }

    /**
     * Adds all built-in functions to the symbol table.
     */
    private void setupBuiltInFunctions(SymbolTable symbols) {
        symbols.addFunction(FUN_ABS);
        symbols.addFunction(FUN_ASC);
        symbols.addFunction(FUN_ATN);
        symbols.addFunction(FUN_CDBL);
        symbols.addFunction(FUN_CINT);
        symbols.addFunction(FUN_CHR);
        symbols.addFunction(FUN_COS);
        symbols.addFunction(FUN_DATE);
        symbols.addFunction(FUN_EXP);
        symbols.addFunction(FUN_FABS);
        symbols.addFunction(FUN_FIX);
        symbols.addFunction(FUN_FMOD); // Used internally
        symbols.addFunction(FUN_GETLINE); // Used internally
        symbols.addFunction(FUN_HEX);
        symbols.addFunction(FUN_INSTR2);
        symbols.addFunction(FUN_INSTR3);
        symbols.addFunction(FUN_INT);
        symbols.addFunction(FUN_LCASE);
        symbols.addFunction(FUN_LEFT);
        symbols.addFunction(FUN_LEN);
        symbols.addFunction(FUN_LOG);
        symbols.addFunction(FUN_LTRIM);
        symbols.addFunction(FUN_MID2);
        symbols.addFunction(FUN_MID3);
        symbols.addFunction(FUN_OCT);
        symbols.addFunction(FUN_RANDOMIZE);
        symbols.addFunction(FUN_RIGHT);
        symbols.addFunction(FUN_RND);
        symbols.addFunction(FUN_RND_F64);
        symbols.addFunction(FUN_RTRIM);
        symbols.addFunction(FUN_SGN);
        symbols.addFunction(FUN_SIN);
        symbols.addFunction(FUN_SQR);
        symbols.addFunction(FUN_SPACE);
        symbols.addFunction(FUN_STR_F64);
        symbols.addFunction(FUN_STR_I64);
        symbols.addFunction(FUN_STRING_I64);
        symbols.addFunction(FUN_STRING_STR);
        symbols.addFunction(FUN_TAN);
        symbols.addFunction(FUN_TIME);
        symbols.addFunction(FUN_TIMER);
        symbols.addFunction(FUN_UCASE);
        symbols.addFunction(FUN_VAL);
    }
}
