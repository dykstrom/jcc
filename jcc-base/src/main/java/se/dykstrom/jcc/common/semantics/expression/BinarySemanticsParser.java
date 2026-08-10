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

package se.dykstrom.jcc.common.semantics.expression;

import se.dykstrom.jcc.common.ast.*;
import se.dykstrom.jcc.common.compiler.SemanticsParser;
import se.dykstrom.jcc.common.compiler.TypeManager;
import se.dykstrom.jcc.common.error.SemanticsException;
import se.dykstrom.jcc.common.semantics.AbstractSemanticsParserComponent;

import java.util.List;

import static java.util.Objects.requireNonNull;
import static se.dykstrom.jcc.common.compiler.AbstractTypeManager.canPromote;

/**
 * Type checks a binary expression: first what the operator demands of its operands, then the
 * promotion that makes two different operand types agree.
 * <p>
 * An operator is defined by the rules composed into it, not by a subclass. The {@code operation}
 * is the verb used in an error message ("cannot <em>add</em> string and i64"), the
 * {@link OperandTypeRule}s are what the operands must satisfy - all of them, with the first one
 * violated reporting - and the {@link OperandValueRule} covers the one demand that is about values
 * rather than types.
 * <pre>
 * new BinarySemanticsParser&lt;&gt;(this, "add", NUMERIC.or(STRINGS))       // adds or concatenates
 * new BinarySemanticsParser&lt;&gt;(this, "compare")                        // == and !=: any equal types
 * new BinarySemanticsParser&lt;&gt;(this, "mod", NON_ZERO_DIVISOR, INTEGER)
 * </pre>
 */
public class BinarySemanticsParser<T extends TypeManager> extends AbstractSemanticsParserComponent<T>
        implements ExpressionSemanticsParser<BinaryExpression> {

    protected final String operation;
    private final OperandValueRule valueRule;
    private final List<OperandTypeRule> typeRules;

    public BinarySemanticsParser(final SemanticsParser<T> semanticsParser,
                                 final String operation,
                                 final OperandTypeRule... typeRules) {
        this(semanticsParser, operation, OperandValueRule.ANY, typeRules);
    }

    public BinarySemanticsParser(final SemanticsParser<T> semanticsParser,
                                 final String operation,
                                 final OperandValueRule valueRule,
                                 final OperandTypeRule... typeRules) {
        super(semanticsParser);
        this.operation = requireNonNull(operation);
        this.valueRule = requireNonNull(valueRule);
        this.typeRules = List.of(typeRules);
    }

    @Override
    public Expression parse(final BinaryExpression expression) {
        final var left = parser.expression(expression.getLeft());
        final var right = parser.expression(expression.getRight());
        return checkType(expression.withLeft(left).withRight(right));
    }

    @Override
    protected Expression checkType(final Expression expression) {
        var e = (BinaryExpression) expression;
        checkOperandTypes(e);
        if (valueRule == OperandValueRule.NON_ZERO_DIVISOR) {
            e = checkDivisionByZero(e);
        }
        return promoteOperands(e);
    }

    /**
     * Reports the first rule the operands violate. Analysis continues either way, so that an
     * operand error and a follow-on promotion error are both reported in one compile.
     */
    private void checkOperandTypes(final BinaryExpression expression) {
        final var lt = getType(expression.getLeft());
        final var rt = getType(expression.getRight());
        typeRules.stream()
                 .filter(rule -> !rule.accepts(lt, rt))
                 .findFirst()
                 .ifPresent(rule -> {
                     final var msg = rule.message(new OperandTypeRule.Operands(expression, lt, rt, types(), operation));
                     reportError(expression, msg, new SemanticsException(msg));
                 });
    }

    /**
     * Makes operands of different types agree by inserting a widening cast. At the moment only
     * i32 to i64 and f32 to f64 can be promoted; operands that are already of the same type - two
     * strings, for example - need nothing.
     */
    private Expression promoteOperands(final BinaryExpression e) {
        final var left = e.getLeft();
        final var right = e.getRight();
        final var lt = getType(left);
        final var rt = getType(right);

        if (lt.equals(rt)) {
            return super.checkType(e);
        } else if (rt.isInteger() && canPromote(lt, rt)) {
            return super.checkType(e.withLeft(new CastToI64Expression(left.line(), left.column(), left)));
        } else if (lt.isInteger() && canPromote(rt, lt)) {
            return super.checkType(e.withRight(new CastToI64Expression(right.line(), right.column(), right)));
        } else if (rt.isFloat() && canPromote(lt, rt)) {
            return super.checkType(e.withLeft(new CastToF64Expression(left.line(), left.column(), left)));
        } else if (lt.isFloat() && canPromote(rt, lt)) {
            return super.checkType(e.withRight(new CastToF64Expression(right.line(), right.column(), right)));
        } else {
            final var msg = "cannot " + operation + " " + types().getTypeName(lt) + " and " + types().getTypeName(rt);
            reportError(e, msg, new SemanticsException(msg));
            return e;
        }
    }
}
