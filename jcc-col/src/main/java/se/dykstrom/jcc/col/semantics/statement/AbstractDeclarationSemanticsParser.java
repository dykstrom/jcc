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

import se.dykstrom.jcc.common.ast.DeclarationAssignment;
import se.dykstrom.jcc.common.ast.Expression;
import se.dykstrom.jcc.common.ast.IdentifierDerefExpression;
import se.dykstrom.jcc.common.ast.Node;
import se.dykstrom.jcc.common.compiler.SemanticsParser;
import se.dykstrom.jcc.common.compiler.TypeManager;
import se.dykstrom.jcc.common.error.AmbiguousException;
import se.dykstrom.jcc.common.error.DuplicateException;
import se.dykstrom.jcc.common.error.InvalidTypeException;
import se.dykstrom.jcc.common.error.InvalidValueException;
import se.dykstrom.jcc.common.semantics.AbstractSemanticsParserComponent;
import se.dykstrom.jcc.common.types.AmbiguousType;
import se.dykstrom.jcc.common.types.Type;
import se.dykstrom.jcc.common.types.Void;

import static se.dykstrom.jcc.common.compiler.AbstractTypeManager.canPromote;
import static se.dykstrom.jcc.common.compiler.AbstractTypeManager.promote;

/**
 * The checks that a val and a const declaration share. The kind - "value" or "constant" - names
 * the declaration in the error messages.
 */
abstract class AbstractDeclarationSemanticsParser<T extends TypeManager> extends AbstractSemanticsParserComponent<T> {

    private final String kind;

    protected AbstractDeclarationSemanticsParser(final SemanticsParser<T> semanticsParser, final String kind) {
        super(semanticsParser);
        this.kind = kind;
    }

    protected boolean checkInitializer(final Node statement, final DeclarationAssignment declaration) {
        if (declaration.expression() == null) {
            final var msg = kind + " '" + declaration.name() + "' must have an initializer";
            reportError(statement, msg, new InvalidValueException(msg, declaration.name()));
            return false;
        }
        return true;
    }

    protected boolean checkName(final Node statement, final String name) {
        if (symbols().contains(name)) {
            final var msg = kind + " '" + name + "' is already defined, with type " +
                            types().getTypeName(symbols().getType(name));
            reportError(statement, msg, new DuplicateException(msg, name));
            return false;
        }
        if (symbols().containsFunction(name)) {
            final var msg = kind + " '" + name + "' is already defined as a function";
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
    protected Expression checkExpression(final Node statement,
                                         final String name,
                                         final Expression expression,
                                         final Type declaredType) {
        final var et = getType(expression);
        if (declaredType == null) {
            return checkInferredType(statement, name, expression, et);
        }
        if ((et instanceof AmbiguousType at) && at.contains(declaredType) && (expression instanceof IdentifierDerefExpression ide)) {
            // Use the declared type to resolve an overloaded function reference
            return ide.withIdentifier(ide.getIdentifier().withType(declaredType));
        }
        if (et.isUnknown()) {
            // The initializer has already been reported, and what it should have been is not the
            // mistake the programmer made. The declaration still gets its declared type.
            return expression;
        }
        if (!declaredType.equals(et)) {
            if (canPromote(et, declaredType)) {
                // The same cast a widening function return gets, for the same reason; the guard is
                // explicit here because the alternative is an error rather than the expression
                return promote(expression, declaredType);
            }
            final var msg = "you cannot initialize " + kind + " '" + name + "' of type " +
                            types().getTypeName(declaredType) + " with an expression of type " +
                            types().getTypeName(et);
            reportError(statement, msg, new InvalidTypeException(msg, et));
            return null;
        }
        return expression;
    }

    private Expression checkInferredType(final Node statement,
                                         final String name,
                                         final Expression expression,
                                         final Type expressionType) {
        if (expressionType instanceof AmbiguousType) {
            final var msg = "ambiguous function reference in initializer of " + kind + " '" + name +
                            "', possible types: " + types().getTypeName(expressionType);
            reportError(statement, msg, new AmbiguousException(msg, name));
            return null;
        }
        if (expressionType instanceof Void) {
            final var msg = "you cannot declare " + kind + " '" + name + "' of type void";
            reportError(statement, msg, new InvalidTypeException(msg, expressionType));
            return null;
        }
        return expression;
    }
}
