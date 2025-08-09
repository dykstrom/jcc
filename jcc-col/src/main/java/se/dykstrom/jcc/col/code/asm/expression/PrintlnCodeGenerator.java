/*
 * Copyright (C) 2025 Johan Dykstrom
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

package se.dykstrom.jcc.col.code.asm.expression;

import se.dykstrom.jcc.col.ast.expression.PrintlnExpression;
import se.dykstrom.jcc.col.compiler.ColCodeGenerator;
import se.dykstrom.jcc.common.ast.Expression;
import se.dykstrom.jcc.common.ast.IdentifierNameExpression;
import se.dykstrom.jcc.common.code.Line;
import se.dykstrom.jcc.common.code.expression.AbstractExpressionCodeGenerator;
import se.dykstrom.jcc.common.compiler.TypeManager;
import se.dykstrom.jcc.common.storage.StorageLocation;
import se.dykstrom.jcc.common.types.Identifier;
import se.dykstrom.jcc.common.types.Str;

import java.util.ArrayList;
import java.util.List;

import static se.dykstrom.jcc.common.code.CodeContainer.withCodeContainer;
import static se.dykstrom.jcc.common.functions.LibcBuiltIns.CF_PRINTF_STR_VAR;
import static se.dykstrom.jcc.common.utils.AsmUtils.getComment;

public class PrintlnCodeGenerator extends AbstractExpressionCodeGenerator<PrintlnExpression, TypeManager, ColCodeGenerator> {

    public PrintlnCodeGenerator(final ColCodeGenerator codeGenerator) { super(codeGenerator); }

    @Override
    public List<Line> generate(final PrintlnExpression expression, final StorageLocation location) {
        return withCodeContainer(cc -> {
            String formatStringName = buildFormatStringName(expression.getExpression());
            String formatStringValue = buildFormatStringValue(expression.getExpression());
            Identifier formatStringIdentifier = new Identifier(formatStringName, Str.INSTANCE);
            symbols().addConstant(formatStringIdentifier, formatStringValue);

            List<Expression> expressions = new ArrayList<>(List.of(expression.getExpression()));
            expressions.addFirst(IdentifierNameExpression.from(expression, formatStringIdentifier));
            cc.addAll(codeGenerator.functionCall(CF_PRINTF_STR_VAR, getComment(expression), expressions));
        });
    }

    private String buildFormatStringName(final Expression expression) {
        if (expression != null) {
            return "_fmt_" + types().getType(expression).getName();
        } else {
            return "_fmt_empty";
        }
    }

    private String buildFormatStringValue(final Expression expression) {
        if (expression != null) {
            return "\"" + types().getType(expression).getFormat() + "\",10,0";
        } else {
            return "\"\",10,0";
        }
    }
}
