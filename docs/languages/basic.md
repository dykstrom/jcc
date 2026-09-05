# BASIC

[BASIC](https://en.wikipedia.org/wiki/BASIC) was invented in the 1960s and became
hugely popular on home computers in the 1980s. JCC's dialect is inspired by
[Microsoft QuickBASIC](https://en.wikipedia.org/wiki/QuickBASIC) 4.5 from 1988.
JCC implements a subset of QuickBASIC, and adds a mark-and-sweep garbage collector
to manage dynamic strings.

## Example

The program below computes all prime numbers less than a given number `N`:

```BASIC
' Calculate all primes less than a number N

CONST N = 100

DIM index AS INTEGER
DIM isPrime AS INTEGER
DIM maxIndex as INTEGER
DIM number AS INTEGER
DIM primes(N) AS INTEGER

number = 2
WHILE number < N

    ' Check if number is prime
    isPrime = 1
    index = 0
    WHILE isPrime AND index < maxIndex
        ' If number is dividable by any prime found so far, it is not prime
        isPrime = number MOD primes(index)
        index = index + 1
    WEND

    ' Print number if prime
    IF isPrime THEN
        PRINT number
        primes(maxIndex) = number
        maxIndex = maxIndex + 1
    END IF

    number = number + 1
WEND
```

## Language summary

The table below lists the BASIC constructs implemented so far.

| Category | Implemented |
|----------|-------------|
| Data Types | `DOUBLE` (64-bit), `INTEGER` (64-bit), `STRING`, and static arrays of those. Dynamic arrays are not supported. |
| Arithmetic Operators | `^` `+` `-` `*` `/` `\` `MOD` |
| Relational Operators | `=` `<>` `>` `>=` `<` `<=` |
| Bitwise Operators | `AND`, `EQV`, `IMP`, `NOT`, `OR`, `XOR` |
| Control Structures | `GOSUB`-`RETURN`, `GOTO`, `IF`-`GOTO`, `IF`-`THEN`-`ELSE` (including `ELSEIF`), `ON`-`GOSUB`-`RETURN`, `ON`-`GOTO`, `WHILE`-`WEND` |
| Statements | `CLS`, `CONST`, `DEFDBL`, `DEFINT`, `DEFSTR`, `DIM`, `END`, `LET`, `LINE INPUT`, `OPTION BASE`, `PRINT`, `RANDOMIZE`, `REM`, `SLEEP`, `SWAP`, `SYSTEM` |
| Functions | `abs`, `asc`, `atn`, `cdbl`, `chr$`, `cint`, `command$`, `cos`, `csrlin`, `cvd`, `cvi`, `date$`, `exp`, `fix`, `hex$`, `inkey$`, `instr`, `int`, `lbound`, `lcase$`, `left$`, `len`, `log`, `ltrim$`, `mid$`, `mkd$`, `mki$`, `oct$`, `pos`, `right$`, `rnd`, `rtrim$`, `sgn`, `sin`, `space$`, `sqr`, `str$`, `string$`, `tan`, `time$`, `timer`, `ubound`, `ucase$`, `val` |
| User-defined Functions | `DEF FN` expression functions |

Note that BASIC keywords are case-insensitive, but built-in function names must be
written in lowercase.

## QuickBASIC statements JCC does not have

A QuickBASIC statement JCC has not implemented is refused by name, together with what to
write instead:

```
prog.bas:1:1 error: 'FOR ... NEXT' is not supported by JCC; use 'WHILE ... WEND'
    1 | FOR i = 1 TO 10
      | ^
```

| Statement | Write instead |
|-----------|---------------|
| `FOR ... NEXT` | `WHILE ... WEND` |
| `DO ... LOOP` | `WHILE ... WEND` |
| `SELECT CASE` | `IF ... ELSEIF ... END IF` |
| `SUB` | `GOSUB ... RETURN` |
| `FUNCTION` | `DEF FN` |
| `EXIT` | `GOTO` |
| `INPUT` | `LINE INPUT` |
| `PRINT USING` | `PRINT` and the string functions |
| `REDIM`, `ERASE` | `DIM` &ndash; arrays are static |
| `DATA`, `READ`, `RESTORE` | assignments in code |
| `OPEN`, `CLOSE` | &ndash; file I/O is not available |
| `LOCATE`, `COLOR` | &ndash; screen control is not available |
| `TYPE` | &ndash; user-defined types are not available |

A whole block is refused once: the `NEXT` of a `FOR`, the `CASE`s and `END SELECT` of a
`SELECT CASE`, and the `END SUB` of a `SUB` do not repeat the message.

The keywords above are *soft* keywords: they are only keywords at the start of a
statement, so a program that uses `data`, `type`, `next` or `step` as a variable name,
a label, or an array still compiles.

## Variable and array types

A variable gets its type from the first of these that applies: the type specifier at
the end of its name (`%` for integer, `$` for string, `#` for double), the `AS` clause
of a `DIM` statement, a `DEFINT`/`DEFSTR`/`DEFDBL` statement covering its first letter,
or the default type, which is `DOUBLE`. (QuickBASIC's default type is `SINGLE`, which
JCC does not have.)

The `AS` clause of a `DIM` statement is therefore optional, as in QuickBASIC:

```BASIC
DIM count%(10)          ' Array of integer
DIM name$(10)           ' Array of string
DEFINT i-n : DIM i(10)  ' Array of integer
DIM value(10)           ' Array of double, the default type
```

An array that is used without having been declared is created implicitly, again as in
QuickBASIC. It gets as many dimensions as its first use has subscripts, and the
inclusive upper bound 10 in every dimension &ndash; so `total%(3) = 7` is equivalent to
writing `DIM total%(10) AS INTEGER` first. Compile with `-Wundefined-variable` to be
warned where this happens.

## Program lines

A statement ends at the end of its line. Several statements may share a line if
they are separated by colons:

```BASIC
a = 1 : b = 2 : PRINT a + b
```

A line may begin with a line number or a label, and either may stand alone on its
own line — useful for labelling the target of a `GOSUB`:

```BASIC
GOSUB printIt
END

printIt:
PRINT "hello"
RETURN
```

Blank lines and comment-only lines are allowed anywhere, and a comment may trail the
last statement on a line without a colon in front of it:

```BASIC
a = 2147483649     ' does not fit in 32 bits
```

To spread one statement over several lines, end each unfinished line with an
underscore, as in QuickBASIC 4.5:

```BASIC
total = price _
      + freight _
      + vat
```

The underscore must be the last character on the line, apart from trailing spaces
or tabs. An underscore inside a comment or a string is just an ordinary character,
so neither a comment nor a `REM` can be continued this way.

Block constructs occupy whole lines: `WHILE` and its `WEND`, and `IF`, `ELSEIF`,
`ELSE` and `END IF`, each need a line of their own. `THEN` followed by a statement
is the single-line form of `IF`; `THEN` at the end of a line — with or without a
trailing comment — opens a block that must be closed by `END IF`.

`ELSEIF` is one word. Written as two, it is refused with a message saying so,
because in QuickBASIC `ELSE IF` is an `ELSE` holding a nested block `IF` and needs
a second `END IF` — so it does not mean what it looks like:

```basic
IF a THEN
    PRINT 1
ELSE IF b THEN    ' error: 'ELSE IF' is not 'ELSEIF'
    PRINT 2
END IF
```

Either close the words up, or put the nested `IF` on a line of its own. The
single-line form is unaffected: `IF a THEN PRINT 1 ELSE IF b THEN PRINT 2` is an
`ELSE` whose statement is a single-line `IF`, and is accepted.

## Operator precedence

When several operators appear in one expression, they are applied in the order
below (following QuickBASIC 4.5). Operators higher in the table bind tighter;
use parentheses to override. Operators on the same row share a precedence level
and are evaluated left to right.

| Precedence | Operators | Category |
|:----------:|-----------|----------|
| highest    | `( )`     | Grouping |
|            | `^`       | Exponentiation |
|            | `-`       | Negation (unary minus) |
|            | `*` `/`   | Multiplication, division |
|            | `\`       | Integer division |
|            | `MOD`     | Modulo |
|            | `+` `-`   | Addition, subtraction |
|            | `=` `<>` `>` `>=` `<` `<=` | Relational |
|            | `NOT`     | Bitwise NOT |
|            | `AND`     | Bitwise AND |
|            | `OR`      | Bitwise OR |
|            | `XOR`     | Bitwise XOR |
|            | `EQV`     | Bitwise EQV |
| lowest     | `IMP`     | Bitwise IMP |

For example, `10 MOD 4 \ 2` is `10 MOD (4 \ 2)` = 0, and `a XOR b OR c` is
`a XOR (b OR c)`. Relational operators are left-associative and may be chained:
`1 = 2 = 3` parses as `(1 = 2) = 3`.

## File extension and runtime

BASIC source files use the `.bas` extension. BASIC executables require the BASIC
standard library to run. This library, `libjccbas.a`, is distributed together
with JCC.
