/*
 * Copyright (C) 2023 Johan Dykstrom
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

package se.dykstrom.jcc.main;

import java.nio.file.Path;
import java.util.List;

import static se.dykstrom.jcc.common.utils.FileUtils.getExtension;

/**
 * Enumerates the supported languages.
 */
public enum Language {

    ASSEMBUNNY(null, "asmb"),
    BASIC("jccbas", "bas", "BAS"),
    COL(null, "col"),
    TINY(null, "tiny");

    private final String stdlib;
    private final List<String> extensions;

    Language(final String stdlib, final String... extensions) {
        this.stdlib = stdlib;
        this.extensions = List.of(extensions);
    }

    /**
     * Returns the name of the standard library for this language.
     */
    public String stdlib() {
        return stdlib;
    }

    /**
     * Returns the primary file extension for this language.
     */
    public String extension() {
        return extensions.getFirst();
    }

    public static Language fromSource(final Path sourcePath) {
        final var extension = getExtension(sourcePath.toString());
        if (extension == null) {
            throw new IllegalArgumentException(sourcePath + ": Cannot determine file type");
        }

        for (var language : values()) {
            if (language.extensions.contains(extension)) {
                return language;
            }
        }

        throw new IllegalArgumentException(sourcePath + ": Invalid file type");
    }
}
