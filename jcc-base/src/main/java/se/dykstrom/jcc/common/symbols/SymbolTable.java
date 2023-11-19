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

import se.dykstrom.jcc.common.ast.ArrayDeclaration;
import se.dykstrom.jcc.common.functions.Function;
import se.dykstrom.jcc.common.types.*;

import java.util.*;

import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toCollection;
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

    /** Contains all defined array identifiers. */
    private final Map<String, Info> arrays = new HashMap<>();

    /** Contains all defined function identifiers. */
    private final Map<String, List<Info>> functions = new HashMap<>();

    private final SymbolTable parent;

    public SymbolTable() {
        this.parent = null;
    }

    public SymbolTable(final SymbolTable parent) {
        this.parent = parent;
    }

    public SymbolTable pop() {
        return parent;
    }

    // Regular identifiers:
    
    /**
     * Adds a variable to the symbol table.
     *
     * @param identifier Variable identifier.
     */
    public void addVariable(final Identifier identifier) {
        symbols.put(identifier.name(), new Info(identifier, identifier.type().getDefaultValue()));
    }

    /**
     * Adds a constant, or constant value, to the symbol table.
     *
     * @param identifier Constant identifier.
     * @param value Constant value.
     * @return The constant identifier, to enable chaining.
     * @see #addFunction(Function)
     */
    public Identifier addConstant(final Identifier identifier, final String value) {
        if (parent != null) {
            // Constants are global, so they are added to the root symbol table
            return parent.addConstant(identifier, value);
        } else {
            symbols.put(identifier.name(), new Info(identifier, value, true));
            return identifier;
        }
    }

    /**
     * Adds a constant, or constant value, to the symbol table.
     *
     * @param constant The constant to add.
     */
    public void addConstant(final Constant constant) {
        addConstant(constant.getIdentifier(), constant.getValue());
    }

    /**
     * Finds a constant with the given type and value, and returns its identifier if exists.
     * Using this method, a code generator can reuse a single constant in several places.
     * 
     * @param type The expected type of the constant.
     * @param value The expected value of the constant.
     * @return The optional identifier of the constant found.
     */
    public Optional<Identifier> getConstantByTypeAndValue(Type type, String value) {
        final var optionalResult = symbols.values().stream()
                .filter(Info::isConstant)
                .filter(info -> info.identifier().type().equals(type))
                .filter(info -> info.value().equals(value))
                .map(Info::identifier)
                .findFirst();
        if (optionalResult.isEmpty() && parent != null) {
            return parent.getConstantByTypeAndValue(type, value);
        } else {
            return optionalResult;
        }
    }

    /**
     * Returns the set of all visible regular identifiers in the symbol table.
     * If a global identifier has been redefined in a local scope, the local
     * identifier will be included in the result, and not the global one.
     */
    public Set<Identifier> identifiers() {
        final var childIdentifiers = symbols.values().stream().map(Info::identifier).collect(toCollection(HashSet::new));
        if (parent != null) {
            final var parentIdentifiers = parent.identifiers();
            parentIdentifiers.forEach(pi -> {
                // If there is no child identifier with the same name, add the parent identifier
                if (childIdentifiers.stream().noneMatch(ci -> ci.name().equals(pi.name()))) {
                    childIdentifiers.add(pi);
                }
            });
        }
        return childIdentifiers;
    }

    /**
     * Returns {@code true} if the symbol table contains a regular identifier with the given {@code name}.
     */
    public boolean contains(final String name) {
        var result = symbols.containsKey(name);
        if (!result && parent != null) {
            result = parent.contains(name);
        }
        return result;
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
        return findByName(name).identifier();
    }

    /**
     * Returns the type of the regular identifier with the given {@code name}.
     */
    public Type getType(String name) {
        return findByName(name).identifier().type();
    }

    /**
     * Returns the value of the regular identifier with the given {@code name}. For constants, 
     * this is the constant value, and for variables, it is the initial value.
     */
    public Object getValue(String name) {
        return findByName(name).value();
    }

    /**
     * Returns {@code true} if the regular identifier with the given {@code name} is a constant, or literal value.
     */
    public boolean isConstant(String name) {
        return findByName(name).isConstant;
    }

    /**
     * Finds an info object for a regular identifier by name.
     */
    private Info findByName(final String name) {
        var result = symbols.get(name);
        if (result != null) {
            return result;
        }
        if (parent != null) {
            return parent.findByName(name);
        }
        throw new IllegalArgumentException("undefined identifier: " + name);
    }

    // -----------------------------------------------------------------------
    // Functions:
    // -----------------------------------------------------------------------

    /**
     * Adds a function definition to the symbol table.
     *
     * @param function Function definition.
     */
    public void addFunction(final Function function) {
        if (parent != null) {
            // Functions are global, so they are added to the root symbol table
            //
            // This means that for BASIC, they will be added to BasicSymbols.
            // Functions defined during semantic analysis will be available from
            // the start during optimization and code generation. Will this be
            // a problem?
            parent.addFunction(function);
        } else {
            final var identifier = function.getIdentifier();
            if (!functions.containsKey(identifier.name())) {
                functions.computeIfAbsent(identifier.name(), name -> new ArrayList<>()).add(new Info(identifier, function));
            } else if (!containsFunction(function.getName(), function.getArgTypes())) {
                functions.get(identifier.name()).add(new Info(identifier, function));
            }
        }
    }

    /**
     * Returns an unordered collection of all function identifiers in the symbol table.
     * Note that some identifiers in the collection may have the same name, as functions
     * can be overloaded.
     */
    public Collection<Identifier> functionIdentifiers() {
        return functions.values().stream().flatMap(list -> list.stream().map(Info::identifier)).collect(toSet());
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
    public boolean containsFunction(final String name) {
        if (parent != null) {
            return parent.containsFunction(name);
        } else {
            return functions.containsKey(name);
        }
    }

    /**
     * Returns the list of functions with the given name, regardless of argument types.
     * If no functions by that name are found, this method returns an empty set.
     */
    public Set<Function> getFunctions(final String name) {
        if (parent != null) {
            return parent.getFunctions(name);
        } else if (functions.containsKey(name)) {
            return functions.get(name).stream().map(Info::value).map(Function.class::cast).collect(toSet());
        } else {
            return emptySet();
        }
    }

    /**
     * Returns {@code true} if the symbol table contains a function that is an exact match of both name and argument types.
     */
    public boolean containsFunction(final String name, final List<Type> argTypes) {
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
    public Function getFunction(final String name, final List<Type> argTypes) {
        if (parent != null) {
            return parent.getFunction(name, argTypes);
        } else if (functions.containsKey(name)) {
            return functions.get(name).stream()
                    .map(Info::value)
                    .map(Function.class::cast)
                    .filter(function -> argTypes.equals(function.getArgTypes()))
                    .findFirst()
                    .orElseThrow(IllegalArgumentException::new);
        } else {
            throw new IllegalArgumentException("undefined identifier: " + name);
        }
    }

    // -----------------------------------------------------------------------
    // Arrays:
    // -----------------------------------------------------------------------

    /**
     * Adds an array identifier to the symbol table. Note that regular symbols and arrays have
     * different name spaces by default. If this does not hold for some language, it must be
     * enforced in the semantics parser.
     *
     * @param identifier The identifier to add. Must represent an array.
     * @param declaration The array declaration containing subscript expressions.
     */
    public void addArray(Identifier identifier, ArrayDeclaration declaration) {
        if (!(identifier.type() instanceof Arr)) {
            throw new IllegalArgumentException("expected type array, not " + identifier.type());
        }
        arrays.put(identifier.name(), new Info(identifier, declaration));
    }

    /**
     * Returns {@code true} if the symbol table contains an array identifier with the given {@code name}.
     */
    public boolean containsArray(final String name) {
        var result = arrays.containsKey(name);
        if (!result && parent != null) {
            result = parent.containsArray(name);
        }
        return result;
    }

    /**
     * Returns the array identifier with the given {@code name}.
     */
    public Identifier getArrayIdentifier(String name) {
        return findArrayByName(name).identifier();
    }

    /**
     * Returns an unordered collection of all array identifiers in the symbol table.
     */
    public Collection<Identifier> arrayIdentifiers() {
        final var childIdentifiers = arrays.values().stream().map(Info::identifier).collect(toCollection(HashSet::new));
        if (parent != null) {
            final var parentIdentifiers = parent.arrayIdentifiers();
            parentIdentifiers.forEach(pi -> {
                // If there is no child identifier with the same name, add the parent identifier
                if (childIdentifiers.stream().noneMatch(ci -> ci.name().equals(pi.name()))) {
                    childIdentifiers.add(pi);
                }
            });
        }
        return childIdentifiers;
    }

    /**
     * Returns the type of the array with the specified name.
     */
    public Arr getArrayType(String name) {
        // We "know" the type is Arr
        return (Arr) findArrayByName(name).identifier().type();
    }

    /**
     * Returns the value (that is, the array declaration) of the array with the specified name.
     */
    public ArrayDeclaration getArrayValue(String name) {
        return (ArrayDeclaration) findArrayByName(name).value();
    }

    /**
     * Finds an info object for an array identifier by name.
     */
    private Info findArrayByName(final String name) {
        var result = arrays.get(name);
        if (result != null) {
            return result;
        }
        if (parent != null) {
            return parent.findArrayByName(name);
        }
        throw new IllegalArgumentException("undefined array: " + name);
    }

    // -----------------------------------------------------------------------
    // Common:
    // -----------------------------------------------------------------------

    /**
     * Returns the total size of the symbol table, that is, the number of regular symbols, arrays, and functions.
     */
    public int size() {
        return symbols.size() + arrays.size() + functions.size();
    }

    /**
     * Returns {@code true} if the symbol table is totally empty, that is, no regular symbols, arrays, or functions.
     */
    public boolean isEmpty() {
        return size() == 0;
    }

    // -----------------------------------------------------------------------

    private record Info(Identifier identifier, Object value, boolean isConstant) {
        private Info(final Identifier identifier, final Object value) {
            this(identifier, value, false);
        }
    }
}
