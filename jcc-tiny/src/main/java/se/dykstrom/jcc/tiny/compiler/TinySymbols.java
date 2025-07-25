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

package se.dykstrom.jcc.tiny.compiler;

import se.dykstrom.jcc.common.symbols.SymbolTable;

import static se.dykstrom.jcc.common.functions.LibcBuiltIns.LF_PRINTF_STR_VAR;
import static se.dykstrom.jcc.common.functions.LibcBuiltIns.LF_SCANF_STR_VAR;

/**
 * A symbol table specific for Tiny, loaded with all built-in functions.
 */
public class TinySymbols extends SymbolTable {

    public TinySymbols() {
        // Not directly callable
        addFunction(LF_PRINTF_STR_VAR);
        addFunction(LF_SCANF_STR_VAR);
    }
}
