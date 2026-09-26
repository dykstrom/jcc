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

package se.dykstrom.jcc.col.semantics.statement;

import se.dykstrom.jcc.col.ast.statement.TypeDefStatement;
import se.dykstrom.jcc.col.compiler.OpaqueConversionFunction;
import se.dykstrom.jcc.col.compiler.OpaqueStringFunction;
import se.dykstrom.jcc.common.ast.Statement;
import se.dykstrom.jcc.common.compiler.SemanticsParser;
import se.dykstrom.jcc.common.compiler.TypeManager;
import se.dykstrom.jcc.common.error.DuplicateException;
import se.dykstrom.jcc.common.error.InvalidTypeException;
import se.dykstrom.jcc.common.semantics.AbstractSemanticsParserComponent;
import se.dykstrom.jcc.common.semantics.statement.StatementSemanticsParser;
import se.dykstrom.jcc.common.types.Bool;
import se.dykstrom.jcc.common.types.F32;
import se.dykstrom.jcc.common.types.F64;
import se.dykstrom.jcc.common.types.Fun;
import se.dykstrom.jcc.common.types.I32;
import se.dykstrom.jcc.common.types.I64;
import se.dykstrom.jcc.common.types.NamedType;
import se.dykstrom.jcc.common.types.Opaque;
import se.dykstrom.jcc.common.types.Str;
import se.dykstrom.jcc.common.types.Type;

public class TypeDefPass1SemanticsParser<T extends TypeManager> extends AbstractSemanticsParserComponent<T>
        implements StatementSemanticsParser<TypeDefStatement> {

    public TypeDefPass1SemanticsParser(final SemanticsParser<T> semanticsParser) {
        super(semanticsParser);
    }

    @Override
    public Statement parse(final TypeDefStatement statement) {
        final var name = statement.name();
        if (types().getTypeFromName(name).isPresent()) {
            final var msg = "cannot redefine type: " + name;
            reportError(statement, msg, new DuplicateException(msg, name));
            return statement;
        }

        final var underlying = resolveType(statement, statement.type(), types());
        if (containsNamedType(underlying)) {
            // An undefined type name has already been reported
            return statement;
        }
        if (!isValidUnderlyingType(underlying)) {
            final var msg = "cannot define type " + name + " as " + types().getTypeName(underlying)
                            + ": the underlying type must be i32, i64, f32, f64, bool, string or a function type";
            reportError(statement, msg, new InvalidTypeException(msg, underlying));
            return statement;
        }

        final var opaque = new Opaque(name, underlying);
        types().defineTypeName(name, opaque);
        defineFunction(statement, new OpaqueConversionFunction(name, underlying, opaque));
        // An opaque function type can be wrapped, but not unwrapped
        if (underlying instanceof Fun) {
            return statement.withType(underlying);
        }
        defineFunction(statement, new OpaqueConversionFunction(types().getTypeName(underlying), opaque, underlying));
        // Over string, the conversion back to the underlying type is already string(Name)
        if (!(underlying instanceof Str)) {
            defineFunction(statement, new OpaqueStringFunction(opaque));
        }
        return statement.withType(underlying);
    }

    private static boolean isValidUnderlyingType(final Type type) {
        return type instanceof I32 || type instanceof I64 || type instanceof F32 || type instanceof F64
               || type instanceof Bool || type instanceof Str || type instanceof Fun;
    }

    private static boolean containsNamedType(final Type type) {
        return switch (type) {
            case NamedType ignored -> true;
            case Fun fun -> containsNamedType(fun.getReturnType())
                            || fun.getArgTypes().stream().anyMatch(TypeDefPass1SemanticsParser::containsNamedType);
            default -> false;
        };
    }
}
