# Code generation

JCC has two code-generation backends sharing a component-based design. Each AST node type maps to a code-generator component; the main generator dispatches by node class via a map lookup, and components recurse into child nodes and emit target instructions.

- **FASM backend** (`jcc-base`, `AbstractCodeGenerator`): emits x86-64 Flat Assembler text. Output is a `.asm` file assembled by `FasmAssembler`. Windows-only.
- **LLVM backend** (`jcc-llvm`, `AbstractLlvmCodeGenerator`): emits LLVM IR. Output is a `.ll` file compiled by `LlvmAssembler` (clang). Experimental.

Both produce a `TargetProgram` whose `toText()` is written to the intermediate file. The backend is selected by `CompilerFactory` from the `--backend` flag (default FASM).

## Component registration

The FASM `AbstractCodeGenerator` populates `statementCodeGenerators` and `expressionCodeGenerators` maps (node class → component) in its constructor; `statement()`/`expression()` dispatch by `getClass()`, with `AssignStatement` handled as a special case. The LLVM `AbstractLlvmCodeGenerator` builds equivalent `statementDictionary`/`expressionDictionary` maps.

Language modules extend a base generator and add or override entries after calling `super(...)`: `BasicCodeGenerator` registers BASIC statements (PRINT, GOSUB, DEF***, etc.) and overrides `FunctionCallExpression`/`IdentifierDerefExpression`; `BasicLlvmCodeGenerator` merges its entries via `putAll`.

Both base generators pre-register components for the shared statement nodes defined in `jcc-base` — control flow and declarations such as `WhileStatement`, `IfStatement`, and `FunctionDefinitionStatement` (the `AbstractCodeGenerator` constructor and `AbstractLlvmCodeGenerator.buildStatementDictionary` both map `WhileStatement` → `WhileCodeGenerator`). A language that reuses one of these nodes inherits its code generator on both backends with no per-language wiring — this is why COL `while` (which reuses the shared `WhileStatement`) compiles on FASM and LLVM, with the COL module adding only grammar, a syntax-visitor case, and a semantics component. A language-specific node type — e.g. COL's `ValDeclarationStatement`, absent from both base maps — must be registered per backend; where it isn't (`val` is LLVM-only), `statement()` throws `IllegalArgumentException: unsupported statement: <Class>`. Omitting a registration is therefore rejection-by-omission only for language-specific nodes, not shared ones.

## Type conversions

By the time code generation runs, every numeric conversion is an explicit cast node in the AST, inserted by the semantics parsers (see `type-system.md`). The shared FASM paths do no type re-derivation: `AbstractCodeGenerator.assignStatement`/`expression` evaluate the value straight into the target location and throw `IllegalStateException` if its type cannot be stored there (a missing-cast bug, not a conversion to perform); `DefaultFunctionCallHelper` moves each argument into its register without converting. A new implicit-conversion site must insert its cast in semantic analysis, not in a code generator.

The one exception is `SWAP`, which carries no cast node: both `SwapCodeGenerator`s insert the conversion casts themselves, the LLVM one wrapping the float→int source in a `RoundExpression` so it rounds like the FASM backend.

Code-generation unit tests bypass semantic analysis, so they must build the cast nodes themselves; `AbstractBasicCodeGeneratorTests` provides `castToInt`/`castToFloat` helpers that mirror what the BASIC semantics parser inserts. A codegen test that feeds a mismatched-type AST without the cast now trips the `IllegalStateException` guard above.

## Assignment evaluation order

The backends evaluate an array-element assignment in opposite orders. The LLVM `AssignCodeGenerator` (which handles scalar and array-element targets) computes the element address — evaluating the subscripts — before the right-hand side, i.e. left-to-right. The FASM `AbstractCodeGenerator.assignStatement` evaluates the right-hand side first, then resolves the target address. The difference is observable only when the subscripts and the right-hand side both call non-pure functions, and the parity ITs do not cover it.

## AST optimization

`DefaultAstExpressionOptimizer` (`jcc-base`, shared by all languages and both backends) constant-folds expressions before code generation. Float folds must preserve IEEE 754 semantics — a fold may not change the result for NaN, ±inf, or signed-zero inputs. This is why `0.0 / x` is not folded to `0.0`, and why an overflowing literal division stays unfolded instead of becoming an inf literal. Division by a literal zero is rejected at compile time (`InvalidValueException`) — deliberate, Go-style; see `col-language.md`.

A fold that replaces an expression with one of its operands must also preserve the expression's static type: the zero-folds (`0 * x` → `0`, `0 + x` → `x`) apply only to integer-typed expressions, and the identity folds (`1 * x` → `x`, `x / 1` → `x`, `x - 0` → `x`) require the operand to have the same type as the expression — notably, a float division is float-typed even with integer operands. Folding two literals is always allowed, because the fold computes the exact IEEE 754 result at compile time.

