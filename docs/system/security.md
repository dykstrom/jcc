# Security

JCC is a command-line compiler with no network surface. Its trust boundary is local: it reads source files supplied on the command line, generates an intermediate `.ll` file, and invokes an external toolchain to produce a native executable.

## Process invocation

External processes are launched via `ProcessUtils.setUpProcess`, which uses `ProcessBuilder` with an argument **list** (no shell), so command-line arguments are not subject to shell injection. `redirectErrorStream(true)` merges stderr into stdout, and the process gets an empty environment.

- `Assembler` runs an external `clang` (path configurable with `--clang`, default `clang`) with the `.ll` file, optimization level, and `-L`/`-l` standard-library flags.

## Argument handling

Validated inputs: the source file extension is checked against an allowlist in `Language.fromSource`.

Not validated: the `--clang` executable path, `-o` output path, and `--library-path` are passed through to the toolchain as-is, with no existence or path checks. Because invocation is list-form (not shell), these are not injection vectors, but an arbitrary `--clang` path means an arbitrary executable is run.

## Intermediate files

Intermediate `.asm`/`.ll` files are written next to the source file via `FileUtils.withExtension` — a predictable, writable location — using `Files.write` with no atomic-rename or symlink protection. They are marked `deleteOnExit` unless `-save-temps` or compile-only (`-S`) is set, so they remain readable until the JVM exits.

## Threat model

All input is assumed trusted. JCC is a local developer tool: the person compiling already controls the machine, the source, and the output paths. Compiling an untrusted source program is **not** a supported, sandboxed use case. Accordingly, the unvalidated `--clang`/`-o`/`--library-path` arguments and the predictable, symlink-unprotected intermediate files above are accepted non-goals, not bugs, under this model.
