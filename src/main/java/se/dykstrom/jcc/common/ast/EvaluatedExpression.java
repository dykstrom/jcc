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

import se.dykstrom.jcc.common.storage.StorageLocation;
import se.dykstrom.jcc.common.types.Type;

import java.util.Objects;

/**
 * Represents an expression that has already been evaluated, and where the result of the
 * evaluation is stored in a storage location.
 *
 * @author Johan Dykstrom
 */
public class EvaluatedExpression extends Expression implements TypedExpression {

    private final Type type;
    private final StorageLocation location;

    public EvaluatedExpression(Type type, StorageLocation location) {
        super(0, 0);
        this.type = type;
        this.location = location;
    }

    @Override
    public String toString() {
        return "Pre-evaluated expression (" + location + ")";
    }

    @Override
    public Type getType() {
        return type;
    }

    /**
     * Returns the storage location that contains the result of the evaluated expression.
     */
    public StorageLocation getLocation() {
        return location;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EvaluatedExpression that = (EvaluatedExpression) o;
        return Objects.equals(type, that.type) &&
                Objects.equals(location, that.location);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, location);
    }
}
