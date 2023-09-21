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

package se.dykstrom.jcc.col.compiler;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import se.dykstrom.jcc.col.ast.AliasStatement;
import se.dykstrom.jcc.col.ast.ImportStatement;
import se.dykstrom.jcc.col.ast.PrintlnStatement;
import se.dykstrom.jcc.col.types.ColTypeManager;
import se.dykstrom.jcc.col.types.NamedType;
import se.dykstrom.jcc.common.ast.*;
import se.dykstrom.jcc.common.compiler.AbstractSemanticsParser;
import se.dykstrom.jcc.common.error.*;
import se.dykstrom.jcc.common.functions.Function;
import se.dykstrom.jcc.common.functions.LibraryFunction;
import se.dykstrom.jcc.common.symbols.SymbolTable;
import se.dykstrom.jcc.common.types.Void;
import se.dykstrom.jcc.common.types.*;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;
import static se.dykstrom.jcc.common.functions.BuiltInFunctions.FUN_FMOD;

public class ColSemanticsParser extends AbstractSemanticsParser {

    private final ColTypeManager types;
    private final SymbolTable symbols;

    public ColSemanticsParser(final CompilationErrorListener errorListener,
                              final SymbolTable symbolTable,
                              final ColTypeManager typeManager) {
        super(errorListener);
        this.types = requireNonNull(typeManager);
        this.symbols = requireNonNull(symbolTable);
    }

    @Override
    public Program parse(final Program program) throws SemanticsException {
        final var statements = program.getStatements().stream().map(this::statement).toList();
        if (errorListener.hasErrors()) {
            throw new SemanticsException("Semantics error");
        }
        return program.withStatements(statements);
    }

    private Statement statement(final Statement statement) {
        if (statement instanceof AliasStatement aliasStatement) {
            return aliasStatement(aliasStatement);
        } else if (statement instanceof ImportStatement importStatement) {
            return importStatement(importStatement);
        } else if (statement instanceof PrintlnStatement printlnStatement) {
            return printlnStatement(printlnStatement);
        } else {
            return statement;
        }
    }

    private Statement aliasStatement(final AliasStatement statement) {
        if (types.getTypeFromName(statement.alias()).isPresent()) {
            final var msg = "cannot redefine type: " + statement.alias();
            reportSemanticsError(statement.line(), statement.column(), msg, new DuplicateException(msg, statement.alias()));
            return statement;
        }

        final var resolvedType = resolveType(statement.type(), types, statement.line(), statement.column());
        types.defineTypeName(statement.alias(), resolvedType);
        return statement.withType(resolvedType);
    }

    private Statement importStatement(final ImportStatement statement) {
        LibraryFunction function = statement.function();

        final var argTypes = function.getArgTypes().stream()
                                     .map(type -> resolveType(type, types, statement.line(), statement.column()))
                                     .toList();
        final var returnType = resolveType(function.getReturnType(), types, statement.line(), statement.column());
        function = function.withArgsTypes(argTypes);
        function = function.withReturnType(returnType);

        // We know the external dependency is just one function in one library
        final var entry = function.getDependencies().entrySet().iterator().next();
        function = function.withExternalFunction(entry.getKey() + ".dll", entry.getValue().iterator().next());

        if (symbols.containsFunction(function.getName(), argTypes)) {
            final var msg = "function '" + toString(function) + "' has already been defined";
            reportSemanticsError(statement.line(), statement.column(), msg, new DuplicateException(msg, function.getName()));
        } else {
            symbols.addFunction(function);
        }

        return statement.withFunction(function);
    }

    private Statement printlnStatement(final PrintlnStatement statement) {
        return statement.withExpression(expression(statement.expression()));
    }

    private Expression expression(Expression expression) {
        if (expression instanceof BinaryExpression binaryExpression) {
            final Expression left = expression(binaryExpression.getLeft());
            final Expression right = expression(binaryExpression.getRight());
            checkDivisionByZero(expression);

            // If this is a MOD expression involving floats, call library function fmod
            // We cannot check the type of the entire expression, because it has not yet been updated with correct types
            if (expression instanceof ModExpression && (getType(left) instanceof F64 || getType(right) instanceof F64)) {
                expression = functionCall(new FunctionCallExpression(expression.line(), expression.column(), FUN_FMOD.getIdentifier(), List.of(left, right)));
            } else {
                expression = binaryExpression.withLeft(left).withRight(right);
                checkType((BinaryExpression) expression);
            }
        } else if (expression instanceof FunctionCallExpression functionCallExpression) {
            expression = functionCall(functionCallExpression);
        } else if (expression instanceof IntegerLiteral integerLiteral) {
            checkInteger(integerLiteral);
        } else if (expression instanceof FloatLiteral floatLiteral) {
            checkFloat(floatLiteral);
        } else if (expression instanceof UnaryExpression unaryExpression) {
            final Expression subExpr = expression(unaryExpression.getExpression());
            expression = unaryExpression.withExpression(subExpr);
            checkType((UnaryExpression) expression);
        }
        return expression;
    }

    /**
     * Parses a function call expression.
     */
    private Expression functionCall(final FunctionCallExpression fce) {
        // Check and update arguments
        final var args = fce.getArgs().stream().map(this::expression).toList();
        // Get types of arguments
        final var argTypes = types.getTypes(args);

        Identifier identifier = fce.getIdentifier();
        String name = identifier.name();

        if (symbols.containsFunction(name)) {
            // If the identifier is a function identifier
            try {
                // Match the function with the expected argument types
                Function function = types.resolveFunction(name, argTypes, symbols);
                identifier = function.getIdentifier();
            } catch (SemanticsException e) {
                reportSemanticsError(fce.line(), fce.column(), e.getMessage(), e);
                // Make sure the type is a function, so we can continue parsing
                identifier = identifier.withType(Fun.from(argTypes, I64.INSTANCE));
            }
        } else {
            String msg = "undefined function: " + name;
            reportSemanticsError(fce.line(), fce.column(), msg, new UndefinedException(msg, name));
        }

        return fce.withIdentifier(identifier).withArgs(args);
    }

