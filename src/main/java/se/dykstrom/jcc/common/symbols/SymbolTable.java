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

package se.dykstrom.jcc.common.symbols;

import se.dykstrom.jcc.common.functions.Function;
import se.dykstrom.jcc.common.types.Type;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static java.util.stream.Collectors.toSet;

/**
 * Contains all symbols defined and used within a program.
 *
 * @author Johan Dykstrom
 */
public class SymbolTable {

    private final Map<String, Info> symbols = new HashMap<>();

    /**
     * Adds a variable to the symbol table.
     *
     * @param identifier Variable identifier.
     */
    public void addVariable(Identifier identifier) {
        symbols.put(identifier.getName(), new Info(identifier, identifier.getType().getDefaultValue(), false));
    }

    /**
     * Adds a constant, or constant value, to the symbol table.
     *
     * @param identifier Constant identifier.
     * @param value Constant value.
     */
    public void addConstant(Identifier identifier, String value) {
        symbols.put(identifier.getName(), new Info(identifier, value, true));
    }

    /**
     * Adds a function definition to the symbol table.
     *
     * @param identifier Function identifier.
     * @param function Function definition.
     */
    public void addFunction(Identifier identifier, Function function) {
        if (!identifier.getName().equals(function.getName())) {
            throw new IllegalArgumentException("expected function name " + identifier.getName() + ", found " + function.getName());
        }
        symbols.put(identifier.getName(), new Info(identifier, function, false));
    }

    /**
     * Returns an unordered collection of all identifiers in the symbol table.
     */
    public Collection<Identifier> identifiers() {
        return symbols.values().stream().map(Info::getIdentifier).collect(toSet());
    }

    /**
     * Returns {@code true} if the symbol table contains an identifier with the given {@code name}.
     */
    public boolean contains(String name) {
        return symbols.containsKey(name);
    }

    /**
     * Returns {@code true} if the symbol table contains all identifiers identified by {@code names}.
     */
    public boolean contains(String... names) {
        return Arrays.stream(names).allMatch(this::contains);
    }

    /**
     * Returns the identifier with the given {@code name}.
     */
    public Identifier getIdentifier(String name) {
        return findByName(name).getIdentifier();
    }

    /**
     * Returns the type of the identifier with the given {@code name}.
     */
    public Type getType(String name) {
        return findByName(name).getIdentifier().getType();
    }

    /**
     * Returns the value of the identifier with the given {@code name}. For constants, this is the constant value, 
     * for variables, it is the initial value, and for functions, it is the function definition.
     */
    public Object getValue(String name) {
        return findByName(name).getValue();
    }

    /**
     * Returns {@code true} if the identifier with the given {@code name} is a constant, or literal value.
     */
    public boolean isConstant(String name) {
        return findByName(name).isConstant();
    }

    /**
     * Finds an info object by name.
     */
    private Info findByName(String name) {
        Info info = symbols.get(name);
        if (info != null) {
            return info;
        }
        throw new IllegalArgumentException("undefined identifier: " + name);
    }

    /**
     * Returns the size of the symbol table, that is, the number of symbols.
     */
    public int size() {
        return symbols.size();
    }

    /**
     * Returns {@code true} if the symbol table is empty.
     */
    public boolean isEmpty() {
        return size() == 0;
    }

    // -----------------------------------------------------------------------

    private static class Info {

        private final Identifier identifier;
        private final Object value;
        private final boolean constant;

        Info(Identifier identifier, Object value, boolean constant) {
            this.identifier = identifier;
            this.value = value;
            this.constant = constant;
        }

        public Identifier getIdentifier() {
            return identifier;
        }

        public Object getValue() {
            return value;
        }

        boolean isConstant() {
            return constant;
        }
    }
}
