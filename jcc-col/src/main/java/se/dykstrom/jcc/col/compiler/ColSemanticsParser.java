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

import se.dykstrom.jcc.col.ast.AliasStatement;
import se.dykstrom.jcc.col.ast.PrintlnStatement;
import se.dykstrom.jcc.col.types.ColTypeManager;
import se.dykstrom.jcc.common.ast.BinaryExpression;
import se.dykstrom.jcc.common.ast.Expression;
import se.dykstrom.jcc.common.ast.FunctionCallExpression;
import se.dykstrom.jcc.common.ast.IntegerLiteral;
import se.dykstrom.jcc.common.ast.Program;
import se.dykstrom.jcc.common.ast.Statement;
import se.dykstrom.jcc.common.compiler.AbstractSemanticsParser;
import se.dykstrom.jcc.common.error.CompilationErrorListener;
import se.dykstrom.jcc.common.error.DuplicateException;
import se.dykstrom.jcc.common.error.InvalidValueException;
import se.dykstrom.jcc.common.error.SemanticsException;
import se.dykstrom.jcc.common.error.UndefinedException;
import se.dykstrom.jcc.common.functions.Function;
import se.dykstrom.jcc.common.symbols.SymbolTable;
import se.dykstrom.jcc.common.types.Fun;
import se.dykstrom.jcc.common.types.I64;
import se.dykstrom.jcc.common.types.Identifier;
import se.dykstrom.jcc.common.types.Type;

import static java.util.Objects.requireNonNull;

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

        final var optionalType = types.getTypeFromName(statement.value());
        if (optionalType.isEmpty()) {
            final var msg = "undefined type: " + statement.value();
            reportSemanticsError(statement.line(), statement.column(), msg, new UndefinedException(msg, statement.value()));
            return statement;
        }

        types.defineTypeName(statement.alias(), optionalType.get());
        return statement.withType(optionalType.get());
    }

    private Statement printlnStatement(final PrintlnStatement statement) {
        return statement.withExpression(expression(statement.expression()));
    }

    private Expression expression(Expression expression) {
        if (expression instanceof BinaryExpression binaryExpression) {
            final Expression left = expression(binaryExpression.getLeft());
            final Expression right = expression(binaryExpression.getRight());
            expression = binaryExpression.withLeft(left).withRight(right);
            checkType((BinaryExpression) expression);
        } else if (expression instanceof FunctionCallExpression functionCallExpression) {
            expression = functionCall(functionCallExpression);
        } else if (expression instanceof IntegerLiteral integerLiteral) {
            checkInteger(integerLiteral);
        }
        return expression;
    }

    /**
     * Parses a function call expression.
     */
    private Expression functionCall(FunctionCallExpression fce) {
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

    private void checkInteger(final IntegerLiteral integer) {
        final String value = integer.getValue();
        try {
            Long.parseLong(value);
        } catch (NumberFormatException nfe) {
            final String msg = "integer out of range: " + value;
            reportSemanticsError(integer.line(), integer.column(), msg, new InvalidValueException(msg, value));
        }
    }

    private void checkType(BinaryExpression expression) {
        getType(expression);
    }

    private Type getType(Expression expression) {
        try {
            return types.getType(expression);
        } catch (SemanticsException se) {
            reportSemanticsError(expression.line(), expression.column(), se.getMessage(), se);
            return I64.INSTANCE;
        }
    }
}
