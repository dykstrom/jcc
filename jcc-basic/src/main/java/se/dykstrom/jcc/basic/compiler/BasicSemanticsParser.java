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

import se.dykstrom.jcc.basic.ast.statement.AbstractDefTypeStatement;
import se.dykstrom.jcc.basic.ast.statement.AbstractOnJumpStatement;
import se.dykstrom.jcc.basic.ast.statement.DefDblStatement;
import se.dykstrom.jcc.basic.ast.statement.DefIntStatement;
import se.dykstrom.jcc.basic.ast.statement.DefStrStatement;
import se.dykstrom.jcc.basic.ast.statement.GosubStatement;
import se.dykstrom.jcc.basic.ast.statement.LineInputStatement;
import se.dykstrom.jcc.basic.ast.statement.OnGosubStatement;
import se.dykstrom.jcc.basic.ast.statement.OnGotoStatement;
import se.dykstrom.jcc.basic.ast.statement.OptionBaseStatement;
import se.dykstrom.jcc.basic.ast.statement.PrintStatement;
import se.dykstrom.jcc.basic.ast.statement.RandomizeStatement;
import se.dykstrom.jcc.basic.ast.statement.SleepStatement;
import se.dykstrom.jcc.basic.ast.statement.SwapStatement;
import se.dykstrom.jcc.basic.type.BasicTypeManager;
import se.dykstrom.jcc.common.ast.AbstractJumpStatement;
import se.dykstrom.jcc.common.ast.AddExpression;
import se.dykstrom.jcc.common.ast.ArrayAccessExpression;
import se.dykstrom.jcc.common.ast.ArrayDeclaration;
import se.dykstrom.jcc.common.ast.AssignStatement;
import se.dykstrom.jcc.common.ast.AstProgram;
import se.dykstrom.jcc.common.ast.BinaryExpression;
import se.dykstrom.jcc.common.ast.BitwiseExpression;
import se.dykstrom.jcc.common.ast.CastToF64Expression;
import se.dykstrom.jcc.common.ast.CastToI64Expression;
import se.dykstrom.jcc.common.ast.ConstDeclarationStatement;
import se.dykstrom.jcc.common.ast.Declaration;
import se.dykstrom.jcc.common.ast.DeclarationAssignment;
import se.dykstrom.jcc.common.ast.DivExpression;
import se.dykstrom.jcc.common.ast.Expression;
import se.dykstrom.jcc.common.ast.FloatLiteral;
import se.dykstrom.jcc.common.ast.FunctionCallExpression;
import se.dykstrom.jcc.common.ast.FunctionDefinitionStatement;
import se.dykstrom.jcc.common.ast.GotoStatement;
import se.dykstrom.jcc.common.ast.IDivExpression;
import se.dykstrom.jcc.common.ast.IdentifierDerefExpression;
import se.dykstrom.jcc.common.ast.IdentifierExpression;
import se.dykstrom.jcc.common.ast.IdentifierNameExpression;
import se.dykstrom.jcc.common.ast.IfStatement;
import se.dykstrom.jcc.common.ast.IntegerLiteral;
import se.dykstrom.jcc.common.ast.LabelledStatement;
import se.dykstrom.jcc.common.ast.LiteralExpression;
import se.dykstrom.jcc.common.ast.ModExpression;
import se.dykstrom.jcc.common.ast.NegateExpression;
import se.dykstrom.jcc.common.ast.RelationalExpression;
import se.dykstrom.jcc.common.ast.RoundExpression;
import se.dykstrom.jcc.common.ast.Statement;
import se.dykstrom.jcc.common.ast.StringLiteral;
import se.dykstrom.jcc.common.ast.UnaryExpression;
import se.dykstrom.jcc.common.ast.VariableDeclarationStatement;
import se.dykstrom.jcc.common.ast.WhileStatement;
import se.dykstrom.jcc.common.compiler.AbstractSemanticsParser;
import se.dykstrom.jcc.common.error.CompilationErrorListener;
import se.dykstrom.jcc.common.error.DuplicateException;
import se.dykstrom.jcc.common.error.InvalidTypeException;
import se.dykstrom.jcc.common.error.InvalidValueException;
import se.dykstrom.jcc.common.error.SemanticsException;
import se.dykstrom.jcc.common.error.UndefinedException;
import se.dykstrom.jcc.common.functions.Function;
import se.dykstrom.jcc.common.functions.UserDefinedFunction;
import se.dykstrom.jcc.common.optimization.AstExpressionOptimizer;
import se.dykstrom.jcc.common.semantics.VariableUsageTracker;
import se.dykstrom.jcc.common.symbols.SymbolTable;
import se.dykstrom.jcc.common.types.Arr;
import se.dykstrom.jcc.common.types.F64;
import se.dykstrom.jcc.common.types.Fun;
import se.dykstrom.jcc.common.types.I64;
import se.dykstrom.jcc.common.types.Identifier;
import se.dykstrom.jcc.common.types.NumericType;
import se.dykstrom.jcc.common.types.Str;
import se.dykstrom.jcc.common.types.Type;
import se.dykstrom.jcc.common.utils.ExpressionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.UnaryOperator;

import static java.util.Objects.requireNonNull;
import static se.dykstrom.jcc.basic.type.BasicTypeHelper.updateTypes;
import static se.dykstrom.jcc.common.error.Warning.FLOAT_CONVERSION;
import static se.dykstrom.jcc.common.error.Warning.UNDEFINED_VARIABLE;
import static se.dykstrom.jcc.common.error.Warning.UNUSED_VARIABLE;
import static se.dykstrom.jcc.common.symbols.Scope.GLOBAL;
import static se.dykstrom.jcc.common.utils.ExpressionUtils.evaluateExpression;
import static se.dykstrom.jcc.llvm.code.LlvmBuiltIns.LF_ROUNDEVEN_F64;

