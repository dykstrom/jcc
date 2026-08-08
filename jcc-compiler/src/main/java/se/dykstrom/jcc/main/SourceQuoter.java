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

package se.dykstrom.jcc.main;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Quotes lines of a source file, so that a compilation message can be followed by the
 * line it refers to, and a caret pointing at the offending column:
 *
 * <pre>
 *     1 | DIM a AS DOBLE
 *       |          ^
 * </pre>
 *
 * The source file is read once, the first time a line is asked for. If it cannot be read,
 * this class quietly quotes nothing, since a missing quote is better than a second error
 * message about the file the compiler has just read itself.
 *
 * @author Johan Dykstrom
 */
public class SourceQuoter {

    /** Width of the line number gutter, not counting the following " | ". */
    private static final int GUTTER_WIDTH = 5;

    private final Path sourcePath;

    private List<String> lines;
    private boolean readFailed;

    public SourceQuoter(final Path sourcePath) {
        this.sourcePath = sourcePath;
    }

    /**
     * Returns the quoted source line and the caret line for the given position, as a
     * single string with the two lines separated by a line break. Returns an empty
     * optional if the source file cannot be read, or does not have the given line.
     *
     * @param line   The line to quote, 1 based.
     * @param column The column to point the caret at, 0 based.
     */
    public Optional<String> quote(final int line, final int column) {
        return sourceLine(line).map(text -> format(line, column, text));
    }

    private Optional<String> sourceLine(final int line) {
        readSourceIfNeeded();
        if (lines == null || line < 1 || line > lines.size()) {
            return Optional.empty();
        }
        return Optional.of(lines.get(line - 1));
    }

    private void readSourceIfNeeded() {
        if (lines != null || readFailed) {
            return;
        }
        try {
            lines = Files.readAllLines(sourcePath, UTF_8);
        } catch (IOException e) {
            readFailed = true;
        }
    }

    private static String format(final int line, final int column, final String text) {
        final String gutter = " ".repeat(GUTTER_WIDTH);
        return String.format("%" + GUTTER_WIDTH + "d | %s%n%s | %s^", line, text, gutter, caretIndent(column, text));
    }

    /**
     * Returns the text that precedes the caret. Tabs in the quoted line are copied as tabs,
     * so that the caret stays aligned with the character it points at.
     */
    private static String caretIndent(final int column, final String text) {
        final int index = Math.clamp(column, 0, text.length());
        final var indent = new StringBuilder();
        for (int i = 0; i < index; i++) {
            indent.append(i < text.length() && text.charAt(i) == '\t' ? '\t' : ' ');
        }
        return indent.toString();
    }
}
