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

package se.dykstrom.jcc.common.types;

/**
 * Represents the type of an expression whose real type could not be determined, because the
 * expression was already reported as an error. It is not a type a program can name, and no
 * expression that reaches code generation ever has it: an unknown type exists only where a
 * diagnostic exists, and semantic analysis fails the compilation before the backend runs.
 *
 * <p>Every check that would compare it with another type accepts it instead, so that one mistake
 * produces one message rather than seeding a second one about a type the compiler invented. See
 * diagnostics.md.
 *
 * @author Johan Dykstrom
 */
public class Unknown extends AbstractType {

    public static final Unknown INSTANCE = new Unknown();

    @Override
    public String getName() {
        return "unknown";
    }

    @Override
    public boolean isUnknown() {
        return true;
    }

    @Override
    public String llvmName() {
        throw new UnsupportedOperationException("unknown");
    }

    @Override
    public String llvmDefaultValue() {
        throw new UnsupportedOperationException("unknown");
    }

    /**
     * Returns a value that is never used: the symbol table stores one for every variable it
     * holds, and a variable of this type is one the front end has already reported. The LLVM
     * methods throw instead, so an unknown type that did reach code generation still fails
     * loudly there.
     */
    @Override
    public String getDefaultValue() {
        return "0";
    }

    @Override
    public String getFormat() {
        throw new UnsupportedOperationException("unknown");
    }
}
