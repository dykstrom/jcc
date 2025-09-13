/*
 * Copyright (C) 2025 Johan Dykstrom
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

import se.dykstrom.jcc.basic.ast.expression.AscExpression;
import se.dykstrom.jcc.basic.ast.expression.EqvExpression;
import se.dykstrom.jcc.basic.ast.expression.ImpExpression;
import se.dykstrom.jcc.basic.ast.statement.*;
import se.dykstrom.jcc.basic.code.llvm.expression.*;
import se.dykstrom.jcc.basic.code.llvm.statement.ClsCodeGenerator;
import se.dykstrom.jcc.basic.code.llvm.statement.DefTypeCodeGenerator;
import se.dykstrom.jcc.basic.code.llvm.statement.PrintCodeGenerator;
import se.dykstrom.jcc.basic.code.llvm.statement.SwapCodeGenerator;
import se.dykstrom.jcc.common.ast.*;
import se.dykstrom.jcc.common.code.Blank;
import se.dykstrom.jcc.common.code.Line;
import se.dykstrom.jcc.common.code.TargetProgram;
import se.dykstrom.jcc.common.compiler.TypeManager;
import se.dykstrom.jcc.common.optimization.AstOptimizer;
import se.dykstrom.jcc.common.symbols.SymbolTable;
import se.dykstrom.jcc.llvm.code.AbstractLlvmCodeGenerator;
import se.dykstrom.jcc.llvm.code.expression.FunctionCallCodeGenerator;
import se.dykstrom.jcc.llvm.code.expression.IdentDerefCodeGenerator;
import se.dykstrom.jcc.llvm.code.expression.LlvmExpressionCodeGenerator;
import se.dykstrom.jcc.llvm.code.statement.*;

import java.util.*;

import static se.dykstrom.jcc.common.symbols.Scope.GLOBAL;

public class BasicLlvmCodeGenerator extends AbstractLlvmCodeGenerator {

    public BasicLlvmCodeGenerator(final TypeManager typeManager,
                                  final SymbolTable symbolTable,
                                  final AstOptimizer optimizer) {
        super(typeManager, symbolTable, optimizer);

        statementDictionary.putAll(buildStatementDictionary());
        expressionDictionary.putAll(buildExpressionDictionary());
    }

    @Override
    public TargetProgram generate(final AstProgram astProgram) {
        final var lines = new ArrayList<Line>();

        // Add user-defined functions to symbol table
        // This is a workaround to make sure all functions have been defined before
        // they are called
        // It would be better if a reference to the function would be stored in the
        // function call expression after semantic analysis
        defineFunctions(astProgram.getStatements());

        // Wrap all statements in a main function
        final var mainFunction = generateMainFunction(astProgram.getStatements(), true);
        // Generate code for main function
        statement(mainFunction, lines, symbolTable());

        // Add implementation of user-defined functions
        lines.addFirst(Blank.INSTANCE);
        lines.addAll(0, generateFunctions(astProgram.getStatements()));

        // Add declares of external functions
        lines.addFirst(Blank.INSTANCE);
        lines.addAll(0, generateDeclares(getCalledFunctions(lines)));

        // Add declarations of global variables/constants
        lines.addFirst(Blank.INSTANCE);
        lines.addAll(0, generateGlobals(symbolTable()));

        // Add file header
        lines.addFirst(Blank.INSTANCE);
        lines.addAll(0, generateHeader(astProgram.getSourcePath()));

        return new TargetProgram(lines);
    }

    private void defineFunctions(final List<Statement> statements) {
        statements.stream()
                .filter(s -> s instanceof FunctionDefinitionStatement)
                .map(s -> (FunctionDefinitionStatement) s)
                .forEach(s -> FunDefCodeGenerator.createFunction(s, symbolTable()));
    }

    private Collection<? extends Line> generateFunctions(final List<Statement> statements) {
        final var lines = new ArrayList<Line>();
        statements.stream()
                .filter(s -> s instanceof FunctionDefinitionStatement)
                .forEach(s -> statement(s, lines, symbolTable()));
        return lines;
    }

    private Map<Class<?>, LlvmStatementCodeGenerator<? extends Statement>> buildStatementDictionary() {
        return Map.of(
                AssignStatement.class, new AssignCodeGenerator(this, GLOBAL),
                ClsStatement.class, new ClsCodeGenerator(),
                DefDblStatement.class, new DefTypeCodeGenerator(),
                DefIntStatement.class, new DefTypeCodeGenerator(),
                DefStrStatement.class, new DefTypeCodeGenerator(),
                IfStatement.class, new IfCodeGenerator(this, new BasicConditionCodeGenerator(this)),
                PrintStatement.class, new PrintCodeGenerator(this),
                SwapStatement.class, new SwapCodeGenerator(this),
                WhileStatement.class, new WhileCodeGenerator(this, new BasicConditionCodeGenerator(this))
        );
    }

    private Map<Class<?>, LlvmExpressionCodeGenerator<? extends Expression>> buildExpressionDictionary() {
        final var map = new HashMap<Class<?>, LlvmExpressionCodeGenerator<? extends Expression>>();
        map.put(EqualExpression.class, new BasicRelationalCodeGenerator(this, eqCodeGenerator));
        map.put(EqvExpression.class, new EqvCodeGenerator(this));
        map.put(FunctionCallExpression.class, new FunctionCallCodeGenerator(this, new BasicLlvmFunctions()));
        map.put(GreaterExpression.class, new BasicRelationalCodeGenerator(this, gtCodeGenerator));
        map.put(GreaterOrEqualExpression.class, new BasicRelationalCodeGenerator(this, geCodeGenerator));
        map.put(IdentifierDerefExpression.class, new IdentDerefCodeGenerator(GLOBAL));
        map.put(AscExpression.class, new AscCodeGenerator(this));
        map.put(ImpExpression.class, new ImpCodeGenerator(this));
        map.put(LessExpression.class, new BasicRelationalCodeGenerator(this, ltCodeGenerator));
        map.put(LessOrEqualExpression.class, new BasicRelationalCodeGenerator(this, leCodeGenerator));
        map.put(NotEqualExpression.class, new BasicRelationalCodeGenerator(this, neCodeGenerator));
        return map;
    }
}
