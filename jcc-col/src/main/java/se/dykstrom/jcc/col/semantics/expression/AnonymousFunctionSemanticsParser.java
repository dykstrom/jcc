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

package se.dykstrom.jcc.col.semantics.expression;

import se.dykstrom.jcc.col.ast.expression.AnonymousFunctionExpression;
import se.dykstrom.jcc.col.semantics.LambdaLifter;
import se.dykstrom.jcc.col.semantics.TailPositionValidator;
import se.dykstrom.jcc.common.ast.Declaration;
import se.dykstrom.jcc.common.ast.Expression;
import se.dykstrom.jcc.common.ast.FunctionDefinitionStatement;
import se.dykstrom.jcc.common.ast.IdentifierDerefExpression;
import se.dykstrom.jcc.common.compiler.SemanticsParser;
import se.dykstrom.jcc.common.compiler.TypeManager;
import se.dykstrom.jcc.common.error.AmbiguousException;
import se.dykstrom.jcc.common.error.DuplicateException;
import se.dykstrom.jcc.common.error.InvalidTypeException;
import se.dykstrom.jcc.common.error.SemanticsException;
import se.dykstrom.jcc.common.semantics.AbstractSemanticsParserComponent;
import se.dykstrom.jcc.common.semantics.VariableUsageTracker;
import se.dykstrom.jcc.common.semantics.expression.ExpressionSemanticsParser;
import se.dykstrom.jcc.common.types.AmbiguousType;
import se.dykstrom.jcc.common.types.Fun;
import se.dykstrom.jcc.common.types.Identifier;
import se.dykstrom.jcc.common.types.Type;
import se.dykstrom.jcc.common.types.Void;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static se.dykstrom.jcc.common.error.Warning.UNUSED_VARIABLE;

/**
 * Type-checks an anonymous function and lifts it to a top-level function definition.
 * <p>
 * The body is checked in a scope built from the <em>global</em> symbol table, exactly as a named
 * function body is. That is what makes an anonymous function a plain function rather than a
 * closure: an enclosing function's parameters, and top-level vals, are simply not in scope, so
 * referencing one is an undefined-identifier error. Since nothing is captured, the function can be
 * lifted out as it stands, and the expression is replaced by a reference to the lifted function.
 *
 * @author Johan Dykstrom
 */
