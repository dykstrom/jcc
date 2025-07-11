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

package se.dykstrom.jcc.col.compiler;

import se.dykstrom.jcc.common.symbols.SymbolTable;

import static se.dykstrom.jcc.col.compiler.ColFunctions.*;

/**
 * A symbol table specific for COL, loaded with all built-in functions.
 */
public class ColSymbols extends SymbolTable {

    public ColSymbols() {
        addFunction(BF_F64_I32);
        addFunction(BF_F64_I64);
        addFunction(BF_I32_F64);
        addFunction(BF_I32_I64);
        addFunction(BF_I64_F64);
        addFunction(BF_I64_I32);

        addFunction(BF_CEIL_F64);
        addFunction(BF_FLOOR_F64);
        addFunction(BF_ROUND_F64);
        addFunction(BF_SQRT_F64);
        addFunction(BF_TRUNC_F64);

        addFunction(BF_PRINTLN_BOOL);
        addFunction(BF_PRINTLN_F64);
        addFunction(BF_PRINTLN_I64);
        addFunction(BF_PRINTLN_I64_TO_I64);
    }
}
