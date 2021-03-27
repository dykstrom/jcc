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

import se.dykstrom.jcc.common.assembly.base.*;
import se.dykstrom.jcc.common.assembly.instruction.*;
import se.dykstrom.jcc.common.assembly.other.DataDefinition;
import se.dykstrom.jcc.common.assembly.other.Snippets;
import se.dykstrom.jcc.common.assembly.section.Section;
import se.dykstrom.jcc.common.ast.*;
import se.dykstrom.jcc.common.functions.MemoryManagementUtils;
import se.dykstrom.jcc.common.optimization.AstOptimizer;
import se.dykstrom.jcc.common.storage.StorageLocation;
import se.dykstrom.jcc.common.symbols.SymbolTable;
import se.dykstrom.jcc.common.types.*;
import se.dykstrom.jcc.common.utils.MapUtils;
import se.dykstrom.jcc.common.utils.SetUtils;

import java.util.*;

import static se.dykstrom.jcc.common.assembly.base.Register.*;
import static se.dykstrom.jcc.common.functions.BuiltInFunctions.*;
import static se.dykstrom.jcc.common.functions.FunctionUtils.LIB_LIBC;
import static se.dykstrom.jcc.common.functions.MemoryManagementUtils.*;
import static se.dykstrom.jcc.common.utils.ExpressionUtils.evaluateConstantIntegerExpressions;

/**
 * Abstract base class for code generators that generate code that includes
 * automatic memory management and garbage collection.
 *
 * @author Johan Dykstrom
 */
public abstract class AbstractGarbageCollectingCodeGenerator extends AbstractCodeGenerator {

    protected AbstractGarbageCollectingCodeGenerator(TypeManager typeManager, AstOptimizer optimizer) {
        super(typeManager, optimizer);
        this.functionCallHelper = new GarbageCollectingFunctionCallHelper(this, this, storageFactory, typeManager);
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

        section.add(new Comment("--- Dynamic memory type pointers -->"));
        section.add(new DataDefinition(TYPE_POINTERS_START.getIdentifier(), NOT_MANAGED, true));

        // Add one type pointer for each scalar identifier
        List<Identifier> identifiers = new ArrayList<>(symbols.identifiers());
        Collections.sort(identifiers);
        identifiers.stream()
                .filter(identifier -> storesDynamicMemory(identifier, symbols.isConstant(identifier.getName())))
                .map(this::getMatchingTypeIdent)
                .forEach(identifier -> section.add(new DataDefinition(identifier, NOT_MANAGED, true)));

        // Add one type pointer for each array identifier
        identifiers = new ArrayList<>(symbols.arrayIdentifiers());
        Collections.sort(identifiers);
        identifiers.stream()
                .filter(identifier -> storesDynamicMemory(identifier, false))
                .forEach(identifier -> {
                    List<Expression> subscripts = symbols.getArrayValue(identifier.getName()).getSubscripts();
                    List<Long> evaluatedSubscripts = evaluateConstantIntegerExpressions(subscripts, optimizer.expressionOptimizer());
                    long numberOfElements = evaluatedSubscripts.stream().reduce(1L, (a, b) -> a * b);
                    Identifier arrayIdent = getMatchingTypeIdent(deriveArrayIdentifier(identifier));
                    String arrayValue = numberOfElements + " dup " + NOT_MANAGED;
                    section.add(new DataDefinition(arrayIdent, arrayValue, true));
                });

        section.add(new DataDefinition(TYPE_POINTERS_STOP.getIdentifier(), NOT_MANAGED, true));
        section.add(new Comment("<-- Dynamic memory type pointers ---"));

        section.add(Blank.INSTANCE);
        return section;
    }

