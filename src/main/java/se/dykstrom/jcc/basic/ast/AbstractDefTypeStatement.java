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

import se.dykstrom.jcc.common.ast.AbstractNode;
import se.dykstrom.jcc.common.ast.Statement;
import se.dykstrom.jcc.common.types.Type;

import java.util.Objects;
import java.util.Set;

import static java.util.stream.Collectors.joining;

/**
 * Abstract base class for the different DEFtype statements.
 *
 * @author Johan Dykstrom
 */
public abstract class AbstractDefTypeStatement extends AbstractNode implements Statement {

    private final Set<Character> letters;
    private final String keyword;
    private final Type type;

    AbstractDefTypeStatement(int line, int column, String keyword, Type type, Set<Character> letters) {
        super(line, column);
        this.keyword = keyword;
        this.type = type;
        this.letters = letters;
    }

    @Override
    public String toString() {
        return keyword + " " + letters.stream().sorted().map(Object::toString).collect(joining(", "));
    }

    /**
     * Returns the keyword.
     */
    public String getKeyword() {
        return keyword;
    }

    /**
     * Returns type.
     */
    public Type getType() {
        return type;
    }

    /**
     * Returns the set of letters defined for this type.
     */
    public Set<Character> getLetters() {
        return letters;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AbstractDefTypeStatement that = (AbstractDefTypeStatement) o;
        return Objects.equals(this.getLetters(), that.getLetters());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getLetters());
    }
}
