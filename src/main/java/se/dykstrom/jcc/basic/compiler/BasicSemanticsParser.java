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

import se.dykstrom.jcc.basic.ast.GotoStatement;
import se.dykstrom.jcc.basic.ast.PrintStatement;
import se.dykstrom.jcc.common.ast.*;
import se.dykstrom.jcc.common.compiler.AbstractSemanticsParser;
import se.dykstrom.jcc.common.error.DuplicateException;
import se.dykstrom.jcc.common.error.InvalidException;
import se.dykstrom.jcc.common.error.SemanticsException;
import se.dykstrom.jcc.common.error.UndefinedException;
import se.dykstrom.jcc.common.symbols.Identifier;
import se.dykstrom.jcc.common.types.Type;
import se.dykstrom.jcc.common.types.Unknown;

import java.util.*;

import static java.util.stream.Collectors.toList;

/**
 * The semantics parser for the Basic language. This parser enforces the semantic rules of the
 * language, including the correct use of line numbers, and the type system. It may return a
 * modified copy of the program, where some types have been better defined.
 *
 * @author Johan Dykstrom
 */
class BasicSemanticsParser extends AbstractSemanticsParser {

    private static final BasicTypeManager TYPE_MANAGER = new BasicTypeManager();

    private final Set<String> lineNumbers = new HashSet<>();

    private final Map<String, Identifier> identifiers = new HashMap<>();

    public Program program(Program program) {
        program.getStatements().forEach(this::saveLineNumber);
        List<Statement> statements = program.getStatements().stream().map(this::statement).collect(toList());
        return program.withStatements(statements);
    }

    /**
     * Save line number of statement to the set of line numbers, and check that there are no duplicates.
     */
    private void saveLineNumber(Statement statement) {
        String line = statement.getLabel();
        if (line != null) {
            if (lineNumbers.contains(line)) {
                String msg = "duplicate line number: " + line;
                reportSemanticsError(statement.getLine(), statement.getColumn(), msg, new DuplicateException(msg, statement.getLabel()));
            } else {
                lineNumbers.add(line);
            }
        }
    }

    private Statement statement(Statement statement) {
        if (statement instanceof AssignStatement) {
            return assignStatement((AssignStatement) statement);
        } else if (statement instanceof GotoStatement) {
            return gotoStatement((GotoStatement) statement);
        } else if (statement instanceof PrintStatement) {
            return printStatement((PrintStatement) statement);
        } else {
            return statement;
        }
    }

    private AssignStatement assignStatement(AssignStatement statement) {
        Identifier newIdentifier = statement.getIdentifier();
        Identifier oldIdentifier = identifiers.get(newIdentifier.getName());

        // If the identifier was already defined, use the old definition
        Identifier identifier = oldIdentifier != null ? oldIdentifier : newIdentifier;

        Type identType = identifier.getType();
        Type exprType = getType(statement.getExpression());

        // If the identifier was not typed, it derives its type from the expression
        if (identType == Unknown.INSTANCE) {
            identType = exprType;
            identifier = identifier.withType(exprType);
            statement = statement.withIdentifier(identifier);
        }

        // If the identifier was not already defined, save it for later
        if (oldIdentifier == null) {
            identifiers.put(identifier.getName(), identifier);
        }

        // Check that expression can be assigned to identifier
        if (!TYPE_MANAGER.isAssignableFrom(identType, exprType)) {
            String msg = "you cannot assign a value of type " + TYPE_MANAGER.getTypeName(exprType)
                    + " to variable '" + identifier.getName()
                    + "' of type " + TYPE_MANAGER.getTypeName(identType);
            reportSemanticsError(statement.getLine(), statement.getColumn(), msg, new SemanticsException(msg));
        }

        return statement;
    }

    private GotoStatement gotoStatement(GotoStatement statement) {
        String line = statement.getGotoLine();
        if (!lineNumbers.contains(line)) {
            String msg = "undefined line number in goto: " + line;
            reportSemanticsError(statement.getLine(), statement.getColumn(), msg, new UndefinedException(msg, line));
        }
        return statement;
    }

    private PrintStatement printStatement(PrintStatement statement) {
        statement.getExpressions().forEach(this::expression);
        return statement;
    }

    private void expression(Expression expression) {
        if (expression instanceof BinaryExpression) {
            expression(((BinaryExpression) expression).getLeft());
            expression(((BinaryExpression) expression).getRight());
            checkType(expression);
        } else if (expression instanceof IntegerLiteral) {
            checkInteger((IntegerLiteral) expression);
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void checkInteger(IntegerLiteral integer) {
        String value = integer.getValue();
        try {
            Long.parseLong(value);
        } catch (NumberFormatException nfe) {
            String msg = "integer out of range: " + value;
            reportSemanticsError(integer.getLine(), integer.getColumn(), msg, new InvalidException(msg, value));
        }
    }

    private void checkType(Expression expression) {
        getType(expression);
    }

    private Type getType(Expression expression) {
        try {
            return TYPE_MANAGER.getType(expression);
        } catch (SemanticsException se) {
            reportSemanticsError(expression.getLine(), expression.getColumn(), se.getMessage(), se);
            return Unknown.INSTANCE;
        }
    }
}