    /**
     * Extends the generic code generation for assign statements with functionality for managing dynamic memory.
     */
    @Override
    protected void assignStatement(AssignStatement statement) {
        super.assignStatement(statement);

        Expression lhsExpression = statement.getLhsExpression();
        if (lhsExpression instanceof IdentifierNameExpression) {
            Identifier lhsIdentifier = ((IdentifierNameExpression) lhsExpression).getIdentifier();
            if (allocatesDynamicMemory(statement.getRhsExpression())) {
                registerDynamicMemory(lhsIdentifier);
            } else if (throwsDynamicMemory(statement.getRhsExpression())) {
                stopDynamicMemory(lhsIdentifier);
            } else if (reassignsDynamicMemory(statement.getRhsExpression())) {
                copyDynamicMemory(lhsIdentifier, statement.getRhsExpression());
            }
        } else {
            ArrayAccessExpression arrayAccessExpression = (ArrayAccessExpression) lhsExpression;
            if (allocatesDynamicMemory(statement.getRhsExpression())) {
                registerDynamicMemory(arrayAccessExpression);
            } else if (throwsDynamicMemory(statement.getRhsExpression())) {
                stopDynamicMemory(arrayAccessExpression);
            } else if (reassignsDynamicMemory(statement.getRhsExpression())) {
                copyDynamicMemory(arrayAccessExpression, statement.getRhsExpression());
            }
        }
    }

    /**
     * Extends the generic code generation for add expressions with functionality for adding strings.
     */
    @Override
    protected void addExpression(AddExpression expression, StorageLocation leftLocation) {
        Expression left = expression.getLeft();
        Expression right = expression.getRight();

        Type leftType = typeManager.getType(left);
        Type rightType = typeManager.getType(right);

        // If this is a string addition (concatenation)
        if (leftType instanceof Str && rightType instanceof Str) {
            add(Blank.INSTANCE);
            add(new Comment("--- " + expression + " -->"));

            // Generate code for left sub expression, and store result in leftLocation
            expression(expression.getLeft(), leftLocation);

            try (StorageLocation rightLocation = storageFactory.allocateNonVolatile(rightType);
                 StorageLocation tmpLocation = storageFactory.allocateNonVolatile(I64.INSTANCE)) {
                // Generate code for right sub expression, and store result in rightLocation
                expression(expression.getRight(), rightLocation);

                // Calculate length of result string
                add(new Comment("Calculate length of strings to add (" + leftLocation + " and " + rightLocation + ")"));

                storageFactory.rcx.moveLocToThis(leftLocation, this);
                addAll(Snippets.strlen(RCX));
                add(new Comment("Move length (rax) to tmp location (" + tmpLocation + ")"));
                tmpLocation.moveLocToThis(storageFactory.rax, this);

                storageFactory.rcx.moveLocToThis(rightLocation, this);
                addAll(Snippets.strlen(RCX));
                add(new Comment("Add length (rax) to tmp location (" + tmpLocation + ")"));
                tmpLocation.addLocToThis(storageFactory.rax, this);

                // Add one for the null character
                tmpLocation.incrementThis(this);

                // Allocate memory for result string
                storageFactory.rcx.moveLocToThis(tmpLocation, this);
                addAll(Snippets.malloc(RCX));              // Address to new string in RAX

                // Copy left string to result string
                add(new Comment("Copy left string (" + leftLocation + ") to result string (rax)"));
                storageFactory.rcx.moveRegToThis(RAX, this);
                storageFactory.rdx.moveLocToThis(leftLocation, this);
                addAll(Snippets.strcpy(RCX, RDX));         // Address to new string still in RAX

                // Copy right string to result string
                add(new Comment("Copy right string (" + rightLocation + ") to result string (rax)"));
                storageFactory.rcx.moveRegToThis(RAX, this);
                storageFactory.rdx.moveLocToThis(rightLocation, this);
                addAll(Snippets.strcat(RCX, RDX));         // Address to new string still in RAX

                // Save result value (address to new string) in tmpLocation
                add(new Comment("Move result string (rax) to tmp location (" + tmpLocation + ")"));
                tmpLocation.moveRegToThis(RAX, this);

                // Free any dynamic memory that we don't need any more
                if (allocatesDynamicMemory(left)) {
                    add(new Comment("Free dynamic memory in " + leftLocation));
                    storageFactory.rcx.moveLocToThis(leftLocation, this);
                    addAll(Snippets.free(RCX));
                }
                if (allocatesDynamicMemory(right)) {
                    add(new Comment("Free dynamic memory in " + rightLocation));
                    storageFactory.rcx.moveLocToThis(rightLocation, this);
                    addAll(Snippets.free(RCX));
                }

                // Move result to leftLocation where it is expected to be
                add(new Comment("Move result string to expected storage location (" + leftLocation + ")"));
                leftLocation.moveLocToThis(tmpLocation, this);
                add(new Comment("<-- " + expression + " ---"));
                add(Blank.INSTANCE);

                addAllFunctionDependencies(MapUtils.of(LIB_LIBC, SetUtils.of(FUN_FREE, FUN_MALLOC, FUN_STRCAT, FUN_STRCPY, FUN_STRLEN)));
            }
        } else {
            // Not a string addition, call super
            super.addExpression(expression, leftLocation);
        }
    }

