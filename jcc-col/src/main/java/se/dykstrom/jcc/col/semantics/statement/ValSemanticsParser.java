/*
 * Copyright (C) 2026 Johan Dykstrom
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

package se.dykstrom.jcc.col.semantics.statement;

import se.dykstrom.jcc.col.ast.statement.ValDeclarationStatement;
import se.dykstrom.jcc.common.ast.CastToFloatExpression;
import se.dykstrom.jcc.common.ast.CastToIntExpression;
import se.dykstrom.jcc.common.ast.DeclarationAssignment;
import se.dykstrom.jcc.common.ast.Expression;
import se.dykstrom.jcc.common.ast.IdentifierDerefExpression;
import se.dykstrom.jcc.common.ast.Statement;
import se.dykstrom.jcc.common.compiler.SemanticsParser;
import se.dykstrom.jcc.common.compiler.TypeManager;
import se.dykstrom.jcc.common.error.AmbiguousException;
import se.dykstrom.jcc.common.error.DuplicateException;
import se.dykstrom.jcc.common.error.InvalidTypeException;
import se.dykstrom.jcc.common.error.InvalidValueException;
import se.dykstrom.jcc.common.error.SemanticsException;
import se.dykstrom.jcc.common.semantics.AbstractSemanticsParserComponent;
import se.dykstrom.jcc.common.semantics.VariableUsageTracker;
import se.dykstrom.jcc.common.semantics.statement.StatementSemanticsParser;
import se.dykstrom.jcc.common.types.AmbiguousType;
import se.dykstrom.jcc.common.types.Identifier;
import se.dykstrom.jcc.common.types.Type;
import se.dykstrom.jcc.common.types.Void;

import static se.dykstrom.jcc.common.compiler.AbstractTypeManager.canPromote;

public class ValSemanticsParser<T extends TypeManager> extends AbstractSemanticsParserComponent<T>
        implements StatementSemanticsParser<ValDeclarationStatement> {

    private final VariableUsageTracker usageTracker;

    public ValSemanticsParser(final SemanticsParser<T> semanticsParser, final VariableUsageTracker usageTracker) {
        super(semanticsParser);
        this.usageTracker = usageTracker;
    }

    @Override
    public Statement parse(final ValDeclarationStatement statement) {
        final var declaration = statement.declaration();
        final var name = declaration.name();

        checkBindOperator(statement, name);

        if (!checkInitializer(statement, declaration) || !checkName(statement, name)) {
            return statement;
        }

        final var declaredType = (declaration.type() != null) ? resolveType(statement, declaration.type(), types()) : null;
        final var expression = checkExpression(statement, parser.expression(declaration.expression()), declaredType);
        if (expression == null) {
            return statement;
        }
        final var type = (declaredType != null) ? declaredType : getType(expression);

        symbols().addValue(new Identifier(name, type));
        usageTracker.declare(name, statement);

        return statement.withDeclaration(declaration.withType(type).withExpression(expression));
    }

    private void checkBindOperator(final ValDeclarationStatement statement, final String name) {
        if (statement.usesEquals()) {
            final var msg = "COL uses ':=' for binding: write 'val " + name + " := " +
                            statement.declaration().expression() + "'";
            reportError(statement, msg, new SemanticsException(msg));
        }
    }

    private boolean checkInitializer(final ValDeclarationStatement statement, final DeclarationAssignment declaration) {
        if (declaration.expression() == null) {
            final var msg = "value '" + declaration.name() + "' must have an initializer";
            reportError(statement, msg, new InvalidValueException(msg, declaration.name()));
            return false;
        }
        return true;
    }

    private boolean checkName(final ValDeclarationStatement statement, final String name) {
        if (symbols().contains(name)) {
            final var msg = "value '" + name + "' is already defined, with type " +
                            types().getTypeName(symbols().getType(name));
            reportError(statement, msg, new DuplicateException(msg, name));
            return false;
        }
        if (symbols().containsFunction(name)) {
            final var msg = "value '" + name + "' is already defined as a function";
            reportError(statement, msg, new DuplicateException(msg, name));
            return false;
        }
        return true;
    }

    /**
     * Checks the initializer expression against the optional declared type, and returns
     * a possibly updated expression: an overloaded function reference is resolved using
     * the declared type, and a widening initializer is wrapped in a cast expression.
     * If the expression does not match the declared type, or its type cannot be
     * determined without a declared type, this method reports a semantics error
     * and returns {@code null}.
     */
    private Expression checkExpression(final ValDeclarationStatement statement,
                                       final Expression expression,
                                       final Type dt) {
        final var et = getType(expression);
        if (dt == null) {
            return checkInferredType(statement, expression, et);
        }
        if ((et instanceof AmbiguousType at) && at.contains(dt) && (expression instanceof IdentifierDerefExpression ide)) {
            // Use the declared type to resolve an overloaded function reference
            return ide.withIdentifier(ide.getIdentifier().withType(dt));
        }
        if (!dt.equals(et)) {
            if (canPromote(et, dt)) {
                return dt.isInteger()
                        ? new CastToIntExpression(expression.line(), expression.column(), expression, dt)
                        : new CastToFloatExpression(expression.line(), expression.column(), expression, dt);
            }
            final var name = statement.declaration().name();
            final var msg = "you cannot initialize value '" + name + "' of type " +
                            types().getTypeName(dt) + " with an expression of type " +
                            types().getTypeName(et);
            reportError(statement, msg, new InvalidTypeException(msg, et));
            return null;
        }
        return expression;
    }

    private Expression checkInferredType(final ValDeclarationStatement statement,
                                         final Expression expression,
                                         final Type expressionType) {
        final var name = statement.declaration().name();
        if (expressionType instanceof AmbiguousType) {
            final var msg = "ambiguous function reference in initializer of value '" + name +
                            "', possible types: " + types().getTypeName(expressionType);
            reportError(statement, msg, new AmbiguousException(msg, name));
            return null;
        }
        if (expressionType instanceof Void) {
            final var msg = "you cannot declare value '" + name + "' of type void";
            reportError(statement, msg, new InvalidTypeException(msg, expressionType));
            return null;
        }
        return expression;
    }
}
