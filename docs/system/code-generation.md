# Code generation

JCC has two code-generation backends sharing a component-based design. Each AST node type maps to a code-generator component; the main generator dispatches by node class via a map lookup, and components recurse into child nodes and emit target instructions.

- **FASM backend** (`jcc-base`, `AbstractCodeGenerator`): emits x86-64 Flat Assembler text. Output is a `.asm` file assembled by `FasmAssembler`. Windows-only.
- **LLVM backend** (`jcc-llvm`, `AbstractLlvmCodeGenerator`): emits LLVM IR. Output is a `.ll` file compiled by `LlvmAssembler` (clang). Experimental.

Both produce a `TargetProgram` whose `toText()` is written to the intermediate file. The backend is selected by `CompilerFactory` from the `--backend` flag (default FASM).

## Component registration

The FASM `AbstractCodeGenerator` populates `statementCodeGenerators` and `expressionCodeGenerators` maps (node class → component) in its constructor; `statement()`/`expression()` dispatch by `getClass()`, with `AssignStatement` handled as a special case. The LLVM `AbstractLlvmCodeGenerator` builds equivalent `statementDictionary`/`expressionDictionary` maps.

Language modules extend a base generator and add or override entries after calling `super(...)`: `BasicCodeGenerator` registers BASIC statements (PRINT, GOSUB, DEF***, etc.) and overrides `FunctionCallExpression`/`IdentifierDerefExpression`; `BasicLlvmCodeGenerator` merges its entries via `putAll`.

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

## Built-in / standard-library functions

Built-ins are resolved through per-backend function tables in each language's `compiler/` package. For BASIC, `BasicAsmFunctions` maps each BASIC built-in to a C-runtime function or a `libjccbas` function; `BasicLlvmFunctions` maps to an LLVM intrinsic, a C function, or a `libjccbas` function, and may instead return an inline expression. COL has the same split (`ColAsmFunctions`/`ColLlvmFunctions`).

Linking differs by backend: FASM emits an import section (`Library`/`Import` directives) resolved by the assembler, while LLVM emits `declare` operations for the `LibraryFunction`s actually called and links via `clang -L<libraryPath> -l<stdlib>` (`-lm` added on Linux).

## Dynamic string memory (LLVM)

In LLVM statement components, a dynamically allocated string that is consumed in place — used once, with no way to reference it afterwards — is freed directly with a libc `free` call (`CF_FREE_I64`) right after use. `PrintCodeGenerator` frees printed string expressions this way; `RandomizeCodeGenerator` frees the `read_line` result after converting it with `atof`. Values stored into variables (e.g. `read_line` in `LineInputCodeGenerator`) are not freed at the call site; their dynamic-memory registration is an open TODO. Follow the same split in new components: free temporaries immediately, never stored values.

## BASIC LLVM coverage

The BASIC LLVM backend is a work in progress. Current coverage is exactly the set of components registered in `BasicLlvmCodeGenerator` merged with the base LLVM dictionaries — narrower than the FASM `BasicCodeGenerator`. Compare the two generators' registrations to see the current gap (array access and several control-flow/statement types registered for FASM are not yet present for LLVM).
