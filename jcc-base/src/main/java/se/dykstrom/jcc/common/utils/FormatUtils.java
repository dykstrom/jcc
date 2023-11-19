/*
 * Copyright (C) 2016 Johan Dykstrom
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

/**
 * Contains static utility methods related to formatting and parsing.
 *
 * @author Johan Dykstrom
 */
public final class FormatUtils {

    /** Platform specific end-of-line string. */
    public static final String EOL = System.lineSeparator();

    private FormatUtils() { }

    public static String normalizeFloatNumber(final String sign,
                                              final String number,
                                              final String exponent,
                                              final String exponentSign,
                                              final String exponentIndicator) {
        return normalizeSign(sign) + normalizeNumber(number) + normalizeExponent(exponent, exponentSign, exponentIndicator);
    }

    private static String normalizeSign(final String sign) {
        return sign == null ? "" : sign;
    }

    public static String normalizeNumber(final String number) {
        final var builder = new StringBuilder();
        if (number.startsWith(".")) {
            builder.append("0");
        }
        builder.append(number);
        if (number.endsWith(".")) {
            builder.append("0");
        } else if (!number.contains(".")) {
            builder.append(".0");
        }
        return builder.toString();
    }

    public static String normalizeExponent(final String exponent,
                                           final String exponentSign,
                                           final String exponentIndicator) {
        if (exponent == null) {
            return "";
        } else {
            final var builder = new StringBuilder();
            builder.append(exponentIndicator);
            if (exponentSign == null) {
                builder.append("+");
            }
            builder.append(exponent.substring(1));
            return builder.toString();
        }
    }
}
