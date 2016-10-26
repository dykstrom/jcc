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

import java.util.HashSet;
import java.util.Set;

/**
 * The semantics parser for the Basic language.
 *
 * @author Johan Dykstrom
 */
class BasicSemanticsParser extends AbstractSemanticsParser {

    private static final BasicTypeManager TYPE_MANAGER = new BasicTypeManager();

    private final Set<String> lineNumbers = new HashSet<>();

    public void program(Program program) {
        program.getStatements().forEach(this::saveLineNumber);
        program.getStatements().forEach(this::statement);
    }

    /**
     * Save line number of statement to set of line numbers, and check that there are no duplicates.
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

    private void statement(Statement statement) {
        if (statement instanceof GotoStatement) {
            gotoStatement((GotoStatement) statement);
        } else if (statement instanceof PrintStatement) {
            printStatement((PrintStatement) statement);
        }
    }

    private void gotoStatement(GotoStatement statement) {
        String line = statement.getGotoLine();
        if (!lineNumbers.contains(line)) {
            String msg = "undefined line number: " + line;
            reportSemanticsError(statement.getLine(), statement.getColumn(), msg, new UndefinedException(msg, line));
        }
    }

    private void printStatement(PrintStatement statement) {
        statement.getExpressions().forEach(this::expression);
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
        try {
            TYPE_MANAGER.getType(expression);
        } catch (SemanticsException se) {
            reportSemanticsError(expression.getLine(), expression.getColumn(), se.getMessage(), se);
        }
    }
}
