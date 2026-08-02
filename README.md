# JCC

[![linux build](https://github.com/dykstrom/jcc/actions/workflows/linux.yml/badge.svg)](https://github.com/dykstrom/jcc/actions/workflows/linux.yml)
[![macos build](https://github.com/dykstrom/jcc/actions/workflows/macos.yml/badge.svg)](https://github.com/dykstrom/jcc/actions/workflows/macos.yml)
[![build windows](https://github.com/dykstrom/jcc/actions/workflows/windows.yml/badge.svg)](https://github.com/dykstrom/jcc/actions/workflows/windows.yml)
[![Open Issues](https://img.shields.io/github/issues/dykstrom/jcc)](https://github.com/dykstrom/jcc/issues)
[![Latest Release](https://img.shields.io/github/v/release/dykstrom/jcc?display_name=release)](https://github.com/dykstrom/jcc/releases)
![Downloads](https://img.shields.io/github/downloads/dykstrom/jcc/total)
![License](https://img.shields.io/github/license/dykstrom/jcc)
![Top Language](https://img.shields.io/github/languages/top/dykstrom/jcc)
[![JDK compatibility: 21+](https://img.shields.io/badge/JDK_compatibility-21+-blue.svg)](https://adoptium.net)

JCC, the Johan Compiler Collection, is a collection of toy compilers built with
[ANTLR4](http://www.antlr.org). It compiles four small programming languages —
[BASIC](docs/languages/basic.md), [COL](docs/languages/col.md),
[Tiny](docs/languages/tiny.md), and [Assembunny](docs/languages/assembunny.md) —
to native executables.

JCC has two backends: [LLVM](docs/LLVM.md), which emits LLVM IR compiled by Clang,
and [flat assembler](http://flatassembler.net) (FASM), which emits x86-64 assembly.
The LLVM backend is the default. The FASM backend is deprecated and will be removed
in a future release; select it with `--backend FASM`.

## System Requirements

The requirements depend on which backend you use.

### LLVM backend (default)

* Windows, Linux, or macOS
* Java 21+
* Clang 20+

The LLVM backend works on Windows, Linux, and macOS, but it is not bundled with JCC: you need to install [Clang](https://clang.llvm.org) (version 20 or later) yourself. See [Using LLVM as Backend](docs/LLVM.md) for installation instructions. BASIC and COL executables depend on the static standard libraries libjccbas.a and libjcccol.a respectively, which are distributed together with JCC.

### FASM backend (deprecated)

* Windows
* Java 21+

Executables created with the FASM backend depend on the library [msvcrt.dll](https://en.wikipedia.org/wiki/Microsoft_Windows_library_files), which is a part of Windows. BASIC executables also depend on the BASIC standard library, libjccbas.dll, that is distributed together with JCC.

You can download the Java runtime from [Adoptium](https://adoptium.net).

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

By default JCC uses the LLVM backend; see [Using LLVM as Backend](docs/LLVM.md) for details. The FASM backend is deprecated and will be removed in a future release; select it with `--backend FASM`.

## Languages

JCC compiles four small languages. Each has its own guide:

| Language | Description | Extension |
|----------|-------------|-----------|
| [BASIC](docs/languages/basic.md) | A subset of Microsoft QuickBASIC 4.5, with a garbage collector for dynamic strings. | `.bas` |
| [COL](docs/languages/col.md) | A statically typed language with functional elements, inspired by BASIC, C, Go, and Rust. | `.col` |
| [Tiny](docs/languages/tiny.md) | A minimal educational language for reading input, computing, and writing output. | `.tiny` |
| [Assembunny](docs/languages/assembunny.md) | A tiny assembly language from Advent of Code 2016. | `.asmb` |

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for the branch workflow, and
[AGENTS.md](AGENTS.md) for build and test commands.
