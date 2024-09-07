/*
 * Copyright (C) 2018 Johan Dykstrom
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

package se.dykstrom.jcc.common.compiler;

import se.dykstrom.jcc.common.ast.Expression;
import se.dykstrom.jcc.common.storage.RegisterStorageLocation;
import se.dykstrom.jcc.common.types.I64;
import se.dykstrom.jcc.common.types.Type;

/**
 * A default type manager that can be used in untyped languages. This type manager returns type I64
 * for all expressions, to be compatible with {@link RegisterStorageLocation#stores(Type)}.
 *
 * @author Johan Dykstrom
 */
public class DefaultTypeManager extends AbstractTypeManager {

    @Override
    public String getTypeName(Type type) {
        return "integer";
    }

    @Override
    public Type getType(Expression expression) {
        return I64.INSTANCE;
    }

    @Override
    public boolean isAssignableFrom(Type thisType, Type thatType) {
        return true;
    }
}
