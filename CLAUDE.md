# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

JCC (Johan Compiler Collection) is a multi-module Maven-based compiler infrastructure for compiling three toy languages (BASIC, Tiny, Assembunny) to executable code. The project supports dual backends: FASM (Flat Assembler) for x86-64 assembly output and LLVM for intermediate code generation.

**Key Technologies:**
- ANTLR4 for lexical and syntax analysis
- Maven for build management
- Java 21 + Kotlin for implementation
- FASM and LLVM as code generation targets

---

## Module Dependencies and Architecture

### Dependency Graph

```
jcc-compiler (main entry point)
├── jcc-basic, jcc-tiny, jcc-assembunny, jcc-col (language modules)
│   ├── jcc-antlr4 (ANTLR utilities)
│   ├── jcc-base (shared infrastructure)
│   └── jcc-llvm (LLVM backend support)
├── jcc-llvm
│   └── jcc-base
├── jcc-antlr4
│   └── jcc-base
└── jcc-base (no module dependencies - foundational layer)
```

### Module Purposes

#### **jcc-base** (Core Foundation)
- **Role:** Provides shared abstractions and infrastructure used by all languages
- **Dependencies:** None (foundational module - only Kotlin test dependencies)
- **Contents:**
  - **AST Nodes** (`ast/`): Abstract syntax tree classes (Expression, Statement, AbstractNode)
  - **Type System** (`types/`): Type representations (I32, I64, F32, F64, Str, Bool, Arr, Fun, Ptr)
  - **Symbol Table** (`symbols/`): Variable/function tracking and scope management
  - **Code Generation Framework** (`code/`): 
    - `TargetProgram`: Container for generated code
    - `Line`, `CodeContainer`: Base code output abstractions
    - `ExpressionCodeGeneratorComponent`, `StatementCodeGeneratorComponent`: Interfaces for generating target code
  - **Compiler Infrastructure** (`compiler/`):
    - `CodeGenerator`: Interface for code generators
    - `SyntaxParser`: Interface for syntax parsing
    - `SemanticsParser`: Interface for semantic analysis
    - `AbstractCodeGenerator`: Base class with common code generation logic
    - `TypeManager`: Type checking and inference
  - **Assembly Abstractions** (`assembly/`): Low-level assembly concepts (instructions, sections, directives)

#### **jcc-antlr4** (ANTLR Integration)
- **Role:** Utilities for working with ANTLR4-generated parsers
- **Dependencies:** jcc-base, ANTLR4 runtime
- **Contents:** `Antlr4Utils` for error handling, stream conversion, and parsing utilities
- **Note:** jcc-base does NOT depend on jcc-antlr4; the dependency flows the other way

#### **jcc-basic, jcc-tiny, jcc-assembunny, jcc-col** (Language Modules)
- **Role:** Language-specific implementations
- **Common Structure:**
  ```
  jcc-{language}/
  ├── src/main/antlr4/          (ANTLR grammar files: *.g4)
  ├── src/main/java/
  │   └── se/dykstrom/jcc/{language}/
  │       ├── ast/              (Language-specific AST nodes)
  │       ├── code/             (Language-specific code generators)
  │       ├── compiler/         (SyntaxParser, SemanticsParser, CodeGenerator impls)
  │       ├── functions/        (Built-in functions)
  │       ├── optimization/     (Language-specific optimizations)
  │       └── semantics/        (Semantic analysis rules)
  ```
- **Dependencies:** jcc-antlr4, jcc-base, jcc-llvm
- **Key Classes:**
  - `*SyntaxParser`: Converts ANTLR parse tree to AST
  - `*SyntaxVisitor`: ANTLR visitor implementing tree traversal
  - `*SemanticsParser`: Type checking and semantic validation
  - `*CodeGenerator`: Targets FASM assembly
  - `*LlvmCodeGenerator`: Targets LLVM IR