/**
 * The semantics parser for the Basic language. This parser enforces the semantic rules of the
 * language, including the correct use of line numbers and the type system. It returns a copy
 * of the parsed program, where some types are better defined than in the source program.
 * <p>
 * The following rules define how the type of an identifier is decided:
 * <p>
 * - If the identifier ends with a type specifier, like "$" for strings, the type specifier decides the type.
 * - If the identifier has been declared in a DIM statement, like "DIM a AS STRING", this decides the type.
 * - If the identifier starts with a letter used in a DEFtype statement, like "DEFSTR a-c", this decides the type.
 * - If neither of the above applies, the default type is used, and that is Double.
 * <p>
 * An array that is used without having been declared in a DIM statement is defined implicitly,
 * as in QuickBASIC: it gets the element type decided by the rules above, as many dimensions as
 * the first use has subscripts, and the inclusive upper bound
 * {@value #IMPLICIT_ARRAY_UPPER_BOUND} in every dimension.
 *
 * @author Johan Dykstrom
 */
public class BasicSemanticsParser extends AbstractSemanticsParser<BasicTypeManager> {

    /** Inclusive upper bound given to each dimension of an implicitly defined array, as in QuickBASIC. */
    private static final long IMPLICIT_ARRAY_UPPER_BOUND = 10;

    /** A set of all line numbers used in the program (for undefined/duplicate line number warnings). */
    private final Set<String> lineNumbers = new HashSet<>();

    /** Arrays implicitly defined by their first use, to be declared at the start of the program. */
    private final List<Declaration> implicitArrays = new ArrayList<>();

    /** Tracks variable declaration and usage for unused variable warnings. */
    private final VariableUsageTracker usageTracker = new VariableUsageTracker();

    private final AstExpressionOptimizer optimizer;

    /** Option base for arrays; null if not set. */
    private OptionBaseStatement optionBase;

    /** Maps statement class to the method that parses statements of that class. */
    private final Map<Class<? extends Statement>, UnaryOperator<Statement>> statementParsers = new HashMap<>();

    public BasicSemanticsParser(final CompilationErrorListener errorListener,
                                final SymbolTable symbolTable,
                                final BasicTypeManager typeManager,
                                final AstExpressionOptimizer optimizer) {
        super(errorListener, symbolTable, typeManager);
        this.optimizer = requireNonNull(optimizer);

        statementParsers.put(AssignStatement.class, s -> assignStatement((AssignStatement) s));
        statementParsers.put(ConstDeclarationStatement.class, s -> constDeclarationStatement((ConstDeclarationStatement) s));
        statementParsers.put(DefDblStatement.class, s -> deftypeStatement((AbstractDefTypeStatement) s));
        statementParsers.put(DefIntStatement.class, s -> deftypeStatement((AbstractDefTypeStatement) s));
        statementParsers.put(DefStrStatement.class, s -> deftypeStatement((AbstractDefTypeStatement) s));
        statementParsers.put(FunctionDefinitionStatement.class, s -> functionDefinitionStatement((FunctionDefinitionStatement) s));
        statementParsers.put(GosubStatement.class, s -> jumpStatement((GosubStatement) s));
        statementParsers.put(GotoStatement.class, s -> jumpStatement((GotoStatement) s));
        statementParsers.put(IfStatement.class, s -> ifStatement((IfStatement) s));
        statementParsers.put(LabelledStatement.class, s -> labelledStatement((LabelledStatement) s));
        statementParsers.put(LineInputStatement.class, s -> lineInputStatement((LineInputStatement) s));
        statementParsers.put(OnGosubStatement.class, s -> onJumpStatement((OnGosubStatement) s, "on-gosub"));
        statementParsers.put(OnGotoStatement.class, s -> onJumpStatement((OnGotoStatement) s, "on-goto"));
        statementParsers.put(OptionBaseStatement.class, s -> optionBaseStatement((OptionBaseStatement) s));
        statementParsers.put(PrintStatement.class, s -> printStatement((PrintStatement) s));
        statementParsers.put(RandomizeStatement.class, s -> randomizeStatement((RandomizeStatement) s));
        statementParsers.put(SleepStatement.class, s -> sleepStatement((SleepStatement) s));
        statementParsers.put(SwapStatement.class, s -> swapStatement((SwapStatement) s));
        statementParsers.put(VariableDeclarationStatement.class, s -> variableDeclarationStatement((VariableDeclarationStatement) s));
        statementParsers.put(WhileStatement.class, s -> whileStatement((WhileStatement) s));
    }

    @Override
    public AstProgram parse(final AstProgram program) throws SemanticsException {
        // Only arrays defined by this call may be declared in the program it returns
        implicitArrays.clear();
        program.getStatements().forEach(this::lineNumber);
        final var statements = new ArrayList<Statement>(program.getStatements().stream().map(this::statement).toList());
        usageTracker.check((n, m) -> reportWarning(n, m, UNUSED_VARIABLE));
        if (errorListener.hasErrors()) {
            throw new SemanticsException("Semantics error");
        }
        // Declare implicitly defined arrays up front, so that code generation allocates them.
        // This puts the declaration before any OPTION BASE, which source code may not do, but
        // neither backend emits code for an array declaration - it only registers the array in
        // the code generator's symbol table - so the base is still set before anything reads it.
        if (!implicitArrays.isEmpty()) {
            statements.addFirst(new VariableDeclarationStatement(0, 0, List.copyOf(implicitArrays), GLOBAL));
        }
        return program.withStatements(statements);
    }

    /**
     * Save line number of statement to the set of line numbers, and check that there are no duplicates.
     */
    private void lineNumber(final Statement statement) {
        final Statement actualStatement;
        if (statement instanceof LabelledStatement labelledStatement) {
            final String line = labelledStatement.label();
            if (lineNumbers.contains(line)) {
                final String msg = "duplicate line number: " + line;
                reportError(statement.line(), statement.column(), msg, new DuplicateException(msg, labelledStatement.label()));
            } else {
                lineNumbers.add(line);
            }
            actualStatement = labelledStatement.statement();
        } else {
            actualStatement = statement;
        }
        
        // If this is a compound statement, also save line numbers of sub statements
        if (actualStatement instanceof IfStatement ifStatement) {
            ifStatement.getThenStatements().forEach(this::lineNumber);
            ifStatement.getElseStatements().forEach(this::lineNumber);
        } else if (actualStatement instanceof WhileStatement whileStatement) {
            whileStatement.getStatements().forEach(this::lineNumber);
        }
    }

