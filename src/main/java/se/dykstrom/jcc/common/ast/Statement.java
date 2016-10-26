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

package se.dykstrom.jcc.common.ast;

/**
 * An abstract base class for all statements. A statement may be proceeded by an optional label.
 *
 * @author Johan Dykstrom
 */
public abstract class Statement extends Node {

    private String label;

    protected Statement(int line, int column) {
        super(line, column);
        this.label = null;
    }

    protected Statement(int line, int column, String label) {
        super(line, column);
        this.label = label;
    }

    /**
     * Sets the label of this statement.
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * Returns the label proceeding this statement, or {@code null} if no label.
     */
    public String getLabel() {
        return label;
    }
}
