# Code generation

JCC generates LLVM IR with a component-based design. Each AST node type maps to a code-generator component; the main generator dispatches by node class via a map lookup, and components recurse into child nodes and emit target operations.

`AbstractLlvmCodeGenerator` (`jcc-llvm`) produces a `TargetProgram` whose `toText()` is written to a `.ll` file, which `Assembler` then compiles with clang. Cross-platform; requires a user-installed Clang 20+.

## Component registration

`AbstractLlvmCodeGenerator` builds `statementDictionary`/`expressionDictionary` maps (node class → component), and `statement()`/`expression()` dispatch by `getClass()`.

Language modules extend the base generator and merge their entries via `putAll` after calling `super(...)`: `BasicCodeGenerator` registers BASIC statements (PRINT, GOSUB, DEF***, etc.) and overrides `FunctionCallExpression`/`IdentifierDerefExpression`.

The base generator pre-registers components for the shared statement nodes defined in `jcc-base` — control flow and declarations such as `WhileStatement`, `IfStatement`, and `FunctionDefinitionStatement` (`buildStatementDictionary` maps `WhileStatement` → `WhileCodeGenerator`). A language that reuses one of these nodes inherits its code generator with no per-language wiring — this is why COL `while` (which reuses the shared `WhileStatement`) needs only grammar, a syntax-visitor case, and a semantics component in the COL module. A language-specific node type — e.g. COL's `ValDeclarationStatement`, absent from the base map — must be registered by its language module; where it isn't, `statement()` throws `IllegalArgumentException: unknown statement: <Class>`. Omitting a registration is therefore rejection-by-omission only for language-specific nodes, not shared ones.

## Synthesized top-level functions

`ColCodeGenerator.defineFunctions` and `BasicCodeGenerator.defineFunctions` walk the top-level statements and register every `FunctionDefinitionStatement` in the symbol table before generating `main`, so order within the statement list does not matter. COL's lambda lifting still prepends the `lambda.<n>` definitions it lifted from anonymous functions (see `col-language.md`) rather than appending them.

## Type conversions

By the time code generation runs, every numeric conversion is an explicit cast node in the AST, inserted by the semantics parsers (see `type-system.md`). A new implicit-conversion site must insert its cast in semantic analysis, not in a code generator.

