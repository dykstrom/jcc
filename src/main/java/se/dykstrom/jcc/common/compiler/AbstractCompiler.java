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

import org.antlr.v4.runtime.ANTLRInputStream;

import se.dykstrom.jcc.common.error.CompilationErrorListener;

/**
 * Abstract base class for all compilers.
 *
 * @author Johan Dykstrom
 */
public abstract class AbstractCompiler implements Compiler {

    private String sourceFilename;

    private ANTLRInputStream inputStream;

    private CompilationErrorListener errorListener;

    @Override
    public void setSourceFilename(String sourceFilename) {
        this.sourceFilename = sourceFilename;
    }

    @Override
    public String getSourceFilename() {
        return sourceFilename;
    }

    @Override
    public void setInputStream(ANTLRInputStream inputStream) {
        this.inputStream = inputStream;
    }

    @Override
    public ANTLRInputStream getInputStream() {
        return inputStream;
    }

    @Override
    public void setErrorListener(CompilationErrorListener errorListener) {
        this.errorListener = errorListener;
    }

    @Override
    public CompilationErrorListener getErrorListener() {
        return errorListener;
    }
}
