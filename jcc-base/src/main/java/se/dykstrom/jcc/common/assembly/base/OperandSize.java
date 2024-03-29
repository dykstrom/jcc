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

package se.dykstrom.jcc.common.assembly.base;

import java.util.function.Function;

/**
 * Enumerates all supported operand sizes.
 *
 * @author Johan Dykstrom
 */
public enum OperandSize {

    BYTE(Byte::valueOf);

    /** Validation function. */
    private final Function<String, Number> validator;

    OperandSize(Function<String, Number> validator) {
        this.validator = validator;
    }

    @Override
    public String toString() {
        return name().toLowerCase();
    }

    /**
     * Validates that the given value actually fits in this operand.
     */
    public void validate(String value) {
        if (value.endsWith("h")) {
            value = Long.valueOf(value.substring(0, value.length() - 1), 16).toString();
        }
        validator.apply(value);
    }
}