    @Override
    public Statement statement(Statement statement) {
        return statementParsers.getOrDefault(statement.getClass(), UnaryOperator.identity()).apply(statement);
    }

    private AssignStatement assignStatement(AssignStatement statement) {
        // Check and update expressions
        Expression lhsExpression = expression(statement.getLhsExpression());
        Expression rhsExpression = expression(statement.getRhsExpression());

        Type lhsType = getType(lhsExpression);
        Type rhsType = getType(rhsExpression);

        // Check that types are compatible
        if (!types.isAssignableFrom(lhsType, rhsType)) {
            String msg = "you cannot assign a value of type " + types.getTypeName(rhsType)
                    + " to a variable of type " + types.getTypeName(lhsType);
            reportError(statement.line(), statement.column(), msg, new InvalidTypeException(msg, rhsType));
        } else if (types.isFloatToInt(rhsType, lhsType)) {
            String msg = "implicit conversion turns floating-point number into integer: " +
                    types.getTypeName(rhsType) + " to " + types.getTypeName(lhsType);
            reportWarning(rhsExpression, msg, FLOAT_CONVERSION);
        }

        // Make any implicit numeric conversion to the variable type explicit (issue #52)
        if (types.isAssignableFrom(lhsType, rhsType)) {
            rhsExpression = makeCastExplicit(rhsExpression, rhsType, lhsType);
        }

        // Check that LHS is not a constant
        if (lhsExpression instanceof IdentifierNameExpression ine && symbols.isConstant(ine.getIdentifier().name())) {
            String msg = "you cannot assign a new value to constant '" + ine.getIdentifier().name() + "'";
            reportError(statement.line(), statement.column(), msg, new InvalidTypeException(msg, rhsType));
        }

        // Return updated statement with the possibly updated expressions
        return statement.withLhsExpression((IdentifierExpression) lhsExpression).withRhsExpression(rhsExpression);
    }

    private Statement constDeclarationStatement(final ConstDeclarationStatement statement) {
        // For each declaration
        final var updatedDeclarations = statement.getDeclarations().stream().map(declaration -> {
            // Check identifier
            final var name = declaration.name();
            final var expression = expression(declaration.expression());
            final var type = types.getType(expression);

            // The type must match the type of the expression
            final var optionalSpecifiedType = types.getTypeByTypeSpecifier(name);
            optionalSpecifiedType.ifPresent(specifiedType -> {
                if (hasInvalidTypeSpecifier(type, specifiedType)) {
                    String msg = "constant '" + name + "' is defined with type specifier "
                            + types.getTypeName(specifiedType) + " and an expression of type "
                            + types.getTypeName(type);
                    reportError(statement.line(), statement.column(), msg, new InvalidTypeException(msg, type));
                }
            });

            // Check that identifier is not defined in symbol table
            if (symbols.contains(name)) {
                String msg = "constant '" + name + "' is already defined, with type " + types.getTypeName(symbols.getType(name));
                reportError(statement.line(), statement.column(), msg, new DuplicateException(msg, name));
            }

            // Check that expression contains only constants and operations on them
            if (!ExpressionUtils.isConstantExpression(expression, symbols)) {
                String msg = "constant '" + name + "' is defined with non-constant expression: " + expression;
                reportError(statement.line(), statement.column(), msg, new InvalidValueException(msg, expression.toString()));
            }

            // Add constant to symbol table
            LiteralExpression literalExpression;
            try {
                literalExpression = evaluateExpression(expression, symbols, optimizer, e -> (LiteralExpression) e);
                symbols.addConstant(new Identifier(name, type), literalExpression.getValue());
                usageTracker.declare(name, declaration);
            } catch (IllegalArgumentException e) {
                String msg = "cannot evaluate constant '" + name + "' expression: " + expression;
                reportError(statement.line(), statement.column(), msg, new InvalidValueException(msg, expression.toString()));
                literalExpression = IntegerLiteral.ZERO;
            }
            // Return updated declaration with correct type and value
            return new DeclarationAssignment(declaration.line(), declaration.column(), name, type, literalExpression);
        })
        .toList();

        return statement.withDeclarations(updatedDeclarations);
    }

    private VariableDeclarationStatement variableDeclarationStatement(VariableDeclarationStatement statement) {
        // For each declaration
        final var updatedDeclarations = statement.getDeclarations().stream().map(declaration -> {
            // Check identifier
            String name = declaration.name();
            Type type = declaration.type();

            // If the variable name has a type specifier, it must match the type
            final var optionalSpecifiedType = types.getTypeByTypeSpecifier(name);
            optionalSpecifiedType.ifPresent(specifiedType -> {
                if (hasInvalidTypeSpecifier(type, specifiedType)) {
                    String msg = "variable '" + name + "' is defined with type specifier "
                            + types.getTypeName(specifiedType) + " and type " + types.getTypeName(type);
                    reportError(statement.line(), statement.column(), msg, new InvalidTypeException(msg, type));
                }
            });

            if (type instanceof Arr) {
                ArrayDeclaration arrayDeclaration = (ArrayDeclaration) declaration;
                List<Expression> subscripts = arrayDeclaration.getSubscripts().stream().map(this::expression).toList();

                // Check that (array) identifier is not defined in symbol table
                if (symbols.containsArray(name)) {
                    String msg = "variable '" + name + "' is already defined, with type " + types.getTypeName(symbols.getArrayType(name));
                    reportError(statement.line(), statement.column(), msg, new DuplicateException(msg, name));
                }
                // Check that array subscripts are of type integer
                if (!allSubscriptsAreIntegers(subscripts)) {
                    String msg = "array '" + name + "' has non-integer subscript";
                    reportError(statement.line(), statement.column(), msg, new InvalidTypeException(msg, type));
                }
                // $DYNAMIC arrays are not implemented yet
                if (isDynamicArray(subscripts)) {
                    String msg = "$DYNAMIC arrays not supported yet";
                    reportError(statement.line(), statement.column(), msg, new InvalidTypeException(msg, type));
                }

                // In BASIC, the upper bound of an array declaration is inclusive, so we add 1
                // to all subscript expressions to make it similar to other languages
                final List<Expression> adjustedSubscripts = subscripts.stream()
                        .map(e -> new AddExpression(e.line(), e.column(), e, IntegerLiteral.ONE))
                        .map(Expression.class::cast)
                        .toList();
                final var updatedDeclaration = arrayDeclaration.withSubscripts(adjustedSubscripts);

                // Add variable to symbol table
                symbols.addArray(new Identifier(name, type), updatedDeclaration);
                usageTracker.declare(name, updatedDeclaration);
                return updatedDeclaration;
            } else {
                // Check that identifier is not defined in symbol table
                if (symbols.contains(name)) {
                    String msg = "variable '" + name + "' is already defined, with type " + types.getTypeName(symbols.getType(name));
                    reportError(statement.line(), statement.column(), msg, new DuplicateException(msg, name));
                }
                // Add variable to symbol table
                symbols.addVariable(new Identifier(name, type));
                usageTracker.declare(name, declaration);
                return declaration;
            }
        })
        .toList();

        return statement.withDeclarations(updatedDeclarations);
    }

