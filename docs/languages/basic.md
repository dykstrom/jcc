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

<table>
  <tr>
    <td>Data Types</td>
    <td>
        DOUBLE (64-bit)<br/>
        INTEGER (64-bit)<br/>
        STRING<br/>
        Static arrays of the types above. Dynamic arrays are not supported.
    </td>
  </tr>
  <tr>
    <td>Arithmetic Operators</td>
    <td>^ + - * / \ MOD</td>
  </tr>
  <tr>
    <td>Relational Operators</td>
    <td>= &lt;&gt; &gt; &gt;= &lt; &lt;=</td>
  </tr>
  <tr>
    <td>Bitwise Operators</td>
    <td>AND, EQV, IMP, NOT, OR, XOR</td>
  </tr>
  <tr>
    <td>Control Structures</td>
    <td>
        GOSUB-RETURN<br>
        GOTO<br>
        IF-GOTO<br>
        IF-THEN-ELSE (including ELSEIF)<br>
        ON-GOSUB-RETURN<br>
        ON-GOTO<br>
        WHILE-WEND
    </td>
  </tr>
  <tr>
    <td>Statements</td>
    <td>
        CLS<br>
        CONST<br>
        DEFDBL<br>
        DEFINT<br>
        DEFSTR<br>
        DIM<br>
        END<br>
        LET<br>
        LINE INPUT<br>
        OPTION BASE<br>
        PRINT<br>
        RANDOMIZE<br>
        REM<br>
        SLEEP<br>
        SWAP<br>
        SYSTEM
    </td>
  </tr>
  <tr>
    <td>Functions</td>
    <td>
        abs, asc, atn, cdbl, chr$, cint, command$, cos, csrlin, cvd, cvi, date$, exp, fix, hex$, inkey$,
        instr, int, lbound, lcase$, left$, len, log, ltrim$, mid$, mkd$, mki$, oct$, pos, right$,
        rnd, rtrim$, sgn, sin, space$, sqr, str$, string$, tan, time$, timer, ubound,
        ucase$, val
    </td>
  </tr>
  <tr>
    <td>User-defined Functions</td>
    <td>
        DEF FN expression functions
    </td>
  </tr>
</table>

Note that BASIC keywords are case-insensitive, but built-in function names must be
written in lowercase.

## File extension and runtime

BASIC source files use the `.bas` extension. BASIC executables require the BASIC
standard library to run. This library is distributed together with JCC as
`libjccbas.dll` for the FASM backend and `libjccbas.a` for the LLVM backend.
