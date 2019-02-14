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
import se.dykstrom.jcc.common.types.*;

import java.util.*;

import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toSet;

/**
 * Contains all symbols defined and used within a program, both regular identifiers like variables, 
 * and function identifiers. Functions can be overloaded. Hence, multiple functions can be saved under 
 * one name. It is possible to retrieve all functions with a given name, or a single function that
 * matches both name and argument types.
 *
 * @author Johan Dykstrom
 */
public class SymbolTable {

    /** Contains all defined regular identifiers. */
    private final Map<String, Info> symbols = new HashMap<>();

    /** Contains all defined function identifiers. */
    private final Map<String, List<Info>> functions = new HashMap<>();

    // Regular identifiers:
    
    /**
     * Adds a variable to the symbol table.
     *
     * @param identifier Variable identifier.
     */
    public void addVariable(Identifier identifier) {
        if (identifier.getType() instanceof Unknown) {
            throw new IllegalArgumentException("variables of type Unknown not allowed in symbol table");
        }
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
     * Adds a constant, or constant value, to the symbol table.
     *
     * @param constant The constant to add.
     */
    public void addConstant(Constant constant) {
        addConstant(constant.getIdentifier(), constant.getValue());
    }

    /**
     * Finds a constant with the given type and value, and returns its identifier.
     * If no such constant is found, this method returns {@code null}. Using this
     * method, a code generator can reuse one constant in several places.
     * 
     * @param type The expected type of the constant.
     * @param value The expected value of the constant.
     * @return The identifier of the constant found, or {@code null} if not found.
     */
    public Identifier getConstantByTypeAndValue(Type type, String value) {
        return symbols.values().stream()
                .filter(info -> info.getIdentifier().getType().equals(type))
                .filter(info -> info.getValue().equals(value))
                .map(Info::getIdentifier)
                .findFirst()
                .orElse(null);
    }

    /**
     * Returns an unordered collection of all regular identifiers in the symbol table.
     */
    public Collection<Identifier> identifiers() {
        return symbols.values().stream().map(Info::getIdentifier).collect(toSet());
    }

    /**
     * Returns {@code true} if the symbol table contains a regular identifier with the given {@code name}.
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
     * Returns the regular identifier with the given {@code name}.
     */
    public Identifier getIdentifier(String name) {
        return findByName(name).getIdentifier();
    }

    /**
     * Returns the type of the regular identifier with the given {@code name}.
     */
    public Type getType(String name) {
        return findByName(name).getIdentifier().getType();
    }

    /**
     * Returns the value of the regular identifier with the given {@code name}. For constants, 
     * this is the constant value, and for variables, it is the initial value.
     */
    public Object getValue(String name) {
        return findByName(name).getValue();
    }

    /**
     * Returns {@code true} if the regular identifier with the given {@code name} is a constant, or literal value.
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

    // Functions:
    
    /**
     * Adds a function definition to the symbol table.
     *
     * @param function Function definition.
     */
    public void addFunction(Function function) {
        Identifier identifier = function.getIdentifier();
        if (!functions.containsKey(identifier.getName())) {
            functions.computeIfAbsent(identifier.getName(), name -> new ArrayList<>()).add(new Info(identifier, function, false));
        } else if (!containsFunction(function.getName(), function.getArgTypes())) {
            functions.get(identifier.getName()).add(new Info(identifier, function, false));
        }
    }

    /**
     * Returns an unordered collection of all function identifiers in the symbol table.
     * Note that some of the identifiers in the collection may have the same name, as
     * functions can be overloaded.
     */
    public Collection<Identifier> functionIdentifiers() {
        return functions.values().stream().flatMap(list -> list.stream().map(Info::getIdentifier)).collect(toSet());
    }

    /**
     * Returns the function identifier with the given {@code name} and argument types.
     * 
     * @param name The name of the function.
     * @param argTypes The argument types.
     * @return The identifier found.
     * @throws IllegalArgumentException If no matching function was found.
     */
    public Identifier getFunctionIdentifier(String name, List<Type> argTypes) {
        Function function = getFunction(name, argTypes);
        return new Identifier(name, Fun.from(argTypes, function.getReturnType()));
    }
    
    /**
     * Returns {@code true} if the symbol table contains one or more function identifiers with the given {@code name}.
     */
    public boolean containsFunction(String name) {
        return functions.containsKey(name);
    }

    /**
     * Returns the list of functions with the given name, regardless of argument types.
     * If no functions by that name are found, this method returns an empty set.
     */
    public Set<Function> getFunctions(String name) {
        if (functions.containsKey(name)) {
            return functions.get(name).stream().map(Info::getValue).map(object -> (Function) object).collect(toSet());
        } else {
            return emptySet();
        }
    }

    /**
     * Returns {@code true} if the symbol table contains a function that is an exact match of both name and argument types.
     */
    public boolean containsFunction(String name, List<Type> argTypes) {
        try {
            return getFunction(name, argTypes) != null;
        } catch (IllegalArgumentException ignore) {
            return false;
        }
    }
    
    /**
     * Returns the function that is an exact match of both name and argument types.
     * 
     * @param name The name of the function.
     * @param argTypes The argument types.
     * @return The function found.
     * @throws IllegalArgumentException If no matching function was found.
     */
    public Function getFunction(String name, List<Type> argTypes) {
        if (functions.containsKey(name)) {
            return functions.get(name).stream()
                    .map(Info::getValue)
                    .map(object -> (Function) object)
                    .filter(function -> argTypes.equals(function.getArgTypes()))
                    .findFirst()
                    .orElseThrow(IllegalArgumentException::new);
        } else {
            throw new IllegalArgumentException("undefined identifier: " + name);
        }
    }

    // Common:
    
    /**
     * Returns the size of the symbol table, that is, the number of regular and function symbols.
     */
    public int size() {
        return symbols.size() + functions.size();
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