    /**
     * Returns {@code true} if all array subscripts are integers.
     */
    private boolean allSubscriptsAreIntegers(List<Expression> subscripts) {
        return ExpressionUtils.areAllIntegerExpressions(subscripts, types);
    }

    /**
     * Returns {@code true} if the array subscripts signal a $DYNAMIC array, that is,
     * the subscripts are not defined by constant expressions only.
     */
    private boolean isDynamicArray(List<Expression> subscripts) {
        return !ExpressionUtils.areAllConstantExpressions(subscripts, symbols);
    }

    /**
     * Returns {@code true} if {@code specifiedType} does not match {@code actualType}.
     *
     * @see BasicSyntaxVisitor#visitIdent(BasicParser.IdentContext)
     */
    private boolean hasInvalidTypeSpecifier(final Type actualType, final Type specifiedType) {
        if (actualType instanceof Arr array) {
            return !specifiedType.equals(array.getElementType());
        }
        return !specifiedType.equals(actualType);
    }

    private Statement functionDefinitionStatement(final FunctionDefinitionStatement statement) {
        return withLocalSymbolTable(() -> {
            final var functionName = statement.identifier().name();
            final var declarations = statement.declarations();

            // Save current tracking state for unused variable checks
            usageTracker.save();

            // Add formal arguments to local symbol table
            // Note: We only support scalar arguments for now
            final var parameterNames = new HashSet<String>();
            declarations.forEach(d -> {
                final var name = d.name();
                if (parameterNames.contains(name)) {
                    String msg = "parameter '" + name + "' is already defined, with type " + types.getTypeName(symbols.getType(name));
                    reportError(statement.line(), statement.column(), msg, new DuplicateException(msg, name));
                }
                parameterNames.add(name);
                symbols.addVariable(new Identifier(name, d.type()));
                usageTracker.declare(name, d);
            });

            // Check and update expression
            var expression = expression(statement.expression());
            // Check for unused parameters (globals are checked at the top level, issue #78)
            usageTracker.check(parameterNames, (n, m) -> reportWarning(n, m, UNUSED_VARIABLE));
            // Restore tracking state
            usageTracker.restore(parameterNames);

            // Check that expression type matches return type
            final var expressionType = getType(expression);
            final var returnType = ((Fun) statement.identifier().type()).getReturnType();
            if (!types.isAssignableFrom(returnType, expressionType)) {
                final String msg = "you cannot return a value of type " + types.getTypeName(expressionType)
                        + " from function '" + functionName + "' with return type " + types.getTypeName(returnType);
                reportError(statement.line(), statement.column(), msg, new InvalidTypeException(msg, expressionType));
            } else if (types.isFloatToInt(expressionType, returnType)) {
                String msg = "implicit conversion turns floating-point number into integer: " +
                        types.getTypeName(expressionType) + " to " + types.getTypeName(returnType);
                reportWarning(expression, msg, FLOAT_CONVERSION);
            }

            // Make any implicit numeric conversion to the return type explicit (issue #52)
            if (types.isAssignableFrom(returnType, expressionType)) {
                expression = makeCastExplicit(expression, expressionType, returnType);
            }

            // Create function
            final var argNames = declarations.stream().map(Declaration::name).toList();
            final var argTypes = declarations.stream().map(Declaration::type).toList();
            final var function = new UserDefinedFunction(functionName, argNames, argTypes, returnType);

            // Check that function has not been defined
            if (symbols.containsFunction(function.getName(), argTypes)) {
                final var msg = "function '" + function + "' has already been defined";
                reportError(statement.line(), statement.column(), msg, new DuplicateException(msg, function.getName()));
            } else {
                symbols.addFunction(function);
            }

            return statement.withExpression(expression);
         });
    }

    /**
     * Parses a DEFtype statement. We don't need to define the type in the type manager
     * because we already did in BasicSyntaxVisitor. And besides, all identifiers are
     * already typed after running BasicSyntaxVisitor.
     */
    private Statement deftypeStatement(AbstractDefTypeStatement statement) {
        if (statement.getLetters().isEmpty()) {
            String msg = "invalid letter interval in " + statement.getKeyword().toLowerCase();
            reportError(statement.line(), statement.column(), msg, new InvalidValueException(msg, null));
        }
        return statement;
    }

    private AbstractJumpStatement jumpStatement(AbstractJumpStatement statement) {
        String line = statement.getJumpLabel();
        if (!lineNumbers.contains(line)) {
            String msg = "undefined line number: " + line;
            reportError(statement.line(), statement.column(), msg, new UndefinedException(msg, line));
        }
        return statement;
    }

