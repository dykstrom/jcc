# Type system

Types are defined in `jcc-base` under `common/types/` as immutable value objects implementing the `Type` interface: numeric types (`I8`, `I32`, `I64`, `F32`, `F64` under `IntegerType`/`FloatType`/`NumericType`), plus `Bool`, `Str`, `Arr`, `Fun`, `Opaque`, `Ptr`, `Void`, and helpers (`AmbiguousType`, `NamedType`, `Varargs`). `Str` extends `AbstractType` directly, not a numeric type.

Type checking and inference run through a `TypeManager` hierarchy: the `TypeManager` interface, `AbstractTypeManager`, and `DefaultTypeManager` in `jcc-base/common/compiler/`, with per-language subclasses — `BasicTypeManager` (`jcc-basic/type/`), `ColTypeManager` (`jcc-col/type/`), `AssembunnyTypeManager` (`jcc-assembunny/types/`). Tiny uses the base `DefaultTypeManager` directly. The `TypeManager` is created by `CompilerFactory` and used by both the semantics parser and the code generators.

## Numeric coercion

Promotion is widening-only: `AbstractTypeManager.canPromote` allows `I8 → I32 → I64` and `F32 → F64` (same category, more bits). `promote()` inserts explicit cast expressions (`CastToFloatExpression`, `CastToIntExpression`) rather than coercing silently.

`AbstractTypeManager.promoteIfPossible(expression, actualType, expectedType)` combines the two: it returns the expression wrapped in a cast when the widening applies, and unchanged otherwise. Use it wherever an accepted implicit widening has to become explicit in the AST — the code generators lower whatever operand an expression evaluates to, so a value left at its narrower type is emitted where the wider one is required. In COL an `f64` function whose body is an unwrapped `f32` emits `ret float` inside a `double` function, which Clang rejects. COL calls it from `FunDefPass2SemanticsParser` and `AnonymousFunctionSemanticsParser`; `ValSemanticsParser` calls `promote` directly, because it must report an error when the widening does not apply.

In COL the call must follow the `become` tail-position check. A `become` must return exactly the enclosing function's return type, so wrapping it first reports a cast consuming its result instead of the rule that was actually broken.

There is one cast node per category, `CastToIntExpression` and `CastToFloatExpression`, each taking the destination type as a constructor argument. That type is part of node equality, so casts to `i32` and to `i64` over the same subexpression are different nodes. `TruncateExpression` works the same way. A typed AST node has to override `equals` and `hashCode` to get that: `UnaryExpression.equals` compares the concrete class and the subexpression only, so a subclass that adds a destination type inherits an equality that ignores it, and two nodes differing only in width compare equal. Nothing in the build catches the omission.

`BasicSemanticsParser` makes every implicit numeric conversion explicit, not just widening — at assignment, function arguments and return, array subscripts, mixed binary/relational operands, and SLEEP/RANDOMIZE. int→float becomes a `CastToFloatExpression`; float→int becomes a `CastToIntExpression` wrapping a `RoundExpression`, so it rounds (half-to-even) rather than truncating like the bare cast COL uses. Code generation only lowers the cast it sees.

Binary-expression result type (`AbstractTypeManager.binaryExpression`): int op int → the larger integer type; float op float → the larger float type; mixed int/float → `F64`; `Str + Str` → `Str`. Division (`/`) always yields `F64`.

Assignability (`isAssignableFrom`) is language-specific: `BasicTypeManager` allows any numeric ↔ numeric; `ColTypeManager` allows integer and float widening (and exact match otherwise); `DefaultTypeManager`/`AssembunnyTypeManager` return `true` (permissive).

`isAssignableFrom` and `canPromote` must agree on which widenings exist. `resolveFunction` ranks candidates with `isAssignableFrom`, while `resolveArgs` inserts the cast only when `canPromote` holds, so a widening that one accepts and the other rejects makes the call unresolvable rather than silently wrong: with float widening missing from `ColTypeManager`, an `f32` argument did not match an `f64` parameter even though `canPromote` calls the conversion lossless.

## AmbiguousType

Created in `IdentifierDerefSemanticsParser` when an identifier name resolves to more than one overloaded function — its type becomes an `AmbiguousType` holding the set of candidate `Fun` types. Resolved later in `AbstractTypeManager.resolveFunction`/`resolveArgs`: when the ambiguous set contains the formal parameter type, the identifier is narrowed to that type (`withType`); a `SemanticsException` is thrown if none matches.

## Opaque

`Opaque` is the type a COL `type Name as T` declaration creates. It has the same LLVM representation as its underlying type `T`, but it is a distinct type.