## Floating-point constants (LLVM)

LLVM's IR parser reads a decimal FP constant as a 64-bit double; for a
`float`-typed constant it additionally requires the double→float conversion to
be lossless. `double 5.3` is valid IR, `float 5.3` is rejected ("floating point
constant invalid for type"). `LiteralCodeGenerator` therefore emits F32-typed
literals as `Double.toString(Float.parseFloat(value))`: `Float.parseFloat`
rounds the decimal to float in one step (C/Rust f32 literal semantics), and
`Double.toString` prints the float's exact double value, which LLVM accepts.
`Float.toString` cannot be used — its shortest-form output (`"5.3"` for
`(float) 5.3`) is precisely the form LLVM rejects. Route any new code path that
emits a `float`-typed constant through the same rounding.

## Storage allocation (FASM)

Register allocation is managed by `RegisterManager` (volatile `R10`, `R11`; non-volatile `RBX`, `RDI`, `RSI`, `R12`–`R15`) and `FloatRegisterManager` (volatile `XMM4`–`XMM5`; non-volatile `XMM6`–`XMM15`). When registers are exhausted, `MemoryManager` spills to stack slots named `_tmp_location_N`. Used non-volatile registers are pushed/popped in the function prologue/epilogue.

Calling convention is Microsoft x64 (`DefaultFunctionCallHelper`): integer/pointer args in `RCX`, `RDX`, `R8`, `R9`; float args in `XMM0`–`XMM3`; 32-byte shadow space reserved before each call; return value in `RAX` (or `XMM0`).

## Calling convention (LLVM)

Each LLVM function definition (`DefineOperation`) and call site (`CallOperation`) carries a calling convention derived from its `Function` by `CallingConvention.of` (`jcc-llvm`). User-defined functions use `tailcc` — the convention built for guaranteed tail calls — except the synthesized `main`, which the C runtime calls and so stays the default C convention. External, library, and built-in functions stay C convention (emitted as empty text). A function-typed value (`ReferenceFunction`) only ever points to a user-defined function, so indirect calls use `tailcc` too.

`tailcc` is what lets a `musttail` call (COL's `become`, see `col-language.md`) stay valid across mismatched prototypes, enabling cross-overload and mutual tail recursion. The convention must match between a function's definition and every call site targeting it; deriving both from the same `Function` keeps them in sync. This applies to all languages on the LLVM backend, not only COL.

## Built-in / standard-library functions

Built-ins are resolved through per-backend function tables in each language's `compiler/` package. For BASIC, `BasicAsmFunctions` maps each BASIC built-in to a C-runtime function or a `libjccbas` function; `BasicLlvmFunctions` maps to an LLVM intrinsic, a C function, or a `libjccbas` function, and may instead return an inline expression. COL has the same split (`ColAsmFunctions`/`ColLlvmFunctions`).

Inlining on the LLVM backend follows one pattern in both `BasicLlvmFunctions` and `ColLlvmFunctions`: two maps keyed by function identifier — a library map (built-in → library function) and an inline map (built-in → lambda building an AST expression from the call's arguments). `getInlineExpression` is a plain lookup. Its signature carries only the function and its arguments — no symbol table, no output lines — so an inline lowering that needs either is expressed as a dedicated AST node plus a code generator registered in the backend's expression dictionary (`AscExpression`, `LboundExpression`, `UboundExpression` in BASIC; `PrintlnExpression` in COL). New inlined built-ins should follow this shape rather than intercepting calls by name in a `FunctionCallExpression` code generator. A library mapping must also preserve the built-in's return type: `FunctionCallCodeGenerator` types the call result from the built-in, so mapping e.g. the I64-returning `fix` to the double-returning `llvm.trunc.f64` emits invalid IR that only Clang rejects — and only when a test exercises the function. When no library function has the right signature, use an inline mapping with a cast instead (`INT`/`FIX`/`CINT` wrap their float intrinsic in `CastToIntExpression`).

Linking differs by backend: FASM emits an import section (`Library`/`Import` directives) resolved by the assembler, while LLVM emits `declare` operations for the `LibraryFunction`s actually called and links via `clang -L<libraryPath> -l<stdlib>` (`-lm` added on Linux).

## Injected identifiers (LLVM)

When a BASIC program uses `command$` on a non-Windows target, the generated `main` takes the program arguments as parameters. Compiler-injected identifiers like these are named with a leading dot (`.argc`/`.argv` in `InitCommandLineCodeGenerator`) — no BASIC identifier can start with a dot, so they can never shadow user variables. The shadowing is silent otherwise: `SymbolTable.mapName` resolves global-vs-local by plain name lookup, so an injected local named like a user variable turns the user's global references into undefined locals that only Clang rejects. Future injected symbols should follow the dot convention.

## Dynamic string memory (LLVM)

In LLVM statement components, a dynamically allocated string that is consumed in place — used once, with no way to reference it afterwards — is freed directly with a libc `free` call (`CF_FREE_I64`) right after use. `PrintCodeGenerator` frees printed string expressions this way; `RandomizeCodeGenerator` frees the `read_line` result after converting it with `atof`. Values stored into variables (e.g. `read_line` in `LineInputCodeGenerator`) are not freed at the call site; their dynamic-memory registration is an open TODO. Follow the same split in new components: free temporaries immediately, never stored values.

Whether a value is such an owned temporary is decided by `LlvmUtils.allocatesTransientDynamicMemory`, used at every LLVM free site (`PrintCodeGenerator`, `FunctionCallCodeGenerator`'s argument cleanup and `become` guard, `BasicAddCodeGenerator`) — not the broader `MemoryManagementUtils.allocatesDynamicMemory`, which also matches user-defined function calls. A user-defined function may return a string it does not own (e.g. `DEF FNid$(x$) = x$` returns its argument, possibly a string-literal global), so freeing its result aborted at runtime; such results are instead leaked until the LLVM backend has a GC. New free sites must use the transient variant.

## COL vals (LLVM)

COL `val` declarations span semantics and codegen:

- **Semantics**: `ColSemanticsParser` runs pass 2 inside a discardable top-level scope (`withLocalSymbolTable`); `ValSemanticsParser` registers vals there via `SymbolTable.addValue` (current scope, `isConstant` — unlike `addConstant`, which climbs to the root table). `FunDefPass2SemanticsParser` builds function scopes with `withGlobalSymbolTable` (parented on the root table), which is what makes vals invisible inside `fun` bodies. The top-level scope is discarded after parsing, so the symbol table handed to codegen contains no vals.
- **Codegen**: vals become locals of the synthesized LLVM `main`. `ValCodeGenerator` only evaluates the initializer, registers the identifier in the local symbol table, and emits the `store` — the `alloca` comes from `FunDefCodeGenerator.generateLocals`, which allocates every non-argument identifier registered during statement generation and places those lines before the statements. A statement component that emits its own `alloca` for a registered local would duplicate it.

## BASIC LLVM coverage

The BASIC LLVM backend is a work in progress. Coverage is exactly the set of components registered in `BasicLlvmCodeGenerator` merged with the base LLVM dictionaries. Statement/expression registrations are at parity with the FASM `BasicCodeGenerator`, including the nodes the shared `DefaultAstOptimizer` emits at `-O1` (Inc/Dec, Add/Sub/Mul/IDivAssign, `ShiftLeftExpression`) — the optimizer runs for both backends, so a node it emits must be registered in both. The LLVM op-assign generators take a `Scope` like `AssignCodeGenerator`: the base dictionary registers them with `NONE` (declared-variable languages), `BasicLlvmCodeGenerator` re-registers them with `GLOBAL`, because at `-O1` an optimized assignment can be a variable's first use.

Known divergences from FASM: `RETURN` without `GOSUB` prints `Error: GOSUB stack underflow (RETURN without GOSUB)` to stderr and exits 1, where FASM prints `Error: RETURN without GOSUB` to stdout and exits 0 (pinned by `BasicLlvmCompileAndRunControlStructuresIT`). `LBOUND`/`UBOUND` with an out-of-range dimension is an unchecked read of the dims global on LLVM, where FASM raises `Error: Illegal function call` at runtime — deliberately untested because the output is not stable.

Arrays are now covered on the LLVM backend at functional parity with FASM (integer/float/string, single- and multi-dimensional, `OPTION BASE`, `LBOUND`/`UBOUND`, `SWAP` of elements, arbitrary-expression subscripts) — the FASM `BasicCompileAndRunArrayIT` is mirrored by `BasicLlvmCompileAndRunArrayIT`. The LLVM representation differs from FASM's: each array is a private `[N x T]` global with a separate `[D x i64]` dimension-size metadata global, `LBOUND`/`UBOUND` are lowered inline (the `libjccbas` `.lbound`/`.ubound`/`.option_base` runtime functions are unused), and `OPTION BASE` is consumed at compile time. See `docs/Arrays.md` §"LLVM Backend" and ADR 0002. Garbage collection of string array elements is still deferred (a dedicated GC issue), matching scalar dynamic strings — see "Dynamic string memory (LLVM)" above.