#### **jcc-llvm** (LLVM Backend)
- **Role:** Provide LLVM IR code generation infrastructure
- **Dependencies:** jcc-base only
- **Contents:**
  - **Operations** (`operation/`): LLVM IR operation types (BinaryOperation, UnaryOperation, CallOperation, etc.)
  - **Operands** (`operand/`): LLVM IR operand types (TempOperand, LiteralOperand)
  - **Code Generation** (`code/`):
    - `AbstractLlvmCodeGenerator`: Base for language-specific LLVM generators
    - `LlvmStatementCodeGenerator`, `LlvmExpressionCodeGenerator`: Component interfaces
    - Statement/expression generators for all AST node types
  - **Type System**: LLVM type mappings
- **Design:** Uses same component-based code generation as FASM backend but emits LLVM IR

#### **jcc-compiler** (Main Entry Point)
- **Role:** CLI driver and compiler assembly
- **Dependencies:** All language modules, jcc-llvm
- **Key Classes:**
  - `Jcc`: CLI argument parsing and main entry point
  - `CompilerFactory`: Creates compiler instances with appropriate components for a language/backend
  - `GenericCompiler`: Orchestrates the compilation pipeline
  - `Assembler`: Interfaces for backend assemblers (FasmAssembler, LlvmAssembler)
  - `Language`: Enum detecting language from file extension

---

## Compilation Pipeline

### End-to-End Flow

```
Source File (.bas/.tiny/.asmb)
    ↓
[CLI parsing] (Jcc.main)
    ↓
[Language detection] (Language.fromSource)
    ↓
[Component creation] (CompilerFactory.create)
    ├── TypeManager (type system)
    ├── SymbolTable (variable/function tracking)
    ├── SyntaxParser (lexing + parsing)
    ├── SemanticsParser (type checking)
    ├── AstOptimizer (optional optimizations)
    └── CodeGenerator (code generation)
    ↓
[Compilation] (GenericCompiler.compile)
    ├── SyntaxParser.parse(InputStream) → AstProgram
    ├── SemanticsParser.parse(AstProgram) → AstProgram (validated)
    ├── AstOptimizer.optimize(AstProgram) → AstProgram
    └── CodeGenerator.generate(AstProgram) → TargetProgram
    ↓
[Assembly/Linking] (Assembler.assemble)
    ├── FasmAssembler: fasm.exe input.asm -o output.exe
    └── LlvmAssembler: clang input.ll -o output.exe
    ↓
Executable (.exe)
```

### Key Pipeline Components

#### 1. **Syntax Parsing**
- **Input:** InputStream (source code)
- **Output:** AstProgram (AST)
- **Implementation:** `SyntaxParser` interface
- **Process:**
  1. ANTLR lexer tokenizes input
  2. ANTLR parser generates parse tree
  3. Language-specific visitor (e.g., `BasicSyntaxVisitor`) traverses parse tree
  4. Visitor builds AST nodes and returns root AstProgram
- **Error Handling:** Syntax errors caught by ANTLR and reported via CompilationErrorListener

#### 2. **Semantic Analysis**
- **Input:** AstProgram (AST from parser)
- **Output:** AstProgram (enhanced with type information)
- **Implementation:** `SemanticsParser` interface
- **Process:**
  1. Traverse AST nodes
  2. Build symbol table (track variables, functions, constants)
  3. Perform type checking and inference
  4. Validate control flow (e.g., return statements in functions)
  5. Apply language-specific semantic rules
- **Error Handling:** Semantic errors reported via CompilationErrorListener
- **Scope Management:** Hierarchical SymbolTable with parent pointers for nested scopes

#### 3. **Optimization** (Optional)
- **Input:** AstProgram
- **Output:** AstProgram (optimized)
- **Implementation:** `AstOptimizer` interface
- **Features:**
  - Expression optimization (constant folding, dead code elimination)
  - Language-specific optimizations (BASIC-specific in BasicAstOptimizer)
- **Activation:** `-O1` or `-O2` compiler flags

#### 4. **Code Generation**
- **Input:** AstProgram (AST)
- **Output:** TargetProgram (assembly or LLVM IR)
- **Implementation:** `CodeGenerator` interface with two backends:

**FASM Backend:**
- Generates x86-64 assembly code in FASM syntax
- Output: `.asm` file
- Components: Statement and expression code generators (Maps AST nodes to assembly instructions)
- Runtime: Linked with msvcrt.dll (C runtime) and language-specific standard libraries (libjccbas.dll for BASIC)

