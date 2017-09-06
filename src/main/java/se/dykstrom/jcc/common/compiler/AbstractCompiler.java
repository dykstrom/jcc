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

package se.dykstrom.jcc.common.compiler;

import org.antlr.v4.runtime.CharStream;
import se.dykstrom.jcc.common.error.CompilationErrorListener;

/**
 * Abstract base class for all compilers.
 *
 * @author Johan Dykstrom
 */
public abstract class AbstractCompiler implements Compiler {

    private String sourceFilename;

    private CharStream inputStream;

    private CompilationErrorListener errorListener;

    public void setSourceFilename(String sourceFilename) {
        this.sourceFilename = sourceFilename;
    }

    public String getSourceFilename() {
        return sourceFilename;
    }

    public void setInputStream(CharStream inputStream) {
        this.inputStream = inputStream;
    }

    public CharStream getInputStream() {
        return inputStream;
    }

    public void setErrorListener(CompilationErrorListener errorListener) {
        this.errorListener = errorListener;
    }

    public CompilationErrorListener getErrorListener() {
        return errorListener;
    }
}
