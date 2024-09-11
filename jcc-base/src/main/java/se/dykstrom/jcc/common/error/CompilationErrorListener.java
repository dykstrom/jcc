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

package se.dykstrom.jcc.common.error;

import java.util.ArrayList;
import java.util.List;

/**
 * A combined error listener, that listens for both syntax and semantics compilation errors,
 * as well as compilation warnings.
 *
 * @author Johan Dykstrom
 */
public class CompilationErrorListener {

    private final List<CompilationError> errors = new ArrayList<>();
    private final List<CompilationWarning> warnings = new ArrayList<>();

    public void error(final int line, final int column, final String msg, final Exception exception) {
        errors.add(new CompilationError(line, column, msg, exception));
    }

    public void warning(final int line, final int column, final String msg, final Warning warning) {
        warnings.add(new CompilationWarning(line, column, msg, warning));
    }

    /**
     * Returns the list of all received errors.
     */
    public List<CompilationError> getErrors() {
        return errors;
    }

    /**
     * Returns the list of all received warnings.
     */
    public List<CompilationWarning> getWarnings() {
        return warnings;
    }

    /**
     * Returns {@code true} if this error listener has received any errors.
     */
    public boolean hasErrors() {
        return !errors.isEmpty();
    }
}
