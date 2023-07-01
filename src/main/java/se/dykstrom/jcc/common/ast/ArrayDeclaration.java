/*
 * Copyright (C) 2019 Johan Dykstrom
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

package se.dykstrom.jcc.common.ast;

import se.dykstrom.jcc.common.types.Arr;

import java.util.List;

/**
 * Represents a declaration of an array variable.
 *
 * @author Johan Dykstrom
 */
public class ArrayDeclaration extends Declaration {

    private final List<Expression> subscripts;

    public ArrayDeclaration(int line, int column, String name, Arr type, List<Expression> subscripts) {
        super(line, column, name, type);
        this.subscripts = subscripts;
        if (subscripts.size() != type.getDimensions()) {
            throw new IllegalArgumentException("number of subscripts (" + subscripts.size()
                    + ") != number of dimensions (" + type.getDimensions() + ")");
        }
    }

    public List<Expression> getSubscripts() {
        return subscripts;
    }

    /**
     * Returns a copy of this instance with the subscripts updated.
     * The original instance remains unchanged.
     */
    public ArrayDeclaration withSubscripts(final List<Expression> subscripts) {
        return new ArrayDeclaration(line(), column(), name(), (Arr) type(), subscripts);
    }
}