    /**
     * Generates code to register the dynamic memory referenced by {@code identifier}
     * in the memory allocation list.
     */
    protected void registerDynamicMemory(Identifier identifier) {
        add(Blank.INSTANCE);
        add(new Comment("Register dynamic memory assigned to " + identifier.getName()));

        add(new MoveImmToReg(identifier.getMappedName(), RCX));
        add(new MoveImmToReg(getMatchingTypeIdent(identifier).getMappedName(), RDX));
        add(new SubImmFromReg(SHADOW_SPACE, RSP));
        add(new CallDirect(new Label(FUN_MEMORY_REGISTER.getMappedName())));
        add(new AddImmToReg(SHADOW_SPACE, RSP));

        addUsedBuiltInFunction(FUN_MEMORY_REGISTER);
        addAllFunctionDependencies(FUN_MEMORY_REGISTER.getDependencies());
        addAllConstantDependencies(FUN_MEMORY_REGISTER.getConstants());
    }

    /**
     * Generates code to register the dynamic memory referenced by the array element
     * identified by {@code expression} in the memory allocation list.
     */
    protected void registerDynamicMemory(ArrayAccessExpression expression) {
        add(Blank.INSTANCE);
        add(new Comment("Register dynamic memory assigned to " + expression));

        withArrayAccessExpression(expression, (base, offset) -> add(new Lea(base, 8, offset, RCX)));
        withArrayAccessExpression(expression, (base, offset) -> add(new Lea(getMatchingTypeIdent(base).getMappedName(), 8, offset, RDX)));
        add(new SubImmFromReg(SHADOW_SPACE, RSP));
        add(new CallDirect(new Label(FUN_MEMORY_REGISTER.getMappedName())));
        add(new AddImmToReg(SHADOW_SPACE, RSP));

        addUsedBuiltInFunction(FUN_MEMORY_REGISTER);
        addAllFunctionDependencies(FUN_MEMORY_REGISTER.getDependencies());
        addAllConstantDependencies(FUN_MEMORY_REGISTER.getConstants());
    }

    /**
     * Generates code that copies the type pointer from the {@code rhsExpression}
     * to the {@code identifier}.
     */
    private void copyDynamicMemory(Identifier identifier, Expression rhsExpression) {
        add(Blank.INSTANCE);
        add(new Comment("Make " + identifier.getName() + " refer to the same memory as " + rhsExpression));

        try (StorageLocation location = storageFactory.allocateNonVolatile()) {
            if (rhsExpression instanceof IdentifierDerefExpression) {
                IdentifierDerefExpression ide = (IdentifierDerefExpression) rhsExpression;
                location.moveMemToThis(getMatchingTypeIdent(ide.getIdentifier()).getMappedName(), this);
            } else if (rhsExpression instanceof ArrayAccessExpression) {
                ArrayAccessExpression aae = (ArrayAccessExpression) rhsExpression;
                withArrayAccessExpression(aae, (base, offset) ->
                        location.moveMemToThis(getMatchingTypeIdent(base).getMappedName(), 8, offset, this));
            } else {
                throw new IllegalArgumentException("unsupported expression " + rhsExpression.getClass().getSimpleName());
            }
            location.moveThisToMem(getMatchingTypeIdent(identifier).getMappedName(), this);
        }
    }