**LLVM Backend:**
- Generates LLVM intermediate representation
- Output: `.ll` file
- Components: Statement and expression code generators (Maps AST nodes to LLVM operations)
- Uses same component architecture as FASM but different code emission

### Code Generation Architecture

The code generation uses a **Component Pattern** for modularity:

```
CodeGenerator (main entry point)
├── StatementCodeGeneratorComponent map
│   ├── IfStatement → IfCodeGenerator
│   ├── WhileStatement → WhileCodeGenerator
│   ├── VariableDeclarationStatement → VariableDeclarationCodeGenerator
│   └── ... (43+ statement types)
└── ExpressionCodeGeneratorComponent map
    ├── AddExpression → AddCodeGenerator
    ├── FunctionCallExpression → FunctionCallCodeGenerator
    ├── IdentifierExpression → IdentifierDerefCodeGenerator
    └── ... (43+ expression types)
```

**Code Generation Process:**
1. Dispatch to appropriate component based on AST node type
2. Component generates code by:
   - Processing child nodes recursively
   - Allocating storage locations (registers or memory)
   - Emitting target instructions
3. Components return List<Line> (assembly/LLVM operations)
4. Results collected into TargetProgram

---

## Code Generation Backends

### FASM Backend (x86-64 Assembly)

**Architecture:**
- Generates Flat Assembler (FASM) syntax for x86-64
- Platform: Windows (uses FASM assembler, msvcrt.dll runtime)
- Base class: `AbstractCodeGenerator` with `AsmCodeGenerator` interface

**Code Output Structure:**
```
Header (includes, format specification)
Import Section (external function declarations)
Data Section (global variables, string literals)
Code Section (main function + user functions)
Epilogue (exit code)
```

**Storage Management:**
- `StorageFactory`: Creates storage locations (registers or memory)
- Registers: RAX, RCX, RDX, RSI, RDI, R8-R15
- Memory: Stack-based local variable storage
- Calling Convention: Microsoft x64 (shadow space, rsp alignment)

**Function Call Helper:**
- `FunctionCallHelper`: Manages argument passing and return value handling
- Built-in functions: Math, string, I/O functions from C runtime
- User functions: Managed through symbol table and function definitions

**Example: Add Expression**
```
Input AST:  AddExpression(left: IntegerLiteral(5), right: IntegerLiteral(3))
Output AST: Generated assembly moves 5 and 3 to registers, uses ADD instruction
```

### LLVM Backend

**Architecture:**
- Generates LLVM Intermediate Representation (.ll files)
- Platform: Platform-independent (uses clang for final compilation)
- Base class: `AbstractLlvmCodeGenerator` with `LlvmCodeGenerator` interface
- Requirements: Java 21+ and Clang 18+
- Status: Experimental support for Assembunny, COL, and Tiny (BASIC support is work in progress)

**LLVM IR Operations:**
- Binary operations (add, sub, mul, div, etc.)
- Unary operations (neg, not, etc.)
- Memory operations (load, store, allocate, getelementptr)
- Control flow (branch, conditional branch, return)
- Function calls (call operations with type signatures)
- Type conversions (convert operations)

**Key Differences from FASM:**
1. **Intermediate Representation:** Outputs LLVM IR instead of assembly directly
2. **Type Information:** LLVM IR includes explicit type information
3. **SSA Form:** Operations produce temporary values (%1, %2, etc.)
4. **Optimization:** LLVM optimizer runs before final code generation
5. **Platform Agnostic:** Works on Linux, macOS, Windows via clang

**Example: Conditional Branch**
```
LLVM IR Output:
  %cmp = fcmp oge double %a, %b
  br i1 %cmp, label %if.then, label %if.else
if.then:
  ...
if.else:
  ...
```