    private IfStatement ifStatement(IfStatement statement) {
        Expression expression = expression(statement.getExpression());
        Type type = getType(expression);
        if (!type.equals(I64.INSTANCE)) {
            String msg = "expression of type " + types.getTypeName(type) + " not allowed in if statement";
            reportError(expression.line(), expression.column(), msg, new InvalidTypeException(msg, type));
        }

        // Process all sub statements recursively
        List<Statement> thenStatements = statement.getThenStatements().stream().map(this::statement).toList();
        List<Statement> elseStatements = statement.getElseStatements().stream().map(this::statement).toList();
        
        return statement.withExpression(expression).withThenStatements(thenStatements).withElseStatements(elseStatements);
    }

    private Statement labelledStatement(LabelledStatement labelledStatement) {
        return labelledStatement.withStatement(statement(labelledStatement.statement()));
    }

    private LineInputStatement lineInputStatement(LineInputStatement statement) {
        statement = updateTypes(statement, symbols);

        Identifier identifier = statement.identifier();
        Type type = identifier.type();
        if (!type.equals(Str.INSTANCE)) {
            String msg = "expected identifier of type " + types.getTypeName(Str.INSTANCE) + ", not " + types.getTypeName(type);
            reportError(statement.line(), statement.column(), msg, new InvalidTypeException(msg, type));
        }

        if (symbols.contains(identifier.name()) && symbols.isConstant(identifier.name())) {
            String msg = "cannot use constant '" + identifier.name() + "' in LINE INPUT";
            reportError(statement.line(), statement.column(), msg, new InvalidTypeException(msg, type));
        }

        return statement;
    }

    private AbstractOnJumpStatement onJumpStatement(AbstractOnJumpStatement statement, String statementName) {
        // Check expression
        final var expression = expression(statement.getExpression());
        final var type = getType(expression);
        if (!type.equals(I64.INSTANCE)) {
            String msg = "expression of type " + types.getTypeName(type) + " not allowed in " + statementName + " statement";
            reportError(expression.line(), expression.column(), msg, new InvalidTypeException(msg, type));
        }

        // Check jump labels
        statement.getJumpLabels().stream()
            .filter(label -> !lineNumbers.contains(label))
            .forEach(label -> {
                String msg = "undefined line number/label: " + label;
                reportError(statement.line(), statement.column(), msg, new UndefinedException(msg, label));
            });
        return statement.withExpression(expression);
    }

    private Statement optionBaseStatement(final OptionBaseStatement statement) {
        if (statement.base() < 0 || statement.base() > 1) {
            final String msg = "invalid option base: " + statement.base();
            reportError(statement.line(), statement.column(), msg, new SemanticsException(msg));
        }
        if (optionBase != null) {
            final String msg = "option base already set on line " + optionBase.line();
            reportError(statement.line(), statement.column(), msg, new SemanticsException(msg));
        }
        if (!symbols.arrayIdentifiers().isEmpty()) {
            final String msg = "option base not allowed after array declaration";
            reportError(statement.line(), statement.column(), msg, new SemanticsException(msg));
        }
        optionBase = statement;
        return statement;
    }

    private PrintStatement printStatement(PrintStatement statement) {
        List<Expression> expressions = statement.getExpressions().stream().map(this::expression).toList();
        return statement.withExpressions(expressions);
    }

    private SleepStatement sleepStatement(final SleepStatement statement) {
        if (statement.getExpression() != null) {
            var expression = expression(statement.getExpression());
            final var type = getType(expression);
            if (!(type instanceof NumericType)) {
                final var msg = "seconds must be a numerical expression: " + expression;
                reportError(expression, msg, new SemanticsException(msg));
            } else {
                // SLEEP takes a double, so make an implicit integer-to-float coercion explicit (issue #52)
                expression = makeCastExplicit(expression, type, F64.INSTANCE);
            }
            return statement.withExpression(expression);
        } else {
            return statement;
        }
    }

    private RandomizeStatement randomizeStatement(RandomizeStatement statement) {
        Expression expression = statement.getExpression();
        if (expression != null) {
            expression = expression(expression);
            final var type = getType(expression);
            if (!(type instanceof NumericType)) {
                final var msg = "seed must be a numerical expression: " + expression;
                reportError(expression, msg, new SemanticsException(msg));
            } else {
                // RANDOMIZE takes a double, so make an implicit integer-to-float coercion explicit (issue #52)
                expression = makeCastExplicit(expression, type, F64.INSTANCE);
            }
            return statement.withExpression(expression);
        } else {
            return statement;
        }
    }

    private SwapStatement swapStatement(final SwapStatement statement) {
        final var first = (IdentifierExpression) expression(statement.first());
        final var second = (IdentifierExpression) expression(statement.second());

        final var firstType = first.type();
        final var secondType = second.type();

        final var swappable = types.isAssignableFrom(firstType, secondType) && types.isAssignableFrom(secondType, firstType);
        if (!swappable) {
            final var msg = "cannot swap variables with types " + types.getTypeName(firstType) + " and " + types.getTypeName(secondType);
            reportError(statement.line(), statement.column(), msg, new SemanticsException(msg));
        } else if (types.isFloatToInt(secondType, firstType)) {
            String msg = "implicit conversion turns floating-point number into integer: " +
                    types.getTypeName(secondType) + " to " + types.getTypeName(firstType);
            reportWarning(second, msg, FLOAT_CONVERSION);
        } else if (types.isFloatToInt(firstType, secondType)) {
            String msg = "implicit conversion turns floating-point number into integer: " +
                    types.getTypeName(firstType) + " to " + types.getTypeName(secondType);
            reportWarning(first, msg, FLOAT_CONVERSION);
        }

        return statement.withFirst(first).withSecond(second);
    }

    private WhileStatement whileStatement(WhileStatement statement) {
        Expression expression = expression(statement.getExpression());
        Type type = getType(expression);
        if (!type.equals(I64.INSTANCE)) {
            String msg = "expression of type " + types.getTypeName(type) + " not allowed in while statement";
            reportError(expression.line(), expression.column(), msg, new InvalidTypeException(msg, type));
        }

        // Process all sub statements recursively
        List<Statement> statements = statement.getStatements().stream().map(this::statement).toList();
        
        return statement.withExpression(expression).withStatements(statements);
    }

