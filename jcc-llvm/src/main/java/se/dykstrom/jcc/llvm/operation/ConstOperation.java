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

import se.dykstrom.jcc.common.types.Identifier;

import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;

public record ConstOperation(Identifier identifier, String value) implements LlvmOperation {

    @Override
    public String toText() {
        return "@" + identifier.getMappedName() + " = private constant " +
               "[" + length(value) + " x i8] " +
               "c\"" + encode(value) + "\"";
    }

    private int length(final String s) {
        return s.getBytes(UTF_8).length;
    }

    private String encode(final String s) {
        final var builder = new StringBuilder();
        s.codePoints()
         .boxed()
         .flatMap(cp -> {
             if (cp < 32) {
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