- Its `equals` compares the name only. `ColTypeManager.isAssignableFrom` has no branch for it: the final `equals` check rejects an opaque type against its underlying type and against every other opaque type.
- It implements no numeric interface and does not override `isNumber()`. This keeps `canPromote` and `promoteNumeric` from treating an opaque type over `f64` as a number.
- `getName()` returns `Opaque.<name>`, not the bare name. `UserDefinedFunction` builds mangled function names from `getName()`, so a bare `I64` would give `f(I64)` and `f(i64)` the same LLVM symbol.

Code that dispatches on the representation must unwrap the type first with `Opaque.unwrap`. See "Garbage collector plumbing" in `code-generation.md`. The operator code generators do this too: `BinaryCodeGenerator` and `RelationalCodeGenerator` unwrap before they pick an LLVM instruction, and `ColAddCodeGenerator` and `ColRelationalCodeGenerator` unwrap before their string check. `CastToIntCodeGenerator` and `CastToFloatCodeGenerator` unwrap their source type. Without the unwrap, `LlvmUtils.typeToOperator` throws on an opaque type, a cast throws `ClassCastException`, and `==` on a type over `string` compares pointers.

### Operators on opaque values

An opaque type inherits these operators only when both operands are the same opaque type:

| Operator | Over a number | Over `string` | Over `bool` |
|---|---|---|---|
| `==` `!=` | `bool` | `bool` | `bool` |
| `<` `<=` `>` `>=` | `bool` | rejected | rejected |
| `+` | the opaque type | the opaque type | rejected |
| `-` | the opaque type | rejected | rejected |

All other operators reject an opaque operand. An opaque type over a function type inherits no operator, not even `==`, and every operator reports *an opaque function type supports no operators* for it. The rules live in `ColOperandTypeRules`:

- `OPAQUE_EQUALITY`, `OPAQUE_ADD` and `OPAQUE_NUMERIC` check the opaque operands of an inherited operator. `ColSemanticsParser` puts one of them first in the operator's rule list, so that an opaque mistake gets its own message instead of the generic "cannot add Meters and f64".
- `SAME_OPAQUE` is combined with `or` into the operator's usual rule, for example `NUMERIC.or(SAME_OPAQUE)`. Without it, `NUMERIC` rejects two `Meters` operands.
- `NOT_OPAQUE` comes first for every other operator, unary operators included. `UnarySemanticsParser` takes several rules for this reason.

`ColTypeManager.getType` gives an inherited `+` or `-` the opaque type. It gives every other arithmetic or unary expression with an opaque operand `Unknown`, so that a call with a rejected expression as its argument reports no second error.

### Opaque function types

`TypeDefPass1SemanticsParser` defines one conversion for an opaque type over a `Fun`: the wrap `Name(Fun) -> Name`. It defines no unwrap and no `OpaqueStringFunction`. An overloaded function passed to the wrap is narrowed to the underlying `Fun` through the `AmbiguousType` handling in `resolveFunction`. A built-in function stays unusable as a value, so `Cmp(max)` is rejected in `IdentifierDerefSemanticsParser`.

A call such as `c(a, b)` on a value of an opaque function type resolves through `AbstractTypeManager.getReferenceFunction`. It looks through `Opaque` to the `Fun`, so the call takes the underlying argument types and returns the underlying return type. Code generation needs no unwrap: `Opaque.llvmName()` delegates to `Fun`, so the value loads and calls as a `ptr`.

### `string` on opaque values

`TypeDefPass1SemanticsParser` defines an `OpaqueStringFunction`, `string(Name) -> string`, for every opaque type over a scalar. `ColFunctions` inlines a call to it into the `string` call that the underlying type resolves to. An `i32` value is first widened to `i64`, and an `f32` value to `f64`, as overload resolution widens a plain argument. The result is the same IR as `string` on the underlying value.

An opaque type over `string` gets no `OpaqueStringFunction`. Its unwrap conversion is already `string(Name) -> string`, and a second function with that signature would clash with it.

## Function overloads

Stored in `SymbolTable.functions` as `Map<String, List<Info>>` (name → overloads). The `Fun` type carries `argTypes` + `returnType` with structural equality. `AbstractTypeManager.resolveFunction` tries an exact match first, then ranks candidates by number of casts needed (via `isAssignableFrom`) and picks the cheapest; a tie throws `AmbiguousException`.

## Known inconsistency

Package naming for language type managers is inconsistent: BASIC and COL use `type/` (singular), Assembunny uses `types/` (plural, matching the base `common/types/`). Not a meaningful distinction — just historical drift.
