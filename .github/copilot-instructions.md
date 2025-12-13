# JCC Copilot Instructions

## Quick Context
JCC (Johan Compiler Collection) is a Maven-based multi-language compiler for BASIC, Tiny, and Assembunny. It uses ANTLR4 for parsing and supports dual backends: FASM (x86-64 assembly) and LLVM IR. This is a production compiler with comprehensive test coverage.

## Architecture Overview

### Module Dependency Chain
```
jcc-compiler (CLI entry point)
├── jcc-{language} modules (BASIC, Tiny, Assembunny, COL)
├── jcc-llvm (LLVM backend)
└── jcc-base (foundational layer - no dependencies)
```

**Critical Insight:** jcc-base is foundational with only Kotlin test dependencies. Language modules depend on jcc-antlr4, jcc-base, and jcc-llvm.

### Compilation Pipeline
```
Source → SyntaxParser (ANTLR) → AST → SemanticsParser (TypeManager + SymbolTable) 
→ AstOptimizer (optional) → CodeGenerator (FASM or LLVM) → Assembler → Executable
```

## Key Abstractions

### 1. Component-Based Code Generation (Critical Pattern)
Every AST node type has a corresponding code generator component registered in a map:

```java
// In AbstractCodeGenerator constructor:
statementCodeGenerators.put(IfStatement.class, new IfCodeGenerator(this));
expressionCodeGenerators.put(AddExpression.class, new AddCodeGenerator(this));
```

**Dispatch:** `AbstractCodeGenerator.statement()` or `expression()` looks up component in map and calls `generate()`.

**Adding a new statement/expression:**
1. Create AST node class (extends Statement or Expression)
2. Add visitor method in language-specific SyntaxVisitor
3. Create code generator component implementing `StatementCodeGeneratorComponent<S>` or `ExpressionCodeGeneratorComponent<E>`
4. Register in both `AbstractCodeGenerator.statementCodeGenerators` map (FASM) and `AbstractLlvmCodeGenerator.statementDictionary` map (LLVM)

### 2. Visitor Pattern for AST Construction
Language-specific `*SyntaxVisitor` extends ANTLR-generated `*BaseVisitor<Node>`. Each visit method:
- Extracts line/column from context
- Recursively visits child nodes via `accept(this)`
- Creates typed AST nodes

**Example:** `BasicSyntaxVisitor.visitPrintStatement()` returns `PrintStatement` node with validated child expressions.

### 3. Hierarchical Symbol Table with Scope Management
```java
SymbolTable table = new SymbolTable();
table.withLocalSymbolTable(() -> {
    table.addVariable(identifier);  // Scoped to local block
    // After lambda, scope reverts to parent
});
```

Used by both SemanticsParser (type checking) and CodeGenerator (variable/function lookups).

### 4. Type System Hierarchy
- NumericType: IntegerType (I8, I32, I64) + FloatType (F32, F64)
- Non-numeric: Bool, Str, Ptr, Arr, Fun, Void
- Str extends AbstractType directly (not a numeric type)
- TypeManager provides inference and coercion rules (language-specific subclasses: BasicTypeManager, etc.)

## Language Implementation Pattern

Every language follows this structure:
```
jcc-{language}/src/main/
├── antlr4/.../{Language}.g4                (ANTLR grammar)
├── java/.../
│   ├── ast/                                (Language-specific nodes only)
│   ├── code/asm/                           (FASM code generators)
│   ├── code/llvm/                          (LLVM code generators)
│   ├── compiler/
│   │   ├── {Language}SyntaxVisitor         (ANTLR visitor)
│   │   ├── {Language}SyntaxParser          (Parsing orchestration)
│   │   ├── {Language}SemanticsParser       (Type checking)
│   │   ├── {Language}CodeGenerator         (FASM entry point)
│   │   ├── {Language}LlvmCodeGenerator     (LLVM entry point)
│   │   └── {Language}TypeManager           (Type inference)
│   ├── functions/                          (Built-in function stubs)
│   └── optimization/                       (AST optimizations)
```

