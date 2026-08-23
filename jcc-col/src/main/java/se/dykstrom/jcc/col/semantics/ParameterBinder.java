/*
 * Copyright (C) 2026 Johan Dykstrom
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

package se.dykstrom.jcc.col.semantics;

import se.dykstrom.jcc.common.ast.Declaration;
import se.dykstrom.jcc.common.ast.Node;
import se.dykstrom.jcc.common.compiler.SemanticsParser;
import se.dykstrom.jcc.common.compiler.TypeManager;
import se.dykstrom.jcc.common.error.DuplicateException;
import se.dykstrom.jcc.common.semantics.AbstractSemanticsParserComponent;
import se.dykstrom.jcc.common.semantics.VariableUsageTracker;
import se.dykstrom.jcc.common.types.Identifier;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Adds the formal parameters of a function to its local symbol table. Named and anonymous
 * functions bind their parameters identically, so both go through this class.
 *
 * @author Johan Dykstrom
 */
public final class ParameterBinder<T extends TypeManager> extends AbstractSemanticsParserComponent<T> {

    private final VariableUsageTracker usageTracker;

    public ParameterBinder(final SemanticsParser<T> semanticsParser, final VariableUsageTracker usageTracker) {
        super(semanticsParser);
        this.usageTracker = usageTracker;
    }

    /**
     * Adds the given declarations to the (function-local) symbol table as parameters, reporting
     * duplicate names against the given node, and returns the names. The declaration types must
     * already be resolved. The returned names are what the caller passes to
     * {@link VariableUsageTracker#check(Set, java.util.function.BiConsumer)} and
     * {@link VariableUsageTracker#restore(Set)} after checking the function body.
     *
     * <p>Note: only scalar parameters are supported for now.
     */
    public Set<String> addParameters(final Node node, final List<Declaration> declarations) {
        final Set<String> parameterNames = new HashSet<>();
        for (final var declaration : declarations) {
            final var name = declaration.name();
            if (parameterNames.contains(name)) {
                final var msg = "parameter '" + name + "' is already defined, with type " +
                                types().getTypeName(symbols().getType(name));
                reportError(node, msg, new DuplicateException(msg, name));
            }
            parameterNames.add(name);
            symbols().addParameter(new Identifier(name, declaration.type()));
            usageTracker.declare(name, declaration);
        }
        return parameterNames;
    }
}
