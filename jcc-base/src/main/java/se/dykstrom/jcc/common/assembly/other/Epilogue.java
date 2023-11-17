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

package se.dykstrom.jcc.common.assembly.other;

import se.dykstrom.jcc.common.assembly.base.AssemblyComment;
import se.dykstrom.jcc.common.assembly.base.FloatRegister;
import se.dykstrom.jcc.common.assembly.base.Register;
import se.dykstrom.jcc.common.assembly.instruction.AddImmToReg;
import se.dykstrom.jcc.common.assembly.instruction.PopReg;
import se.dykstrom.jcc.common.assembly.instruction.floating.MoveDquMemToFloatReg;
import se.dykstrom.jcc.common.intermediate.Blank;
import se.dykstrom.jcc.common.intermediate.CodeContainer;

import java.util.Comparator;
import java.util.Set;

import static se.dykstrom.jcc.common.assembly.base.Register.RSP;

/**
 * Represents a function epilogue, where non-volatile registers are restored from the stack.
 *
 * @author Johan Dykstrom
 */
public class Epilogue extends CodeContainer {

    public Epilogue(final Set<Register> registers, final Set<FloatRegister> floatRegisters) {
        if (registers.size() + floatRegisters.size() > 0) {
            add(Blank.INSTANCE);

            // Calculate possible stack alignment, only care about g.p. registers, as float registers are 16 bytes
            final int stackSpace = ((registers.size() % 2 != 0) ? 0x0 : 0x8);

            // Un-align stack
            if (stackSpace != 0) {
                add(new AssemblyComment("Undo align stack"));
                add(new AddImmToReg(Integer.toString(stackSpace), RSP));
            }

            add(new AssemblyComment("Restore used non-volatile registers"));

            // Add pop instructions for all used non-volatile registers
            floatRegisters.stream()
                    .sorted(Comparator.reverseOrder())
                    .forEach(register -> {
                        add(new MoveDquMemToFloatReg(RSP, register));
                        add(new AddImmToReg("16", RSP));
                    });
            registers.stream().sorted(Comparator.reverseOrder()).map(PopReg::new).forEach(this::add);
        }
    }
}
