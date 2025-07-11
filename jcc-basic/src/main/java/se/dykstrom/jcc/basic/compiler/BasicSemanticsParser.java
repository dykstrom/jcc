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

import se.dykstrom.jcc.basic.ast.*;
import se.dykstrom.jcc.common.ast.*;
import se.dykstrom.jcc.common.compiler.AbstractSemanticsParser;
import se.dykstrom.jcc.common.error.*;
import se.dykstrom.jcc.common.functions.Function;
import se.dykstrom.jcc.common.functions.UserDefinedFunction;
import se.dykstrom.jcc.common.optimization.AstExpressionOptimizer;
import se.dykstrom.jcc.common.symbols.SymbolTable;
import se.dykstrom.jcc.common.types.*;
import se.dykstrom.jcc.common.utils.ExpressionUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static se.dykstrom.jcc.basic.compiler.BasicTypeHelper.updateTypes;
import static se.dykstrom.jcc.common.error.Warning.FLOAT_CONVERSION;
import static se.dykstrom.jcc.common.error.Warning.UNDEFINED_VARIABLE;
import static se.dykstrom.jcc.common.functions.LibcBuiltIns.LF_FMOD_F64_F64;
import static se.dykstrom.jcc.common.functions.LibcBuiltIns.LF_POW_F64_F64;
import static se.dykstrom.jcc.common.utils.ExpressionUtils.evaluateExpression;

/**
 * The semantics parser for the Basic language. This parser enforces the semantic rules of the
 * language, including the correct use of line numbers and the type system. It returns a copy
 * of the parsed program, where some types are better defined than in the source program.
 *
 * The following rules define how the type of an identifier is decided:
 *
 * - If the identifier ends with a type specifier, like "$" for strings, the type specifier decides the type.
 * - If the identifier has been declared in a DIM statement, like "DIM a AS STRING", this decides the type.
 * - If the identifier starts with a letter used in a DEFtype statement, like "DEFSTR a-c", this decides the type.
 * - If neither of the above applies, the default type is used, and that is Double.
 *
 * @author Johan Dykstrom
 */
public class BasicSemanticsParser extends AbstractSemanticsParser<BasicTypeManager> {

    /** A set of all line numbers used in the program. */
    private final Set<String> lineNumbers = new HashSet<>();

    private final AstExpressionOptimizer optimizer;

    /** Option base for arrays; null if not set. */
    private OptionBaseStatement optionBase;

    public BasicSemanticsParser(final CompilationErrorListener errorListener,
                                final SymbolTable symbolTable,
                                final BasicTypeManager typeManager,
                                final AstExpressionOptimizer optimizer) {
        super(errorListener, symbolTable, typeManager);
        this.optimizer = requireNonNull(optimizer);
    }

