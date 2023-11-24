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

import se.dykstrom.jcc.common.ast.*;
import se.dykstrom.jcc.common.compiler.AbstractCodeGenerator;
import se.dykstrom.jcc.common.compiler.TypeManager;
import se.dykstrom.jcc.common.intermediate.Blank;
import se.dykstrom.jcc.common.intermediate.IntermediateProgram;
import se.dykstrom.jcc.common.optimization.AstOptimizer;
import se.dykstrom.jcc.common.symbols.SymbolTable;
import se.dykstrom.jcc.common.types.Identifier;
import se.dykstrom.jcc.common.types.Str;
import se.dykstrom.jcc.tiny.ast.ReadStatement;
import se.dykstrom.jcc.tiny.ast.WriteStatement;
import se.dykstrom.jcc.tiny.code.expression.TinyIdentifierDerefCodeGenerator;

import static java.util.Arrays.asList;
import static se.dykstrom.jcc.common.functions.BuiltInFunctions.FUN_PRINTF;
import static se.dykstrom.jcc.common.functions.BuiltInFunctions.FUN_SCANF;

/**
 * The code generator for the Tiny language.
 *
 * @author Johan Dykstrom
 */
public class TinyCodeGenerator extends AbstractCodeGenerator {

    private static final Identifier IDENT_FMT_PRINTF = new Identifier("_fmt_printf", Str.INSTANCE);
    private static final Identifier IDENT_FMT_SCANF = new Identifier("_fmt_scanf", Str.INSTANCE);

    private static final String VALUE_FMT_PRINTF = "\"%lld\",10,0";
    private static final String VALUE_FMT_SCANF = "\"%lld\",0";

    public TinyCodeGenerator(final TypeManager typeManager,
                             final SymbolTable symbolTable,
                             final AstOptimizer optimizer) {
        super(typeManager, symbolTable, optimizer);
        // Expressions
        expressionCodeGenerators.put(IdentifierDerefExpression.class, new TinyIdentifierDerefCodeGenerator(this));
    }

    @Override
    public IntermediateProgram generate(final Program program) {
        // Add program statements
        program.getStatements().forEach(this::statement);

        // Add an exit statement to make sure the program exits
        statement(new ExitStatement(0, 0, IntegerLiteral.ZERO));

        // Create main program
        IntermediateProgram asmProgram = new IntermediateProgram();

        // Add file header
        fileHeader(program.getSourcePath()).lines().forEach(asmProgram::add);

        // Add import section
        importSection(dependencies).lines().forEach(asmProgram::add);

        // Add data section
        dataSection(symbols).lines().forEach(asmProgram::add);

        // Add code section
        codeSection(lines()).lines().forEach(asmProgram::add);

        return asmProgram;
    }

    @Override
    protected void statement(final Statement statement) {
        final var codeGeneratorComponent = getCodeGeneratorComponent(statement.getClass());
        if (codeGeneratorComponent != null) {
            addAll(codeGeneratorComponent.generate(statement));
        } else if (statement instanceof ReadStatement readStatement) {
            readStatement(readStatement);
        } else if (statement instanceof WriteStatement writeStatement) {
            writeStatement(writeStatement);
        } else {
            super.statement(statement);
        }
        add(Blank.INSTANCE);
    }

    private void readStatement(ReadStatement statement) {
        symbols.addConstant(IDENT_FMT_SCANF, VALUE_FMT_SCANF);

        Expression fmtExpression = IdentifierNameExpression.from(statement, IDENT_FMT_SCANF);
        statement.getIdentifiers().forEach(identifier -> {
            symbols.addVariable(identifier);
            Expression expression = IdentifierNameExpression.from(statement, identifier);
            addAll(functionCall(FUN_SCANF, formatComment(statement), asList(fmtExpression, expression)));
        });
    }

    private void writeStatement(WriteStatement statement) {
        symbols.addConstant(IDENT_FMT_PRINTF, VALUE_FMT_PRINTF);

        Expression fmtExpression = IdentifierNameExpression.from(statement, IDENT_FMT_PRINTF);
        statement.getExpressions().forEach(expression ->
            addAll(functionCall(FUN_PRINTF, formatComment(statement), asList(fmtExpression, expression)))
        );
    }
}
