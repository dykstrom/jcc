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

import se.dykstrom.jcc.common.ast.StringLiteral;
import se.dykstrom.jcc.common.code.Line;
import se.dykstrom.jcc.common.code.expression.ExpressionCodeGeneratorComponent;
import se.dykstrom.jcc.common.storage.StorageLocation;

import java.util.List;

/**
 * COL strings are implemented on the LLVM backend only: they need libjcccol and the garbage
 * collector, neither of which the FASM backend has. This overrides the generic string literal
 * generator that {@code AbstractCodeGenerator} installs, so a string-typed COL program is
 * rejected with a clear message instead of partially compiling. (The FASM backend is being
 * phased out.)
 */
public class StringLiteralCodeGenerator implements ExpressionCodeGeneratorComponent<StringLiteral> {

    @Override
    public List<Line> generate(final StringLiteral expression, final StorageLocation location) {
        throw new UnsupportedOperationException("strings are not supported by the FASM backend");
    }
}
