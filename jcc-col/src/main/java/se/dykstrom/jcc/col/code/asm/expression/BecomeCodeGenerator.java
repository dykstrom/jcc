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

package se.dykstrom.jcc.col.code.asm.expression;

import se.dykstrom.jcc.col.ast.expression.BecomeExpression;
import se.dykstrom.jcc.common.code.Line;
import se.dykstrom.jcc.common.code.expression.ExpressionCodeGeneratorComponent;
import se.dykstrom.jcc.common.storage.StorageLocation;

import java.util.List;

/**
 * The FASM backend does not implement guaranteed tail calls. Since {@code become} is only useful
 * with {@code musttail}, which the FASM backend cannot emit, it is rejected here with a clear
 * message rather than generating an unguaranteed call. (The FASM backend is being phased out.)
 */
public class BecomeCodeGenerator implements ExpressionCodeGeneratorComponent<BecomeExpression> {

    @Override
    public List<Line> generate(final BecomeExpression expression, final StorageLocation location) {
        throw new UnsupportedOperationException("become is not supported by the FASM backend");
    }
}
