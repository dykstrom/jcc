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

package se.dykstrom.jcc.col.semantics;

import se.dykstrom.jcc.col.types.ColTypeManager;
import se.dykstrom.jcc.col.types.NamedType;
import se.dykstrom.jcc.common.ast.BinaryExpression;
import se.dykstrom.jcc.common.ast.Expression;
import se.dykstrom.jcc.common.ast.Node;
import se.dykstrom.jcc.common.compiler.SemanticsParser;
import se.dykstrom.jcc.common.compiler.TypeManager;
import se.dykstrom.jcc.common.error.InvalidValueException;
import se.dykstrom.jcc.common.error.SemanticsException;
import se.dykstrom.jcc.common.error.UndefinedException;
import se.dykstrom.jcc.common.symbols.SymbolTable;
import se.dykstrom.jcc.common.types.I64;
import se.dykstrom.jcc.common.types.Type;
import se.dykstrom.jcc.common.utils.ExpressionUtils;

import java.util.stream.Stream;

public abstract class AbstractSemanticsParserComponent<T extends TypeManager, P extends SemanticsParser<T>> {

    protected final P parser;

    protected AbstractSemanticsParserComponent(final P semanticsParser) {
        this.parser = semanticsParser;
    }

    protected T types() { return parser.types(); }

    protected SymbolTable symbols() { return parser.symbols(); }

    /**
     * Resolves the given type from the type name if it is a {@link NamedType}.
     * Otherwise, just returns the type as is. If the type name is unknown, and
     * the type cannot be resolved, this method reports a semantics error, and
     * returns the given type.
     */
    protected Type resolveType(final Node node, final Type type, final ColTypeManager typeManager) {
        if (type instanceof NamedType namedType) {
            final var typeName = namedType.getName();
            final var optionalType = typeManager.getTypeFromName(typeName);
            if (optionalType.isPresent()) {
                return optionalType.get();
            }
            final var msg = "undefined type: " + typeName;
            reportSemanticsError(node, msg, new UndefinedException(msg, typeName));
        }
        return type;
    }

    protected Type getType(final Expression expression) {
        try {
            return types().getType(expression);
        } catch (SemanticsException se) {
            reportSemanticsError(expression, se.getMessage(), se);
            return I64.INSTANCE;
        }
    }

    /**
     * Returns true if any of the types in actualTypes does not match the expectedType.
     */
    protected static boolean isTypeMismatch(final Class<? extends Type> expectedType, final Type... actualTypes) {
        return !Stream.of(actualTypes).allMatch(expectedType::isInstance);
    }

    protected Expression checkType(final Expression expression) {
        getType(expression);
        return expression;
    }

    protected BinaryExpression checkDivisionByZero(final BinaryExpression expression) {
        try {
            return ExpressionUtils.checkDivisionByZero(expression);
        } catch (InvalidValueException e) {
            reportSemanticsError(expression, e.getMessage(), e);
            return expression;
        }
    }

    /**
     * Reports a semantics error for the given AST node.
     */
    protected void reportSemanticsError(final Node node, final String msg, final SemanticsException exception) {
        parser.reportError(node.line(), node.column(), msg, exception);
    }
}
