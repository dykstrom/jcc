/*
 * Copyright (C) 2024 Johan Dykstrom
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

import se.dykstrom.jcc.col.ast.expression.BecomeExpression;
import se.dykstrom.jcc.common.ast.BinaryExpression;
import se.dykstrom.jcc.common.ast.CastToFloatExpression;
import se.dykstrom.jcc.common.ast.CastToIntExpression;
import se.dykstrom.jcc.common.ast.Expression;
import se.dykstrom.jcc.common.ast.FunctionCallExpression;
import se.dykstrom.jcc.common.ast.FunctionDefinitionStatement;
import se.dykstrom.jcc.common.ast.IfExpression;
import se.dykstrom.jcc.common.ast.Statement;
import se.dykstrom.jcc.common.ast.UnaryExpression;
import se.dykstrom.jcc.common.compiler.SemanticsParser;
import se.dykstrom.jcc.common.compiler.TypeManager;
import se.dykstrom.jcc.common.error.DuplicateException;
import se.dykstrom.jcc.common.error.InvalidTypeException;
import se.dykstrom.jcc.common.error.SemanticsException;
import se.dykstrom.jcc.common.functions.UserDefinedFunction;
import se.dykstrom.jcc.common.semantics.AbstractSemanticsParserComponent;
import se.dykstrom.jcc.common.semantics.VariableUsageTracker;
import se.dykstrom.jcc.common.semantics.statement.StatementSemanticsParser;
import se.dykstrom.jcc.common.types.Fun;
import se.dykstrom.jcc.common.types.Identifier;
import se.dykstrom.jcc.common.types.Type;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Map.entry;

import static se.dykstrom.jcc.common.error.Warning.UNUSED_VARIABLE;

public class FunDefPass2SemanticsParser<T extends TypeManager> extends AbstractSemanticsParserComponent<T>
        implements StatementSemanticsParser<FunctionDefinitionStatement> {

    private final VariableUsageTracker usageTracker;

    public FunDefPass2SemanticsParser(final SemanticsParser<T> semanticsParser, final VariableUsageTracker usageTracker) {
        super(semanticsParser);
        this.usageTracker = usageTracker;
    }

    @Override
    public Statement parse(final FunctionDefinitionStatement statement) {
        // The function scope is built from the global symbol table so that
        // top-level vals are not visible inside the function body
        return parser.withGlobalSymbolTable(() -> {
            final var functionName = statement.identifier().name();
            final var declarations = statement.declarations();

            // Save current tracking state for unused variable checks
            usageTracker.save();

            // Add formal arguments to local symbol table
            // Note: We only support scalar arguments for now
            final Set<String> parameterNames = new HashSet<>();
            declarations.forEach(d -> {
                final var name = d.name();
                if (parameterNames.contains(name)) {
                    final var msg = "parameter '" + name + "' is already defined, with type " +
                                    types().getTypeName(symbols().getType(name));
                    reportError(statement, msg, new DuplicateException(msg, name));
                }
                parameterNames.add(name);
                symbols().addParameter(new Identifier(name, d.type()));
                usageTracker.declare(name, d);
            });

            // Check and update expression
            final var expression = parser.expression(statement.expression());
            // Check for unused parameters (globals are checked at the top level, issue #78)
            usageTracker.check(parameterNames, (n, m) -> reportWarning(n, m, UNUSED_VARIABLE));
            // Restore tracking state
            usageTracker.restore(parameterNames);

            // Check that expression type matches return type
            final var expressionType = getType(expression);
            final var returnType = ((Fun) statement.identifier().type()).getReturnType();
            if (!types().isAssignableFrom(returnType, expressionType)) {
                final var msg = "you cannot return a value of type " + types().getTypeName(expressionType) +
                                " from function '" + functionName + "' with return type " + types().getTypeName(returnType);
                reportError(statement, msg, new InvalidTypeException(msg, expressionType));
            }

            // Verify any become expressions: they must be in tail position, tail-call a
            // user-defined function, and return exactly the function's return type
            new TailPositionValidator(functionName, returnType).check(expression, true, null);

            // The types were resolved and the function was added to the symbol table in pass 1,
            // so we just return the statement with the updated expression
            return statement.withExpression(expression);
        });
    }

    /**
     * Walks one function body, validating its become expressions against the tail-position rules.
     * The enclosing function's name and return type are held as fields so the recursive walk only
     * threads what varies: whether the current position is a tail position, and the name of the
     * construct that would consume a non-tail result.
     *
     * <p>Tail position is the body expression itself and the then/else branches of an
     * if-expression that is itself in tail position. Implicit widening casts (inserted by
     * if-branch promotion) are transparent here so the underlying become is still validated, which
     * surfaces the rule-3 widening error rather than a misleading "consumed by a cast" message.
     * Explicit casts and the cast built-ins remain function calls at this stage, so a become used
     * by one is correctly an argument, i.e. not in tail position.
     */
    private final class TailPositionValidator {

        /** Operator symbols used to name the construct that consumes a non-tail become's result. */
        private static final Map<String, String> OPERATOR_SYMBOLS = Map.ofEntries(
                entry("AddExpression", "+"),
                entry("SubExpression", "-"),
                entry("MulExpression", "*"),
                entry("DivExpression", "/"),
                entry("IDivExpression", "div"),
                entry("ModExpression", "mod"),
                entry("NegateExpression", "-"),
                entry("AndExpression", "&"),
                entry("OrExpression", "|"),
                entry("XorExpression", "^"),
                entry("NotExpression", "~"),
                entry("LogicalAndExpression", "and"),
                entry("LogicalOrExpression", "or"),
                entry("LogicalXorExpression", "xor"),
                entry("LogicalNotExpression", "not"),
                entry("EqualExpression", "=="),
                entry("NotEqualExpression", "!="),
                entry("GreaterExpression", ">"),
                entry("GreaterOrEqualExpression", ">="),
                entry("LessExpression", "<"),
                entry("LessOrEqualExpression", "<=")
        );

        private final String functionName;
        private final Type returnType;

        private TailPositionValidator(final String functionName, final Type returnType) {
            this.functionName = functionName;
            this.returnType = returnType;
        }

        private void check(final Expression expression, final boolean inTail, final String consumer) {
            switch (expression) {
                case CastToIntExpression ce -> check(ce.getExpression(), inTail, consumer);
                case CastToFloatExpression ce -> check(ce.getExpression(), inTail, consumer);
                case BecomeExpression be -> checkBecome(be, inTail, consumer);
                case IfExpression ie -> checkIf(ie, inTail, consumer);
                case FunctionCallExpression fce -> checkArguments(fce.getArgs());
                case BinaryExpression be -> checkBinary(be);
                case UnaryExpression ue -> check(ue.getExpression(), false, operator(ue));
                default -> { /* literals and identifier dereferences contain no become expressions */ }
            }
        }

        private void checkBecome(final BecomeExpression become, final boolean inTail, final String consumer) {
            if (inTail) {
                validate(become);
            } else {
                final var msg = "become is not in tail position: its result is used by " + consumer +
                        ". The tail call must be the function's final action — consider an accumulator parameter.";
                reportError(become, msg, new SemanticsException(msg));
            }
            // Arguments are evaluated before the call, so they are never in tail position
            checkArguments(become.functionCall().getArgs());
        }

        private void checkIf(IfExpression ie, boolean inTail, String consumer) {
            check(ie.ifExpr(), false, "the condition of an if-expression");
            check(ie.thenExpr(), inTail, consumer);
            check(ie.elseExpr(), inTail, consumer);
        }

        private void checkArguments(final List<Expression> arguments) {
            arguments.forEach(argument -> check(argument, false, "a function-call argument"));
        }

        private void checkBinary(final BinaryExpression be) {
            check(be.getLeft(), false, operator(be));
            check(be.getRight(), false, operator(be));
        }

        private void validate(final BecomeExpression become) {
            final var function = become.functionCall().function();
            if (function == null) {
                return; // The call was not resolved; an error has already been reported
            }
            // Rule 4: become may only tail-call user-defined functions, not externals or built-ins
            if (!(function instanceof UserDefinedFunction)) {
                final var msg = "become can only tail-call a user-defined function, not '" + function.getName() +
                        "' which is an external or built-in function";
                reportError(become, msg, new SemanticsException(msg));
                return;
            }
            // Rule 3: the callee's return type must exactly equal the enclosing function's return type;
            // an implicit widening would be a sext/fpext that runs after the call, destroying tail position
            final var calleeReturnType = function.getReturnType();
            if (!calleeReturnType.equals(returnType)) {
                final var msg = "tail call returns " + types().getTypeName(calleeReturnType) + " but function '" +
                        functionName + "' returns " + types().getTypeName(returnType) +
                        "; the implicit widening would run after the call. Declare matching return types.";
                reportError(become, msg, new InvalidTypeException(msg, calleeReturnType));
            }
        }

        private static String operator(final Expression expression) {
            final var symbol = OPERATOR_SYMBOLS.get(expression.getClass().getSimpleName());
            return symbol != null ? "'" + symbol + "'" : "an enclosing operator";
        }
    }
}
