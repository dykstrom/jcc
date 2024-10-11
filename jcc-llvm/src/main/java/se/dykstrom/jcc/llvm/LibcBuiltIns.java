/*
 * Copyright (C) 2024 Johan Dykstrom
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

package se.dykstrom.jcc.llvm;

import se.dykstrom.jcc.common.functions.ExternalFunction;
import se.dykstrom.jcc.common.functions.LibraryFunction;
import se.dykstrom.jcc.common.types.I32;
import se.dykstrom.jcc.common.types.Str;
import se.dykstrom.jcc.common.types.Varargs;

import java.util.List;

public final class LibcBuiltIns {

    public static final LibraryFunction FUN_PRINTF_STR_VAR = new LibraryFunction(".printf", List.of(Str.INSTANCE, Varargs.INSTANCE), I32.INSTANCE, "N/A", new ExternalFunction("printf"));
    public static final LibraryFunction FUN_SCANF_STR_VAR = new LibraryFunction(".scanf", List.of(Str.INSTANCE, Varargs.INSTANCE), I32.INSTANCE, "N/A", new ExternalFunction("scanf"));

    private LibcBuiltIns() { }
}