    @Override
    public Expression expression(Expression expression) {
        if (expression instanceof BinaryExpression binaryExpression) {
            final var left = expression(binaryExpression.getLeft());
            final var right = expression(binaryExpression.getRight());
            expression = binaryExpression.withLeft(left).withRight(right);
            checkType((BinaryExpression) expression);
            // Division-by-zero is detected on the raw literals, before any cast is inserted
            checkDivisionByZero((BinaryExpression) expression);
            // Make the implicit integer-to-float promotion of a mixed operand explicit (issue #52)
            expression = promoteBinaryOperands((BinaryExpression) expression);
        } else if (expression instanceof FunctionCallExpression functionCallExpression) {
            expression = functionCall(functionCallExpression);
        } else if (expression instanceof ArrayAccessExpression arrayAccessExpression) {
            expression = arrayAccessExpression(arrayAccessExpression);
        } else if (expression instanceof IdentifierDerefExpression identifierDerefExpression) {
            expression = identifierDerefExpression(identifierDerefExpression);
        } else if (expression instanceof IdentifierNameExpression identifierNameExpression) {
            expression = identifierNameExpression(identifierNameExpression);
        } else if (expression instanceof IntegerLiteral integerLiteral) {
            checkInteger(integerLiteral);
        } else if (expression instanceof FloatLiteral floatLiteral) {
            checkFloat(floatLiteral);
        } else if (expression instanceof UnaryExpression unaryExpression) {
            Expression subExpr = expression(unaryExpression.getExpression());
            expression = unaryExpression.withExpression(subExpr);
            checkType((UnaryExpression) expression);
        }
        return expression;
    }

    /**
     * Parses a function call expression. An FCE may also turn out be an array access expression,
     * in which case this method will instead return an array access expression. The array may be
     * undefined, in which case it is defined implicitly, as in QuickBASIC.
     */
	private Expression functionCall(FunctionCallExpression fce) {
        // Check and update arguments
        List<Expression> args = fce.getArgs().stream().map(this::expression).toList();
        // Get types of arguments
        List<Type> argTypes = types.getTypes(args);

        Identifier identifier = fce.getIdentifier();
        String name = identifier.name();

        if (symbols.containsArray(name) && functionCallArgsAreActuallyArrayIndices(argTypes, name)) {
            // If the identifier is actually an array identifier
            Type arrayType = symbols.getArrayType(name);
            // Evaluate as array access expression with original arguments
            return expression(new ArrayAccessExpression(fce.line(), fce.column(), identifier.withType(arrayType), fce.getArgs()));
        } else if (symbols.containsFunction(name)) {
            // If the identifier is a function identifier
            try {
                Function function;
                try {
                    // Match the function with the expected argument types
                    function = types.resolveFunction(name, argTypes, symbols);
                    identifier = function.getIdentifier();
                } catch (UndefinedException e) {
                    // Try again, but with all IDEs replaced by identifier name expressions when possible.
                    // The problem is that scalars and arrays have different namespaces. The parser may have
                    // chosen a scalar variable instead of an array variable. Note that this only happens
                    // when there is both a scalar variable and an array variable with the same name. See
                    // also method identifierDerefExpression(IdentifierDerefExpression).
                    args = replaceIdesWithInesForArrays(args, symbols);
                    argTypes = types.getTypes(args);
                    function = types.resolveFunction(name, argTypes, symbols);
                    identifier = function.getIdentifier();
                }

                // For each argument, check for an implicit float-to-int conversion (warn), and make
                // any implicit numeric conversion to the formal parameter type explicit (issue #52)
                final var resolvedArgs = new ArrayList<Expression>(args.size());
                for (int i = 0; i < argTypes.size(); i++) {
                    final var actualType = argTypes.get(i);
                    final var formalType = function.getArgTypes().get(i);
                    if (types.isFloatToInt(actualType, formalType)) {
                        String msg = "implicit conversion turns floating-point number into integer: " +
                                types.getTypeName(actualType) + " to " + types.getTypeName(formalType);
                        reportWarning(fce, msg, FLOAT_CONVERSION);
                    }
                    resolvedArgs.add(makeCastExplicit(args.get(i), actualType, formalType));
                }

                return fce.withIdentifier(identifier).withArgs(resolvedArgs).withFunction(function);
            } catch (SemanticsException e) {
                reportError(fce.line(), fce.column(), e.getMessage(), e);
            }
        } else if (symbols.containsArray(name)) {
            // The identifier is an array, but the arguments are not valid subscripts.
            // Note that this case is checked after functions, so that an identifier that
            // is both an array and a function is still resolved as a function.
            reportInvalidArraySubscripts(fce, name, argTypes, symbols.getArrayType(name).getDimensions());
        } else if (argsAreValidArraySubscripts(argTypes)) {
            // The identifier is an undefined array, so define it implicitly (QuickBASIC allows this)
            final var arrayType = Arr.from(args.size(), ((Fun) identifier.type()).getReturnType());
            defineImplicitArray(fce, name, arrayType);
            // Evaluate as array access expression with original arguments
            return expression(new ArrayAccessExpression(fce.line(), fce.column(), identifier.withType(arrayType), fce.getArgs()));
        } else {
            String msg = "undefined function: " + name;
            reportError(fce.line(), fce.column(), msg, new UndefinedException(msg, name));
        }

	    return fce;
    }

    /**
     * Reports why {@code argTypes} are not valid subscripts in an access of the array {@code name},
     * which has {@code dimensions} dimensions.
     */
    private void reportInvalidArraySubscripts(final Expression expression,
                                              final String name,
                                              final List<Type> argTypes,
                                              final int dimensions) {
        final String msg;
        if (argTypes.stream().allMatch(NumericType.class::isInstance)) {
            msg = "array '" + name + "' has " + dimensions + " dimension" + (dimensions == 1 ? "" : "s")
                    + ", not " + argTypes.size();
        } else {
            msg = "array '" + name + "' has non-integer subscript";
        }
        reportError(expression.line(), expression.column(), msg, new InvalidTypeException(msg, Arr.INSTANCE));
    }

    /**
     * Returns {@code true} if the given types can be subscripts in an array access, that is,
     * if there is at least one of them and they are all numeric.
     */
    private boolean argsAreValidArraySubscripts(final List<Type> argTypes) {
        return !argTypes.isEmpty() && argTypes.stream().allMatch(NumericType.class::isInstance);
    }

