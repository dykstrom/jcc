/*
 * Copyright (C) 2019 Johan Dykstrom
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

import se.dykstrom.jcc.common.assembly.base.CodeContainer;
import se.dykstrom.jcc.common.assembly.base.Label;
import se.dykstrom.jcc.common.assembly.base.Line;
import se.dykstrom.jcc.common.assembly.instruction.*;
import se.dykstrom.jcc.common.assembly.other.Snippets;
import se.dykstrom.jcc.common.functions.AssemblyFunction;
import se.dykstrom.jcc.common.types.Constant;
import se.dykstrom.jcc.common.types.I64;
import se.dykstrom.jcc.common.types.Identifier;
import se.dykstrom.jcc.common.types.Str;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Arrays.asList;
import static se.dykstrom.jcc.basic.functions.BasicBuiltInFunctions.FUN_RIGHT;
import static se.dykstrom.jcc.common.assembly.base.Register.*;
import static se.dykstrom.jcc.common.functions.BuiltInFunctions.FUN_STRLEN;
import static se.dykstrom.jcc.common.functions.FunctionUtils.LIB_LIBC;

/**
 * Implements the two-argument {@code mid$(string, start)} function. This function returns a substring
 * of {@code string} that contains all characters from {@code start} to the end of {@code string}. If
 * {@code start} is greater than the length of {@code string} an empty string is returned. The index
 * of the first character in the string is 1.
 * <p/>
 * If {@code start} is less than 1, an illegal function call occurs.
 * <p/>
 * The {@code mid$} function allocates memory for the returned string. This memory must be managed
 * and freed when not needed.
 * <p/>
 * Signature: {@code mid$(string : Str, start : I64) : Str}
 *
 * @author Johan Dykstrom
 */
public class BasicMid2Function extends AssemblyFunction {

    public static final String NAME = "mid$";

    private static final String STRING_OFFSET = "10h";
    private static final String START_OFFSET = "18h";
    private static final String SHADOW_SPACE = "20h";

    private static final Constant ERROR_MSG = new Constant(new Identifier("_err_function_mid$", Str.INSTANCE), "\"Error: Illegal function call: mid$\",0");

    BasicMid2Function() {
        super(NAME, asList(Str.INSTANCE, I64.INSTANCE), Str.INSTANCE, Map.of(LIB_LIBC, Set.of(FUN_RIGHT, FUN_STRLEN)), Set.of(ERROR_MSG));
    }

    @Override
    public List<Line> lines() {
        return new BasicMid2Function.InternalCodeContainer().lines();
    }

    private static class InternalCodeContainer extends CodeContainer {
        private InternalCodeContainer() {
            // Create jump labels
            Label doneLabel = new Label("_mid2$_done");
            Label errorLabel = new Label("_mid2$_error");
            Label rightLabel = new Label("_mid2$_right");

            // Save arguments in home locations
            addAll(Snippets.enter(2));

            // Check bounds
            add(new CmpRegWithImm(RDX, "1h")); // Basic uses 1-based indices
            add(new Jl(errorLabel));

            // Find length of string
            addAll(Snippets.strlen(RCX)); // Length of string in RAX

            // Find out how many characters to copy
            add(new SubMemFromReg(RBP, START_OFFSET, RAX));
            add(new IncReg(RAX));
            add(new CmpRegWithImm(RAX, "0"));
            add(new Jge(rightLabel));
            add(new XorRegWithReg(RAX, RAX)); // If number is negative, set it to 0

            // Call right$ to copy characters
            add(rightLabel);
            add(new MoveMemToReg(RBP, STRING_OFFSET, RCX));
            add(new MoveRegToReg(RAX, RDX));
            add(new SubImmFromReg(SHADOW_SPACE, RSP));
            add(new CallDirect(new Label(FUN_RIGHT.getMappedName())));
            add(new AddImmToReg(SHADOW_SPACE, RSP));
            add(new Jmp(doneLabel));

            // ERROR
            add(errorLabel);
            addAll(Snippets.printf(ERROR_MSG.getIdentifier().getMappedName()));
            addAll(Snippets.exit("1h"));

            // DONE
            add(doneLabel);
            add(new PopReg(RBP));
            add(new Ret());
        }
    }
}
