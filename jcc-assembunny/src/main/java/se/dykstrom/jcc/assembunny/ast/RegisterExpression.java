/*
 * Copyright (C) 2024 Johan Dykstrom
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

package se.dykstrom.jcc.assembunny.ast;

import se.dykstrom.jcc.common.ast.IdentifierExpression;
import se.dykstrom.jcc.common.ast.TypedExpression;
import se.dykstrom.jcc.common.types.I64;
import se.dykstrom.jcc.common.types.Identifier;

import java.util.Objects;

/**
 * Represents an expression referencing an Assembunny register.
 *
 * @author Johan Dykstrom
 */
public class RegisterExpression extends IdentifierExpression implements TypedExpression {

    private final AssembunnyRegister register;

    public RegisterExpression(final int line, final int column, final AssembunnyRegister register) {
        super(line, column, new Identifier(register.name(), I64.INSTANCE));
        this.register = register;
    }

    public AssembunnyRegister register() {
        return register;
    }

    @Override
    public String toString() {
        return register.toString().toLowerCase();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        RegisterExpression that = (RegisterExpression) o;
        return register == that.register;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), register);
    }
}
