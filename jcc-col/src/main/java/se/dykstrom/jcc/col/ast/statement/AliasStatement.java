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

package se.dykstrom.jcc.col.ast.statement;

import se.dykstrom.jcc.common.ast.AbstractNode;
import se.dykstrom.jcc.common.ast.Statement;
import se.dykstrom.jcc.common.types.Type;

import java.util.Objects;

import static java.util.Objects.requireNonNull;

/**
 * Represents an alias statement such as 'alias Long = i64'.
 *
 * @author Johan Dykstrom
 */
public class AliasStatement extends AbstractNode implements Statement {

    private final String alias;
    private final Type type;

    public AliasStatement(final int line, final int column, final String alias, final Type type) {
        super(line, column);
        this.alias = requireNonNull(alias);
        this.type = requireNonNull(type);
    }

    public AliasStatement(final String alias, final Type type) {
        this(0, 0, alias, type);
    }

    @Override
    public String toString() {
        return "alias " + alias + " = " + type;
    }

    public String alias() {
        return alias;
    }

    public Type type() {
        return type;
    }

    public AliasStatement withType(final Type type) {
        return new AliasStatement(line(), column(), alias, type);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final AliasStatement that = (AliasStatement) o;
        return Objects.equals(alias, that.alias) && Objects.equals(type, that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(alias, type);
    }
}