    /**
     * Defines the array {@code name} of type {@code arrayType} implicitly, as QuickBASIC does for
     * arrays that are used without having been declared. Every dimension gets the inclusive upper
     * bound {@value #IMPLICIT_ARRAY_UPPER_BOUND}, making the array equivalent to one declared with
     * a DIM statement. The declaration is remembered, and added to the program by {@link #parse}.
     */
    private void defineImplicitArray(final Expression expression, final String name, final Arr arrayType) {
        reportWarning(expression, "undefined array: " + name, UNDEFINED_VARIABLE);

        // The upper bound of an array declaration is inclusive, so the size is one more than that,
        // just like for the subscripts of an explicit array declaration
        final Expression size = new IntegerLiteral(expression.line(), expression.column(), IMPLICIT_ARRAY_UPPER_BOUND + 1);
        final var subscripts = Collections.nCopies(arrayType.getDimensions(), size);
        final var declaration = new ArrayDeclaration(expression.line(), expression.column(), name, arrayType, subscripts);

        // Arrays are global in Basic, also when first used inside a user-defined function
        symbols.addGlobalArray(new Identifier(name, arrayType), declaration);
        implicitArrays.add(declaration);
    }

    /**
     * Replaces identifier deref expressions with identifier name expressions when
     * there exists an array with the given name.
     */
    public List<Expression> replaceIdesWithInesForArrays(final List<Expression> args, final SymbolTable symbols) {
        return args.stream().map(expression -> replaceSingleIdeWithIne(expression, symbols)).toList();
    }

    /**
     * Replaces a single IDE with an INE if there exists an array with the name given
     * in the IDE.
     */
    private Expression replaceSingleIdeWithIne(final Expression expression, SymbolTable symbols) {
        if (expression instanceof IdentifierDerefExpression ide) {
            final var name = ide.getIdentifier().name();
            if (symbols.containsArray(name)) {
                return new IdentifierNameExpression(ide.line(), ide.column(), symbols.getArrayIdentifier(name));
            }
        }
        return expression;
    }

    /**
     * Returns {@code true} if the list of function call argument types are actually indices in an array access.
     * The arguments must all be numerical, and must be as many as the number of array dimensions.
     */
    private boolean functionCallArgsAreActuallyArrayIndices(final List<Type> argTypes, final String name) {
        if (argTypes.isEmpty()) {
            return false;
        }
        if (!argTypes.stream().allMatch(NumericType.class::isInstance)) {
            return false;
        }
        return argTypes.size() == symbols.getArrayType(name).getDimensions();
    }

    private Expression arrayAccessExpression(ArrayAccessExpression expression) {
        Identifier identifier = expression.getIdentifier();
        final String name = identifier.name();
        if (symbols.containsArray(name)) {
            usageTracker.use(name);
            // If the identifier is present in the symbol table, reuse that one
            identifier = symbols.getArrayIdentifier(name);
        } else {
            // The array is undefined, so define it implicitly (QuickBASIC allows this)
            defineImplicitArray(expression, name, (Arr) identifier.type());
        }
        final List<Expression> subscripts = expression.getSubscripts().stream().map(this::expression).toList();

        // Check that the subscripts are numeric, and that there are as many of them as the
        // array has dimensions. Return the original expression if not, since the updated
        // expression below requires the number of subscripts to match the number of dimensions.
        final var subscriptTypes = types.getTypes(subscripts);
        final int dimensions = ((Arr) identifier.type()).getDimensions();
        if (subscripts.size() != dimensions || !argsAreValidArraySubscripts(subscriptTypes)) {
            reportInvalidArraySubscripts(expression, name, subscriptTypes, dimensions);
            return expression;
        }

        // For each subscript, warn about an implicit float-to-int conversion, and make it explicit (issue #52)
        final List<Expression> castSubscripts = subscripts.stream().map(subscript -> {
            final var type = getType(subscript);
            if (type.isFloat()) {
                String msg = "implicit conversion turns floating-point number into integer: " +
                        types.getTypeName(type) + " to " + types.getTypeName(I64.INSTANCE);
                reportWarning(subscript, msg, FLOAT_CONVERSION);
            }
            return makeCastExplicit(subscript, type, I64.INSTANCE);
        }).toList();

        return expression.withIdentifier(identifier).withSubscripts(castSubscripts);
    }

    private Expression identifierNameExpression(IdentifierNameExpression expression) {
        String name = expression.getIdentifier().name();
        if (symbols.contains(name)) {
            usageTracker.use(name);
            return expression.withIdentifier(symbols.getIdentifier(name));
        } else {
            reportWarning(expression, "undefined variable: " + name, UNDEFINED_VARIABLE);
            symbols.addVariable(expression.getIdentifier());
            return expression;
        }
    }

    /**
     * Parses an identifier dereference expression. An IDE may also turn out be a function call
     * to a function with no arguments, in which case this method will instead return a function
     * call expression. An IDE may also turn out to be a reference to an array (not to an array
     * element), in which case this method will return an identifier name expression.
     */
    private Expression identifierDerefExpression(IdentifierDerefExpression ide) {
        String name = ide.getIdentifier().name();
        if (symbols.contains(name)) {
            usageTracker.use(name);
            // If the identifier is a string constant, return a string literal instead
            // We cannot dereference a string constant like we can a string variable
            if (symbols.isConstant(name) && symbols.getType(name) instanceof Str) {
                return new StringLiteral(ide.line(), ide.column(), (String) symbols.getValue(name));
            }
            // If the identifier is present in the symbol table, reuse that one
            Identifier definedIdentifier = symbols.getIdentifier(name);
            return ide.withIdentifier(definedIdentifier);
        } else if (symbols.containsArray(name)) {
            usageTracker.use(name);
            // Identifier is a reference to an array (not an array access expression),
            // return an identifier name expression instead
            Identifier definedIdentifier = symbols.getArrayIdentifier(name);
            return new IdentifierNameExpression(ide.line(), ide.column(), definedIdentifier);
        } else if (symbols.containsFunction(name)) {
            // Identifier is a function with no arguments, return a function call expression instead
            final var function = symbols.getFunction(name, List.of());
            return new FunctionCallExpression(ide.line(), ide.column(), function.getIdentifier(), List.of(), function);
        } else {
            reportWarning(ide, "undefined variable: " + name, UNDEFINED_VARIABLE);
            // If the identifier is undefined, add it to the symbol table now
            symbols.addVariable(ide.getIdentifier());
            return ide;
        }
    }