public class AnonymousFunctionSemanticsParser<T extends TypeManager> extends AbstractSemanticsParserComponent<T>
        implements ExpressionSemanticsParser<AnonymousFunctionExpression> {

    /** Names the anonymous function in error messages; its synthesized name would mean nothing. */
    private static final String DESCRIPTION = "the anonymous function";

    private final VariableUsageTracker usageTracker;
    private final LambdaLifter lambdaLifter;

    public AnonymousFunctionSemanticsParser(final SemanticsParser<T> semanticsParser,
                                            final VariableUsageTracker usageTracker,
                                            final LambdaLifter lambdaLifter) {
        super(semanticsParser);
        this.usageTracker = usageTracker;
        this.lambdaLifter = lambdaLifter;
    }

    @Override
    public Expression parse(final AnonymousFunctionExpression expression) {
        // The function scope is built from the global symbol table, so neither an enclosing
        // function's parameters nor top-level vals are visible inside the body
        return parser.withGlobalSymbolTable(() -> lift(expression));
    }

    private Expression lift(final AnonymousFunctionExpression expression) {
        final var declarations = expression.declarations();
        final var argTypes = declarations.stream()
                                         .map(d -> resolveType(expression, d.type(), types()))
                                         .toList();
        if (!checkParameterTypes(expression, declarations, argTypes)) {
            return expression;
        }

        final var parameterNames = addParameters(expression, declarations, argTypes);
        final var body = parser.expression(expression.expression());
        usageTracker.check(parameterNames, (n, m) -> reportWarning(n, m, UNUSED_VARIABLE));
        usageTracker.restore(parameterNames);

        final var returnType = resolveReturnType(expression, body);
        if (returnType == null) {
            return expression;
        }

        // Verify any become expressions in the body, just as for a named function
        new TailPositionValidator<>(parser, DESCRIPTION, returnType).check(body);

        final var identifier = new Identifier(lambdaLifter.nextName(), Fun.from(argTypes, returnType));
        lambdaLifter.add(new FunctionDefinitionStatement(
                expression.line(),
                expression.column(),
                identifier,
                updateDeclarations(declarations, argTypes),
                body
        ));
        // The lifted function is an ordinary user-defined function, so a reference to it by name
        // lowers to '@mangledName' with no further support from code generation
        return new IdentifierDerefExpression(expression.line(), expression.column(), identifier);
    }

    /**
     * Reports every parameter whose type was omitted, and returns {@code true} if there were none.
     * An omitted parameter type is resolved to void by the syntax visitor.
     */
    private boolean checkParameterTypes(final AnonymousFunctionExpression expression,
                                        final List<Declaration> declarations,
                                        final List<Type> argTypes) {
        boolean complete = true;
        for (int i = 0; i < argTypes.size(); i++) {
            if (argTypes.get(i) instanceof Void) {
                final var msg = "parameter '" + declarations.get(i).name() + "' must declare a type";
                reportError(expression, msg, new SemanticsException(msg));
                complete = false;
            }
        }
        return complete;
    }

    /**
     * Adds the formal parameters to the (function-local) symbol table, reporting duplicates,
     * and returns their names. Tracking state is saved first, so the caller can restore it
     * after checking the body for unused parameters.
     */
    private Set<String> addParameters(final AnonymousFunctionExpression expression,
                                      final List<Declaration> declarations,
                                      final List<Type> argTypes) {
        usageTracker.save();
        final Set<String> parameterNames = new HashSet<>();
        for (int i = 0; i < declarations.size(); i++) {
            final var declaration = declarations.get(i);
            final var name = declaration.name();
            if (parameterNames.contains(name)) {
                final var msg = "parameter '" + name + "' is already defined, with type " +
                                types().getTypeName(symbols().getType(name));
                reportError(expression, msg, new DuplicateException(msg, name));
            }
            parameterNames.add(name);
            symbols().addParameter(new Identifier(name, argTypes.get(i)));
            usageTracker.declare(name, declaration);
        }
        return parameterNames;
    }

    /**
     * Returns the return type of the anonymous function: the declared type if there is one, and
     * otherwise the type inferred from the body. Returns {@code null} if the return type is
     * invalid, or if the body does not match a declared return type; an error has then been
     * reported.
     */
    private Type resolveReturnType(final AnonymousFunctionExpression expression, final Expression body) {
        final var bodyType = getType(body);
        final var declaredType = (expression.returnType() != null)
                ? resolveType(expression, expression.returnType(), types())
                : null;

        if (declaredType == null) {
            // Type inference: the body is a single expression, so its type is the return type
            if (bodyType instanceof AmbiguousType) {
                final var msg = "ambiguous function reference in the body of an anonymous function, " +
                                "possible types: " + types().getTypeName(bodyType) +
                                ". Declare a return type to select one.";
                reportError(expression, msg, new AmbiguousException(msg, DESCRIPTION));
                return null;
            }
            if (bodyType instanceof Void) {
                final var msg = "cannot infer the return type of an anonymous function with a body of type void";
                reportError(expression, msg, new InvalidTypeException(msg, bodyType));
                return null;
            }
            return bodyType;
        }

        if (declaredType instanceof Void) {
            final var msg = "an anonymous function cannot have return type void";
            reportError(expression, msg, new InvalidTypeException(msg, declaredType));
            return null;
        }
        if (!types().isAssignableFrom(declaredType, bodyType)) {
            final var msg = "you cannot return a value of type " + types().getTypeName(bodyType) +
                            " from an anonymous function with return type " + types().getTypeName(declaredType);
            reportError(expression, msg, new InvalidTypeException(msg, bodyType));
            return null;
        }
        return declaredType;
    }

    private static List<Declaration> updateDeclarations(final List<Declaration> declarations, final List<Type> argTypes) {
        final var result = new ArrayList<Declaration>();
        for (int i = 0; i < declarations.size(); i++) {
            result.add(declarations.get(i).withType(argTypes.get(i)));
        }
        return result;
    }
}
