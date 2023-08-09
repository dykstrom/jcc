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

import java.nio.file.Path;

/**
 * Contains static utility methods related to files.
 *
 * @author Johan Dykstrom
 */
public final class FileUtils {

    private FileUtils() { }

    /**
     * Returns the base name, that is, the file name sans extension.
     */
    public static String getBasename(String filename) {
        int index = filename.lastIndexOf(".");
        return (index != -1) ? filename.substring(0, index): filename;
    }

    /**
     * Returns the file extension, or {@code null} if there is no extension.
     */
    public static String getExtension(String filename) {
        int index = filename.lastIndexOf(".");
        return (index != -1) ? filename.substring(index + 1) : null;
    }

    /**
     * Returns the given path with the file extension replaced by the given extension.
     */
    public static Path withExtension(final Path path, final String extension) {
        return Path.of(getBasename(path.toString()) + "." + extension);
    }
}
