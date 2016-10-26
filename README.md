# JCC

JCC, the Johan Compiler Collection, is a collection of toy compilers built using 
[ANTLR4](http://www.antlr.org) and [flat assembler](http://flatassembler.net). 
The current version of JCC is far from being complete. It supports the 
[Tiny](https://en.wikipedia.org/wiki/Tiny_programming_language) programming language, 
as well as a very small subset of [Basic](https://en.wikipedia.org/wiki/BASIC).

Enough of the Basic language has been implemented to enable you to write the kind of
program everyone started out with when I was young:

    10 PRINT "JOHAN"
    20 GOTO 10

Actually, you can also write simple arithmetic expressions, and expect the correct result:

    10 PRINT 1 + 2 * 3 - 4 / 5

The current version of JCC has a number of limitations. Some of them are:

* It runs only on Windows.
* It generates x86-64 machine code that again runs only on Windows.
* There is no optimization, sometimes resulting in funny code.

### System Requirements

You will need Java 8 to run JCC, and as mentioned above, you will also need Windows.

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