### Backend Selection
- **CLI Flag:** `--backend FASM|LLVM`
- **Default:** FASM
- **Factory Logic:** `CompilerFactory.createCodeGenerator()` selects implementation
- **Language Support:**
  - BASIC: BasicCodeGenerator (FASM) / BasicLlvmCodeGenerator (LLVM - in progress)
  - Tiny: TinyCodeGenerator (FASM) / TinyLlvmCodeGenerator (LLVM)
  - Assembunny: AssembunnyCodeGenerator (FASM) / AssembunnyLlvmCodeGenerator (LLVM)
  - COL: ColCodeGenerator (FASM) / ColLlvmCodeGenerator (LLVM)

**LLVM Installation:**
- Windows: Download LLVM MinGW prebuilt release
- macOS: `brew install llvm` or use Xcode Clang
- Linux: `sudo apt-get install clang`
- Set custom clang path: Use `-assembler /path/to/clang` flag

---

## Language-Specific Architecture

### Shared vs. Language-Specific Code

**Shared (in jcc-base):**
- Common AST node types (Expression, Statement, IfStatement, WhileStatement, etc.)
- Common types (I32, I64, F64, Str, Bool, Arr)
- Common code generators (e.g., AddCodeGenerator works for all languages)
- Common compiler framework (TypeManager, SymbolTable, SyntaxParser, etc.)

**Language-Specific:**
- Lexer/Parser rules (*.g4 ANTLR grammar files)
- AST visitor implementation
- Language-specific AST nodes (e.g., `GosubStatement` in BASIC)
- Language-specific code generators (e.g., GosubCodeGenerator in BASIC)
- Built-in functions (math, I/O, string manipulation)
- Type inference rules
- Semantic validation rules
- Optimization passes

### Language Module Organization Example: BASIC

**Module Structure:**
```
jcc-basic/src/main/
├── antlr4/se/dykstrom/jcc/basic/compiler/
│   └── Basic.g4                          (ANTLR grammar)
└── java/se/dykstrom/jcc/basic/
    ├── ast/
    │   ├── expression/
    │   │   ├── EqvExpression              (EQV logical operation)
    │   │   └── ImpExpression              (IMP logical operation)
    │   └── statement/
    │       ├── GosubStatement             (GOSUB control flow)
    │       ├── PrintStatement             (PRINT I/O)
    │       ├── DefDblStatement            (Type declarations)
    │       └── ... (BASIC-specific statements)
    ├── code/asm/                          (FASM code generation)
    │   ├── expression/
    │   │   ├── BasicFunctionCallCodeGenerator
    │   │   └── BasicIdentifierDerefCodeGenerator
    │   └── statement/
    │       ├── PrintCodeGenerator
    │       ├── GosubCodeGenerator
    │       └── ...
    ├── code/llvm/                         (LLVM code generation)
    │   ├── expression/
    │   └── statement/
    ├── compiler/
    │   ├── BasicSyntaxParser              (Lexing + parsing)
    │   ├── BasicSyntaxVisitor             (Parse tree → AST)
    │   ├── BasicSemanticsParser           (Type checking)
    │   ├── BasicTypeManager               (BASIC type system)
    │   ├── BasicSymbols                   (Built-in symbols)
    │   ├── BasicCodeGenerator             (FASM code generation)
    │   └── BasicLlvmCodeGenerator         (LLVM code generation)
    ├── functions/
    │   ├── BasicAsmFunctions              (Built-in function stubs for FASM)
    │   └── BasicLlvmFunctions             (Built-in function stubs for LLVM)
    └── optimization/
        └── BasicAstOptimizer             (BASIC-specific optimizations)
```

**BASIC-Specific Features:**
1. **Type Declarations:** DEFDBL, DEFINT, DEFSTR statements affect type inference
2. **Built-in Functions:** 43+ functions (abs, sin, cos, len, mid$, chr$, etc.)
3. **String Type:** Dynamic string support with garbage collection
4. **Numeric Types:** INTEGER (i64), DOUBLE (f64)
5. **Arrays:** Static arrays with OPTION BASE support
6. **Control Flow:** GOSUB/RETURN, GOTO, ON-GOSUB, ON-GOTO
7. **I/O:** PRINT, LINE INPUT, CLS (clear screen)
8. **User Functions:** DEF FN single-expression functions

