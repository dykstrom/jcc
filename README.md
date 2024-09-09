# JCC

[![Build Status](https://github.com/dykstrom/jcc/actions/workflows/maven.yml/badge.svg)](https://github.com/dykstrom/jcc/actions/workflows/maven.yml)
[![Open Issues](https://img.shields.io/github/issues/dykstrom/jcc)](https://github.com/dykstrom/jcc/issues)
[![Latest Release](https://img.shields.io/github/v/release/dykstrom/jcc?display_name=release)](https://github.com/dykstrom/jcc/releases)
![Downloads](https://img.shields.io/github/downloads/dykstrom/jcc/total)
![License](https://img.shields.io/github/license/dykstrom/jcc)
![Top Language](https://img.shields.io/github/languages/top/dykstrom/jcc)
[![JDK compatibility: 17+](https://img.shields.io/badge/JDK_compatibility-17+-blue.svg)](https://adoptium.net)

JCC, the Johan Compiler Collection, is a collection of toy compilers built using [ANTLR4](http://www.antlr.org) and [flat assembler](http://flatassembler.net). The current version of JCC compiles three programming languages: [Tiny](https://github.com/antlr/grammars-v4/tree/master/tiny), [Assembunny](http://adventofcode.com/2016/day/12), and a subset of [BASIC](https://en.wikipedia.org/wiki/BASIC).

## System Requirements

* Windows
* Java 17 or later

You can download the Java runtime from [Adoptium](https://adoptium.net).

Executables created with JCC depend on the library [msvcrt.dll](https://en.wikipedia.org/wiki/Microsoft_Windows_library_files), which is a part of Windows. BASIC executables also depend on the BASIC standard library, jccbasic.dll, that is distributed together with JCC.

## Installation

Download the latest zip file from the GitHub [releases page](https://github.com/dykstrom/jcc/releases), 
and unzip it somewhere on your hard drive. Add the bin directory of the JCC distribution 
to the Path environment variable for your account.

Please note that while JCC itself is licensed under GPLv3, the included version of
[flat assembler](http://flatassembler.net) is licensed under a specific license.
A copy of this license can be found in the fasm subdirectory of the project.

## Usage

With JCC in your Path, you can run it like this:

```
jcc <source file>
```

To get help, type:

```
jcc --help
```

This will print a message similar to this:

```
Usage: jcc [options] <source file>
  Options:
    --help
      Show this help text
    --version
      Show compiler version
    -O, -O1
      Optimize output
      Default: false
    -S
      Compile only; do not assemble
      Default: false
    -Wall
      Enable all warnings
      Default: false
    -Wundefined-variable
      Warn about undefined variables
      Default: false
    -assembler
      Use <assembler> as the backend assembler
      Default: fasm
    -assembler-include
      Set the assembler's include directory to <directory>
    -initial-gc-threshold
      Set the number of allocations before first garbage collection
      Default: 100
    -o
      Place output in <file>
    -print-gc
      Print messages at garbage collection
      Default: false
    -save-temps
      Save temporary intermediate files permanently
      Default: false
    -v
      Verbose mode
      Default: false
```

## Supported Languages

### Assembunny

[Assembunny](http://adventofcode.com/2016/day/12) is a made up programming language from the programming challenge [Advent of Code 2016](http://adventofcode.com/2016). It is a small assembly language with only four instructions: _inc_, _dec_, _cpy_, and _jnz_. To make the language more interesting I have also added support for the _outn_ instruction from the Assembunny extension [Assembunny-Plus](https://github.com/broad-well/assembunny-plus/blob/master/doc/spec.md).

This is an example of Assembunny code:

```
cpy 3 a
inc a
outn a
```

Assembunny files end with the file extension ".asmb".

### BASIC

[BASIC](https://en.wikipedia.org/wiki/BASIC) was invented in the sixties, and became very popular on home computers in the eighties. JCC BASIC is inspired by
[Microsoft QuickBASIC](https://en.wikipedia.org/wiki/QuickBASIC) 4.5 from 1988. The current version of JCC implements a subset of BASIC. It does, however, come with a mark-and-sweep garbage collector to keep track of dynamic strings.

The example below is a short program to compute prime numbers:

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

This table specifies the BASIC constructs that have been implemented so far:

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
    <td>+ - * / \ MOD</td>
  </tr>
  <tr>
    <td>Relational Operators</td>
    <td>= <> > >= < <=</td>
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
        SWAP
    </td>
  </tr>
  <tr>
    <td>Functions</td>
    <td>
        abs, asc, atn, cdbl, chr$, cint, cos, cvd, cvi, date$, exp, fix, hex$, instr, 
        int, lbound, lcase$, left$, len, log, ltrim$, mid$, mkd$, mki$, oct$, right$, 
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

BASIC files end with the file extension ".bas". BASIC executables require the BASIC standard library to run. This library is distributed together with JCC in the form of a DLL file: jccbasic.dll.

### Tiny

[Tiny](https://github.com/antlr/grammars-v4/tree/master/tiny) is a small programming language, designed for educational purposes.

A typical Tiny program looks like this:

```
BEGIN
    READ a, b
    c := a + b
    WRITE c
END
```

Tiny files end with the file extension ".tiny".
