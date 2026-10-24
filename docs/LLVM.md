# Using LLVM as Backend

LLVM is JCC's backend. JCC compiles every program to LLVM IR and hands it to Clang.

The LLVM backend supports all four JCC languages: Assembunny, BASIC, COL, and Tiny. LLVM is not
included in the JCC package, however. You need to install LLVM, or to be more precise
[Clang](https://clang.llvm.org), yourself. JCC with LLVM as backend has been tested on Windows, Linux, and macOS.
Thus, the system requirements for JCC with LLVM are:

* Java 21+
* Clang 20+, except Clang 22


### Installing Clang

Please refer to the [LLVM documentation](https://llvm.org) or your package manager for complete
instructions on how to install LLVM. The list below is a very brief summary.

* **Windows**: Download a prebuilt release from the [LLVM MinGW](https://github.com/mstorsjo/llvm-mingw)
  project. JCC has been tested with release `llvm-mingw-20261020-ucrt` (x86_64 and aarch64), which
  ships Clang 23. MSYS2's Clang is version 22 at the time of writing, which does not work (see below).
* **macOS**: Install LLVM/Clang using [Homebrew](https://brew.sh): `brew install llvm`. Clang is
  also part of Xcode.
* **Linux**: Install LLVM/Clang using your package manager. For example on Ubuntu:
  `sudo apt-get install clang`.

Clang 22 miscompiles COL's `become` on x86-64: a function with more parameters than fit in
registers loses the ones passed on the stack. JCC prints a warning when it compiles a program
that uses `become` with Clang 22. Use Clang 20, 21, or 23 and later.


### Building and Testing JCC with LLVM

To build JCC and run its tests, clone the repo and build it with Maven. The integration
tests compile and run real programs, so Clang must be on the path:

```bash
$ git clone https://github.com/dykstrom/jcc.git
$ cd jcc
$ mvn clean install
```


### Temporary Files

JCC generates LLVM IR code in a file with extension `.ll` and passes this file to Clang. If any of
the flags `-S` and `-save-temps` is specified, it is also passed on to Clang. The temporary files
generated and left behind in that case will include an assembly file with extension `.s` and an 
object file with extension `.o`.


### Using a Specific Version of Clang

You can specify which Clang you want to use by setting the `--clang` flag to the full path of
Clang.
