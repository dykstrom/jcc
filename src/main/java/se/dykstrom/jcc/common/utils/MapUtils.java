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

import java.util.HashMap;
import java.util.Map;

/**
 * Contains static utility methods related to maps.
 *
 * @author Johan Dykstrom
 */
public final class MapUtils {

    private MapUtils() { }

    /**
     * Creates a new map containing the given key-value pair.
     */
    public static <K, V> Map<K, V> of(K key, V value) {
        Map<K, V> map = new HashMap<>(1);
        map.put(key, value);
        return map;
    }

    /**
     * Creates a new map containing the given key-value pairs.
     */
    public static <K, V> Map<K, V> of(K key0, V value0, K key1, V value1) {
        Map<K, V> map = new HashMap<>(2);
        map.put(key0, value0);
        map.put(key1, value1);
        return map;
    }
}
