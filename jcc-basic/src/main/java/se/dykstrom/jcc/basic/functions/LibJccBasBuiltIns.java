/*
 * Copyright (C) 2017 Johan Dykstrom
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

package se.dykstrom.jcc.basic.functions;

import se.dykstrom.jcc.common.functions.*;
import se.dykstrom.jcc.common.types.*;
import se.dykstrom.jcc.common.types.Void;

import java.util.List;

import static se.dykstrom.jcc.common.functions.FunctionUtils.LIB_JCC_BAS;

/**
 * This class defines library function that are implemented in the JCC BASIC standard library libjccbas.
 * <p>
 * Libjccbas function constants are prefixed with the string "JF".
 */
public final class LibJccBasBuiltIns {

    public static final Function JF_ASC_STR = new LibraryFunction(".asc", List.of(Str.INSTANCE), I64.INSTANCE, LIB_JCC_BAS, new ExternalFunction("asc"));
    public static final Function JF_CDBL_F64 = new LibraryFunction(".cdbl", List.of(F64.INSTANCE), F64.INSTANCE, LIB_JCC_BAS, new ExternalFunction("cdbl"));
    public static final Function JF_CHR_I64 = new LibraryFunction(".chr$", List.of(I64.INSTANCE), Str.INSTANCE, LIB_JCC_BAS, new ExternalFunction("chr$"));
    public static final Function JF_CINT_F64 = new LibraryFunction(".cint", List.of(F64.INSTANCE), I64.INSTANCE, LIB_JCC_BAS, new ExternalFunction("cint"));
    public static final Function JF_COMMAND = new LibraryFunction(".command$", List.of(), Str.INSTANCE, LIB_JCC_BAS, new ExternalFunction("command$"));
    public static final Function JF_CVD_STR = new LibraryFunction(".cvd", List.of(Str.INSTANCE), F64.INSTANCE, LIB_JCC_BAS, new ExternalFunction("cvd"));
    public static final Function JF_CVI_STR = new LibraryFunction(".cvi", List.of(Str.INSTANCE), I64.INSTANCE, LIB_JCC_BAS, new ExternalFunction("cvi"));
    public static final Function JF_DATE = new LibraryFunction(".date$", List.of(), Str.INSTANCE, LIB_JCC_BAS, new ExternalFunction("date$"));
    public static final Function JF_FIX_F64 = new LibraryFunction(".fix", List.of(F64.INSTANCE), I64.INSTANCE, LIB_JCC_BAS, new ExternalFunction("fix"));
    public static final Function JF_HEX_I64 = new LibraryFunction(".hex$", List.of(I64.INSTANCE), Str.INSTANCE, LIB_JCC_BAS, new ExternalFunction("hex$"));
    public static final Function JF_INKEY = new LibraryFunction(".inkey$", List.of(), Str.INSTANCE, LIB_JCC_BAS, new ExternalFunction("inkey$"));
    public static final Function JF_INT_F64 = new LibraryFunction(".int", List.of(F64.INSTANCE), I64.INSTANCE, LIB_JCC_BAS, new ExternalFunction("int_F64"));
    public static final Function JF_LTRIM_STR = new LibraryFunction(".ltrim$", List.of(Str.INSTANCE), Str.INSTANCE, LIB_JCC_BAS, new ExternalFunction("ltrim"));
    public static final Function JF_MKD_F64 = new LibraryFunction(".mkd$", List.of(F64.INSTANCE), Str.INSTANCE, LIB_JCC_BAS, new ExternalFunction("mkd$"));
    public static final Function JF_MKI_I64 = new LibraryFunction(".mki$", List.of(I64.INSTANCE), Str.INSTANCE, LIB_JCC_BAS, new ExternalFunction("mki$"));
    public static final Function JF_LBOUND_ARR = new LibraryFunction(".lbound", List.of(Arr.INSTANCE), I64.INSTANCE, LIB_JCC_BAS, new ExternalFunction("lbound"));
    public static final Function JF_LBOUND_ARR_I64 = new LibraryFunction(".lbound", List.of(Arr.INSTANCE, I64.INSTANCE), I64.INSTANCE, LIB_JCC_BAS, new ExternalFunction("lbound_I64"));
    public static final Function JF_OCT_I64 = new LibraryFunction(".oct$", List.of(I64.INSTANCE), Str.INSTANCE, LIB_JCC_BAS, new ExternalFunction("oct$"));
    public static final Function JF_RANDOMIZE_F64 = new LibraryFunction(".randomize", List.of(F64.INSTANCE), Void.INSTANCE, LIB_JCC_BAS, new ExternalFunction("randomize"));
    public static final Function JF_SLEEP_F64 = new LibraryFunction(".sleep", List.of(F64.INSTANCE), Void.INSTANCE, LIB_JCC_BAS, new ExternalFunction("sleep_F64"));
    public static final Function JF_UBOUND_ARR = new LibraryFunction(".ubound", List.of(Arr.INSTANCE), I64.INSTANCE, LIB_JCC_BAS, new ExternalFunction("ubound"));
    public static final Function JF_UBOUND_ARR_I64 = new LibraryFunction(".ubound", List.of(Arr.INSTANCE, I64.INSTANCE), I64.INSTANCE, LIB_JCC_BAS, new ExternalFunction("ubound_I64"));
    public static final Function JF_RND = new LibraryFunction(".rnd", List.of(), F64.INSTANCE, LIB_JCC_BAS, new ExternalFunction("rnd"));
    public static final Function JF_RND_F64 = new LibraryFunction(".rnd", List.of(F64.INSTANCE), F64.INSTANCE, LIB_JCC_BAS, new ExternalFunction("rnd_F64"));
    public static final Function JF_RTRIM_STR = new LibraryFunction(".rtrim$", List.of(Str.INSTANCE), Str.INSTANCE, LIB_JCC_BAS, new ExternalFunction("rtrim"));
    public static final Function JF_OPTION_BASE_I64 = new LibraryFunction(".option_base", List.of(I64.INSTANCE), Void.INSTANCE, LIB_JCC_BAS, new ExternalFunction("option_base"));
    public static final Function JF_SGN_F64 = new LibraryFunction(".sgn", List.of(F64.INSTANCE), I64.INSTANCE, LIB_JCC_BAS, new ExternalFunction("sgn"));
    public static final Function JF_SPACE_I64 = new LibraryFunction(".space$", List.of(I64.INSTANCE), Str.INSTANCE, LIB_JCC_BAS, new ExternalFunction("space$"));
    public static final Function JF_STR_F64 = new LibraryFunction(".str$", List.of(F64.INSTANCE), Str.INSTANCE, LIB_JCC_BAS, new ExternalFunction("str_F64"));
    public static final Function JF_STR_I64 = new LibraryFunction(".str$", List.of(I64.INSTANCE), Str.INSTANCE, LIB_JCC_BAS, new ExternalFunction("str_I64"));
    public static final Function JF_STRING_I64_I64 = new LibraryFunction(".string$", List.of(I64.INSTANCE, I64.INSTANCE), Str.INSTANCE, LIB_JCC_BAS, new ExternalFunction("string$_I64"));
    public static final Function JF_STRING_I64_STR = new LibraryFunction(".string$", List.of(I64.INSTANCE, Str.INSTANCE), Str.INSTANCE, LIB_JCC_BAS, new ExternalFunction("string$_Str"));
    public static final Function JF_TIME = new LibraryFunction(".time$", List.of(), Str.INSTANCE, LIB_JCC_BAS, new ExternalFunction("time$"));
    public static final Function JF_TIMER = new LibraryFunction(".timer", List.of(), F64.INSTANCE, LIB_JCC_BAS, new ExternalFunction("timer"));

    public static final Function JF_GETLINE = new GetLineFunction();
    public static final Function JF_INSTR_STR_STR = new BasicInstr2Function();
    public static final Function JF_INSTR_I64_STR_STR = new BasicInstr3Function();
    public static final Function JF_LCASE_STR = new BasicLcaseFunction();
    public static final Function JF_LEFT_STR_I64 = new BasicLeftFunction();
    public static final Function JF_MID_STR_I64_I64 = new BasicMid3Function();
    public static final Function JF_RIGHT_STR_I64 = new BasicRightFunction();
    public static final Function JF_MID_STR_I64 = new BasicMid2Function(); // Depends on JF_RIGHT_STR_I64
    public static final Function JF_UCASE_STR = new BasicUcaseFunction();

    private LibJccBasBuiltIns() { }
}
