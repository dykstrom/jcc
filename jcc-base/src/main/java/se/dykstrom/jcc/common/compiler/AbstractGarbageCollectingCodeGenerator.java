/*
 * Copyright (C) 2019 Johan Dykstrom
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package se.dykstrom.jcc.common.compiler;

import se.dykstrom.jcc.common.assembly.base.AssemblyComment;
import se.dykstrom.jcc.common.assembly.directive.Label;
import se.dykstrom.jcc.common.assembly.instruction.AddImmToReg;
import se.dykstrom.jcc.common.assembly.instruction.CallDirect;
import se.dykstrom.jcc.common.assembly.instruction.SubImmFromReg;
import se.dykstrom.jcc.common.assembly.directive.DataDefinition;
import se.dykstrom.jcc.common.assembly.section.Section;
import se.dykstrom.jcc.common.ast.*;
import se.dykstrom.jcc.common.code.expression.GcAddCodeGenerator;
import se.dykstrom.jcc.common.functions.MemoryManagementUtils;
import se.dykstrom.jcc.common.code.Blank;
import se.dykstrom.jcc.common.code.CodeContainer;
import se.dykstrom.jcc.common.code.Line;
import se.dykstrom.jcc.common.optimization.AstOptimizer;
import se.dykstrom.jcc.common.storage.StorageLocation;
import se.dykstrom.jcc.common.symbols.SymbolTable;
import se.dykstrom.jcc.common.types.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static se.dykstrom.jcc.common.assembly.base.Register.*;
import static se.dykstrom.jcc.common.functions.BuiltInFunctions.FUN_MEMORY_REGISTER;
import static se.dykstrom.jcc.common.functions.MemoryManagementUtils.*;
import static se.dykstrom.jcc.common.utils.ExpressionUtils.evaluateIntegerExpressions;

/**
 * Abstract base class for code generators that generate code that includes
 * automatic memory management and garbage collection.
 *
 * @author Johan Dykstrom
 */
public abstract class AbstractGarbageCollectingCodeGenerator extends AbstractCodeGenerator {

    protected AbstractGarbageCollectingCodeGenerator(final TypeManager typeManager,
                                                     final SymbolTable symbolTable,
                                                     final AstOptimizer optimizer) {
        super(typeManager, symbolTable, optimizer);
        this.functionCallHelper = new GarbageCollectingFunctionCallHelper(this);
        // Expressions
        expressionCodeGenerators.put(AddExpression.class, new GcAddCodeGenerator(this));
    }

    /**
     * Adds type pointers for all identifiers that can identify any type of dynamic memory. These
     * type pointers contain pointers to corresponding nodes in the memory allocation list. Initially,
     * all type pointers are 0.
     */
    @Override
    protected Section dataSection(SymbolTable symbols) {
        // Add definitions for all normal variables
        Section section = super.dataSection(symbols);

        section.add(new AssemblyComment("--- Dynamic memory type pointers -->"));
        section.add(new DataDefinition(TYPE_POINTERS_START.getIdentifier(), NOT_MANAGED, true));

        // Add one type pointer for each scalar identifier
        List<Identifier> identifiers = new ArrayList<>(symbols.identifiers());
        Collections.sort(identifiers);
        identifiers.stream()
                .filter(identifier -> storesDynamicMemory(identifier, symbols.isConstant(identifier.name())))
                .map(this::deriveTypeIdentifier)
                .forEach(identifier -> section.add(new DataDefinition(identifier, NOT_MANAGED, true)));

        // Add one type pointer for each array identifier
        identifiers = new ArrayList<>(symbols.arrayIdentifiers());
        Collections.sort(identifiers);
        identifiers.stream()
                .filter(identifier -> storesDynamicMemory(identifier, false))
                .forEach(identifier -> {
                    List<Expression> subscripts = symbols.getArrayValue(identifier.name()).getSubscripts();
                    List<Long> evaluatedSubscripts = evaluateIntegerExpressions(subscripts, symbols, optimizer.expressionOptimizer());
                    long numberOfElements = evaluatedSubscripts.stream().reduce(1L, (a, b) -> a * b);
                    Identifier arrayIdent = deriveTypeIdentifier(deriveArrayIdentifier(identifier));
                    String arrayValue = numberOfElements + " dup " + NOT_MANAGED;
                    section.add(new DataDefinition(arrayIdent, arrayValue, true));
                });

        section.add(new DataDefinition(TYPE_POINTERS_STOP.getIdentifier(), NOT_MANAGED, true));
        section.add(new AssemblyComment("<-- Dynamic memory type pointers ---"));

        section.add(Blank.INSTANCE);
        return section;
    }

    /**
     * Extends the generic code generation for assign statements with functionality for managing dynamic memory.
     */
    @Override
    protected void assignStatement(AssignStatement statement) {
        super.assignStatement(statement);

        if (allocatesDynamicMemory(statement.getRhsExpression())) {
            addAll(registerDynamicMemory(statement.getLhsExpression()));
        } else if (throwsDynamicMemory(statement.getRhsExpression())) {
            stopDynamicMemory(statement.getLhsExpression());
        } else if (reassignsDynamicMemory(statement.getRhsExpression())) {
            copyDynamicMemory(statement.getLhsExpression(), (IdentifierExpression) statement.getRhsExpression());
        }
    }