    private void checkInteger(IntegerLiteral literal) {
        String value = literal.getValue();
        try {
            Long.parseLong(value);
        } catch (NumberFormatException nfe) {
            String msg = "integer out of range: " + value;
            reportError(literal, msg, new InvalidValueException(msg, value));
        }
    }

    private void checkFloat(final FloatLiteral literal) {
        final String value = literal.getValue();
        final double parsedValue = Double.parseDouble(value);
        if (Double.isInfinite(parsedValue)) {
            String msg = "float out of range: " + value;
            reportError(literal, msg, new InvalidValueException(msg, value));
        }
    }

    private void checkDivisionByZero(final BinaryExpression expression) {
		if (expression instanceof DivExpression || expression instanceof IDivExpression || expression instanceof ModExpression) {
            try {
                ExpressionUtils.checkDivisionByZero(expression);
            } catch (InvalidValueException e) {
                reportError(expression, e.getMessage(), e);
            }
		}
	}

    private void checkType(UnaryExpression expression) {
        Type type = getType(expression.getExpression());
        
        if (expression instanceof BitwiseExpression) {
            // Bitwise expressions require subexpression to be integers
            if (!type.equals(I64.INSTANCE)) {
                String msg = "expected subexpression of type integer: " + expression;
                reportError(expression, msg, new InvalidTypeException(msg, type));
            }
        } else if (expression instanceof NegateExpression) {
            // Negate expressions require subexpression to be numeric
            if (!(type instanceof NumericType)) {
                String msg = "expected numeric subexpression: " + expression;
                reportError(expression, msg, new InvalidTypeException(msg, type));
            }
        } else {
            getType(expression);
        }
    }

    private void checkType(BinaryExpression expression) {
        Type leftType = getType(expression.getLeft());
        Type rightType = getType(expression.getRight());

        if (expression instanceof BitwiseExpression || expression instanceof IDivExpression) {
            // Bitwise and integer division expressions require both subexpressions to be integers
            checkIntegerTypes(expression, leftType, rightType);
        } else if (expression instanceof RelationalExpression) {
            // Relational expressions require both subexpressions to be either strings or numbers
            checkComparableTypes(expression, leftType, rightType);
        } else {
            getType(expression);
        }
    }

    private void checkIntegerTypes(BinaryExpression expression, Type leftType, Type rightType) {
        if (!(leftType instanceof I64 && rightType instanceof I64)) {
            String msg = "expected subexpressions of type integer: " + expression;
            reportError(expression, msg, new SemanticsException(msg));
        }
    }

    private void checkComparableTypes(BinaryExpression expression, Type leftType, Type rightType) {
        boolean bothNumeric = leftType instanceof NumericType && rightType instanceof NumericType;
        boolean bothStrings = leftType instanceof Str && rightType instanceof Str;
        if (!(bothNumeric || bothStrings)) {
            String msg = "cannot compare " + types.getTypeName(leftType) + " and " + types.getTypeName(rightType);
            reportError(expression, msg, new SemanticsException(msg));
        }
    }

    private Type getType(Expression expression) {
        try {
            return types.getType(expression);
        } catch (SemanticsException se) {
            reportError(expression, se.getMessage(), se);
            return F64.INSTANCE;
        }
    }

    /**
     * Makes an implicit numeric conversion explicit by wrapping {@code expression} in a cast node,
     * so that code generation only has to lower the cast it sees (issue #52). Integer to float
     * becomes a {@link CastToF64Expression}; float to integer becomes a truncating
     * {@link CastToI64Expression} composed with a {@link RoundExpression} that rounds half-to-even
     * (QuickBASIC 4.5 semantics). Returns {@code expression} unchanged when no conversion is needed.
     */
    private Expression makeCastExplicit(final Expression expression, final Type sourceType, final Type destType) {
        if (destType.isFloat() && sourceType.isInteger()) {
            return new CastToF64Expression(expression.line(), expression.column(), expression);
        }
        if (destType.isInteger() && sourceType.isFloat()) {
            return new CastToI64Expression(expression.line(), expression.column(), new RoundExpression(expression, LF_ROUNDEVEN_F64));
        }
        return expression;
    }

    /**
     * Promotes the operands of a binary expression to a common type by inserting an explicit cast
     * on the integer operand when the other operand is a float. Mirrors {@code promoteNumeric} in
     * the type manager, but at the AST level. Only mixed integer/float numeric operands are
     * affected; equal-type, integer-only and string operands are returned unchanged.
     */
    private Expression promoteBinaryOperands(final BinaryExpression expression) {
        final var leftType = getType(expression.getLeft());
        final var rightType = getType(expression.getRight());
        if (!(leftType instanceof NumericType && rightType instanceof NumericType)) {
            return expression;
        }
        if (leftType.isInteger() && rightType.isFloat()) {
            return expression.withLeft(makeCastExplicit(expression.getLeft(), leftType, F64.INSTANCE));
        }
        if (leftType.isFloat() && rightType.isInteger()) {
            return expression.withRight(makeCastExplicit(expression.getRight(), rightType, F64.INSTANCE));
        }
        // Both operands have the same numeric type, but an operator that always yields a float (such
        // as '/', which is floating-point division in BASIC) still needs two integer operands widened.
        if (leftType.isInteger() && rightType.isInteger() && getType(expression).isFloat()) {
            return expression
                    .withLeft(makeCastExplicit(expression.getLeft(), leftType, F64.INSTANCE))
                    .withRight(makeCastExplicit(expression.getRight(), rightType, F64.INSTANCE));
        }
        return expression;
    }
}
