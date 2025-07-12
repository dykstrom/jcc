/*
 * Copyright (C) 2021 Johan Dykstrom
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

package se.dykstrom.jcc.basic.code.statement;

import se.dykstrom.jcc.basic.ast.PrintStatement;
import se.dykstrom.jcc.basic.compiler.BasicCodeGenerator;
import se.dykstrom.jcc.basic.compiler.BasicTypeManager;
import se.dykstrom.jcc.common.ast.Expression;
import se.dykstrom.jcc.common.ast.IdentifierNameExpression;
import se.dykstrom.jcc.common.code.Line;
import se.dykstrom.jcc.common.code.statement.AbstractStatementCodeGenerator;
import se.dykstrom.jcc.common.types.Identifier;
import se.dykstrom.jcc.common.types.Str;
import se.dykstrom.jcc.common.types.Type;

import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.joining;
import static se.dykstrom.jcc.common.code.CodeContainer.withCodeContainer;
import static se.dykstrom.jcc.common.functions.LibcBuiltIns.FUN_PRINTF_STR_VAR;
import static se.dykstrom.jcc.common.utils.AsmUtils.getComment;

public class PrintCodeGenerator extends AbstractStatementCodeGenerator<PrintStatement, BasicTypeManager, BasicCodeGenerator> {

    public PrintCodeGenerator(final BasicCodeGenerator codeGenerator) {
        super(codeGenerator);
    }

    @Override
    public List<Line> generate(PrintStatement statement) {
        return withCodeContainer(cc -> {
            String formatStringName = buildFormatStringName(statement.getExpressions());
            String formatStringValue = buildFormatStringValue(statement.getExpressions());
            Identifier formatStringIdentifier = new Identifier(formatStringName, Str.INSTANCE);
            symbols().addConstant(formatStringIdentifier, formatStringValue);

            List<Expression> expressions = new ArrayList<>(statement.getExpressions());
            expressions.addFirst(IdentifierNameExpression.from(statement, formatStringIdentifier));
            cc.addAll(codeGenerator.functionCall(FUN_PRINTF_STR_VAR, getComment(statement), expressions));
        });
    }

    private String buildFormatStringName(List<Expression> expressions) {
        return "_fmt_" + expressions.stream()
                .map(types()::getType)
                .map(Type::getName)
                .collect(joining("_"));
    }

    private String buildFormatStringValue(List<Expression> expressions) {
        return "\"" + expressions.stream()
                .map(types()::getType)
                .map(Type::getFormat)
                .collect(joining()) + "\",10,0";
    }
}
