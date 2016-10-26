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

package se.dykstrom.jcc.common.assembly.instruction;

import se.dykstrom.jcc.common.assembly.base.Instruction;
import se.dykstrom.jcc.common.assembly.base.Label;

/**
 * Represents an assembly "jmp" instruction.
 *
 * @author Johan Dykstrom
 */
public class Jmp implements Instruction {

    private final Label target;

    public Jmp(Label target) {
        this.target = target;
    }

    /**
     * Returns the jump target.
     */
    public Label getTarget() {
        return target;
    }

    @Override
    public String toAsm() {
        return "jmp " + target.getMappedName();
    }
}
