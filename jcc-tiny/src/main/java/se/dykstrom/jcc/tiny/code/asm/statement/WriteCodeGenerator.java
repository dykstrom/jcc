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

package se.dykstrom.jcc.tiny.code.asm.statement;

import se.dykstrom.jcc.common.ast.IdentifierNameExpression;
import se.dykstrom.jcc.common.code.AbstractCodeGeneratorComponent;
import se.dykstrom.jcc.common.code.Line;
import se.dykstrom.jcc.common.code.statement.StatementCodeGeneratorComponent;
import se.dykstrom.jcc.common.compiler.TypeManager;
import se.dykstrom.jcc.common.types.Identifier;
import se.dykstrom.jcc.common.types.Str;
import se.dykstrom.jcc.tiny.ast.WriteStatement;
import se.dykstrom.jcc.tiny.compiler.TinyCodeGenerator;

import java.util.List;

import static se.dykstrom.jcc.common.code.CodeContainer.withCodeContainer;
import static se.dykstrom.jcc.common.functions.LibcBuiltIns.CF_PRINTF_STR_VAR;
import static se.dykstrom.jcc.common.utils.AsmUtils.getComment;

public class WriteCodeGenerator extends AbstractCodeGeneratorComponent<TypeManager, TinyCodeGenerator>
        implements StatementCodeGeneratorComponent<WriteStatement> {

    private static final Identifier IDENT_FMT_PRINTF = new Identifier("_fmt_printf", Str.INSTANCE);
    private static final String VALUE_FMT_PRINTF = "\"%lld\",10,0";

    public WriteCodeGenerator(final TinyCodeGenerator codeGenerator) {
        super(codeGenerator);
    }

    @Override
    public List<Line> generate(final WriteStatement statement) {
        return withCodeContainer(cc -> {
            symbols().addConstant(IDENT_FMT_PRINTF, VALUE_FMT_PRINTF);

            final var fmtExpression = IdentifierNameExpression.from(statement, IDENT_FMT_PRINTF);
            statement.getExpressions().stream()
                    .map(expr -> List.of(fmtExpression, expr))
                    .map(args -> codeGenerator.functionCall(CF_PRINTF_STR_VAR, getComment(statement), args))
                    .forEach(cc::addAll);
        });
    }
}
