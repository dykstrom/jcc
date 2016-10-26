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
import se.dykstrom.jcc.common.compiler.AbstractSemanticsParser;
import se.dykstrom.jcc.common.error.InvalidException;
import se.dykstrom.jcc.common.error.UndefinedException;
import se.dykstrom.jcc.common.symbols.SymbolTable;
import se.dykstrom.jcc.tiny.ast.AssignStatement;
import se.dykstrom.jcc.tiny.ast.ReadStatement;
import se.dykstrom.jcc.tiny.ast.WriteStatement;

/**
 * The semantics parser for the Tiny language.
 *
 * @author Johan Dykstrom
 */
class TinySemanticsParser extends AbstractSemanticsParser {

    private final SymbolTable symbols = new SymbolTable();

    public SymbolTable getSymbols() {
        return symbols;
    }

    public void program(Program program) {
        program.getStatements().forEach(this::statement);
    }

    private void statement(Statement statement) {
        if (statement instanceof AssignStatement) {
            assignStatement((AssignStatement) statement);
        } else if (statement instanceof ReadStatement) {
            readStatement((ReadStatement) statement);
        } else if (statement instanceof WriteStatement) {
            writeStatement((WriteStatement) statement);
        }
    }

    private void assignStatement(AssignStatement statement) {
        expression(statement.getExpression());
        symbols.add(statement.getIdentifier());
    }

    private void readStatement(ReadStatement statement) {
        statement.getIdentifiers().forEach(symbols::add);
    }

    private void writeStatement(WriteStatement statement) {
        statement.getExpressions().forEach(this::expression);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void expression(Expression expression) {
        if (expression instanceof AddExpression) {
            expression(((AddExpression) expression).getLeft());
            expression(((AddExpression) expression).getRight());
        } else if (expression instanceof IdentifierReferenceExpression) {
            IdentifierReferenceExpression ie = (IdentifierReferenceExpression) expression;
            String name = ie.getIdentifier().getName();
            if (!symbols.contains(name)) {
                String msg = "undefined identifier: " + name;
                reportSemanticsError(ie.getLine(), ie.getColumn(), msg, new UndefinedException(msg, name));
            }
        } else if (expression instanceof IntegerLiteral) {
            String value = ((IntegerLiteral) expression).getValue();
            try {
                Long.parseLong(value);
            } catch (NumberFormatException nfe) {
                String msg = "integer out of range: " + value;
                reportSemanticsError(expression.getLine(), expression.getColumn(), msg, new InvalidException(msg, value));
            }
        } else if (expression instanceof SubExpression) {
            expression(((SubExpression) expression).getLeft());
            expression(((SubExpression) expression).getRight());
        }
    }
}