The one exception is `SWAP`, which carries no cast node: `SwapCodeGenerator` inserts the conversion casts itself, wrapping the float→int source in a `RoundExpression` so it rounds half-to-even like QuickBASIC 4.5 (issue #52).

Code-generation unit tests bypass semantic analysis, so they must build the cast nodes themselves; `AbstractBasicCodeGeneratorTests` provides `castToInt`/`castToFloat` helpers that mirror what the BASIC semantics parser inserts.

## Assignment evaluation order

`AssignCodeGenerator` (which handles scalar and array-element targets) computes the element address — evaluating the subscripts — before the right-hand side, i.e. left-to-right.

## AST optimization

`DefaultAstExpressionOptimizer` (`jcc-base`, shared by all languages) constant-folds expressions before code generation. Float folds must preserve IEEE 754 semantics — a fold may not change the result for NaN, ±inf, or signed-zero inputs. This is why `0.0 / x` is not folded to `0.0`, and why an overflowing literal division stays unfolded instead of becoming an inf literal. Division by a literal zero is rejected at compile time (`InvalidValueException`) — deliberate, Go-style; see `col-language.md`.

A fold that replaces an expression with one of its operands must also preserve the expression's static type: the zero-folds (`0 * x` → `0`, `0 + x` → `x`) apply only to integer-typed expressions, and the identity folds (`1 * x` → `x`, `x / 1` → `x`, `x - 0` → `x`) require the operand to have the same type as the expression — notably, a float division is float-typed even with integer operands. Folding two literals is always allowed, because the fold computes the exact IEEE 754 result at compile time.

The whole pass is gated on the optimization level: `DefaultAstOptimizer.program` returns the program unchanged unless `-O1`/`-O2` was passed, and the default level is 0. A default build therefore folds nothing — `val s := "a" + "b"` emits a `col_concat_str_str` call at `-O0` and a folded `"ab"` literal at `-O1`.

Even at `-O1` the pass reaches only the statement types in `DefaultAstOptimizer.statement`'s switch: `AssignStatement`, `FunctionDefinitionStatement`, `IfStatement`, `LabelledStatement`, `ConstDeclarationStatement`, `WhileStatement`. Every other statement falls through unchanged, so expressions inside a language-specific statement are never folded — COL's `ValDeclarationStatement` and `FunCallStatement` are the current examples, which is why at `-O1` a two-literal concatenation is folded inside a `fun` body but survives in a `val` initializer or a `call` argument. A new language-specific statement that should be optimized must be added to that switch.

The pass rebuilds every statement it visits, and the shared copy methods construct the *base* class: `AssignStatement.withRhsExpression` returns `new AssignStatement(...)`, silently downgrading any subclass that does not override it. The generator dispatches on the node's class, so a downgraded node stops matching its language's handler and falls through to the shared one. `CpyStatement` — Assembunny's `cpy`, and the only subclass of `AssignStatement` — overrides `withRhsExpression` and `withLhsExpression` for that reason: without them, at `-O1` the generic assign path lowered a copy to a memory variable named after the Assembunny register instead of to the CPU register `inc`/`dec`/`jnz` read, and added that register to the symbol table, where it reached the data section. A new subclass of a shared AST node must override the `with*` methods it inherits; `UnaryExpression.withExpression` sidesteps the problem by locating the subclass constructor reflectively.

## Floating-point constants

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

## String constants

`ConstOperation` renders a string constant as `c"…"` bytes plus a trailing NUL, with the array
length counted from the *raw* UTF-8 bytes — multi-byte sequences are passed through untouched, which
is what makes non-ASCII text byte-transparent. LLVM's `c"…"` syntax has exactly one escape, `\XX`
for a single byte, and three characters need it: a control character, the `"` that would otherwise
end the constant, and the `\` that would otherwise start an escape. Escaping does not change the
byte count, so the declared length stays correct. Only the control-character case was handled until
COL grew string escapes (`"say \"hi\""` emitted `c"say "hi"…"`, which Clang rejects with
*constant expression type mismatch*); a BASIC literal containing a backslash had the same latent
fault. Any new path that emits a string constant should go through `ConstOperation` rather than
building the `c"…"` text itself.

## x86-64 baseline

`Assembler` passes `-msse4.1` to clang on x86-64 hosts. BASIC rounds half-to-even
(issue #52), which jcc emits as `llvm.roundeven.f64`. LLVM lowers that to the SSE4.1
`roundsd` instruction when SSE4.1 is available, and to a libm call to `roundeven`
otherwise. glibc and the macOS libm export `roundeven`; mingw-w64's libm does not, so
a default-baseline build fails to link on Windows with `undefined reference to
'roundeven'`. The flag raises the x86-64 baseline to SSE4.1 (Penryn, 2008). AArch64
needs nothing — it has a native `frintn`.

## Calling convention

Each LLVM function definition (`DefineOperation`) and call site (`CallOperation`) carries a calling convention derived from its `Function` by `CallingConvention.of` (`jcc-llvm`). User-defined functions use `tailcc` — the convention built for guaranteed tail calls — except the synthesized `main`, which the C runtime calls and so stays the default C convention. External, library, and built-in functions stay C convention (emitted as empty text). A function-typed value (`ReferenceFunction`) only ever points to a user-defined function, so indirect calls use `tailcc` too.

`tailcc` is what lets a `musttail` call (COL's `become`, see `col-language.md`) stay valid across mismatched prototypes, enabling cross-overload and mutual tail recursion. The convention must match between a function's definition and every call site targeting it; deriving both from the same `Function` keeps them in sync. This applies to all languages, not only COL.

## Parameter attributes

jcc emits no LLVM parameter or return attributes. `DeclareOperation.toText` and
`CallOperation.toText` render every type through `Type.llvmName()`, which yields the bare type
name, so a declare, a call site and a definition all agree with each other but not with what Clang
would write for the same signature. Two instances exist today, both benign:

- **`zeroext` on a C `_Bool`.** Clang lowers `_Bool` as `i1 zeroext` in every position — see
  `declare zeroext i1 @col_eof()` and `declare ptr @col_string_bool(i1 noundef zeroext)`. jcc emits
  `declare i1 @col_eof()` and `declare ptr @col_string_bool(i1)`. COL's `eof` and `string(bool)`
  (`JF_EOF`, `JF_STRING_BOOL`) are the only C boundaries carrying a `Bool` in either direction.
- **`immarg` on `llvm.abs`.** LLVM documents the second argument of `llvm.abs.*` as
  `i1 immarg` (`is_int_min_poison`); jcc emits `declare i64 @llvm.abs.i64(i64, i1)`. LLVM accepts it.

**The `zeroext` omission is safe, and was measured rather than assumed** — do not re-derive it from
first principles. Passing a bool generates byte-identical machine code with and without the
attribute on arm64, x86-64 Linux and x86-64 Windows, because LLVM's ABI lowering already masks an
`i1` argument down to bit 0 (`and w0, w8, #0x1` / `andq $1, %rdi` / `andq $1, %rcx`), even when the
value is a `trunc` of a wider integer with dirty upper bits. Receiving a bool is identical on
x86-64 and costs one redundant instruction on arm64 (`and` then `ands`, where Clang emits a single
`ands`), because jcc's caller masks the returned byte defensively instead of trusting the callee's
guarantee. jcc is therefore *more* conservative than Clang here, never less.

Should the attributes ever be emitted, they cannot come from the type. `Bool.llvmName()` returning
`"i1 zeroext"` would leak into `alloca`/`store`/`load`/`phi`/`select` and into COL's own
`define tailcc i1 @negate_Bool(i1 %0)`, which is a `tailcc` internal call rather than a C boundary;
the syntax position differs between the two ends of a signature (a return attribute *precedes* the
type, a parameter attribute *follows* it); and the same `i1` needs `zeroext` at a C boundary but
`immarg` in `llvm.abs`, so no blanket rule keyed on the type — or on being a `LibraryFunction` —
is correct. It has to be opt-in per signature, rendered in `DeclareOperation` and `CallOperation`,
with the latter zipping the operand list against `function.getArgTypes()` positionally (minding
varargs, whose formal list is shorter and holds a `Varargs` slot, and `ReferenceFunction`, which is
never a C boundary) plus a separate hook for the return position.

## Built-in / standard-library functions

Built-ins are resolved through a function table in each language's `compiler/` package. `BasicFunctions` maps each BASIC built-in to an LLVM intrinsic, a C function, or a `libjccbas` function, and may instead return an inline expression. COL has the same, in `ColFunctions`.

Inlining follows one pattern in both `BasicFunctions` and `ColFunctions`: two maps keyed by function identifier — a library map (built-in → library function) and an inline map (built-in → lambda building an AST expression from the call's arguments). `getInlineExpression` is a plain lookup. Its signature carries only the function and its arguments — no symbol table, no output lines — so an inline lowering that needs either is expressed as a dedicated AST node plus a code generator registered in the expression dictionary (`AscExpression`, `LboundExpression`, `UboundExpression` in BASIC; `PrintlnExpression` in COL). New inlined built-ins should follow this shape rather than intercepting calls by name in a `FunctionCallExpression` code generator. A library mapping must also preserve the built-in's return type: `FunctionCallCodeGenerator` types the call result from the built-in, so mapping e.g. the I64-returning `fix` to the double-returning `llvm.trunc.f64` emits invalid IR that only Clang rejects — and only when a test exercises the function. When no library function has the right signature, use an inline mapping with a cast instead (`INT`/`FIX`/`CINT` wrap their float intrinsic in `CastToIntExpression`).

Linking emits `declare` operations for the `LibraryFunction`s actually called, and links via `clang -L<libraryPath> -l<stdlib>` (`-lm` added on Linux).

## Injected identifiers

When a BASIC program uses `command$` on a non-Windows target, the generated `main` takes the program arguments as parameters. Compiler-injected identifiers like these are named with a leading dot (`.argc`/`.argv` in `InitCommandLineCodeGenerator`) — no BASIC identifier can start with a dot, so they can never shadow user variables. The shadowing is silent otherwise: `SymbolTable.mapName` resolves global-vs-local by plain name lookup, so an injected local named like a user variable turns the user's global references into undefined locals that only Clang rejects. Future injected symbols should follow the dot convention.

## Dynamic string memory

As of issue #63 phase 4 there is one ownership model for dynamic strings: every freshly-allocated string is handed to the garbage collector, and nothing is ever freed at a call site. The old eager-`free`/leak split (and its `LlvmUtils.allocatesTransientDynamicMemory` predicate) is gone.

A string that a runtime/library function or a string operation just allocated is registered with `jcc_gc_register` and rooted, through the `GcCodeGenerator` strategy (`register*` methods; §"Garbage collector plumbing" below). There are three cases:

- **Register and root in a synthetic slot** (`registerResult`) — for a fresh allocation whose result flows on as an expression value: string concatenation (`BasicAddCodeGenerator`, `ColAddCodeGenerator`) and the result of a built-in/library function call (`FunctionCallCodeGenerator`). The register call is chained onto the producing call, and the returned pointer is stored into a synthetic `.gc.slot.N` string local so it survives the next registration (which may collect). The slot's `alloca`, null-init, and `jcc_gc_add_root` come for free from the function prologue, which allocates and roots every non-parameter string local after the body is generated.
- **Root only** (`protectResult`) — for the result of a *user-defined* function call. The callee registered its own result inside its body (it may return an argument, a literal, or a global it does not own), so the caller only roots it in a synthetic slot; registering it again would be a double registration.
- **Register without a slot** (`register`) — when the result is stored immediately into a slot that is already a root (`LineInputCodeGenerator` writing into its destination variable), or is transient and provably dead before the next registration (`RandomizeCodeGenerator`'s seed line, consumed by `atof` at once).

Function *arguments* need no per-call handling: a string-producing argument was already registered and rooted by its own code generator, so it stays reachable across the call. This is what makes a callee that stashes or returns an argument safe (requirement 4) — the old post-call argument free that broke this is deleted. `PrintCodeGenerator` likewise frees nothing; its operands are already rooted by the code generators that produced them.

The registered pointer keeps the value's own type (e.g. `Str`), not `Ptr` — both render as `ptr` in the IR, but downstream consumers (a `PRINT` format string, say) still see it as a string. The API and semantics are specified in `docs/GarbageCollection.md` and ADR 0003.

## Garbage collector plumbing

The GC design is in `docs/GarbageCollection.md` and ADR 0003. The backend knows the `jcc_gc_*` runtime API (`JccGcBuiltIns` in `jcc-llvm`, constants prefixed `GF_`) and emits both the shadow-stack plumbing and registration. The plumbing: a `jcc_gc_push_frame`/`jcc_gc_pop_frame` frame around every function, `jcc_gc_add_root` for each string parameter and (null-initialized) string local, and — for `main` only — `jcc_gc_init(threshold, flags)` then `jcc_gc_set_global_roots` then the frame push, in that order, because `init` must be the first `jcc_gc_*` call. `threshold` is `-initial-gc-threshold`; `flags` carries the `JCC_GC_DEBUG` bit (1) when `-print-gc` is set. The `@jcc.gc.global.roots` table (`GcRootsOperation`) lists every non-constant string scalar (count 1) and string array (element count), null-terminated. Registration (`jcc_gc_register`) of freshly-allocated strings and the deletion of the old eager frees are covered in §"Dynamic string memory" above.

The frame pop is emitted before control leaves the function, not after. `ReturnCodeGenerator` evaluates the return expression, emits `jcc_gc_pop_frame`, then `ret`. A guaranteed tail call pops the same way: `FunctionCallCodeGenerator.toLlvmTailCall` evaluates the arguments, emits `jcc_gc_pop_frame`, then the `musttail` call — a `musttail` call admits no post-call plumbing. This is safe only because no allocation, hence no collection, can occur between the pop and the callee's prologue re-rooting its parameters; a string argument is register-resident and unrooted across that window, so emitting any allocating operation between the pop and the `musttail` call would break it. The tail path roots no result — the frame is gone and the callee already registered its result, which propagates up the chain of `ret`s.

A code generator that emits its own `ret` must emit the pop itself, because `ReturnCodeGenerator` is the only place the return path pops. `ColFunDefCodeGenerator.generateTail` does exactly that for the non-`become` leaf of a tail if-expression (COL's accumulator idiom `if n == 0 then acc else become f(...)`); it did not before COL wired the runtime collector, and the omission would have leaked one shadow-stack frame per call in every COL function using that shape, string-typed or not.

GC emission is gated by composition, not inheritance: a `GcCodeGenerator` strategy is injected into the shared `AbstractLlvmCodeGenerator` (which exposes it to subclasses via a `protected gc()` accessor, so they can thread it into the string-producing code generators they wire up), `FunDefCodeGenerator`, and `ReturnCodeGenerator`. `NoOpGcCodeGenerator` (the default via the three-argument `AbstractLlvmCodeGenerator` constructor) emits nothing and its `register*` methods return their argument unchanged, so Tiny and Assembunny — which have no heap type — produce no `jcc_gc_*` calls; `BasicCodeGenerator` and `ColCodeGenerator` pass `RuntimeGcCodeGenerator`, which owns `main`'s init sequence, all frame/root emission, and registration. A language enables the collector by wiring in that strategy — no subclass, no flag (requirement 7, which COL exercised when it grew strings: the only jcc-llvm change needed was none).

A language whose own code generators construct shared components must thread `gc()` into them, or those components silently keep the no-op default. `ColFunDefCodeGenerator` builds its own `FunctionCallCodeGenerator` for the `become` path and takes the strategy as a constructor argument for exactly this reason; wiring `RuntimeGcCodeGenerator` into `ColCodeGenerator` alone would not have reached it.

`AbstractLlvmCodeGenerator.generateDeclares` emits an ordinary `declare` for every called GC function, like any other library function; the symbols resolve at link time against the language's standard library, where the runtime ships (linked via the single `-l<stdlib>` — there is no separate `libjccgc` library). `libjccbas` holds the canonical copy and `libjcccol` vendors an identical one, so BASIC resolves them from `-ljccbas` and COL from `-ljcccol`. GC functions still carry the `FunctionUtils.LIB_JCC_GC` marker, but it is now only a logical tag and affects neither declaration nor linking. The runtime versions are `libjccbas.version` 2.2.0 and `libjcccol.version` 0.2.0 in the root `pom.xml`.

## PRINT newline encoding

`PrintStatement` holds the print arguments as a list encoding three source forms, and
`PrintCodeGenerator` derives the trailing-newline flag from it. `PRINT x`
is `[x]` (prints a newline). `PRINT x;` is `[x, null]` — the trailing `null`, added by
`BasicSyntaxVisitor.visitPrintStmt`, suppresses the newline. Bare `PRINT` is the empty list `[]`
and must still print a newline. The flag is
`eol = expressions.isEmpty() || expressions.getLast() != null`; the earlier
`!expressions.isEmpty() && ...` form dropped the newline for bare `PRINT` and shipped as a bug
(issue #77). The `null` sentinel is filtered out before building the printf arguments
(`filter(Objects::nonNull)`).

## COL vals

COL `val` declarations span semantics and codegen:

- **Semantics**: `ColSemanticsParser` runs pass 2 inside a discardable top-level scope (`withLocalSymbolTable`); `ValSemanticsParser` registers vals there via `SymbolTable.addValue` (current scope, `isConstant` — unlike `addConstant`, which climbs to the root table). `FunDefPass2SemanticsParser` builds function scopes with `withGlobalSymbolTable` (parented on the root table), which is what makes vals invisible inside `fun` bodies. The top-level scope is discarded after parsing, so the symbol table handed to codegen contains no vals.
- **Codegen**: vals become locals of the synthesized `main`. `ValCodeGenerator` only evaluates the initializer, registers the identifier in the local symbol table, and emits the `store` — the `alloca` comes from `FunDefCodeGenerator.generateLocals`, which allocates every non-argument identifier registered during statement generation and places those lines before the statements. A statement component that emits its own `alloca` for a registered local would duplicate it.
- **GC**: because a val is a local, a *string*-typed val needs no special rooting — `RuntimeGcCodeGenerator.rootVariables` null-inits and roots every non-parameter string local of the enclosing function, which for a top-level val (and for one declared in a `while` body, since `WhileCodeGenerator` reuses the same symbol table) is `main`. COL therefore has no global string roots at all: `@jcc.gc.global.roots` is emitted, because `main`'s `jcc_gc_set_global_roots` call references it unconditionally, but holds only the `{null, 0}` terminator. A val in a loop body is one static slot reused every iteration, so the loop retains at most one dead value.

## BASIC coverage

Coverage is exactly the set of components registered in `BasicCodeGenerator` merged with the base dictionaries, including the nodes the shared `DefaultAstOptimizer` emits at `-O1` (Inc/Dec, Add/Sub/Mul/IDivAssign, `ShiftLeftExpression`) — a node the optimizer emits must be registered. The op-assign generators take a `Scope` like `AssignCodeGenerator`: the base dictionary registers them with `NONE` (declared-variable languages), `BasicCodeGenerator` re-registers them with `GLOBAL`, because at `-O1` an optimized assignment can be a variable's first use.

`RETURN` without `GOSUB` prints `Error: GOSUB stack underflow (RETURN without GOSUB)` to stderr and exits 1 (pinned by `BasicCompileAndRunControlStructuresIT`). `LBOUND`/`UBOUND` with an out-of-range dimension is an unchecked read of the dims global — deliberately untested because the output is not stable.

Arrays are covered for integer/float/string elements, single- and multi-dimensional, with `OPTION BASE`, `LBOUND`/`UBOUND`, `SWAP` of elements, and arbitrary-expression subscripts (`BasicCompileAndRunArrayIT`). Each array is a private `[N x T]` global with a separate `[D x i64]` dimension-size metadata global, `LBOUND`/`UBOUND` are lowered inline (the `libjccbas` `.lbound`/`.ubound`/`.option_base` runtime functions are unused), and `OPTION BASE` is consumed at compile time. See `docs/Arrays.md` and ADR 0002.
