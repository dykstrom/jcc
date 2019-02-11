# JCC

JCC, the Johan Compiler Collection, is a collection of toy compilers built using 
[ANTLR4](http://www.antlr.org) and [flat assembler](http://flatassembler.net). 
The current version of JCC supports three programming languages: 
[Tiny](https://github.com/antlr/grammars-v4/tree/master/tiny), 
[Assembunny](http://adventofcode.com/2016/day/12),
and a subset of [BASIC](https://en.wikipedia.org/wiki/BASIC).

## System Requirements

To run JCC you need Windows and [Java 8](https://www.java.com) or later. Executables created
with JCC depend on the library msvcrt.dll, which is usually present in all Windows systems.

## Installation

Download the latest zip file from the GitHub 
[releases page](https://github.com/dykstrom/jcc/releases),
and unzip it somewhere on your hard drive. Add the bin directory of the JCC distribution 
to your PATH environment variable. Now you should be able to run JCC like this:

    jcc foo.bas

To get some help, type:

    jcc -help

Please note that while JCC itself is licensed under GPLv3, the included version of 
[flat assembler](http://flatassembler.net) is licensed under a specific license.
A copy of this license can be found in the fasm sub directory of the project.

## Languages

### Assembunny

[Assembunny](http://adventofcode.com/2016/day/12) is a made up programming language from
the programming challenge [Advent of Code 2016](http://adventofcode.com/2016). It is a small
assembly language with only four instructions: _inc_, _dec_, _cpy_, and _jnz_. To make 
the language more interesting I have also added support for the _outn_ instruction from 
the Assembunny extension
[Assembunny-Plus](https://github.com/broad-well/assembunny-plus/blob/master/doc/spec.md).
This is an example of Assembunny code:

    cpy 3 a
    inc a
    outn a

Assembunny files end with the file extension ".asmb".

### BASIC

[BASIC](https://en.wikipedia.org/wiki/BASIC) was invented in the sixties, and became very 
popular on home computers in the eighties. JCC finds inspiration in 
[Microsoft QuickBASIC](https://en.wikipedia.org/wiki/QuickBASIC) 4.5 from 1988, but does 
not aim to be 100% compatible. The current version of JCC implements only a subset of
BASIC.

The example below is a short program to compute prime numbers:

```vbnet
REM Calculate all primes less than a number N
N = 100

number = 2
WHILE number < N

    REM Check if number is prime
    isPrime = 1
    divisor = 2
    WHILE isPrime = 1 AND divisor <= number \ 2
        REM If number is dividable by divisor, it is not prime
        IF number MOD divisor = 0 THEN
            isPrime = 0
        END IF
        divisor = divisor + 1
    WEND

    REM Print prime number
    IF isPrime = 1 THEN
        PRINT number
    END IF

    number = number + 1
WEND
```

This table specifies the BASIC constructs that have been implemented so far:

<table>
  <tr>
    <td valign='top'>Types</td>
    <td>
        BOOLEAN<br/>
        DOUBLE (64-bit)<br/>
        INTEGER (64-bit)<br/>
        STRING
    </td>
  </tr>
  <tr>
    <td>Arithmetic Operators</td>
    <td>+ - * / \ MOD</td>
  </tr>
  <tr>
    <td>Relational Operators</td>
    <td>= <> > >= < <=</td>
  </tr>
  <tr>
    <td>Conditional Operators</td>
    <td>AND, NOT, OR, XOR</td>
  </tr>
  <tr>
    <td valign='top'>Control Structures</td>
    <td>
        GOSUB-RETURN<br>
        GOTO<br>
        IF-GOTO<br>
        IF-THEN-ELSE (including ELSEIF)<br>
        ON-GOSUB-RETURN<br>
        ON-GOTO<br>
        WHILE
    </td>
  </tr>
  <tr>
    <td>Statements</td>
    <td>
        DEFBOOL<br>
        DEFDBL<br>
        DEFINT<br>
        DEFSTR<br>
        DIM<br>
        END<br>
        LET<br>
        PRINT<br>
        REM
    </td>
  <tr>
    <td>Functions</td>
    <td>
        abs, asc, atn, cdbl, chr$, cint, cos, exp, fix, hex$, instr, 
        int, lcase$, len, log, oct$, sgn, sin, space$, sqr, tan, ucase$, 
        val
    </td>
  </tr>
</table>

BASIC files end with the file extension ".bas".

### Tiny

[Tiny](https://github.com/antlr/grammars-v4/tree/master/tiny) is a small programming language, 
designed for educational purposes. A typical Tiny program looks like this:

    BEGIN
        READ a, b
        c := a + b
        WRITE c
    END

Tiny files end with the file extension ".tiny".

[![Build Status](https://travis-ci.org/dykstrom/jcc.svg?branch=master)](https://travis-ci.org/dykstrom/jcc)
