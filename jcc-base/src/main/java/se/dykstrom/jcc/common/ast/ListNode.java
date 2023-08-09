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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * An AST {@link Node} that represents a list of something, for example expressions.
 *
 * @author Johan Dykstrom
 */
public class ListNode<T> extends AbstractNode {

    private final List<T> list;

    public ListNode(int line, int column, List<T> list) {
        super(line, column);
        this.list = list;
    }

    /**
     * Returns a copy of this list node, with the first list item replaced by {@code item}.
     * This list remains unchanged.
     */
    public ListNode<T> withHead(final T item) {
        final List<T> updatedList = new ArrayList<>(list.size());
        updatedList.add(item);
        updatedList.addAll(list.subList(1, list.size()));
        return new ListNode<>(line(), column(), updatedList);
    }

    /**
     * Returns the contents of the list node, that is, the list.
     */
    public List<T> contents() {
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
