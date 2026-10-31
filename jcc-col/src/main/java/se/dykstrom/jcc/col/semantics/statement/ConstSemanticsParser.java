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

import se.dykstrom.jcc.col.semantics.ColConstantEvaluator;
import se.dykstrom.jcc.col.semantics.ColConstantEvaluator.NonConstantException;
import se.dykstrom.jcc.common.ast.ConstDeclarationStatement;
import se.dykstrom.jcc.common.ast.DeclarationAssignment;
import se.dykstrom.jcc.common.ast.Expression;
import se.dykstrom.jcc.common.ast.Statement;
import se.dykstrom.jcc.common.compiler.SemanticsParser;
import se.dykstrom.jcc.common.compiler.TypeManager;
import se.dykstrom.jcc.common.error.InvalidTypeException;
import se.dykstrom.jcc.common.error.InvalidValueException;
import se.dykstrom.jcc.common.error.SemanticsException;
import se.dykstrom.jcc.common.semantics.VariableUsageTracker;
import se.dykstrom.jcc.common.semantics.statement.StatementSemanticsParser;
import se.dykstrom.jcc.common.types.Bool;
import se.dykstrom.jcc.common.types.Identifier;
import se.dykstrom.jcc.common.types.NumericType;
import se.dykstrom.jcc.common.types.Opaque;
import se.dykstrom.jcc.common.types.Str;
import se.dykstrom.jcc.common.types.Type;
import se.dykstrom.jcc.common.types.Unknown;

import java.util.Optional;

/**
 * Parses a const declaration, and evaluates its initializer. Unlike a val, a const is defined
 * in the root symbol table with its value, so it is visible in every function body, and code
 * generation emits it as a global constant.
 * <p>
 * Consts are parsed in pass 2, after pass 1 and before pass 3. Any conversion function
 * a type defines is then available to an initializer, and every function body and
 * top-level statement sees every const. An initializer sees only the consts declared above it.
 */
public class ConstSemanticsParser<T extends TypeManager> extends AbstractDeclarationSemanticsParser<T>
        implements StatementSemanticsParser<ConstDeclarationStatement> {

    private final VariableUsageTracker usageTracker;

    public ConstSemanticsParser(final SemanticsParser<T> semanticsParser, final VariableUsageTracker usageTracker) {
        super(semanticsParser, "constant");
        this.usageTracker = usageTracker;
    }

    @Override
    public Statement parse(final ConstDeclarationStatement statement) {
        final var declarations = statement.getDeclarations().stream()
                .map(declaration -> declaration(statement, declaration))
                .toList();
        return statement.withDeclarations(declarations);
    }

    private DeclarationAssignment declaration(final ConstDeclarationStatement statement,
                                              final DeclarationAssignment declaration) {
        final var name = declaration.name();

        checkBindOperator(statement, declaration);

        if (!checkInitializer(statement, declaration) || !checkName(statement, name)) {
            return declaration;
        }

        final var declaredType = (declaration.type() != null) ? resolveType(statement, declaration.type(), types()) : null;
        final var expression = checkExpression(statement, name, parser.expression(declaration.expression()), declaredType);
        // An initializer of unknown type has already been reported, and evaluating it would report it again
        if (expression == null || getType(expression).isUnknown()) {
            return failed(declaration, declaredType);
        }
        final var type = (declaredType != null) ? declaredType : getType(expression);
        if (!checkType(statement, name, type)) {
            return failed(declaration, declaredType);
        }

        return evaluate(statement, declaration.withType(type), expression)
                .orElseGet(() -> failed(declaration, declaredType));
    }

    /**
     * Evaluates the initializer, and defines the constant with its value. Returns the declaration
     * with the value as its expression, or an empty optional if the initializer cannot be evaluated.
     */
    private Optional<DeclarationAssignment> evaluate(final ConstDeclarationStatement statement,
                                                     final DeclarationAssignment declaration,
                                                     final Expression expression) {
        final var name = declaration.name();
        try {
            return new ColConstantEvaluator(symbols()).evaluate(expression).map(value -> {
                symbols().addConstant(new Identifier(name, declaration.type()), value.getValue());
                usageTracker.declare(name, declaration);
                return declaration.withExpression(value);
            });
        } catch (NonConstantException e) {
            final var msg = "constant '" + name + "' is defined with " + e.getMessage();
            reportError(statement, msg, new InvalidValueException(msg, name));
        } catch (InvalidValueException e) {
            final var msg = "cannot evaluate constant '" + name + "': " + e.getMessage();
            reportError(statement, msg, new InvalidValueException(msg, name));
        }
        return Optional.empty();
    }

    private void checkBindOperator(final ConstDeclarationStatement statement, final DeclarationAssignment declaration) {
        if (statement.usesEquals()) {
            final var msg = "COL uses ':=' for binding: write 'const " + declaration.name() + " := " +
                            declaration.expression() + "'";
            reportError(statement, msg, new SemanticsException(msg));
        }
    }

    private boolean checkType(final ConstDeclarationStatement statement, final String name, final Type type) {
        final var underlying = Opaque.unwrap(type);
        if (underlying instanceof NumericType || underlying instanceof Bool || underlying instanceof Str) {
            return true;
        }
        final var msg = "you cannot declare constant '" + name + "' of type " + types().getTypeName(type);
        reportError(statement, msg, new InvalidTypeException(msg, type));
        return false;
    }

    /**
     * Defines a constant whose initializer failed, so that a later use is not reported as an
     * undefined variable. A constant without a value tells the evaluator not to report it again.
     */
    private DeclarationAssignment failed(final DeclarationAssignment declaration, final Type declaredType) {
        final var type = (declaredType != null) ? declaredType : Unknown.INSTANCE;
        symbols().addConstant(new Identifier(declaration.name(), type), null);
        return declaration;
    }
}
