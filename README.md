# JCC

[![linux build](https://github.com/dykstrom/jcc/actions/workflows/linux.yml/badge.svg)](https://github.com/dykstrom/jcc/actions/workflows/linux.yml)
[![macos build](https://github.com/dykstrom/jcc/actions/workflows/macos.yml/badge.svg)](https://github.com/dykstrom/jcc/actions/workflows/macos.yml)
[![build windows](https://github.com/dykstrom/jcc/actions/workflows/windows.yml/badge.svg)](https://github.com/dykstrom/jcc/actions/workflows/windows.yml)
[![Open Issues](https://img.shields.io/github/issues/dykstrom/jcc)](https://github.com/dykstrom/jcc/issues)
[![Latest Release](https://img.shields.io/github/v/release/dykstrom/jcc?display_name=release)](https://github.com/dykstrom/jcc/releases)
![Downloads](https://img.shields.io/github/downloads/dykstrom/jcc/total)
![License](https://img.shields.io/github/license/dykstrom/jcc)
![Top Language](https://img.shields.io/github/languages/top/dykstrom/jcc)

JCC, the Johan Compiler Collection, is a collection of toy compilers built with
[ANTLR4](http://www.antlr.org). It compiles four small programming languages —
[BASIC](docs/languages/basic.md), [COL](docs/languages/col.md),
[Tiny](docs/languages/tiny.md), and [Assembunny](docs/languages/assembunny.md) —
to native executables.

JCC compiles to [LLVM IR](docs/LLVM.md), which Clang turns into a native executable.

## System Requirements

* Windows, Linux, or macOS
* Clang 20+, except Clang 22

JCC runs on Windows, Linux, and macOS. On x86-64, the executables JCC produces require SSE4.1 (any CPU from 2008 onwards). Clang is not bundled with JCC: you need to install [Clang](https://clang.llvm.org) (version 20 or later, but not 22, which miscompiles COL's `become`) yourself. See [Using LLVM as Backend](docs/LLVM.md) for installation instructions. BASIC and COL executables depend on the static standard libraries libjccbas.a and libjcccol.a respectively, which are distributed together with JCC.

JCC is written in Java, but you do not need Java to run it: each JCC package includes its own Java runtime, and JCC always uses that runtime. To build JCC yourself, you need JDK 25 or later; see [Building and Testing JCC with LLVM](docs/LLVM.md#building-and-testing-jcc-with-llvm).

## Installation

Download the latest package for your operating system and CPU from the GitHub
[releases page](https://github.com/dykstrom/jcc/releases), for example `jcc-<version>-linux-x86_64.tar.gz`.
Unpack it somewhere on your hard drive. Add the bin directory of the JCC distribution
to the Path environment variable for your account.

On macOS, if you downloaded the package with a web browser, macOS can block the bundled Java runtime.
To remove the block, run:

```
xattr -dr com.apple.quarantine <jcc directory>
```

## Usage

With JCC in your Path, you can run it like this:

```
jcc <source file>
```

To get help, type:

```
jcc --help
```

JCC compiles through Clang; see [Using LLVM as Backend](docs/LLVM.md) for details.

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