    /**
     * Generates code to register the dynamic memory referenced by {@code expression}
     * in the memory allocation list.
     */
    public List<Line> registerDynamicMemory(IdentifierExpression expression) {
        CodeContainer cc = new CodeContainer();

        cc.add(Blank.INSTANCE);
        cc.add(new AssemblyComment("Register dynamic memory assigned to " + expression));

        cc.addAll(withAddressOfIdentifier(expression, (base, offset) -> withCodeContainer(it -> {
            storageFactory.get(RCX).moveAddressToThis(base + offset, it);
            storageFactory.get(RDX).moveAddressToThis(deriveMappedTypeName(base) + offset, it);
        })));
        cc.add(new SubImmFromReg(SHADOW_SPACE, RSP));
        cc.add(new CallDirect(new Label(FUN_MEMORY_REGISTER.getMappedName())));
        cc.add(new AddImmToReg(SHADOW_SPACE, RSP));

        addUsedBuiltInFunction(FUN_MEMORY_REGISTER);
        addAllFunctionDependencies(FUN_MEMORY_REGISTER.getDependencies());
        addAllConstantDependencies(FUN_MEMORY_REGISTER.getConstants());

        return cc.lines();
    }

    /**
     * Generates code that copies the type pointer from the {@code rhsExpression} to the {@code lhsExpression}.
     */
    private void copyDynamicMemory(IdentifierExpression lhsExpression, IdentifierExpression rhsExpression) {
        add(Blank.INSTANCE);
        add(new AssemblyComment("Make " + lhsExpression + " refer to the same memory as " + rhsExpression));

        try (StorageLocation location = storageFactory.allocateNonVolatile()) {
            addAll(withAddressOfIdentifier(
                    rhsExpression,
                    (base, offset) -> withCodeContainer(cc -> location.moveMemToThis(deriveMappedTypeName(base) + offset, cc))
            ));
            addAll(withAddressOfIdentifier(
                    lhsExpression,
                    (base, offset) -> withCodeContainer(cc -> location.moveThisToMem(deriveMappedTypeName(base) + offset, cc))
            ));
        }
    }

    /**
     * Generates code that stops {@code expression} from referencing any dynamic memory,
     * by setting the type pointer to 0.
     */
    private void stopDynamicMemory(IdentifierExpression expression) {
        add(Blank.INSTANCE);
        add(new AssemblyComment("Make sure " + expression + " does not refer to dynamic memory"));
        addAll(withAddressOfIdentifier(expression, (base, offset) -> withCodeContainer(cc -> {
            storageFactory.get(RCX).moveImmToThis(NOT_MANAGED, cc);
            storageFactory.get(RCX).moveThisToMem(deriveMappedTypeName(base) + offset, cc);
        })));
    }

    /**
     * Derives an identifier for the "variable type pointer" that matches the given {@code identifier}.
     */
    public Identifier deriveTypeIdentifier(Identifier identifier) {
        return deriveTypeIdentifier(identifier.getMappedName());
    }

    /**
     * Derives an identifier for the "variable type pointer" that matches the given {@code variableName}.
     */
    public Identifier deriveTypeIdentifier(String variableName) {
        return new Identifier(variableName + "_type", I64.INSTANCE);
    }

    /**
     * Derives a mapped variable name for the "variable type pointer" that matches the given {@code variableName}.
     */
    public String deriveMappedTypeName(String variableName) {
        return deriveTypeIdentifier(variableName).getMappedName();
    }

    /**
     * Returns true if the given identifier stores any type of dynamic memory.
     */
    private boolean storesDynamicMemory(Identifier identifier, boolean constant) {
        if (constant) {
            return false;
        } else {
            // Strings and arrays of strings are dynamic
            Type type = identifier.type();
            if (type instanceof Arr arr) {
                type = arr.getElementType();
            }
            return type instanceof Str;
        }
    }

    /**
     * Returns {@code true} if evaluating the given expression will allocate dynamic memory.
     */
    private boolean allocatesDynamicMemory(Expression expression) {
        return MemoryManagementUtils.allocatesDynamicMemory(expression, typeManager.getType(expression));
    }

    /**
     * Returns {@code true} if assigning the value of the given expression might lead to
     * reassigning dynamic memory to a new variable. Examples:
     *
     * - assigning a variable to another variable might lead to also reassigning dynamic memory
     *   if the first variable refers to dynamic memory
     *
     * @param expression The expression to check.
     * @return True if assigning {@code expression} might lead to reassigning dynamic memory.
     */
    private boolean reassignsDynamicMemory(Expression expression) {
        return (typeManager.getType(expression) instanceof Str) && (expression instanceof IdentifierExpression);
    }

    /**
     * Returns {@code true} if assigning the value of the given expression to a variable might
     * lead to throwing away (forgetting) dynamic memory. Examples:
     *
     * - assigning a string literal to variable might lead to throwing away dynamic memory
     *   if the variable referred to dynamic memory before
     *
     * @param expression The expression to check.
     * @return True if assigning {@code expression} might lead to throwing dynamic memory.
     */
    private boolean throwsDynamicMemory(Expression expression) {
        return (typeManager.getType(expression) instanceof Str) && (expression instanceof StringLiteral);
    }
}
