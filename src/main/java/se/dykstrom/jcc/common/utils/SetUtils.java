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

import java.util.HashSet;
import java.util.Set;

/**
 * Contains static utility methods related to sets.
 *
 * @author Johan Dykstrom
 */
public final class SetUtils {

    private SetUtils() { }

    /**
     * Creates a new set containing the given element.
     */
    public static <E> Set<E> of(E element) {
        Set<E> set = new HashSet<>(1);
        set.add(element);
        return set;
    }
}
