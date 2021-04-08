/*
 * Copyright (C) 2017 Johan Dykstrom
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

package se.dykstrom.jcc.common.utils;

import java.util.*;

/**
 * Contains static utility methods related to sets.
 *
 * @author Johan Dykstrom
 */
public final class SetUtils {

    private SetUtils() { }

    /**
     * Returns the minimum value in the given set of comparable values.
     *
     * @param set The set to examine.
     * @param <E> The type of the elements in the set.
     * @return The minimum value found.
     * @throws NoSuchElementException If the set is empty.
     */
    public static <E extends Comparable<E>> E min(Set<E> set) {
        return set.stream().min(Comparable::compareTo).orElseThrow(NoSuchElementException::new);
    }
}
