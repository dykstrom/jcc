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

package se.dykstrom.jcc.common.utils;

import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;

/**
 * Contains static utility methods related to strings, used to suggest a correction
 * for a name the user probably misspelled.
 *
 * @author Johan Dykstrom
 */
public final class StringUtils {

    /** The greatest edit distance at which {@link #findSimilar} still calls two names similar. */
    private static final int MAX_DISTANCE = 2;

    private StringUtils() { }

    /**
     * Returns the candidate most similar to {@code name}, or an empty optional if no candidate
     * is similar enough to be worth suggesting. Comparison is case-insensitive, and a candidate
     * is returned as spelled in {@code candidates}.
     */
    public static Optional<String> findSimilar(final String name, final Collection<String> candidates) {
        return candidates.stream()
                .filter(candidate -> distance(name, candidate) <= maxDistance(name))
                .min(Comparator.comparingInt(candidate -> distance(name, candidate)));
    }

    /**
     * Returns the Levenshtein distance between {@code one} and {@code two}, that is, the number of
     * single character insertions, deletions and substitutions needed to turn one into the other.
     * The comparison is case-insensitive.
     */
    public static int distance(final String one, final String two) {
        final var source = one.toLowerCase();
        final var target = two.toLowerCase();

        // Distance from the empty prefix of source to each prefix of target
        var previous = new int[target.length() + 1];
        for (int j = 0; j <= target.length(); j++) {
            previous[j] = j;
        }

        for (int i = 1; i <= source.length(); i++) {
            final var current = new int[target.length() + 1];
            current[0] = i;
            for (int j = 1; j <= target.length(); j++) {
                final var cost = (source.charAt(i - 1) == target.charAt(j - 1)) ? 0 : 1;
                current[j] = Math.min(Math.min(current[j - 1] + 1, previous[j] + 1), previous[j - 1] + cost);
            }
            previous = current;
        }

        return previous[target.length()];
    }

    /**
     * Short names must match more closely than long ones, or any two of them look similar.
     */
    private static int maxDistance(final String name) {
        return Math.min(MAX_DISTANCE, name.length() / 2);
    }
}