    /**
     * Generates code that copies the type pointer from the {@code rhsExpression}
     * to the {@code lhsExpression}.
     */
    private void copyDynamicMemory(ArrayAccessExpression lhsExpression, Expression rhsExpression) {
        add(Blank.INSTANCE);
        add(new Comment("Make " + lhsExpression + " refer to the same memory as " + rhsExpression));

        try (StorageLocation location = storageFactory.allocateNonVolatile()) {
            if (rhsExpression instanceof IdentifierDerefExpression) {
                IdentifierDerefExpression ide = (IdentifierDerefExpression) rhsExpression;
                location.moveMemToThis(getMatchingTypeIdent(ide.getIdentifier()).getMappedName(), this);
            } else if (rhsExpression instanceof ArrayAccessExpression) {
                ArrayAccessExpression aae = (ArrayAccessExpression) rhsExpression;
                withArrayAccessExpression(aae, (base, offset) ->
                        location.moveMemToThis(getMatchingTypeIdent(base).getMappedName(), 8, offset, this));
            } else {
                throw new IllegalArgumentException("unsupported expression " + rhsExpression.getClass().getSimpleName());
            }
            withArrayAccessExpression(lhsExpression, (base, offset) ->
                    location.moveThisToMem(getMatchingTypeIdent(base).getMappedName(), 8, offset, this));
        }
    }

    /**
     * Generates code that stops {@code identifier} from referencing any dynamic memory,
     * by setting the type pointer to 0.
     */
    private void stopDynamicMemory(Identifier identifier) {
        add(Blank.INSTANCE);
        add(new Comment("Make sure " + identifier.getName() + " does not refer to dynamic memory"));
        add(new MoveImmToReg(NOT_MANAGED, RCX));
        add(new MoveRegToMem(RCX, getMatchingTypeIdent(identifier).getMappedName()));
    }

    /**
     * Generates code that stops {@code expression} from referencing any dynamic memory,
     * by setting the type pointer to 0.
     */
    private void stopDynamicMemory(ArrayAccessExpression expression) {
        add(Blank.INSTANCE);
        add(new Comment("Make sure " + expression + " does not refer to dynamic memory"));
        try (StorageLocation location = storageFactory.allocateNonVolatile()) {
            location.moveImmToThis(NOT_MANAGED, this);
            withArrayAccessExpression(expression, (base, offset) ->
                    location.moveThisToMem(getMatchingTypeIdent(base).getMappedName(), 8, offset, this));
        }
    }

    /**
     * Returns an identifier for the "variable type pointer" that matches the given identifier.
     */
    protected Identifier getMatchingTypeIdent(Identifier identifier) {
        return getMatchingTypeIdent(identifier.getMappedName());
    }

    /**
     * Returns an identifier for the "variable type pointer" that matches the given variable name.
     */
    protected Identifier getMatchingTypeIdent(String variableName) {
        return new Identifier(variableName + "_type", I64.INSTANCE);
    }

    /**
     * Returns true if the given identifier stores any type of dynamic memory.
     */
    private boolean storesDynamicMemory(Identifier identifier, boolean constant) {
        if (constant) {
            return false;
        } else {
            // strings and arrays of strings are dynamic
            Type type = identifier.getType();
            if (type instanceof Arr) {
                type = ((Arr) type).getElementType();
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
        return (typeManager.getType(expression) instanceof Str) &&
                (expression instanceof IdentifierDerefExpression || expression instanceof ArrayAccessExpression);
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
