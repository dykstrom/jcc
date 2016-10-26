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

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

import java.util.ArrayList;
import java.util.List;

/**
 * A combined error listener, that listens for both syntax and semantics compilation errors.
 *
 * @author Johan Dykstrom
 */
public class CompilationErrorListener extends BaseErrorListener implements SemanticsErrorListener {

    private final List<CompilationError> errors = new ArrayList<>();

    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException exception) {
        errors.add(new CompilationError(line, charPositionInLine, msg, exception));
    }

    @Override
    public void semanticsError(int line, int column, String msg, SemanticsException exception) {
        errors.add(new CompilationError(line, column, msg, exception));
    }

    /**
     * Returns the list of all all received errors.
     */
    public List<CompilationError> getErrors() {
        return errors;
    }
}
