/*
 * Copyright (C) 2025 Johan Dykstrom
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

package se.dykstrom.jcc.basic.compiler;

import se.dykstrom.jcc.common.functions.BuiltInFunction;
import se.dykstrom.jcc.common.functions.Function;
import se.dykstrom.jcc.common.types.F64;
import se.dykstrom.jcc.common.types.I64;
import se.dykstrom.jcc.common.types.Str;

import java.util.List;

/**
 * This class defines all built-in functions in the BASIC language.
 * The function instances defined here are added to the symbol table
 * and used for semantic analysis.
 */
public final class BasicFunctions {

    public static final Function BF_ABS_F64 = new BuiltInFunction("abs", List.of(F64.INSTANCE), F64.INSTANCE);
    public static final Function BF_ABS_I64 = new BuiltInFunction("abs", List.of(I64.INSTANCE), I64.INSTANCE);
    public static final Function BF_ASC_STR = new BuiltInFunction("asc", List.of(Str.INSTANCE), I64.INSTANCE);
    public static final Function BF_SQR_F64 = new BuiltInFunction("sqr", List.of(F64.INSTANCE), F64.INSTANCE);

    private BasicFunctions() { }
}
