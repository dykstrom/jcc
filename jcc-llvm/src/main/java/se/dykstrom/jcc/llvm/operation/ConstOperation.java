/*
 * Copyright (C) 2024 Johan Dykstrom
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

package se.dykstrom.jcc.llvm.operation;

import se.dykstrom.jcc.common.ast.BooleanLiteral;
import se.dykstrom.jcc.common.types.Bool;
import se.dykstrom.jcc.common.types.FloatType;
import se.dykstrom.jcc.common.types.Identifier;
import se.dykstrom.jcc.common.types.Opaque;
import se.dykstrom.jcc.common.types.Str;
import se.dykstrom.jcc.common.types.Type;

import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static se.dykstrom.jcc.llvm.LlvmUtils.floatToLlvm;

public record ConstOperation(Identifier identifier, String value) implements LlvmOperation {

    @Override
    public String toText() {
        // An opaque constant is written as a constant of its underlying type
        final var type = Opaque.unwrap(identifier.type());
        // String constants must end with a null character
        final var v = (type instanceof Str) ? value + "\0" : value;
        return "@" + identifier.getMappedName() + " = private constant " +
                typeToText(type, v) + " " +
                valueToText(type, v);
    }

    private String typeToText(final Type type, final String value) {
        if (type instanceof Str) {
            return "[" + length(value) + " x i8]";
        } else {
            return type.llvmName();
        }
    }

    private String valueToText(final Type type, final String value) {
        return switch (type) {
            case Str _ -> "c\"" + encode(value) + "\"";
            case FloatType _ -> floatToLlvm(value, type);
            // A boolean literal holds -1 for true, as LiteralCodeGenerator notes, but LLVM writes it as 1
            case Bool _ -> BooleanLiteral.FALSE.getValue().equals(value) ? "0" : "1";
            default -> value;
        };
    }

    private int length(final String s) {
        return s.getBytes(UTF_8).length;
    }

    /**
     * Encodes a string for an LLVM {@code c"..."} constant. The only escape LLVM's syntax has is
     * {@code \XX} for one byte, and three characters need it: a control character, the double quote
     * that would otherwise end the constant, and the backslash that would otherwise start an
     * escape. Every other byte - including the continuation bytes of a multi-byte UTF-8 sequence -
     * is passed through, so the constant holds exactly the source bytes, and the byte count the
     * array type declares is unaffected by the escaping.
     */
    private String encode(final String s) {
        final var builder = new StringBuilder();
        s.codePoints()
         .boxed()
         .flatMap(cp -> {
             if (cp < 32 || cp == '"' || cp == '\\') {
                 return String.format("\\%02X", cp).codePoints().boxed();
             } else {
                 return Stream.of(cp);
             }
         })
         .forEach(builder::appendCodePoint);
        return builder.toString();
    }

    @Override
    public String toString() {
        return toText();
    }
}
