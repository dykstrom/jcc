/*
 * Copyright (C) 2023 Johan Dykstrom
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

import se.dykstrom.jcc.common.symbols.SymbolTable;

import static se.dykstrom.jcc.basic.functions.BasicBuiltInFunctions.*;
import static se.dykstrom.jcc.common.functions.BuiltInFunctions.FUN_GETLINE;

/**
 * A symbol table specific for BASIC, loaded with all standard library functions.
 */
public class BasicSymbols extends SymbolTable {

    public BasicSymbols() {
        addFunction(FUN_ABS);
        addFunction(FUN_ASC);
        addFunction(FUN_ATN);
        addFunction(FUN_CDBL);
        addFunction(FUN_CINT);
        addFunction(FUN_CHR);
        addFunction(FUN_COS);
        addFunction(FUN_CVD);
        addFunction(FUN_CVI);
        addFunction(FUN_DATE);
        addFunction(FUN_EXP);
        addFunction(FUN_FABS);
        addFunction(FUN_FIX);
        addFunction(FUN_FMOD); // Used internally
        addFunction(FUN_GETLINE); // Used internally
        addFunction(FUN_HEX);
        addFunction(FUN_INSTR2);
        addFunction(FUN_INSTR3);
        addFunction(FUN_INT);
        addFunction(FUN_LBOUND);
        addFunction(FUN_LBOUND_I64);
        addFunction(FUN_LCASE);
        addFunction(FUN_LEFT);
        addFunction(FUN_LEN);
        addFunction(FUN_LOG);
        addFunction(FUN_LTRIM);
        addFunction(FUN_MID2);
        addFunction(FUN_MID3);
        addFunction(FUN_MKD);
        addFunction(FUN_MKI);
        addFunction(FUN_OCT);
        addFunction(FUN_RANDOMIZE);
        addFunction(FUN_UBOUND);
        addFunction(FUN_UBOUND_I64);
        addFunction(FUN_RIGHT);
        addFunction(FUN_RND);
        addFunction(FUN_RND_F64);
        addFunction(FUN_RTRIM);
        addFunction(FUN_SGN);
        addFunction(FUN_SIN);
        addFunction(FUN_SQR);
        addFunction(FUN_SPACE);
        addFunction(FUN_STR_F64);
        addFunction(FUN_STR_I64);
        addFunction(FUN_STRING_I64);
        addFunction(FUN_STRING_STR);
        addFunction(FUN_TAN);
        addFunction(FUN_TIME);
        addFunction(FUN_TIMER);
        addFunction(FUN_UCASE);
        addFunction(FUN_VAL);
    }
}
