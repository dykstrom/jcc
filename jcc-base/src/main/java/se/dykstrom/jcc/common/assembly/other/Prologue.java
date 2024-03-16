/*
 * Copyright (C) 2016 Johan Dykstrom
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

package se.dykstrom.jcc.common.assembly.other;

import se.dykstrom.jcc.common.assembly.base.AssemblyComment;
import se.dykstrom.jcc.common.assembly.base.FloatRegister;
import se.dykstrom.jcc.common.assembly.base.Register;
import se.dykstrom.jcc.common.assembly.instruction.MoveRegToReg;
import se.dykstrom.jcc.common.assembly.instruction.PushReg;
import se.dykstrom.jcc.common.assembly.instruction.SubImmFromReg;
import se.dykstrom.jcc.common.assembly.instruction.floating.MoveDquFloatRegToMem;
import se.dykstrom.jcc.common.intermediate.Blank;
import se.dykstrom.jcc.common.intermediate.CodeContainer;

import java.util.Set;

import static se.dykstrom.jcc.common.assembly.base.Register.RBP;
import static se.dykstrom.jcc.common.assembly.base.Register.RSP;

/**
 * Represents a function prologue, where non-volatile registers are pushed to the stack.
 *
 * @author Johan Dykstrom
 */
public class Prologue extends CodeContainer {

    public Prologue(final Set<Register> registers, final Set<FloatRegister> floatRegisters) {
        add(new AssemblyComment("Save base pointer"));
        add(new PushReg(RBP));
        add(new MoveRegToReg(RSP, RBP));

        if (!registers.isEmpty()) {
            add(new AssemblyComment("Save g.p. registers"));
            // Push all used non-volatile g.p. registers
            registers.stream().sorted().map(PushReg::new).forEach(this::add);
        }

        // Calculate possible stack alignment, only care about g.p. registers, as float registers are 16 bytes
        final var stackSpace = ((registers.size() % 2 == 0) ? null : "8h");
        if (stackSpace != null) {
            add(new AssemblyComment("Align stack"));
            add(new SubImmFromReg(stackSpace, RSP));
        }

        if (!floatRegisters.isEmpty()) {
            add(new AssemblyComment("Save float registers"));
            // Push all used non-volatile float registers
            floatRegisters.stream()
                    .sorted()
                    .forEach(register -> {
                        add(new SubImmFromReg("10h", RSP));
                        add(new MoveDquFloatRegToMem(register, RSP));
                    });
        }

        add(Blank.INSTANCE);
    }
}
