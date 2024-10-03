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

package se.dykstrom.jcc.common.code.expression;

import se.dykstrom.jcc.common.ast.NegateExpression;
import se.dykstrom.jcc.common.code.CodeContainer;
import se.dykstrom.jcc.common.code.Line;
import se.dykstrom.jcc.common.compiler.AsmCodeGenerator;
import se.dykstrom.jcc.common.compiler.TypeManager;
import se.dykstrom.jcc.common.storage.StorageLocation;
import se.dykstrom.jcc.common.types.F64;
import se.dykstrom.jcc.common.types.Identifier;

import java.util.List;

import static se.dykstrom.jcc.common.utils.AsmUtils.getComment;

public class NegateCodeGenerator extends AbstractExpressionCodeGenerator<NegateExpression, TypeManager, AsmCodeGenerator> {

    private static final String SIGN_MASK_NAME = "_float_sign_mask";
    private static final String SIGN_MASK_VALUE = "8000000000000000h";
    private static final Identifier SIGN_MASK_IDENTIFIER = new Identifier(SIGN_MASK_NAME, F64.INSTANCE);

    public NegateCodeGenerator(final AsmCodeGenerator codeGenerator) { super(codeGenerator); }

    @Override
    public List<Line> generate(final NegateExpression expression, final StorageLocation location) {
        CodeContainer cc = new CodeContainer();

        final var arg = expression.getExpression();
        final var argType = types().getType(arg);
        // Generate code for sub expression, and store result in location
        cc.addAll(codeGenerator.expression(arg, location));
        // Generate code for negating sub expression, and store result in location
        cc.add(getComment(expression));
        if (argType instanceof F64) {
            // Add sign mask to symbol table
            symbols().addConstant(SIGN_MASK_IDENTIFIER, SIGN_MASK_VALUE);
            try (StorageLocation tmpLocation = codeGenerator.storageFactory().allocateVolatile(argType)) {
                tmpLocation.moveMemToThis(SIGN_MASK_IDENTIFIER.getMappedName(), cc);
                location.xorLocWithThis(tmpLocation, cc);
            }
        } else {
            location.negateThis(cc);
        }

        return cc.lines();
    }
}
