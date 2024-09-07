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

package se.dykstrom.jcc.col.code.statement;

import java.util.ArrayList;
import java.util.List;

import se.dykstrom.jcc.col.ast.PrintlnStatement;
import se.dykstrom.jcc.col.compiler.ColCodeGenerator;
import se.dykstrom.jcc.common.ast.Expression;
import se.dykstrom.jcc.common.ast.IdentifierNameExpression;
import se.dykstrom.jcc.common.code.AbstractCodeGeneratorComponent;
import se.dykstrom.jcc.common.code.statement.StatementCodeGeneratorComponent;
import se.dykstrom.jcc.common.compiler.TypeManager;
import se.dykstrom.jcc.common.code.Line;
import se.dykstrom.jcc.common.types.Identifier;
import se.dykstrom.jcc.common.types.Str;

import static se.dykstrom.jcc.common.functions.BuiltInFunctions.FUN_PRINTF;
import static se.dykstrom.jcc.common.code.CodeContainer.withCodeContainer;

public class PrintlnCodeGenerator extends AbstractCodeGeneratorComponent<TypeManager, ColCodeGenerator>
        implements StatementCodeGeneratorComponent<PrintlnStatement> {

    public PrintlnCodeGenerator(final ColCodeGenerator codeGenerator) {
        super(codeGenerator);
    }

    @Override
    public List<Line> generate(final PrintlnStatement statement) {
        return withCodeContainer(cc -> {
            String formatStringName = buildFormatStringName(statement.expression());
            String formatStringValue = buildFormatStringValue(statement.expression());
            Identifier formatStringIdentifier = new Identifier(formatStringName, Str.INSTANCE);
            symbols().addConstant(formatStringIdentifier, formatStringValue);

            List<Expression> expressions = new ArrayList<>(List.of(statement.expression()));
            expressions.add(0, IdentifierNameExpression.from(statement, formatStringIdentifier));
            cc.addAll(codeGenerator.functionCall(FUN_PRINTF, getComment(statement), expressions));
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
