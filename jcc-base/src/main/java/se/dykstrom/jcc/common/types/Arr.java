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

package se.dykstrom.jcc.common.types;

import java.util.Objects;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.joining;

/**
 * Represents the array type. Array types are parameterized by their number of dimensions and element type.
 * Instances of class {@code Arr} are created by calling the static factory method {@link #from(int, Type)}.
 *
 * @author Johan Dykstrom
 */
public class Arr extends AbstractType {

    /** The instance that represents the type in general, and not the type of any specific array. */
    public static final Arr INSTANCE = new Arr(0, Void.INSTANCE);

    public static final String SUFFIX = "_arr";

    private final int dimensions;
    private final Type elementType;

    /**
     * Creates a new array type representing an array with the given dimensions and element type.
     */
    private Arr(int dimensions, Type elementType) {
        this.dimensions = dimensions;
        this.elementType = elementType;
    }

    /**
     * Returns an {@code Arr} instance that represents an array type with the given {@code dimensions},
     * having elements of type {@code elementType}.
     */
    public static Arr from(int dimensions, Type elementType) {
        return new Arr(dimensions, elementType);
    }

    @Override
    public String toString() {
        return elementType + toString(dimensions);
    }

    private String toString(int dimensions) {
        return IntStream.range(0, dimensions).mapToObj(i -> "[]").collect(joining());
    }

    /**
     * Returns the dimensions of this array type.
     */
    public int getDimensions() {
        return dimensions;
    }
    
    /**
     * Returns the element type of this array type.
     */
    public Type getElementType() {
        return elementType;
    }

    @Override
    public String llvmName() {
        throw new UnsupportedOperationException("array");
    }

    @Override
    public String getDefaultValue() {
        throw new UnsupportedOperationException("array");
    }

    @Override
    public String getFormat() {
        throw new UnsupportedOperationException("array");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Arr that = (Arr) o;
        return dimensions == that.dimensions && Objects.equals(elementType, that.elementType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dimensions, elementType);
    }
}
