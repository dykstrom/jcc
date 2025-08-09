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

import se.dykstrom.jcc.common.functions.BuiltInFunction;
import se.dykstrom.jcc.common.functions.Function;
import se.dykstrom.jcc.common.symbols.SymbolTable;
import se.dykstrom.jcc.common.types.Arr;
import se.dykstrom.jcc.common.types.F64;
import se.dykstrom.jcc.common.types.I64;
import se.dykstrom.jcc.common.types.Str;

import java.util.List;

import static se.dykstrom.jcc.basic.functions.LibJccBasBuiltIns.*;
import static se.dykstrom.jcc.common.functions.LibcBuiltIns.*;

/**
 * A symbol table specific for BASIC, loaded with all built-in functions.
 * This class defines all built-in functions in the BASIC language, and makes
 * them available for semantic analysis.
 * <p>
 * Built-in function constants are prefixed with the string "BF".
 */
public class BasicSymbols extends SymbolTable {

    public static final Function BF_ABS_F64 = new BuiltInFunction("abs", List.of(F64.INSTANCE), F64.INSTANCE);
    public static final Function BF_ABS_I64 = new BuiltInFunction("abs", List.of(I64.INSTANCE), I64.INSTANCE);
    public static final Function BF_ASC_STR = new BuiltInFunction("asc", List.of(Str.INSTANCE), I64.INSTANCE);
    public static final Function BF_ATN_F64 = new BuiltInFunction("atn", List.of(F64.INSTANCE), F64.INSTANCE);
    public static final Function BF_CDBL_F64 = new BuiltInFunction("cdbl", List.of(F64.INSTANCE), F64.INSTANCE);
    public static final Function BF_CHR_I64 = new BuiltInFunction("chr$", List.of(I64.INSTANCE), Str.INSTANCE);
    public static final Function BF_CINT_F64 = new BuiltInFunction("cint", List.of(F64.INSTANCE), I64.INSTANCE);
    public static final Function BF_COMMAND = new BuiltInFunction("command$", List.of(), Str.INSTANCE);
    public static final Function BF_COS_F64 = new BuiltInFunction("cos", List.of(F64.INSTANCE), F64.INSTANCE);
    public static final Function BF_CVD_STR = new BuiltInFunction("cvd", List.of(Str.INSTANCE), F64.INSTANCE);
    public static final Function BF_CVI_STR = new BuiltInFunction("cvi", List.of(Str.INSTANCE), I64.INSTANCE);
    public static final Function BF_DATE = new BuiltInFunction("date$", List.of(), Str.INSTANCE);
    public static final Function BF_EXP_F64 = new BuiltInFunction("exp", List.of(F64.INSTANCE), F64.INSTANCE);
    public static final Function BF_FIX_F64 = new BuiltInFunction("fix", List.of(F64.INSTANCE), I64.INSTANCE);
    public static final Function BF_HEX_I64 = new BuiltInFunction("hex$", List.of(I64.INSTANCE), Str.INSTANCE);
    public static final Function BF_INKEY = new BuiltInFunction("inkey$", List.of(), Str.INSTANCE);
    public static final Function BF_INSTR_STR_STR = new BuiltInFunction("instr", List.of(Str.INSTANCE, Str.INSTANCE), I64.INSTANCE);
    public static final Function BF_INSTR_I64_STR_STR = new BuiltInFunction("instr", List.of(I64.INSTANCE, Str.INSTANCE, Str.INSTANCE), I64.INSTANCE);
    public static final Function BF_INT_F64 = new BuiltInFunction("int", List.of(F64.INSTANCE), I64.INSTANCE);
    public static final Function BF_LBOUND_ARR = new BuiltInFunction("lbound", List.of(Arr.INSTANCE), I64.INSTANCE);
    public static final Function BF_LBOUND_ARR_I64 = new BuiltInFunction("lbound", List.of(Arr.INSTANCE, I64.INSTANCE), I64.INSTANCE);
    public static final Function BF_LCASE_STR = new BuiltInFunction("lcase$", List.of(Str.INSTANCE), Str.INSTANCE);
    public static final Function BF_LEFT_STR_I64 = new BuiltInFunction("left$", List.of(Str.INSTANCE, I64.INSTANCE), Str.INSTANCE);
    public static final Function BF_LEN_STR = new BuiltInFunction("len", List.of(Str.INSTANCE), I64.INSTANCE);
    public static final Function BF_LOG_F64 = new BuiltInFunction("log", List.of(F64.INSTANCE), F64.INSTANCE);
    public static final Function BF_LTRIM_STR = new BuiltInFunction("ltrim$", List.of(Str.INSTANCE), Str.INSTANCE);
    public static final Function BF_MID_STR_I64 = new BuiltInFunction("mid$", List.of(Str.INSTANCE, I64.INSTANCE), Str.INSTANCE);
    public static final Function BF_MID_STR_I64_I64 = new BuiltInFunction("mid$", List.of(Str.INSTANCE, I64.INSTANCE, I64.INSTANCE), Str.INSTANCE);
    public static final Function BF_MKD_F64 = new BuiltInFunction("mkd$", List.of(F64.INSTANCE), Str.INSTANCE);
    public static final Function BF_MKI_I64 = new BuiltInFunction("mki$", List.of(I64.INSTANCE), Str.INSTANCE);
    public static final Function BF_OCT_I64 = new BuiltInFunction("oct$", List.of(I64.INSTANCE), Str.INSTANCE);
    public static final Function BF_RIGHT_STR_I64 = new BuiltInFunction("right$", List.of(Str.INSTANCE, I64.INSTANCE), Str.INSTANCE);
    public static final Function BF_RND = new BuiltInFunction("rnd", List.of(), F64.INSTANCE);
    public static final Function BF_RND_F64 = new BuiltInFunction("rnd", List.of(F64.INSTANCE), F64.INSTANCE);
    public static final Function BF_RTRIM_STR = new BuiltInFunction("rtrim$", List.of(Str.INSTANCE), Str.INSTANCE);
    public static final Function BF_SGN_F64 = new BuiltInFunction("sgn", List.of(F64.INSTANCE), I64.INSTANCE);
    public static final Function BF_SIN_F64 = new BuiltInFunction("sin", List.of(F64.INSTANCE), F64.INSTANCE);
    public static final Function BF_SPACE_I64 = new BuiltInFunction("space$", List.of(I64.INSTANCE), Str.INSTANCE);
    public static final Function BF_STR_F64 = new BuiltInFunction("str$", List.of(F64.INSTANCE), Str.INSTANCE);
    public static final Function BF_STR_I64 = new BuiltInFunction("str$", List.of(I64.INSTANCE), Str.INSTANCE);
    public static final Function BF_STRING_I64_I64 = new BuiltInFunction("string$", List.of(I64.INSTANCE, I64.INSTANCE), Str.INSTANCE);
    public static final Function BF_STRING_I64_STR = new BuiltInFunction("string$", List.of(I64.INSTANCE, Str.INSTANCE), Str.INSTANCE);
    public static final Function BF_SQR_F64 = new BuiltInFunction("sqr", List.of(F64.INSTANCE), F64.INSTANCE);
    public static final Function BF_TAN_F64 = new BuiltInFunction("tan", List.of(F64.INSTANCE), F64.INSTANCE);
    public static final Function BF_TIME = new BuiltInFunction("time$", List.of(), Str.INSTANCE);
    public static final Function BF_TIMER = new BuiltInFunction("timer", List.of(), F64.INSTANCE);
    public static final Function BF_UBOUND_ARR = new BuiltInFunction("ubound", List.of(Arr.INSTANCE), I64.INSTANCE);
    public static final Function BF_UBOUND_ARR_I64 = new BuiltInFunction("ubound", List.of(Arr.INSTANCE, I64.INSTANCE), I64.INSTANCE);
    public static final Function BF_UCASE_STR = new BuiltInFunction("ucase$", List.of(Str.INSTANCE), Str.INSTANCE);
    public static final Function BF_VAL_STR = new BuiltInFunction("val", List.of(Str.INSTANCE), F64.INSTANCE);

