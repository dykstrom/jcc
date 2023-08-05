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

package se.dykstrom.jcc.common.assembly.instruction;

import se.dykstrom.jcc.common.assembly.base.Instruction;
import se.dykstrom.jcc.common.assembly.base.Label;

/**
 * Base class for all jump instructions, for example "jmp" or "je".
 *
 * @author Johan Dykstrom
 */
public abstract class Jump implements Instruction {

    private final String instruction;
    private final Label target;

    public Jump(String instruction, Label target) {
        this.instruction = instruction;
        this.target = target;
    }

    /**
     * Returns the jump target.
     */
    public Label getTarget() {
        return target;
    }

    @Override
    public String toText() {
        return instruction + " " + target.getMappedName();
    }
}