**Type System Hierarchy:**
```
BasicTypeManager extends DefaultTypeManager
├── Type inference rules (BASIC string suffix convention: $ for strings)
├── Type coercion (implicit conversions)
└── Type checking (validates operations between types)
```

### Language Implementation Pattern

Each language follows this template:

```
1. Grammar Definition (Basic.g4)
   ├── Lexer rules (tokens)
   └── Parser rules (syntax)

2. AST Building (BasicSyntaxVisitor)
   ├── Visitor for each parser rule
   └── Creates language-specific AST nodes

3. Semantic Analysis (BasicSemanticsParser)
   ├── Traverse AST
   ├── Build symbol table
   ├── Type checking
   └── Report errors

4. Code Generation (BasicCodeGenerator)
   ├── Traverse AST
   ├── Allocate storage
   ├── Emit instructions (FASM or LLVM)
   └── Return TargetProgram

5. Built-in Functions (BasicAsmFunctions, BasicLlvmFunctions)
   ├── Function prototypes
   ├── Type signatures
   └── Implementation stubs (resolved by linker)
```

---

## Key Abstractions and Design Patterns

### 1. **AST Node Hierarchy**

```
Node (interface)
├── Statement
│   ├── AbstractNode
│   ├── IfStatement
│   ├── WhileStatement
│   ├── FunctionDefinitionStatement
│   ├── VariableDeclarationStatement
│   └── ... (19+ statement types in base)
└── Expression
    ├── AbstractNode
    ├── BinaryExpression
    │   ├── AddExpression
    │   ├── MulExpression
    │   └── ... (arithmetic, relational, logical)
    ├── UnaryExpression
    │   ├── NegateExpression
    │   └── NotExpression
    ├── LiteralExpression
    │   ├── IntegerLiteral
    │   ├── FloatLiteral
    │   └── StringLiteral
    └── ... (identifier references, function calls, casts)
```

**Design:** Every AST node extends AbstractNode (tracking line/column for error messages), implements visitor pattern for tree traversal.

### 2. **Type System**

```
Type (interface)
├── AbstractType (base class)
│   ├── NumericType (interface)
│   │   ├── IntegerType (interface)
│   │   │   └── I8, I32, I64
│   │   └── FloatType (interface)
│   │       └── F32, F64
│   ├── Bool (boolean type)
│   ├── Str (string type)
│   ├── Ptr (pointer/reference type)
│   ├── Arr (array type)
│   ├── Fun (function type with parameters/return)
│   └── Void
```

**Design:** Types are immutable singletons or value objects. TypeManager provides type inference and coercion rules. Note that Str extends AbstractType directly, not FloatType or NumericType.

### 3. **Component-Based Code Generation**

```
CodeGenerator (main interface)
├── AbstractCodeGenerator (FASM base)
│   ├── statementCodeGenerators: Map<Class, StatementCodeGeneratorComponent>
│   └── expressionCodeGenerators: Map<Class, ExpressionCodeGeneratorComponent>
└── AbstractLlvmCodeGenerator (LLVM base)
    ├── statementDictionary: Map<Class, LlvmStatementCodeGenerator>
    └── expressionDictionary: Map<Class, LlvmExpressionCodeGenerator>
```

**Design:** Each AST node type has a corresponding code generator component. The main generator dispatches to components via map lookup. Components are registered in constructor.

**Benefits:**
- Easy to add new expression/statement types
- Language-specific code generators extend and override components
- Testable in isolation

### 4. **Symbol Table with Scope Management**

```
SymbolTable (hierarchical)
├── parent: SymbolTable (null for global scope)
├── symbols: Map<String, Info>  (regular variables)
├── arrays: Map<String, Info>   (array variables)
├── functions: Map<String, List<Info>>  (function overloads)
├── Constants: Map<String, String>  (compile-time constants)
└── Temporary Names: Counter for generated unique names
```

**Operations:**
- `addVariable(Identifier)`: Register variable in current scope
- `addGlobal(Identifier, value)`: Register in global (root) scope
- `withLocalSymbolTable(Supplier)`: Create child scope, execute code, restore parent
- `lookup(name)`: Find symbol starting from current scope, traversing parents

**Design:** Supports nested scopes (functions, blocks) with fallback to parent scope.

