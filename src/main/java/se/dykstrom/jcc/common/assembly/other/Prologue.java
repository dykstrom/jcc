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

import se.dykstrom.jcc.common.assembly.base.Blank;
import se.dykstrom.jcc.common.assembly.base.CodeContainer;
import se.dykstrom.jcc.common.assembly.base.Comment;
import se.dykstrom.jcc.common.assembly.base.Register;
import se.dykstrom.jcc.common.assembly.instruction.PushReg;
import se.dykstrom.jcc.common.assembly.instruction.SubImmFromReg;

import java.util.Set;

import static se.dykstrom.jcc.common.assembly.base.Register.RSP;

/**
 * Represents a function prologue, where non-volatile registers are pushed to the stack and shadow space is allocated.
 *
 * @author Johan Dykstrom
 */
public class Prologue extends CodeContainer {

    public Prologue(Set<Register> registers) {
        if (!registers.isEmpty()) {
            add(new Comment("Save used non-volatile registers"));
        }
        // Add push instructions for all used non-volatile registers
        registers.stream().sorted().map(PushReg::new).forEach(this::add);

        // Calculate size of shadow space plus possible stack alignment
        Integer stackSpace = 0x20 + ((registers.size() % 2 != 0) ? 0x0 : 0x8);

        // Allocate shadow space
        add(new Comment("Allocate shadow space, and align stack"));
        add(new SubImmFromReg(stackSpace.toString(), RSP)).add(Blank.INSTANCE);
    }
}
