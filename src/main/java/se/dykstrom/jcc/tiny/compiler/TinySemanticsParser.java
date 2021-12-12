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
        if (statement instanceof AssignStatement assignStatement) {
            assignStatement(assignStatement);
        } else if (statement instanceof ReadStatement readStatement) {
            readStatement(readStatement);
        } else if (statement instanceof WriteStatement writeStatement) {
            writeStatement(writeStatement);
        }
    }

    private void assignStatement(AssignStatement statement) {
        expression(statement.getLhsExpression());
        expression(statement.getRhsExpression());
    }

    private void readStatement(ReadStatement statement) {
        statement.getIdentifiers().forEach(symbols::addVariable);
    }

    private void writeStatement(WriteStatement statement) {
        statement.getExpressions().forEach(this::expression);
    }

    private void expression(Expression expression) {
        if (expression instanceof BinaryExpression binaryExpression) {
            expression(binaryExpression.getLeft());
            expression(binaryExpression.getRight());
        } else if (expression instanceof IdentifierDerefExpression ide) {
            String name = ide.getIdentifier().getName();
            if (!symbols.contains(name)) {
                String msg = "undefined identifier: " + name;
                reportSemanticsError(ide.line(), ide.column(), msg, new UndefinedException(msg, name));
            }
        } else if (expression instanceof IdentifierNameExpression ine) {
            symbols.addVariable(ine.getIdentifier());
        } else if (expression instanceof IntegerLiteral integerLiteral) {
            String value = integerLiteral.getValue();
            try {
                Long.parseLong(value);
            } catch (NumberFormatException nfe) {
                String msg = "integer out of range: " + value;
                reportSemanticsError(expression.line(), expression.column(), msg, new InvalidException(msg, value));
            }
        }
    }
}
