package se.dykstrom.jcc.llvm.operation;

import se.dykstrom.jcc.common.types.Identifier;

import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;

public record ConstOperation(Identifier identifier, String value) implements LlvmOperation {

    @Override
    public String toText() {
        return identifier.name() + " = private constant " +
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
