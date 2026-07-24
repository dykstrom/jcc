# Tiny

[Tiny](https://github.com/antlr/grammars-v4/tree/master/tiny) is a small
programming language designed for educational purposes. It is the simplest of the
languages JCC compiles — just enough to read input, do arithmetic, and write
output.

## Example

```
BEGIN
    READ a, b
    c := a + b
    WRITE c
END
```

## Language summary

- `READ` a list of variables from input.
- `WRITE` a list of expressions to output.
- Assignment with `:=`.
- Integer arithmetic.

## File extension and runtime

Tiny source files use the `.tiny` extension. Tiny programs have no runtime
library dependency beyond the C runtime.
