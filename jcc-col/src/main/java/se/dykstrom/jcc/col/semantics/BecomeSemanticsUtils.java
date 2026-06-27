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

package se.dykstrom.jcc.col.semantics;

import se.dykstrom.jcc.col.ast.expression.BecomeExpression;
import se.dykstrom.jcc.col.ast.statement.FunCallStatement;
import se.dykstrom.jcc.col.ast.statement.ValDeclarationStatement;
import se.dykstrom.jcc.common.ast.BinaryExpression;
import se.dykstrom.jcc.common.ast.Expression;
import se.dykstrom.jcc.common.ast.FunctionCallExpression;
import se.dykstrom.jcc.common.ast.IfExpression;
import se.dykstrom.jcc.common.ast.Node;
import se.dykstrom.jcc.common.ast.Statement;
import se.dykstrom.jcc.common.ast.UnaryExpression;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

/**
 * Helpers for analysing {@link BecomeExpression}s during semantic analysis.
 */
public final class BecomeSemanticsUtils {

    private BecomeSemanticsUtils() { }

    /**
     * Reports an error for every become expression found in a top-level statement, since become is
     * only legal inside a function body. Top-level statements (val initializers, call-statement
     * arguments) reach this check via the expressions they carry. The reporter is invoked with the
     * offending node and the error message, mirroring {@code VariableUsageTracker.check}.
     */
    public static void checkNoTopLevelBecome(final List<Statement> statements, final BiConsumer<Node, String> errorReporter) {
        statements.stream()
                  .flatMap(BecomeSemanticsUtils::topLevelExpressions)
                  .flatMap(expression -> collectBecomes(expression).stream())
                  .forEach(become -> errorReporter.accept(become, "become is only allowed inside a function body"));
    }

    private static Stream<Expression> topLevelExpressions(final Statement statement) {
        return switch (statement) {
            case ValDeclarationStatement val when val.declaration().expression() != null -> Stream.of(val.declaration().expression());
            case FunCallStatement call -> Stream.of(call.expression());
            default -> Stream.empty();
        };
    }

    /**
     * Collects every become expression occurring anywhere within the given expression tree,
     * regardless of position.
     */
    private static List<BecomeExpression> collectBecomes(final Expression expression) {
        final var result = new ArrayList<BecomeExpression>();
        collectBecomes(expression, result);
        return result;
    }

    private static void collectBecomes(final Expression expression, final List<BecomeExpression> result) {
        if (expression == null) {
            return;
        }
        switch (expression) {
            case BecomeExpression be -> {
                result.add(be);
                collectBecomes(be.functionCall(), result);
            }
            case FunctionCallExpression fce -> fce.getArgs().forEach(arg -> collectBecomes(arg, result));
            case IfExpression ife -> {
                collectBecomes(ife.ifExpr(), result);
                collectBecomes(ife.thenExpr(), result);
                collectBecomes(ife.elseExpr(), result);
            }
            case BinaryExpression be -> {
                collectBecomes(be.getLeft(), result);
                collectBecomes(be.getRight(), result);
            }
            case UnaryExpression ue -> collectBecomes(ue.getExpression(), result);
            default -> { /* literals, identifiers: no nested expressions */ }
        }
    }
}
