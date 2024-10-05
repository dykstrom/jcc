# Using LLVM as Backend

JCC has experimental support for using [LLVM](https://llvm.org) as backend. You enable LLVM support
using the command line argument `--backend`:

```bash
$ jcc --backend LLVM ...
```

Currently, LLVM support is limited to the Tiny language. Furthermore, LLVM is not included in the
JCC package. You need to install LLVM, or to be more precise [Clang](https://clang.llvm.org), 
yourself. JCC with LLVM as backend has been tested on Windows, Linux, and macOS. Thus, the system
requirements for JCC with LLVM are:

* Java 17 or later
* Clang 18


### Installing Clang

Please refer to the [LLVM documentation](https://llvm.org) or your package manager for complete
instructions on how to install LLVM. The list below is a very brief summary.

* **Windows**: Download a prebuilt release from the [LLVM MinGW](https://github.com/mstorsjo/llvm-mingw)
  project. JCC has been tested with release `llvm-mingw-20240619-ucrt-x86_64.zip`.
* **macOS**: Install LLVM/Clang using [Homebrew](https://brew.sh): `brew install llvm`. Clang is
  also part of Xcode.
* **Linux**: Install LLVM/Clang using your package manager. For example on Ubuntu:
  `sudo apt-get install clang`.


### Building and Testing JCC with LLVM

To build JCC with LLVM and run the LLVM tests, you clone the JCC repo, and build it with Maven 
profile `llvm-tests`, for example:

```bash
$ git clone https://github.com/dykstrom/jcc.git
$ cd jcc
$ mvn -P llvm-tests clean verify
```


### Temporary Files

JCC generates LLVM IR code in a file with extension `.ll` and passes this file to Clang. If any of
the flags `-S` and `-save-temps` is specified, it is also passed on to Clang. The temporary files
generated and left behind in that case will include an assembly file with extension `.s` and an 
object file with extension `.o`.


### Using a Specific Version of Clang

You can specify which Clang you want to use by setting the `-assembler` flag to the full path of
Clang.
