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
import se.dykstrom.jcc.basic.code.llvm.statement.*;
import se.dykstrom.jcc.common.ast.*;
import se.dykstrom.jcc.common.code.Blank;
import se.dykstrom.jcc.common.code.Label;
import se.dykstrom.jcc.common.code.Line;
import se.dykstrom.jcc.common.code.TargetProgram;
import se.dykstrom.jcc.common.compiler.TypeManager;
import se.dykstrom.jcc.common.optimization.AstOptimizer;
import se.dykstrom.jcc.common.symbols.SymbolTable;
import se.dykstrom.jcc.llvm.code.AbstractLlvmCodeGenerator;
import se.dykstrom.jcc.llvm.code.expression.BinaryCodeGenerator;
import se.dykstrom.jcc.llvm.code.expression.FunctionCallCodeGenerator;
import se.dykstrom.jcc.llvm.code.expression.IdentDerefCodeGenerator;
import se.dykstrom.jcc.llvm.code.expression.LlvmExpressionCodeGenerator;
import se.dykstrom.jcc.llvm.code.statement.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static se.dykstrom.jcc.common.symbols.Scope.GLOBAL;
import static se.dykstrom.jcc.llvm.LlvmOperator.ADD;
import static se.dykstrom.jcc.llvm.LlvmOperator.FADD;

public class BasicLlvmCodeGenerator extends AbstractLlvmCodeGenerator {

    private final List<Label> possibleReturnTargets = new ArrayList<>();
    // Counter for generating unique `after.gosub.*` labels. Reset at the start of
    // each `generate(...)` invocation so labels are unique per generated module.
    private final AtomicInteger gosubLabelCounter = new AtomicInteger();

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
        // This is a workaround to make sure all functions have been defined before they are called
        // It would be better if a reference to the function would be stored in the
        // function call expression after semantic analysis
        defineFunctions(astProgram.getStatements());

        // Ensure that all GOSUB statements are followed by labelled statements,
        // and collect all labels following a GOSUB statement
        // This is essential for code generation of GOSUB and RETURN statements
        gosubLabelCounter.set(0);
        final var statements = insertAndCollectLabelledStatements(astProgram.getStatements());

        // Wrap all statements in a main function
        final var mainFunction = generateMainFunction(statements, true);
        // Generate code for main function
        statement(mainFunction, lines, symbolTable());

        // Add implementation of user-defined functions
        lines.addFirst(Blank.INSTANCE);
        lines.addAll(0, generateFunctions(statements));

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

    private List<Statement> insertAndCollectLabelledStatements(final List<Statement> statements) {
        final var afterGosubLabels = new HashSet<String>();

        final var updatedStatements = new ArrayList<>(statements);

        for (int i = 0; i < updatedStatements.size(); i++) {
            final var statement = updatedStatements.get(i);
            if (statement instanceof LabelledStatement ls) {
                updatedStatements.set(i, ls.withStatement(updateStatement(ls.statement(), i, updatedStatements, afterGosubLabels)));
            } else {
                updatedStatements.set(i, updateStatement(statement, i, updatedStatements, afterGosubLabels));
            }
        }

        // Convert collected label names to labels and store them in
        // a list that is later used by ReturnFromGosubCodeGenerator
        afterGosubLabels.stream()
                .sorted()
                .map(Label::new)
                .forEach(possibleReturnTargets::add);

        return updatedStatements;
    }

    /**
     * Returns an updated version of the given statement, recursing into while and if
     * statements, and adding "after gosub" labels to GOSUB and ON-GOSUB statements.
     * Statements of other types are returned unchanged.
     */
    private Statement updateStatement(final Statement statement,
                                      final int index,
                                      final List<Statement> statements,
                                      final Set<String> afterGosubLabels) {
        return switch (statement) {
            case GosubStatement gs -> gs.withNextLabel(collectLabel(index, statements, afterGosubLabels));
            case OnGosubStatement ogs -> ogs.withNextLabel(collectLabel(index, statements, afterGosubLabels));
            case WhileStatement ws -> ws.withStatements(insertAndCollectLabelledStatements(ws.getStatements()));
            case IfStatement is -> is
                    .withThenStatements(insertAndCollectLabelledStatements(is.getThenStatements()))
                    .withElseStatements(insertAndCollectLabelledStatements(is.getElseStatements()));
            default -> statement;
        };
    }

    private String collectLabel(final int index,
                                final List<Statement> statements,
                                final Set<String> afterGosubLabels) {
        final var label = checkAndUpdateNextStatement(index, statements);
        afterGosubLabels.add(label);
        return label;
    }

    private String checkAndUpdateNextStatement(final int index, final List<Statement> statements) {
        final String label;
        if (index == statements.size() - 1) {
            // This is the last statement
            label = "after.gosub." + gosubLabelCounter.getAndIncrement();
            statements.add(new LabelledStatement(label, new CommentStatement(0, 0, "Generated label " + label)));
        } else if (statements.get(index + 1) instanceof LabelledStatement ls) {
            // Next statement is a labelled statement - reuse the label
            label = ls.label();
        } else {
            // Next statement is not a labelled statement - create new label
            label = "after.gosub." + gosubLabelCounter.getAndIncrement();
            statements.set(index + 1, new LabelledStatement(label, statements.get(index + 1)));
        }
        return label;
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
        final var map = new HashMap<Class<?>, LlvmStatementCodeGenerator<? extends Statement>>();
        map.put(AssignStatement.class, new AssignCodeGenerator(this, GLOBAL));
        map.put(ClsStatement.class, new ClsCodeGenerator());
        map.put(DefDblStatement.class, new DefTypeCodeGenerator());
        map.put(DefIntStatement.class, new DefTypeCodeGenerator());
        map.put(DefStrStatement.class, new DefTypeCodeGenerator());
        map.put(EndStatement.class, new EndCodeGenerator());
        map.put(GosubStatement.class, new GosubCodeGenerator());
        map.put(IfStatement.class, new IfCodeGenerator(this, new BasicConditionCodeGenerator(this)));
        map.put(LineInputStatement.class, new LineInputCodeGenerator(this, GLOBAL));
        map.put(OnGotoStatement.class, new OnGotoCodeGenerator(this));
        map.put(OnGosubStatement.class, new OnGosubCodeGenerator(this));
        map.put(PrintStatement.class, new PrintCodeGenerator(this));
        map.put(RandomizeStatement.class, new RandomizeCodeGenerator(this));
        map.put(ReturnFromGosubStatement.class, new ReturnFromGosubCodeGenerator(possibleReturnTargets));
        map.put(SleepStatement.class, new SleepCodeGenerator(this));
        map.put(SwapStatement.class, new SwapCodeGenerator(this));
        map.put(SystemStatement.class, new SystemCodeGenerator());
        map.put(WhileStatement.class, new WhileCodeGenerator(this, new BasicConditionCodeGenerator(this)));
        return map;
    }

    private Map<Class<?>, LlvmExpressionCodeGenerator<? extends Expression>> buildExpressionDictionary() {
        final var addCodeGenerator = new BinaryCodeGenerator(this, FADD, ADD);

        final var map = new HashMap<Class<?>, LlvmExpressionCodeGenerator<? extends Expression>>();
        map.put(AddExpression.class, new BasicAddCodeGenerator(this, addCodeGenerator));
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