    @Override
    public AstProgram parse(final AstProgram program) throws SemanticsException {
        program.getStatements().forEach(this::lineNumber);
        List<Statement> statements = program.getStatements().stream().map(this::statement).toList();
        if (errorListener.hasErrors()) {
            throw new SemanticsException("Semantics error");
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
        if (statement instanceof AssignStatement assignStatement) {
            return assignStatement(assignStatement);
        } else if (statement instanceof ConstDeclarationStatement constDeclarationStatement) {
            return constDeclarationStatement(constDeclarationStatement);
        } else if (statement instanceof AbstractDefTypeStatement abstractDefTypeStatement) {
            return deftypeStatement(abstractDefTypeStatement);
        } else if (statement instanceof FunctionDefinitionStatement functionDefinitionStatement) {
            return functionDefinitionStatement(functionDefinitionStatement);
        } else if (statement instanceof GosubStatement gosubStatement) {
            return jumpStatement(gosubStatement);
        } else if (statement instanceof GotoStatement gotoStatement) {
            return jumpStatement(gotoStatement);
        } else if (statement instanceof IfStatement ifStatement) {
            return ifStatement(ifStatement);
        } else if (statement instanceof LabelledStatement labelledStatement) {
            return labelledStatement(labelledStatement);
        } else if (statement instanceof LineInputStatement lineInputStatement) {
            return lineInputStatement(lineInputStatement);
        } else if (statement instanceof OnGosubStatement onGosubStatement) {
            return onJumpStatement(onGosubStatement, "on-gosub");
        } else if (statement instanceof OnGotoStatement onGotoStatement) {
            return onJumpStatement(onGotoStatement, "on-goto");
        } else if (statement instanceof OptionBaseStatement optionBaseStatement) {
            return optionBaseStatement(optionBaseStatement);
        } else if (statement instanceof PrintStatement printStatement) {
            return printStatement(printStatement);
        } else if (statement instanceof SwapStatement swapStatement) {
            return swapStatement(swapStatement);
        } else if (statement instanceof SleepStatement sleepStatement) {
            return sleepStatement(sleepStatement);
        } else if (statement instanceof RandomizeStatement randomizeStatement) {
            return randomizeStatement(randomizeStatement);
        } else if (statement instanceof VariableDeclarationStatement variableDeclarationStatement) {
            return variableDeclarationStatement(variableDeclarationStatement);
        } else if (statement instanceof WhileStatement whileStatement) {
            return whileStatement(whileStatement);
        } else {
            return statement;
        }
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
        } else if (types.isFloatToInt(lhsType, rhsType)) {
            String msg = "implicit conversion turns floating-point number into integer: " +
                    types.getTypeName(rhsType) + " to " + types.getTypeName(lhsType);
            reportWarning(rhsExpression, msg, FLOAT_CONVERSION);
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
                return updatedDeclaration;
            } else {
                // Check that identifier is not defined in symbol table
                if (symbols.contains(name)) {
                    String msg = "variable '" + name + "' is already defined, with type " + types.getTypeName(symbols.getType(name));
                    reportError(statement.line(), statement.column(), msg, new DuplicateException(msg, name));
                }
                // Add variable to symbol table
                symbols.addVariable(new Identifier(name, type));
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

            // Add formal arguments to local symbol table
            // Note: We only support scalar arguments for now
            final Set<String> usedArgNames = new HashSet<>();
            declarations.forEach(d -> {
                final var name = d.name();
                if (usedArgNames.contains(name)) {
                    String msg = "parameter '" + name + "' is already defined, with type " + types.getTypeName(symbols.getType(name));
                    reportError(statement.line(), statement.column(), msg, new DuplicateException(msg, name));
                }
                usedArgNames.add(name);
                symbols.addVariable(new Identifier(name, d.type()));
            });

            // Check and update expression
            final var expression = expression(statement.expression());

            // Check that expression type matches return type
            final var expressionType = getType(expression);
            final var returnType = ((Fun) statement.identifier().type()).getReturnType();
            if (!types.isAssignableFrom(returnType, expressionType)) {
                final String msg = "you cannot return a value of type " + types.getTypeName(expressionType)
                        + " from function '" + functionName + "' with return type " + types.getTypeName(returnType);
                reportError(statement.line(), statement.column(), msg, new InvalidTypeException(msg, expressionType));
            } else if (types.isFloatToInt(returnType, expressionType)) {
                String msg = "implicit conversion turns floating-point number into integer: " +
                        types.getTypeName(expressionType) + " to " + types.getTypeName(returnType);
                reportWarning(expression, msg, FLOAT_CONVERSION);
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
        Expression expression = expression(statement.getExpression());
        Type type = getType(expression);
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
        return statement;
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
            final var expression = expression(statement.getExpression());
            if (!(getType(expression) instanceof NumericType)) {
                final var msg = "seconds must be a numerical expression: " + expression;
                reportError(expression, msg, new SemanticsException(msg));
            }
            return statement.withExpression(expression);
        } else {
            return statement;
        }
    }

    private RandomizeStatement randomizeStatement(RandomizeStatement statement) {
        Expression expression = statement.getExpression();
        if (expression != null) {
            return statement.withExpression(expression(expression));
        } else {
            return statement;
        }
    }

    private SwapStatement swapStatement(final SwapStatement statement) {
        final var first = (IdentifierExpression) expression(statement.first());
        final var second = (IdentifierExpression) expression(statement.second());

        final var firstType = first.getType();
        final var secondType = second.getType();

        final var swappable = types.isAssignableFrom(firstType, secondType) && types.isAssignableFrom(secondType, firstType);
        if (!swappable) {
            final var msg = "cannot swap variables with types " + types.getTypeName(firstType) + " and " + types.getTypeName(secondType);
            reportError(statement.line(), statement.column(), msg, new SemanticsException(msg));
        } else if (types.isFloatToInt(firstType, secondType)) {
            String msg = "implicit conversion turns floating-point number into integer: " +
                    types.getTypeName(secondType) + " to " + types.getTypeName(firstType);
            reportWarning(second, msg, FLOAT_CONVERSION);
        } else if (types.isFloatToInt(secondType, firstType)) {
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
            checkDivisionByZero(expression);

            // If this is a MOD expression involving floats, call library function fmod
            // We cannot check the type of the entire expression, because it has not yet been updated with correct types
            if (expression instanceof ModExpression && (getType(left) instanceof F64 || getType(right) instanceof F64)) {
                expression = functionCall(new FunctionCallExpression(expression.line(), expression.column(), LF_FMOD_F64_F64.getIdentifier(), asList(left, right)));
            }

            // If this is an exponentiation expression, call library function pow
            else if (expression instanceof ExpExpression) {
                expression = functionCall(new FunctionCallExpression(expression.line(), expression.column(), LF_POW_F64_F64.getIdentifier(), asList(left, right)));
            }

            else {
                expression = binaryExpression.withLeft(left).withRight(right);
                checkType((BinaryExpression) expression);
            }
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
     * in which case this method will instead return an array access expression.
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

                // For each argument, check if there is an implicit conversion from float to int, and warn about it
                for (int i = 0; i < argTypes.size(); i++) {
                    final var actualType = argTypes.get(i);
                    final var formalType = function.getArgTypes().get(i);
                    if (types.isFloatToInt(formalType, actualType)) {
                        String msg = "implicit conversion turns floating-point number into integer: " +
                                types.getTypeName(actualType) + " to " + types.getTypeName(formalType);
                        reportWarning(fce, msg, FLOAT_CONVERSION);
                    }
                }
            } catch (SemanticsException e) {
                reportError(fce.line(), fce.column(), e.getMessage(), e);
                // Make sure the type is a function, so we can continue parsing
                identifier = identifier.withType(Fun.from(argTypes, F64.INSTANCE));
            }
        } else {
            // Note that this can also be an array access expression with
            // an undefined array (which is allowed in QuickBASIC)
            String msg = "undefined function: " + name;
            reportError(fce.line(), fce.column(), msg, new UndefinedException(msg, name));
        }

	    return fce.withIdentifier(identifier).withArgs(args);
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
            // If the identifier is present in the symbol table, reuse that one
            identifier = symbols.getArrayIdentifier(name);
        }
        final List<Expression> subscripts = expression.getSubscripts().stream().map(this::expression).toList();

