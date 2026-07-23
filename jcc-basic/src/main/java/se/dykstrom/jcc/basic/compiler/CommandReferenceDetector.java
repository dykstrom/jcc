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

package se.dykstrom.jcc.basic.compiler;

import se.dykstrom.jcc.basic.ast.statement.PrintStatement;
import se.dykstrom.jcc.basic.ast.statement.RandomizeStatement;
import se.dykstrom.jcc.common.ast.ArrayAccessExpression;
import se.dykstrom.jcc.common.ast.AssignStatement;
import se.dykstrom.jcc.common.ast.BinaryExpression;
import se.dykstrom.jcc.common.ast.Expression;
import se.dykstrom.jcc.common.ast.FunctionCallExpression;
import se.dykstrom.jcc.common.ast.FunctionDefinitionStatement;
import se.dykstrom.jcc.common.ast.IfStatement;
import se.dykstrom.jcc.common.ast.LabelledStatement;
import se.dykstrom.jcc.common.ast.Statement;
import se.dykstrom.jcc.common.ast.UnaryExpression;
import se.dykstrom.jcc.common.ast.WhileStatement;

import java.util.List;

/**
 * Detects whether a BASIC program references the {@code command$} built-in function.
 * Used to decide whether the generated main function must initialize the command line.
 *
 * @author Johan Dykstrom
 */
public final class CommandReferenceDetector {

    private CommandReferenceDetector() { }

    /**
     * Returns {@code true} if the program references the command$ function anywhere, including inside
     * user-defined functions. Used to decide whether main must initialize the command line.
     */
    public static boolean referencesCommand(final List<Statement> statements) {
        // statements is null for an expression-defined function (DEF FN), which has no statement body
        return statements != null && statements.stream().anyMatch(CommandReferenceDetector::referencesCommand);
    }

    private static boolean referencesCommand(final Statement statement) {
        return switch (statement) {
            case PrintStatement s -> s.getExpressions().stream().anyMatch(CommandReferenceDetector::referencesCommand);
            case AssignStatement s -> referencesCommand(s.getLhsExpression()) || referencesCommand(s.getRhsExpression());
            case IfStatement s -> ifReferencesCommand(s);
            case WhileStatement s -> whileReferencesCommand(s);
            case LabelledStatement s -> referencesCommand(s.statement());
            case FunctionDefinitionStatement s -> functionReferencesCommand(s);
            case RandomizeStatement s -> referencesCommand(s.getExpression());
            default -> false;
        };
    }

    private static boolean ifReferencesCommand(final IfStatement statement) {
        return referencesCommand(statement.getExpression())
                || referencesCommand(statement.getThenStatements())
                || referencesCommand(statement.getElseStatements());
    }

    private static boolean whileReferencesCommand(final WhileStatement statement) {
        return referencesCommand(statement.getExpression()) || referencesCommand(statement.getStatements());
    }

    private static boolean functionReferencesCommand(final FunctionDefinitionStatement statement) {
        return referencesCommand(statement.expression()) || referencesCommand(statement.statements());
    }

    private static boolean referencesCommand(final Expression expression) {
        return switch (expression) {
            case FunctionCallExpression e -> e.getIdentifier().name().equals(BasicSymbols.BF_COMMAND.getName())
                    || e.getArgs().stream().anyMatch(CommandReferenceDetector::referencesCommand);
            case BinaryExpression e -> referencesCommand(e.getLeft()) || referencesCommand(e.getRight());
            case UnaryExpression e -> referencesCommand(e.getExpression());
            case ArrayAccessExpression e -> e.getSubscripts().stream().anyMatch(CommandReferenceDetector::referencesCommand);
            case null, default -> false;
        };
    }
}
