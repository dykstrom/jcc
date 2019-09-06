# JCC

JCC, the Johan Compiler Collection, is a collection of toy compilers built using [ANTLR4](http://www.antlr.org) and [flat assembler](http://flatassembler.net). The current version of JCC compiles three programming languages: [Tiny](https://github.com/antlr/grammars-v4/tree/master/tiny), [Assembunny](http://adventofcode.com/2016/day/12), and a subset of [BASIC](https://en.wikipedia.org/wiki/BASIC).

## System Requirements

As of now, JCC runs only on Windows. You also need a Java runtime.

JCC version 0.6.5 and earlier requires Java 8; download it [here](https://java.com/download) or [here](https://docs.aws.amazon.com/corretto/latest/corretto-8-ug/downloads-list.html).

JCC version 0.7.0 and later requires Java 11; download it [here](https://jdk.java.net/java-se-ri/11) or [here](https://docs.aws.amazon.com/corretto/latest/corretto-11-ug/downloads-list.html).

Executables created with JCC depend on the library [msvcrt.dll](https://en.wikipedia.org/wiki/Microsoft_Windows_library_files), which is a part of Windows. BASIC executables also depend on the BASIC standard library, jccbasic.dll, that is distributed together with JCC.

## Installation

Download the latest zip file from the GitHub [releases page](https://github.com/dykstrom/jcc/releases), and unzip it somewhere on your hard drive. Add the bin directory of the JCC distribution to your PATH environment variable. Now you should be able to run JCC like this:

    jcc foo.bas

To get some help, type:

    jcc -help

Please note that while JCC itself is licensed under GPLv3, the included version of 
[flat assembler](http://flatassembler.net) is licensed under a specific license. A copy of this license can be found in the fasm sub directory of the project.

## Languages

### Assembunny

[Assembunny](http://adventofcode.com/2016/day/12) is a made up programming language from the programming challenge [Advent of Code 2016](http://adventofcode.com/2016). It is a small assembly language with only four instructions: _inc_, _dec_, _cpy_, and _jnz_. To make the language more interesting I have also added support for the _outn_ instruction from the Assembunny extension [Assembunny-Plus](https://github.com/broad-well/assembunny-plus/blob/master/doc/spec.md).

This is an example of Assembunny code:

    cpy 3 a
    inc a
    outn a

Assembunny files end with the file extension ".asmb".

### BASIC

[BASIC](https://en.wikipedia.org/wiki/BASIC) was invented in the sixties, and became very popular on home computers in the eighties. JCC BASIC is inspired by
[Microsoft QuickBASIC](https://en.wikipedia.org/wiki/QuickBASIC) 4.5 from 1988. The current version of JCC implements a subset of BASIC. It does, however, come with a mark-and-sweep garbage collector to keep track of dynamic strings.

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
    <td valign='top'>Data Types</td>
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
        LINE INPUT<br>
        PRINT<br>
        RANDOMIZE<br>
        REM<br>
        SWAP
    </td>
  <tr>
    <td>Functions</td>
    <td>
        abs, asc, atn, cdbl, chr$, cint, cos, date$, exp, fix, hex$, instr, 
        int, lcase$, left$, len, log, ltrim$, mid$, oct$, right$, rnd, rtrim$,
        sgn, sin, space$, sqr, str$, string$, tan, time$, timer, ucase$, val
    </td>
  </tr>
</table>

BASIC files end with the file extension ".bas". BASIC executables require the BASIC standard library to run. This library is distributed together with JCC in the form of a DLL file: jccbasic.dll.

### Tiny

[Tiny](https://github.com/antlr/grammars-v4/tree/master/tiny) is a small programming language, designed for educational purposes.

A typical Tiny program looks like this:

    BEGIN
        READ a, b
        c := a + b
        WRITE c
    END

Tiny files end with the file extension ".tiny".

[![Build Status](https://travis-ci.org/dykstrom/jcc.svg?branch=master)](https://travis-ci.org/dykstrom/jcc)
