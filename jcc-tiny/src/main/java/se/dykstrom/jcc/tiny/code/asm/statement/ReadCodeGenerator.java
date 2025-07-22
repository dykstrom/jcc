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

import se.dykstrom.jcc.common.ast.Expression;
import se.dykstrom.jcc.common.ast.IdentifierNameExpression;
import se.dykstrom.jcc.common.code.AbstractCodeGeneratorComponent;
import se.dykstrom.jcc.common.code.Line;
import se.dykstrom.jcc.common.code.statement.StatementCodeGeneratorComponent;
import se.dykstrom.jcc.common.compiler.TypeManager;
import se.dykstrom.jcc.common.types.Identifier;
import se.dykstrom.jcc.common.types.Str;
import se.dykstrom.jcc.tiny.ast.ReadStatement;
import se.dykstrom.jcc.tiny.compiler.TinyCodeGenerator;

import java.util.List;

import static se.dykstrom.jcc.common.code.CodeContainer.withCodeContainer;
import static se.dykstrom.jcc.common.functions.LibcBuiltIns.LF_SCANF_STR_VAR;
import static se.dykstrom.jcc.common.utils.AsmUtils.getComment;

public class ReadCodeGenerator extends AbstractCodeGeneratorComponent<TypeManager, TinyCodeGenerator>
        implements StatementCodeGeneratorComponent<ReadStatement> {

    private static final Identifier IDENT_FMT_SCANF = new Identifier("_fmt_scanf", Str.INSTANCE);
    private static final String VALUE_FMT_SCANF = "\"%lld\",0";

    public ReadCodeGenerator(final TinyCodeGenerator codeGenerator) {
        super(codeGenerator);
    }

    @Override
    public List<Line> generate(final ReadStatement statement) {
        return withCodeContainer(cc -> {
            symbols().addConstant(IDENT_FMT_SCANF, VALUE_FMT_SCANF);

            final var fmtExpression = IdentifierNameExpression.from(statement, IDENT_FMT_SCANF);
            statement.getIdentifiers().forEach(identifier -> {
                symbols().addVariable(identifier);
                final var expression = IdentifierNameExpression.from(statement, identifier);
                final var args = List.<Expression>of(fmtExpression, expression);
                cc.addAll(codeGenerator.functionCall(LF_SCANF_STR_VAR, getComment(statement), args));
            });
        });
    }
}
