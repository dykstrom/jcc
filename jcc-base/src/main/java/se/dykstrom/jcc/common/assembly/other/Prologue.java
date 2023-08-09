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

import se.dykstrom.jcc.common.assembly.base.*;
import se.dykstrom.jcc.common.assembly.instruction.PushReg;
import se.dykstrom.jcc.common.assembly.instruction.SubImmFromReg;
import se.dykstrom.jcc.common.assembly.instruction.floating.MoveDquFloatRegToMem;
import se.dykstrom.jcc.common.intermediate.Blank;
import se.dykstrom.jcc.common.intermediate.CodeContainer;

import java.util.Set;

import static se.dykstrom.jcc.common.assembly.base.Register.RSP;

/**
 * Represents a function prologue, where non-volatile registers are pushed to the stack and shadow space is allocated.
 *
 * @author Johan Dykstrom
 */
public class Prologue extends CodeContainer {

    public Prologue(Set<Register> registers, Set<FloatRegister> floatRegisters) {
        if (registers.size() + floatRegisters.size() > 0) {
            add(new AssemblyComment("Save used non-volatile registers"));
        }
        // Add push instructions for all used non-volatile registers
        registers.stream().sorted().map(PushReg::new).forEach(this::add);
        floatRegisters.stream()
                .sorted()
                .forEach(register -> {
                    add(new SubImmFromReg("16", RSP));
                    add(new MoveDquFloatRegToMem(register, RSP));
                });

        // Calculate possible stack alignment, only care about g.p. registers, as float registers are 16 bytes
        int stackSpace = ((registers.size() % 2 != 0) ? 0x0 : 0x8);

        // Align stack
        if (stackSpace != 0) {
            add(new AssemblyComment("Align stack"));
            add(new SubImmFromReg(Integer.toString(stackSpace), RSP));
        }
        add(Blank.INSTANCE);
    }
}
