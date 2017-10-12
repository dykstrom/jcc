# JCC

JCC, the Johan Compiler Collection, is a collection of toy compilers built using 
[ANTLR4](http://www.antlr.org) and [flat assembler](http://flatassembler.net). 
The current version of JCC is far from being complete. It supports the 
[Tiny](https://en.wikipedia.org/wiki/Tiny_programming_language) programming language, 
as well as a small subset of [Basic](https://en.wikipedia.org/wiki/BASIC).

Enough of the Basic language has been implemented to enable you to write the kind of
program everyone started out with when I was young:

    10 PRINT "JOHAN"
    20 GOTO 10

Actually, you can also write simple arithmetic expressions, and expect the correct 
result:

    10 PRINT 1 + 2 * (3 - 4) / 5

Version 0.1.2 of the compiler added support for variables, so now you can write something 
like:

    10 LET X% = 5
    20 LET SQUARE% = X% * X%
    30 PRINT "The square of "; X%; " is "; SQUARE%

The line numbers, the LET keyword, and the % type specifier are all optional, so the above code can also
be written:

    X = 5
    SQUARE = X * X
    PRINT "The square of "; X; " is "; SQUARE

Version 0.1.3 added support for boolean expressions, as well as IF and WHILE statements:

```vb.net
REM Calculate all primes less than a number N
N = 100

number = 2
WHILE number < N

    REM Check if number is prime
    isPrime = 1
    divisor = 2
    WHILE isPrime = 1 AND divisor <= number / 2
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

The current version of JCC has a number of limitations. Some of them are:

* It runs only on Windows.
* It generates x86-64 machine code that again runs only on Windows.
* There is no optimization, sometimes resulting in funny code.

### System Requirements

You will need Java 8 to run JCC and, as mentioned above, you will also need Windows.

### Installation

Download the zip file and unzip it somewhere on your hard drive. Add the bin directory 
of the JCC distribution to your PATH environment variable. Now you should be able to 
run JCC like this:

    jcc foo.bas

To get some help, type:

    jcc -help

Please note that while JCC itself is licensed under GPLv3, the included version of 
the [flat assembler](http://flatassembler.net) is licensed under a specific license.
A copy of this license can be found in the fasm sub directory of the project.

[![Build Status](https://travis-ci.org/dykstrom/jcc.svg?branch=master)](https://travis-ci.org/dykstrom/jcc)
