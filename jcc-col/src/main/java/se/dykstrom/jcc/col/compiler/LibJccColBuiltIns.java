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

package se.dykstrom.jcc.col.compiler;

import se.dykstrom.jcc.common.functions.ExternalFunction;
import se.dykstrom.jcc.common.functions.Function;
import se.dykstrom.jcc.common.functions.LibraryFunction;
import se.dykstrom.jcc.common.types.I64;

import java.util.List;

import static se.dykstrom.jcc.common.utils.FunctionUtils.LIB_JCC_COL;

/**
 * This class defines library functions that are implemented in the JCC COL standard library libjcccol.
 * <p>
 * Libjcccol function constants are prefixed with the string "JF".
 */
public final class LibJccColBuiltIns {

    public static final Function JF_MILLIS = new LibraryFunction(".millis", List.of(), I64.INSTANCE, LIB_JCC_COL, new ExternalFunction("col_millis"));

    private LibJccColBuiltIns() { }
}
