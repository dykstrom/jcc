/*
 * Copyright (C) 2018 Johan Dykstrom
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

package se.dykstrom.jcc.basic.ast;

import se.dykstrom.jcc.common.types.Str;

import java.util.Set;

/**
 * Represents a defstr statement, such as "DEFSTR a-c".
 *
 * @author Johan Dykstrom
 */
public class DefStrStatement extends AbstractDefTypeStatement {

    public DefStrStatement(int line, int column, Set<Character> letters) {
        super(line, column, "DEFSTR", Str.INSTANCE, letters);
    }
}
