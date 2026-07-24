# Assembunny

[Assembunny](http://adventofcode.com/2016/day/12) is a made-up assembly language
from the programming challenge [Advent of Code 2016](http://adventofcode.com/2016).
It has four core instructions — `inc`, `dec`, `cpy`, and `jnz`. JCC also supports
the `outn` instruction from the
[Assembunny-Plus](https://github.com/broad-well/assembunny-plus/blob/master/doc/spec.md)
extension, which makes the language interesting enough to produce output.

## Example

```
cpy 3 a
inc a
outn a
```

## Language summary

| Instruction | Meaning |
|-------------|---------|
| `inc x` | Increment register `x` |
| `dec x` | Decrement register `x` |
| `cpy x y` | Copy value `x` into register `y` |
| `jnz x y` | Jump `y` instructions if `x` is non-zero |
| `outn x` | Print the value of `x` as a number (Assembunny-Plus) |

## File extension and runtime

Assembunny source files use the `.asmb` extension. Assembunny programs have no
runtime library dependency beyond the C runtime.