        // For subscript, check if there is an implicit conversion from float to int, and warn about it
        for (Expression subscript : subscripts) {
            final var type = getType(subscript);
            if (types.isFloatToInt(I64.INSTANCE, type)) {
                String msg = "implicit conversion turns floating-point number into integer: " +
                        types.getTypeName(type) + " to " + types.getTypeName(I64.INSTANCE);
                reportWarning(subscript, msg, FLOAT_CONVERSION);
            }
        }

        return expression.withIdentifier(identifier).withSubscripts(subscripts);
    }

    private Expression identifierNameExpression(IdentifierNameExpression expression) {
        String name = expression.getIdentifier().name();
        if (symbols.contains(name)) {
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
            // If the identifier is a string constant, return a string literal instead
            // We cannot dereference a string constant like we can a string variable
            if (symbols.isConstant(name) && symbols.getType(name) instanceof Str) {
                return new StringLiteral(ide.line(), ide.column(), (String) symbols.getValue(name));
            }
            // If the identifier is present in the symbol table, reuse that one
            Identifier definedIdentifier = symbols.getIdentifier(name);
            return ide.withIdentifier(definedIdentifier);
        } else if (symbols.containsArray(name)) {
            // Identifier is a reference to an array (not an array access expression),
            // return an identifier name expression instead
            Identifier definedIdentifier = symbols.getArrayIdentifier(name);
            return new IdentifierNameExpression(ide.line(), ide.column(), definedIdentifier);
        } else if (symbols.containsFunction(name)) {
            // Identifier is a function with no arguments, return a function call expression instead
            Identifier definedIdentifier = symbols.getFunctionIdentifier(name, emptyList());
            return new FunctionCallExpression(ide.line(), ide.column(), definedIdentifier, emptyList());
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

    private void checkDivisionByZero(final Expression expression) {
		if (expression instanceof DivExpression || expression instanceof IDivExpression || expression instanceof ModExpression) {
            try {
                ExpressionUtils.checkDivisionByZero((BinaryExpression) expression);
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

        if (expression instanceof BitwiseExpression) {
            // Bitwise expressions require both subexpressions to be integers
            if (leftType instanceof I64 && rightType instanceof I64) {
                return;
            } else {
                String msg = "expected subexpressions of type integer: " + expression;
                reportError(expression, msg, new SemanticsException(msg));
            }
        } else if (expression instanceof RelationalExpression) {
            // Relational expressions require both subexpressions to be either strings or numbers
            if (leftType instanceof NumericType && rightType instanceof NumericType) {
                return;
            } else if (leftType instanceof Str && rightType instanceof Str) {
                return;
            } else {
                String msg = "cannot compare " + types.getTypeName(leftType) + " and " + types.getTypeName(rightType);
                reportError(expression, msg, new SemanticsException(msg));
            }
        } else if (expression instanceof IDivExpression) {
            if (leftType instanceof I64 && rightType instanceof I64) {
                return;
            } else {
                String msg = "expected subexpressions of type integer: " + expression;
                reportError(expression, msg, new SemanticsException(msg));
            }
        } else {
            getType(expression);
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
}
