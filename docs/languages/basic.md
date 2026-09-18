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

## Reserved words

Every keyword above that JCC implements is reserved and cannot be used as a variable name,
as in QuickBASIC 4.5. Using one is refused by name, rather than by the token set of the
statement the keyword begins:

```
prog.bas:1:1 error: 'print' is a reserved word and cannot be used as a variable name
    1 | print = 5
      | ^
```

The reserved words are `AND`, `AS`, `BASE`, `CLS`, `CONST`, `DEF`, `DEFDBL`, `DEFINT`,
`DEFSTR`, `DIM`, `ELSE`, `ELSEIF`, `END`, `EQV`, `GOSUB`, `GOTO`, `IF`, `IMP`, `INPUT`,
`LET`, `LINE`, `MOD`, `NOT`, `ON`, `OPTION`, `OR`, `PRINT`, `RANDOMIZE`, `REM`, `RETURN`,
`SLEEP`, `SWAP`, `SYSTEM`, `THEN`, `WEND`, `WHILE` and `XOR`.

`AS`, `BASE`, `INPUT` and `LINE` mean something in one position each &ndash; `AS` in a type
clause, `BASE` after `OPTION`, and `LINE` with the `INPUT` after it &ndash; so they are
refused by name wherever else they appear, in an expression as well as on the left of an
assignment:

```
prog.bas:2:7 error: 'line' is a reserved word and cannot be used as a variable name
    2 | PRINT line
      |       ^
```

Two of the words say something more useful than the sentence above. `ELSE`, `ELSEIF` and
`WEND` report that they have no matching block. `LET` still gives the parser's own message,
because `LET = 7` has to keep reading as an assignment with its variable left out.

The keywords of the statements JCC does *not* implement are the exception: they are soft
keywords, listed in the section above, and stay available as variable names.

## Variable and array types

A variable gets its type from the first of these that applies: the type suffix at
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

QuickBASIC's other two suffixes are refused by name, since JCC does not have the types they
stand for:

```
prog.bas:1:2 error: type suffix '!' (single precision) is not supported by JCC; use '#' for double precision
    1 | a! = 1.5
      |  ^
```

| Suffix | QuickBASIC type | Write instead |
|--------|-----------------|---------------|
| `!` | single precision | `#` |
| `&` | long | `%` |

A `DEFINT`, `DEFSTR` or `DEFDBL` statement takes single letters and letter ranges, separated
by commas, and a range must run in alphabetical order:

```BASIC
DEFINT i-n, x           ' i, j, k, l, m, n and x are integer
```

Anything else is refused by name:

```
prog.bas:1:8 error: 'ab' is not a single letter; DEFINT takes single letters and letter ranges: write 'DEFINT a-n'
    1 | DEFINT ab
      |        ^
prog.bas:2:8 error: 'n-a' is a reversed letter range; DEFINT takes ranges in alphabetical order: write 'DEFINT a-n'
    2 | DEFINT n-a
      |        ^
```

JCC has three types: `DOUBLE`, `INTEGER` and `STRING`. The type name in an `AS` clause is
case-insensitive like every other keyword, and it is not a reserved word &ndash; `double`,
`integer` and `string` are ordinary variable names outside an `AS` clause.

A name that is not one of the three is refused by name, and a QuickBASIC type JCC does not
have is refused with the type to use instead:

```
prog.bas:1:10 error: unknown type 'DOBLE'; did you mean 'DOUBLE'?
    1 | DIM a AS DOBLE
      |          ^
```

| Type | Write instead |
|------|---------------|
| `SINGLE` | `DOUBLE` |
| `LONG` | `INTEGER` |
| `CURRENCY` | `DOUBLE` |

Any other unknown name &ndash; including the name of a user-defined `TYPE`, which JCC does
not have either &ndash; gives `unknown type '<name>'`, with a suggestion when the name is
close to one of the three.

An array that is used without having been declared is created implicitly, again as in
QuickBASIC. It gets as many dimensions as its first use has subscripts, and the
inclusive upper bound 10 in every dimension &ndash; so `total%(3) = 7` is equivalent to
writing `DIM total%(10) AS INTEGER` first. Compile with `-Wundefined-variable` to be
warned where this happens.

## User-defined functions

A user-defined function is a single expression, defined with `DEF`, and its name must start
with `FN`, as in QuickBASIC:

```BASIC
DEF FNhyp(a, b) = SQR(a * a + b * b)
PRINT FNhyp(3, 4)
```

Without the prefix the name is refused, and the message names the function to write:

```
prog.bas:1:5 error: user-defined function names must start with 'FN': write 'DEF FNhyp'
    1 | DEF hyp(a, b) = SQR(a * a + b * b)
      |     ^
```

A parameter may have a type specifier or an `AS` clause, and the function's own return type
comes from the specifier on its name &ndash; `DEF FNhyp#(...)` returns a double. QuickBASIC's
multi-statement `FUNCTION` and `SUB` are not supported; see the table above.

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

BASIC writes equality as `=` &ndash; the same character as assignment &ndash; and inequality
as `<>`, and it spells conjunction and disjunction `AND` and `OR`. The C-style `==`, `!=`, `&&`
and `||` are refused by name:

```
prog.bas:1:7 error: BASIC uses '=' for equality, not '==': write 'a% = 1'
    1 | IF a% == 1 THEN PRINT "yes"
      |       ^
```

| Written | Write instead |
|---------|---------------|
| `==` | `=` |
| `!=` | `<>` |
| `&&` | `AND` |
| <code>&#124;&#124;</code> | `OR` |

`&&` and `||` bind like the `AND` and `OR` that replace them, which is the same relative order
they have in C, so `a || b && c` means `a OR (b AND c)` either way. Note that `AND` and `OR` are
bitwise operators taking integer operands, so replacing `&&` with `AND` may need a `%` or a `CINT`
as well.

With no space in front of it, `!=` is ambiguous: QuickBASIC reads `a!=1` as the single-precision
type suffix `!` followed by `=`, which JCC does not support either. That form names both readings
and leaves the choice to you.

## File extension and runtime

BASIC source files use the `.bas` extension. BASIC executables require the BASIC
standard library to run. This library, `libjccbas.a`, is distributed together
with JCC.
