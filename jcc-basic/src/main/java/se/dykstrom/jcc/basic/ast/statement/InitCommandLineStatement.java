/*
 * Copyright (C) 2026 Johan Dykstrom
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

package se.dykstrom.jcc.basic.ast.statement;

import se.dykstrom.jcc.common.ast.AbstractNode;
import se.dykstrom.jcc.common.ast.Statement;

/**
 * Represents the call to the runtime function that initializes the command line from the program
 * arguments (argc, argv). This statement is synthesized during LLVM code generation and inserted
 * at the start of the main function; it is never produced by the parser.
 *
 * @author Johan Dykstrom
 */
public class InitCommandLineStatement extends AbstractNode implements Statement {

    public InitCommandLineStatement(int line, int column) {
        super(line, column);
    }

    @Override
    public String toString() {
        return "init_command_line(argc, argv)";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return o != null && getClass() == o.getClass();
    }

    @Override
    public int hashCode() {
        return InitCommandLineStatement.class.hashCode();
    }
}