### 5. **Visitor Pattern for Code Generation**

Each code generator component implements the visitor pattern:

```
ExpressionCodeGeneratorComponent<E extends Expression>
├── generate(E expression, StorageLocation location): List<Line>
│   ├── Visit child expressions recursively
│   ├── Allocate storage for intermediate results
│   └── Emit code instructions

Example: AddCodeGenerator
├── Generate left expression
├── Generate right expression
├── Emit ADD instruction combining results
└── Return result in storage location
```

### 6. **Storage Location Abstraction**

```
StorageLocation (interface)
├── RegisterStorageLocation (CPU register)
│   └── name: String (rax, rcx, etc.)
└── MemoryStorageLocation (stack or global memory)
    ├── base: String (register base pointer)
    └── offset: long (offset from base)
```

**Use:** Code generators receive target storage location and emit code to place result there. Enables flexible register/memory allocation.

### 7. **Compiler Factory Pattern**

```
CompilerFactory
├── language: Language  (detected from file extension)
├── backend: Backend    (FASM or LLVM)
└── create(sourcePath, outputPath): Compiler
    ├── Language-specific component creation
    │   ├── TypeManager (e.g., BasicTypeManager)
    │   ├── SymbolTable (e.g., BasicSymbols)
    │   ├── SyntaxParser (e.g., BasicSyntaxParser)
    │   ├── SemanticsParser (e.g., BasicSemanticsParser)
    │   └── CodeGenerator (e.g., BasicCodeGenerator or BasicLlvmCodeGenerator)
    └── Return GenericCompiler with all components
```

**Design:** Factory abstracts away complexity of component creation. Supports adding new languages/backends without modifying main compiler logic.

---

## Data Flow and Dependencies

### Compilation Data Flow

```
Source Text
    ↓
    Lexer (ANTLR-generated)
    ↓ (Tokens)
    Parser (ANTLR-generated)
    ↓ (Parse Tree)
    SyntaxVisitor (Language-specific)
    ↓ (AST with type stubs)
    SemanticsParser (Language-specific)
    ├── Use TypeManager to resolve types
    ├── Use SymbolTable to track symbols
    └── Return validated/enhanced AST
    ↓
    AstOptimizer (optional)
    ├── Constant folding
    ├── Dead code elimination
    └── Language-specific optimizations
    ↓
    CodeGenerator (FASM or LLVM)
    ├── Use TypeManager for type queries
    ├── Use SymbolTable for symbol lookups
    ├── Use StorageFactory for location allocation
    └── Return TargetProgram
    ↓
    Assembler (FasmAssembler or LlvmAssembler)
    ├── Invoke system assembler (fasm.exe or clang)
    └── Produce Executable
```

### Component Interdependencies

```
CompilerFactory
    ├── TypeManager
    │   ├── Used by: SemanticsParser, CodeGenerator
    │   └── Provides: Type inference, type checking, type coercion rules
    ├── SymbolTable
    │   ├── Used by: SemanticsParser, CodeGenerator
    │   └── Provides: Variable/function/constant tracking, scope management
    ├── SyntaxParser
    │   ├── Depends on: ANTLR grammar, SyntaxVisitor
    │   └── Returns: AST
    ├── SemanticsParser
    │   ├── Depends on: TypeManager, SymbolTable
    │   └── Validates: Type correctness, control flow, name resolution
    ├── CodeGenerator
    │   ├── Depends on: TypeManager, SymbolTable, StorageFactory
    │   ├── Uses: ExpressionCodeGeneratorComponent map
    │   ├── Uses: StatementCodeGeneratorComponent map
    │   └── Returns: TargetProgram
    └── AstOptimizer
        ├── Depends on: TypeManager, SymbolTable
        └── Optimizes: Expressions, statements
```

---

## Extension Points

### Adding a New Language

