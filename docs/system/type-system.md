# Type system

Types are defined in `jcc-base` under `common/types/` as immutable value objects implementing the `Type` interface: numeric types (`I8`, `I32`, `I64`, `F32`, `F64` under `IntegerType`/`FloatType`/`NumericType`), plus `Bool`, `Str`, `Arr`, `Fun`, `Ptr`, `Void`, and helpers (`AmbiguousType`, `NamedType`, `Varargs`). `Str` extends `AbstractType` directly, not a numeric type.

Type checking and inference run through a `TypeManager` hierarchy: the `TypeManager` interface, `AbstractTypeManager`, and `DefaultTypeManager` in `jcc-base/common/compiler/`, with per-language subclasses — `BasicTypeManager` (`jcc-basic/type/`), `ColTypeManager` (`jcc-col/type/`), `AssembunnyTypeManager` (`jcc-assembunny/types/`). Tiny uses the base `DefaultTypeManager` directly. The `TypeManager` is created by `CompilerFactory` and used by both the semantics parser and the code generators.

## Numeric coercion

Promotion is widening-only: `AbstractTypeManager.canPromote` allows `I8 → I32 → I64` and `F32 → F64` (same category, more bits). `promote()` inserts explicit cast expressions (`CastToFloatExpression`, `CastToIntExpression`, etc.) rather than coercing silently.

`AbstractTypeManager.promoteIfPossible(expression, actualType, expectedType)` combines the two: it returns the expression wrapped in a cast when the widening applies, and unchanged otherwise. Use it wherever an accepted implicit widening has to become explicit in the AST — the code generators lower whatever operand an expression evaluates to, so a value left at its narrower type is emitted where the wider one is required. In COL an `f64` function whose body is an unwrapped `f32` emits `ret float` inside a `double` function, which Clang rejects. COL calls it from `FunDefPass2SemanticsParser` and `AnonymousFunctionSemanticsParser`; `ValSemanticsParser` calls `promote` directly, because it must report an error when the widening does not apply.

In COL the call must follow the `become` tail-position check. A `become` must return exactly the enclosing function's return type, so wrapping it first reports a cast consuming its result instead of the rule that was actually broken.

When inserting casts in new code, use the generic `CastToIntExpression`/`CastToFloatExpression`, which take the destination type as a constructor argument. The type-specific nodes (`CastToI32Expression`, `CastToI64Expression`, `CastToF64Expression`) are `@Deprecated` — older code such as `AbstractTypeManager.resolveArgs` and `BasicSemanticsParser` still uses them, but don't copy that pattern.

`BasicSemanticsParser` makes every implicit numeric conversion explicit, not just widening — at assignment, function arguments and return, array subscripts, mixed binary/relational operands, and SLEEP/RANDOMIZE. int→float becomes a `CastToF64Expression`; float→int becomes a `CastToI64Expression` wrapping a `RoundExpression`, so it rounds (half-to-even) rather than truncating like the bare cast COL uses. It still uses the bit-width nodes, which `BasicCodeGenerator` registers alongside the generic ones; code generation only lowers the cast it sees. These sites can move to the generic nodes.

Binary-expression result type (`AbstractTypeManager.binaryExpression`): int op int → the larger integer type; float op float → the larger float type; mixed int/float → `F64`; `Str + Str` → `Str`. Division (`/`) always yields `F64`.

Assignability (`isAssignableFrom`) is language-specific: `BasicTypeManager` allows any numeric ↔ numeric; `ColTypeManager` allows integer and float widening (and exact match otherwise); `DefaultTypeManager`/`AssembunnyTypeManager` return `true` (permissive).

`isAssignableFrom` and `canPromote` must agree on which widenings exist. `resolveFunction` ranks candidates with `isAssignableFrom`, while `resolveArgs` inserts the cast only when `canPromote` holds, so a widening that one accepts and the other rejects makes the call unresolvable rather than silently wrong: with float widening missing from `ColTypeManager`, an `f32` argument did not match an `f64` parameter even though `canPromote` calls the conversion lossless.

## AmbiguousType

Created in `IdentifierDerefSemanticsParser` when an identifier name resolves to more than one overloaded function — its type becomes an `AmbiguousType` holding the set of candidate `Fun` types. Resolved later in `AbstractTypeManager.resolveFunction`/`resolveArgs`: when the ambiguous set contains the formal parameter type, the identifier is narrowed to that type (`withType`); a `SemanticsException` is thrown if none matches.

## Function overloads

Stored in `SymbolTable.functions` as `Map<String, List<Info>>` (name → overloads). The `Fun` type carries `argTypes` + `returnType` with structural equality. `AbstractTypeManager.resolveFunction` tries an exact match first, then ranks candidates by number of casts needed (via `isAssignableFrom`) and picks the cheapest; a tie throws `AmbiguousException`.

## Known inconsistency

Package naming for language type managers is inconsistent: BASIC and COL use `type/` (singular), Assembunny uses `types/` (plural, matching the base `common/types/`). Not a meaningful distinction — just historical drift.
