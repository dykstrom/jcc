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
import se.dykstrom.jcc.common.types.Type;
import se.dykstrom.jcc.common.types.Unknown;

/**
 * A default type manager that can be used in untyped languages.
 *
 * @author Johan Dykstrom
 */
public class DefaultTypeManager implements TypeManager {

    public static final DefaultTypeManager INSTANCE = new DefaultTypeManager();

    @Override
    public String getTypeName(Type type) {
        return "unknown";
    }

    @Override
    public Type getType(Expression expression) {
        return Unknown.INSTANCE;
    }

    @Override
    public boolean isAssignableFrom(Type thisType, Type thatType) {
        return true;
    }
}