**BASIC-Specific:** Uses type suffix convention (`$` for strings). Has GosubStatement, PrintStatement, and DEFDBL/DEFINT/DEFSTR type declaration statements. Contains ~43 built-in functions.

## Backend Selection

**FASM Backend (Default)**
- Generates x86-64 assembly (FASM syntax)
- Windows-only via `fasm.exe` assembler
- Supports all languages (BASIC, Tiny, Assembunny, COL)
- Base class: `AbstractCodeGenerator` → language-specific subclass

**LLVM Backend**
- Generates LLVM IR (platform-agnostic)
- Requires Clang 18+ for final compilation
- Status: Full support for Assembunny/COL/Tiny; BASIC in progress
- Base class: `AbstractLlvmCodeGenerator` → language-specific subclass
- Activate: `--backend LLVM` flag or in CompilerFactory

## Build & Test Commands

```bash
# Full build
mvn clean install

# Language-specific module
mvn -pl jcc-basic clean install

# Unit tests (skips LLVM tests by default)
mvn test

# Integration tests
mvn verify

# LLVM tests (requires Clang 18+)
mvn -P llvm-tests verify

# Single test
mvn -Dtest=BasicTypeManagerTest#testGetType test

# Regression tests (compile all examples, compare to reference)
./regression_test
```

## Critical Design Decisions

1. **AST-based, not bytecode:** Compiler works directly on AST for code generation—no intermediate bytecode stage.
2. **Component maps, not inheritance chains:** Rather than subclassing code generators, components are registered in maps. This enables language-specific overrides without deeply nested class hierarchies.
3. **StorageLocation abstraction:** Code generators emit code to target storage locations (registers or memory), not hardcoded locations.
4. **Shared base AST & types:** Common statement/expression types in jcc-base; language-specific types only when necessary.
5. **Symbol table scopes:** Hierarchical scoping with parent pointers supports nested functions/blocks without flattening symbols.

## Common Tasks & Patterns

**Adding a new built-in function:** Update `*Functions` class with function signature, register in SemanticsParser, implement code generator.

**Adding language-specific AST node:** Create class in `jcc-{language}/ast/`, add visitor method, create code generators (FASM + LLVM), register in both code generators.

**Fixing type inference:** Override `getType(Expression)` in language-specific TypeManager (e.g., `BasicTypeManager.getType()`) to customize type resolution for expressions. For function argument type matching, override `resolveFunction()` in `AbstractTypeManager`.

**Type checking in semantics:** Use `AbstractSemanticsParserComponent.getType(Expression)` to retrieve expression types; use `TypeManager.isAssignableFrom(thisType, thatType)` to validate assignments; use `TypeManager.resolveFunction()` to find matching functions with implicit type conversion.

**Optimizing expression:** Implement transformation in `*AstOptimizer` (e.g., constant folding in BasicAstOptimizer).

**Debugging codegen:** Enable `-v` (verbose) flag, check generated `.asm` or `.ll` files in target directory.

## Key Files

| File | Purpose |
|------|---------|
| `jcc-base/src/main/java/.../ast/` | Shared AST nodes (if, while, expressions) |
| `jcc-base/src/main/java/.../compiler/AbstractCodeGenerator.java` | FASM code gen dispatcher + shared logic |
| `jcc-llvm/.../AbstractLlvmCodeGenerator.java` | LLVM code gen dispatcher |
| `jcc-compiler/src/main/java/.../CompilerFactory.java` | Component instantiation + language/backend selection |
| `jcc-compiler/src/main/java/.../Jcc.java` | CLI argument parsing |

## Testing Strategy

- **Unit tests:** Component isolation (one *CodeGeneratorTest per component)
- **Integration tests:** Full compilation pipeline (BasicCompilerIT, TinyCompilerIT, etc.)
- **Regression tests:** Exact assembly output comparison against reference baselines
- **Conditional LLVM tests:** Marked with `@Tag("LLVM")` to skip on systems without Clang 18

See `AbstractBasicCodeGeneratorComponentTests.kt` for test base class pattern.
