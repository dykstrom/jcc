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

package se.dykstrom.jcc.common.functions;

import se.dykstrom.jcc.common.assembly.base.Code;
import se.dykstrom.jcc.common.assembly.base.CodeContainer;
import se.dykstrom.jcc.common.assembly.base.Label;
import se.dykstrom.jcc.common.assembly.instruction.*;
import se.dykstrom.jcc.common.assembly.other.Snippets;
import se.dykstrom.jcc.common.types.Str;
import se.dykstrom.jcc.common.utils.MapUtils;
import se.dykstrom.jcc.common.utils.SetUtils;

import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static se.dykstrom.jcc.common.assembly.base.Register.*;
import static se.dykstrom.jcc.common.functions.BuiltInFunctions.*;
import static se.dykstrom.jcc.common.functions.FunctionUtils.LIB_LIBC;

/**
 * Implements the {@code getline()} function. This function reads a line of input entered by the user,
 * and returns it as a string. The newline character that ends the line is not included in the string.
 * <p/>
 * The {@code getline} function allocates memory for the returned string. This memory must be managed
 * and freed when not needed.
 * <p/>
 * Signature: {@code getline() : Str}
 *
 * @author Johan Dykstrom
 */
public class GetLineFunction extends AssemblyFunction {

    public static final String NAME = "getline";

    private static final String ASCII_NEWLINE = "10";
    private static final String ASCII_NULL = "0";
    private static final String EOF = "-1";

    GetLineFunction() {
        super(NAME, emptyList(), Str.INSTANCE, MapUtils.of(LIB_LIBC, SetUtils.of(FUN_GETCHAR, FUN_MALLOC, FUN_REALLOC)), emptySet());
    }

    @Override
    public List<Code> codes() {
        return new GetLineFunction.InternalCodeContainer().codes();
    }

    private static class InternalCodeContainer extends CodeContainer {

        private InternalCodeContainer() {
            // Create jump labels
            Label doneLabel = new Label("_getline_done");
            Label loopLabel = new Label("_getline_loop");

            // Save registers
            add(new PushReg(RBX));
            add(new PushReg(RDI));
            add(new PushReg(RSI));

            add(new MoveImmToReg("0", RBX)); // Length in RBX
            add(new MoveImmToReg("64", RDI)); // Size in RDI

            // Allocate initial buffer
            {
                addAll(Snippets.malloc(RDI));
                add(new MoveRegToReg(RAX, RSI)); // Pointer to buffer in RSI
            }

            // LOOP
            add(loopLabel);

            // Read next character and check if we are done
            {
                addAll(Snippets.getchar()); // Character read in RAX (actually AL)
                add(new CmpRegWithImm(RAX.asLowRegister8(), ASCII_NEWLINE));
                add(new Je(doneLabel));
                add(new CmpRegWithImm(RAX.asLowRegister8(), EOF));
                add(new Je(doneLabel));
            }

            // Store character in buffer
            {
                // TODO: We can do better: "mov [rsi + rbx], al"
                add(new Lea(RSI, RBX, R11));
                add(new MoveRegToMem(RAX.asLowRegister8(), R11));
            }

            // Increase length and check that it is still less than buffer size
            add(new IncReg(RBX));
            add(new CmpRegWithReg(RBX, RDI));
            add(new Jl(loopLabel));

            // Extend buffer
            {
                add(new SalRegWithImm(RDI, "1")); // Double buffer size
                addAll(Snippets.realloc(RSI, RDI));
                add(new MoveRegToReg(RAX, RSI)); // Pointer to new buffer in RSI
            }
            add(new Jmp(loopLabel));

            // DONE
            add(doneLabel);

            // Add null character at the end
            {
                // TODO: We can do better: "mov [rsi + rbx], 0"
                add(new Lea(RSI, RBX, R11));
                add(new MoveByteImmToMem(ASCII_NULL, R11));
            }
            add(new MoveRegToReg(RSI, RAX));

            add(new PopReg(RSI));
            add(new PopReg(RDI));
            add(new PopReg(RBX));
            add(new Ret());
        }
    }
}
