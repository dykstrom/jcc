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
 * A simple logger that logs messages to stdout if the verbose flag is set.
 *
 * @author Johan Dykstrom
 */
@SuppressWarnings("java:S106")
public final class VerboseLogger {

    private static boolean verbose;

    private VerboseLogger() { }

    /**
     * Set verbose flag on (true) or off (false).
     */
    public static void setVerbose(boolean verbose) {
        VerboseLogger.verbose = verbose;
    }

    /**
     * Logs the given message to stdout if the verbose flag is set (to true).
     */
    public static void log(String msg) {
        if (verbose) {
            System.out.println(msg);
        }
    }
}