    private void checkInteger(final IntegerLiteral literal) {
        final var value = literal.getValue();
        try {
            Long.parseLong(value);
        } catch (NumberFormatException nfe) {
            final String msg = "integer out of range: " + value;
            reportSemanticsError(literal.line(), literal.column(), msg, new InvalidValueException(msg, value));
        }
    }

    private void checkFloat(final FloatLiteral literal) {
        final var value = literal.getValue();
        final var parsedValue = Double.parseDouble(value);
        if (Double.isInfinite(parsedValue)) {
            String msg = "float out of range: " + value;
            reportSemanticsError(literal.line(), literal.column(), msg, new InvalidValueException(msg, value));
        }
    }

    private void checkDivisionByZero(final Expression expression) {
        if (expression instanceof DivExpression || expression instanceof IDivExpression || expression instanceof ModExpression) {
            final Expression right = ((BinaryExpression) expression).getRight();
            if (right instanceof LiteralExpression literal) {
                final String value = literal.getValue();
                if (isZero(value)) {
                    final String msg = "division by zero: " + value;
                    reportSemanticsError(expression.line(), expression.column(), msg, new InvalidValueException(msg, value));
                }
            }
        }
    }

    /**
     * Returns {@code true} if the string {@code value} represents a zero value.
     */
    private boolean isZero(final String value) {
        final Pattern zeroPattern = Pattern.compile("0(\\.0*)?");
        return zeroPattern.matcher(value).matches();
    }

    private void checkType(final UnaryExpression expression) {
        final Type type = getType(expression.getExpression());

        if (expression instanceof BitwiseExpression) {
            // Bitwise expressions require subexpression to be integers
            if (!type.equals(I64.INSTANCE)) {
                String msg = "expected integer subexpression: " + expression;
                reportSemanticsError(expression.line(), expression.column(), msg, new InvalidTypeException(msg, type));
            }
        } else if (expression instanceof NegateExpression) {
            // Negate expressions require subexpression to be numeric
            if (!(type instanceof NumericType)) {
                String msg = "expected numeric subexpression: " + expression;
                reportSemanticsError(expression.line(), expression.column(), msg, new InvalidTypeException(msg, type));
            }
        } else {
            getType(expression);
        }
    }

    private void checkType(BinaryExpression expression) {
        final Type leftType = getType(expression.getLeft());
        final Type rightType = getType(expression.getRight());

        if (expression instanceof BitwiseExpression) {
            // Bitwise expressions require both subexpressions to be integers
            if (isTypeMismatch(I64.class, leftType, rightType)) {
                String msg = "expected integer subexpressions: " + expression;
                reportSemanticsError(expression.line(), expression.column(), msg, new SemanticsException(msg));
            }
        } else if (expression instanceof RelationalExpression) {
            // Relational expressions require both subexpressions to be either strings or numbers
            if (isTypeMismatch(NumericType.class, leftType, rightType) && isTypeMismatch(Str.class, leftType, rightType)) {
                String msg = "cannot compare " + types.getTypeName(leftType) + " and " + types.getTypeName(rightType);
                reportSemanticsError(expression.line(), expression.column(), msg, new SemanticsException(msg));
            }
        } else if (expression instanceof IDivExpression) {
            if (isTypeMismatch(I64.class, leftType, rightType)) {
                String msg = "expected integer subexpressions: " + expression;
                reportSemanticsError(expression.line(), expression.column(), msg, new SemanticsException(msg));
            }
        } else {
            getType(expression);
        }
    }

    private static boolean isTypeMismatch(final Class<? extends Type> expectedType, final Type... actualTypes) {
        return !Stream.of(actualTypes).allMatch(expectedType::isInstance);
    }

    private Type getType(Expression expression) {
        try {
            return types.getType(expression);
        } catch (SemanticsException se) {
            reportSemanticsError(expression.line(), expression.column(), se.getMessage(), se);
            return I64.INSTANCE;
        }
    }

    /**
     * Resolves the given type from the type name if it is a {@link NamedType}.
     * Otherwise, just returns the type as is. If the type name is unknown, and
     * the type cannot be resolved, this method reports a semantics error, and
     * returns the given type.
     */
    private Type resolveType(final Type type, final ColTypeManager typeManager, final int line, final int column) {
        if (type instanceof NamedType namedType) {
            final var typeName = namedType.getName();
            final var optionalType = typeManager.getTypeFromName(typeName);
            if (optionalType.isPresent()) {
                return optionalType.get();
            }
            final var msg = "undefined type: " + typeName;
            reportSemanticsError(line, column, msg, new UndefinedException(msg, typeName));
        }
        return type;
    }

    /**
     * Returns a string representation of the given function in COL syntax.
     */
    private String toString(final Function function) {
        final var builder = new StringBuilder();
        builder.append(function.getName()).append("(");
        builder.append(
                function.getArgTypes().stream()
                        .map(types::getTypeName)
                        .collect(joining(", "))
        );
        builder.append(")");
        if (function.getReturnType() != Void.INSTANCE) {
               builder.append(" -> ").append(types.getTypeName(function.getReturnType()));
        }
        return builder.toString();
    }
}
