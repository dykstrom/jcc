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

package se.dykstrom.jcc.common.ast;

import java.util.List;
import java.util.Objects;

/**
 * Represents a list of something, for example expressions.
 *
 * @author Johan Dykstrom
 */
public class ListNode<T extends Node> extends Node {

    private final List<T> list;

    public ListNode(int line, int column, List<T> list) {
        super(line, column);
        this.list = list;
    }

    /**
     * Returns the contents of the list node, that is, the list.
     */
    public List<T> getContents() {
        return list;
    }

    @Override
    public String toString() {
        return list.toString();
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ListNode<T> program = (ListNode<T>) o;
        return Objects.equals(list, program.list);
    }

    @Override
    public int hashCode() {
        return Objects.hash(list);
    }
}
