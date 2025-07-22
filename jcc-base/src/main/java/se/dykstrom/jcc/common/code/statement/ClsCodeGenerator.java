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

package se.dykstrom.jcc.common.code.statement;

import se.dykstrom.jcc.common.ast.ClsStatement;
import se.dykstrom.jcc.common.ast.Expression;
import se.dykstrom.jcc.common.ast.IdentifierNameExpression;
import se.dykstrom.jcc.common.code.Line;
import se.dykstrom.jcc.common.compiler.AbstractCodeGenerator;
import se.dykstrom.jcc.common.compiler.TypeManager;
import se.dykstrom.jcc.common.types.Identifier;
import se.dykstrom.jcc.common.types.Str;

import java.util.List;

import static se.dykstrom.jcc.common.code.CodeContainer.withCodeContainer;
import static se.dykstrom.jcc.common.functions.LibcBuiltIns.LF_PRINTF_STR_VAR;
import static se.dykstrom.jcc.common.utils.AsmUtils.getComment;

public class ClsCodeGenerator extends AbstractStatementCodeGenerator<ClsStatement, TypeManager, AbstractCodeGenerator> {

    public ClsCodeGenerator(final AbstractCodeGenerator codeGenerator) { super(codeGenerator); }

    @Override
    public List<Line> generate(final ClsStatement statement) {
        return withCodeContainer(cc -> {
            final var formatStringName = "_cls_ansi_codes";
            final var formatStringValue = "27,\"[2J\",27,\"[H\",0";
            final var formatStringIdentifier = new Identifier(formatStringName, Str.INSTANCE);
            symbols().addConstant(formatStringIdentifier, formatStringValue);

            final List<Expression> expressions = List.of(IdentifierNameExpression.from(statement, formatStringIdentifier));
            cc.addAll(codeGenerator.functionCall(LF_PRINTF_STR_VAR, getComment(statement), expressions));
        });
    }
}