1. Create `jcc-{language}` module with pom.xml depending on jcc-antlr4 and jcc-base
2. Write ANTLR4 grammar file `src/main/antlr4/.../Language.g4`
3. Implement `*SyntaxVisitor` extending generated visitor
4. Create `*SyntaxParser` implementing SyntaxParser interface
5. Create `*SemanticsParser` implementing SemanticsParser interface
6. Create language-specific AST node types if needed
7. Create `*CodeGenerator` extending AbstractCodeGenerator
8. Create `*LlvmCodeGenerator` extending AbstractLlvmCodeGenerator (optional)
9. Implement code generators for all statement/expression types
10. Register compilers in CompilerFactory and Language enum

### Adding a New Code Generation Target

1. Create new backend interface inheriting from CodeGenerator
2. Implement code generation for all AST node types
3. Create custom components for target-specific operations
4. Update CompilerFactory to select new backend
5. Create assembler implementation for target toolchain

### Adding a New Statement Type

1. Create AST node class extending Statement
2. Add parser rule in ANTLR grammar
3. Add visitor method in *SyntaxVisitor
4. Add semantic validation in *SemanticsParser (if needed)
5. Create code generator component implementing StatementCodeGeneratorComponent
6. Register component in AbstractCodeGenerator.statementCodeGenerators map
7. Register component in AbstractLlvmCodeGenerator.statementDictionary map

---

## Key Files Reference

| File                                                                                                   | Purpose                              |
|--------------------------------------------------------------------------------------------------------|--------------------------------------|
| `/jcc-base/src/main/java/se/dykstrom/jcc/common/ast/`                                                  | AST node definitions                 |
| `/jcc-base/src/main/java/se/dykstrom/jcc/common/types/`                                                | Type system                          |
| `/jcc-base/src/main/java/se/dykstrom/jcc/common/symbols/SymbolTable.java`                              | Scope/symbol management              |
| `/jcc-base/src/main/java/se/dykstrom/jcc/common/compiler/AbstractCodeGenerator.java`                   | FASM code generation base            |
| `/jcc-base/src/main/java/se/dykstrom/jcc/common/code/expression/ExpressionCodeGeneratorComponent.java` | Expression code generation interface |
| `/jcc-base/src/main/java/se/dykstrom/jcc/common/code/statement/StatementCodeGeneratorComponent.java`   | Statement code generation interface  |
| `/jcc-llvm/src/main/java/se/dykstrom/jcc/llvm/code/AbstractLlvmCodeGenerator.java`                     | LLVM code generation base            |
| `/jcc-basic/src/main/antlr4/.../Basic.g4`                                                              | BASIC grammar                        |
| `/jcc-basic/src/main/java/se/dykstrom/jcc/basic/compiler/BasicSyntaxVisitor.java`                      | BASIC parse tree visitor             |
| `/jcc-basic/src/main/java/se/dykstrom/jcc/basic/compiler/BasicCodeGenerator.java`                      | BASIC code generator                 |
| `/jcc-compiler/src/main/java/se/dykstrom/jcc/main/Jcc.java`                                            | CLI entry point                      |
| `/jcc-compiler/src/main/java/se/dykstrom/jcc/main/CompilerFactory.java`                                | Component factory                    |

---

## Build and Test

### Building the Project

```bash
mvn clean install              # Build all modules
mvn -pl jcc-compiler package  # Build only compiler
```

### Running the Compiler

```bash
java -jar jcc-compiler/target/jcc-*.jar program.bas
jcc program.bas               # If in PATH
```

### LLVM Backend

```bash
jcc --backend LLVM program.bas
```

### Testing

```bash
mvn test                         # Unit tests (excludes LLVM tests)
mvn verify                       # Integration tests (excludes LLVM tests)
mvn -P llvm-tests verify         # All tests including LLVM (requires Clang 18+)
```

### Running Single Tests

```bash
# Run a specific test class
mvn -Dtest=BasicTypeManagerTest test

# Run a specific test method
mvn -Dtest=BasicTypeManagerTest#testGetType test

# Run integration tests for a specific language
mvn -Dit.test=BasicCompilerIT verify
```

### Regression Testing

```bash
# Run regression test for BASIC code generation
./regression_test                    # Uses jcc from PATH
./regression_test /path/to/jcc      # Uses specific jcc script
```

This compiles all BASIC examples with `-S` flag and compares the generated assembly to pre-compiled reference files in `jcc-compiler/src/test/resources/`.

