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
import se.dykstrom.jcc.col.semantics.ParameterBinder;
import se.dykstrom.jcc.col.semantics.TailPositionValidator;
import se.dykstrom.jcc.common.ast.Declaration;
import se.dykstrom.jcc.common.ast.Expression;
import se.dykstrom.jcc.common.ast.FunctionDefinitionStatement;
import se.dykstrom.jcc.common.ast.IdentifierDerefExpression;
import se.dykstrom.jcc.common.compiler.SemanticsParser;
import se.dykstrom.jcc.common.compiler.TypeManager;
import se.dykstrom.jcc.common.error.AmbiguousException;
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

import java.util.List;

import static se.dykstrom.jcc.common.compiler.AbstractTypeManager.promoteIfPossible;
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
    private final ParameterBinder<T> parameterBinder;

    public AnonymousFunctionSemanticsParser(final SemanticsParser<T> semanticsParser,
                                            final VariableUsageTracker usageTracker,
                                            final LambdaLifter lambdaLifter) {
        super(semanticsParser);
        this.usageTracker = usageTracker;
        this.lambdaLifter = lambdaLifter;
        this.parameterBinder = new ParameterBinder<>(semanticsParser, usageTracker);
    }

    @Override
    public Expression parse(final AnonymousFunctionExpression expression) {
        // The function scope is built from the global symbol table, so neither an enclosing
        // function's parameters nor top-level vals are visible inside the body
        return parser.withGlobalSymbolTable(() -> lift(expression));
    }

    private Expression lift(final AnonymousFunctionExpression expression) {
        final var declarations = updateDeclarations(expression, expression.declarations());
        final var argTypes = declarations.stream().map(Declaration::type).toList();
        if (!checkSignature(expression, declarations)) {
            return expression;
        }

        // Save current tracking state for unused variable checks
        usageTracker.save();
        final var parameterNames = parameterBinder.addParameters(expression, declarations);
        final var body = parser.expression(expression.expression());
        usageTracker.check(parameterNames, (n, m) -> reportWarning(n, m, UNUSED_VARIABLE));
        usageTracker.restore(parameterNames);

        final var returnType = resolveReturnType(expression, body);
        if (returnType == null) {
            return expression;
        }

        // Verify any become expressions in the body, just as for a named function
        new TailPositionValidator<>(parser, DESCRIPTION, returnType).check(body);

        // A body that merely widens to the declared return type needs the cast made explicit, or
        // the lifted function would return the body's own type; see the note in
        // FunDefPass2SemanticsParser on why this must follow the become check
        final var returnedBody = promoteIfPossible(body, getType(body), returnType);

        final var identifier = new Identifier(lambdaLifter.nextName(), Fun.from(argTypes, returnType));
        lambdaLifter.add(new FunctionDefinitionStatement(
                expression.line(),
                expression.column(),
                identifier,
                declarations,
                returnedBody
        ));
        // The lifted function is an ordinary user-defined function, so a reference to it by name
        // lowers to '@mangledName' with no further support from code generation
        return new IdentifierDerefExpression(expression.line(), expression.column(), identifier);
    }

    /**
     * Reports every parameter whose type was omitted, and returns {@code true} if there were none.
     * An omitted parameter type is resolved to void by the syntax visitor. The return type is not
     * checked here, since an anonymous function may leave it to be inferred from the body.
     */
    private boolean checkSignature(final AnonymousFunctionExpression expression,
                                   final List<Declaration> declarations) {
        boolean complete = true;
        for (final var declaration : declarations) {
            if (declaration.type() instanceof Void) {
                final var msg = "parameter '" + declaration.name() + "' must declare a type";
                reportError(expression, msg, new SemanticsException(msg));
                complete = false;
            }
        }
        return complete;
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

    /**
     * Returns the given declarations with their types resolved. The syntax visitor leaves a
     * declared parameter type as a {@link se.dykstrom.jcc.common.types.NamedType}, so resolving it
     * once here means everything downstream — the signature check, the symbol table, the lifted
     * function definition and its {@link Fun} type — works from real types rather than names.
     */
    private List<Declaration> updateDeclarations(final AnonymousFunctionExpression expression,
                                                 final List<Declaration> declarations) {
        return declarations.stream()
                           .map(d -> d.withType(resolveType(expression, d.type(), types())))
                           .toList();
    }
}
