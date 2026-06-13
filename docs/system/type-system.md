# Type system

Types are defined in `jcc-base` under `common/types/` as immutable value objects implementing the `Type` interface: numeric types (`I8`, `I32`, `I64`, `F32`, `F64` under `IntegerType`/`FloatType`/`NumericType`), plus `Bool`, `Str`, `Arr`, `Fun`, `Ptr`, `Void`, and helpers (`AmbiguousType`, `NamedType`, `Varargs`). `Str` extends `AbstractType` directly, not a numeric type.

Type checking and inference run through a `TypeManager` hierarchy: the `TypeManager` interface, `AbstractTypeManager`, and `DefaultTypeManager` in `jcc-base/common/compiler/`, with per-language subclasses — `BasicTypeManager` (`jcc-basic/type/`), `ColTypeManager` (`jcc-col/type/`), `AssembunnyTypeManager` (`jcc-assembunny/types/`). Tiny uses the base `DefaultTypeManager` directly. The `TypeManager` is created by `CompilerFactory` and used by both the semantics parser and the code generators.

## Numeric coercion

Promotion is widening-only: `AbstractTypeManager.canPromote` allows `I8 → I32 → I64` and `F32 → F64` (same category, more bits). `promote()` inserts explicit cast expressions (`CastToFloatExpression`, `CastToIntExpression`, etc.) rather than coercing silently.

When inserting casts in new code, use the generic `CastToIntExpression`/`CastToFloatExpression`, which take the destination type as a constructor argument. The type-specific nodes (`CastToI32Expression`, `CastToI64Expression`, `CastToF64Expression`) are `@Deprecated` — older code such as `AbstractTypeManager.resolveArgs` still uses them, but don't copy that pattern. The exception is code that must lower on the FASM backend: it registers cast generators only for the bit-width nodes (`CastToF64CodeGenerator`/`CastToI64CodeGenerator`), so the generic nodes — lowered by the LLVM backend — have no FASM equivalent.

`BasicSemanticsParser` makes every implicit numeric conversion explicit, not just widening — at assignment, function arguments and return, array subscripts, mixed binary/relational operands, and SLEEP/RANDOMIZE. int→float becomes a `CastToF64Expression`; float→int becomes a `CastToI64Expression` wrapping a `RoundExpression`, so it rounds (half-to-even) rather than truncating like the bare cast COL uses. It uses the bit-width nodes (registered in `BasicCodeGenerator`) precisely so both backends can lower them; code generation only lowers the cast it sees. Using the deprecated bit-width nodes is a temporary accommodation for the FASM backend, which is slated for deprecation; once it is gone, these sites can move to the generic nodes.

Binary-expression result type (`AbstractTypeManager.binaryExpression`): int op int → the larger integer type; float op float → the larger float type; mixed int/float → `F64`; `Str + Str` → `Str`. Division (`/`) always yields `F64`.

Assignability (`isAssignableFrom`) is language-specific: `BasicTypeManager` allows any numeric ↔ numeric; `ColTypeManager` allows integer widening only (and exact match otherwise); `DefaultTypeManager`/`AssembunnyTypeManager` return `true` (permissive).

## AmbiguousType

Created in `IdentifierDerefSemanticsParser` when an identifier name resolves to more than one overloaded function — its type becomes an `AmbiguousType` holding the set of candidate `Fun` types. Resolved later in `AbstractTypeManager.resolveFunction`/`resolveArgs`: when the ambiguous set contains the formal parameter type, the identifier is narrowed to that type (`withType`); a `SemanticsException` is thrown if none matches.

## Function overloads

Stored in `SymbolTable.functions` as `Map<String, List<Info>>` (name → overloads). The `Fun` type carries `argTypes` + `returnType` with structural equality. `AbstractTypeManager.resolveFunction` tries an exact match first, then ranks candidates by number of casts needed (via `isAssignableFrom`) and picks the cheapest; a tie throws `AmbiguousException`.

## Known inconsistency

Package naming for language type managers is inconsistent: BASIC and COL use `type/` (singular), Assembunny uses `types/` (plural, matching the base `common/types/`). Not a meaningful distinction — just historical drift.