    public BasicSymbols() {
        addFunction(BF_ABS_F64);
        addFunction(BF_ABS_I64);
        addFunction(BF_ASC_STR);
        addFunction(BF_ATN_F64);
        addFunction(BF_CDBL_F64);
        addFunction(BF_CHR_I64);
        addFunction(BF_CINT_F64);
        addFunction(BF_COMMAND);
        addFunction(BF_COS_F64);
        addFunction(BF_CVD_STR);
        addFunction(BF_CVI_STR);
        addFunction(BF_DATE);
        addFunction(BF_EXP_F64);
        addFunction(BF_FIX_F64);
        addFunction(BF_HEX_I64);
        addFunction(BF_INKEY);
        addFunction(BF_INSTR_STR_STR);
        addFunction(BF_INSTR_I64_STR_STR);
        addFunction(BF_INT_F64);
        addFunction(BF_LBOUND_ARR);
        addFunction(BF_LBOUND_ARR_I64);
        addFunction(BF_LCASE_STR);
        addFunction(BF_LEFT_STR_I64);
        addFunction(BF_LEN_STR);
        addFunction(BF_LOG_F64);
        addFunction(BF_LTRIM_STR);
        addFunction(BF_MID_STR_I64);
        addFunction(BF_MID_STR_I64_I64);
        addFunction(BF_MKD_F64);
        addFunction(BF_MKI_I64);
        addFunction(BF_OCT_I64);
        addFunction(BF_RIGHT_STR_I64);
        addFunction(BF_RND);
        addFunction(BF_RND_F64);
        addFunction(BF_RTRIM_STR);
        addFunction(BF_SGN_F64);
        addFunction(BF_SIN_F64);
        addFunction(BF_SPACE_I64);
        addFunction(BF_SQR_F64);
        addFunction(BF_STR_F64);
        addFunction(BF_STR_I64);
        addFunction(BF_STRING_I64_I64);
        addFunction(BF_STRING_I64_STR);
        addFunction(BF_TAN_F64);
        addFunction(BF_TIME);
        addFunction(BF_TIMER);
        addFunction(BF_UBOUND_ARR);
        addFunction(BF_UBOUND_ARR_I64);
        addFunction(BF_UCASE_STR);
        addFunction(BF_VAL_STR);

        // Not directly callable - libc functions
        addFunction(CF_FMOD_F64_F64);
        addFunction(CF_POW_F64_F64);
        addFunction(CF_PRINTF_STR_VAR);

        // Not directly callable - libjccbas functions
        addFunction(JF_GETLINE);
        addFunction(JF_RANDOMIZE_F64);
        addFunction(JF_SLEEP_F64);
    }
}
