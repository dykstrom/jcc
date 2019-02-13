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

import se.dykstrom.jcc.common.assembly.base.Blank;
import se.dykstrom.jcc.common.assembly.base.Comment;
import se.dykstrom.jcc.common.assembly.base.Label;
import se.dykstrom.jcc.common.assembly.instruction.*;
import se.dykstrom.jcc.common.assembly.other.DataDefinition;
import se.dykstrom.jcc.common.assembly.other.Snippets;
import se.dykstrom.jcc.common.assembly.section.Section;
import se.dykstrom.jcc.common.ast.*;
import se.dykstrom.jcc.common.storage.StorageLocation;
import se.dykstrom.jcc.common.symbols.SymbolTable;
import se.dykstrom.jcc.common.types.I64;
import se.dykstrom.jcc.common.types.Identifier;
import se.dykstrom.jcc.common.types.Str;
import se.dykstrom.jcc.common.types.Type;
import se.dykstrom.jcc.common.utils.MapUtils;
import se.dykstrom.jcc.common.utils.SetUtils;

import java.util.List;

import static se.dykstrom.jcc.common.assembly.base.Register.*;
import static se.dykstrom.jcc.common.functions.BuiltInFunctions.*;
import static se.dykstrom.jcc.common.functions.FunctionUtils.LIB_LIBC;
import static se.dykstrom.jcc.common.functions.MemoryManagementFunction.NOT_MANAGED;

/**
 * Abstract base class for code generators that generate code that includes
 * automatic memory management and garbage collection.
 *
 * @author Johan Dykstrom
 */
public abstract class AbstractGarbageCollectingCodeGenerator extends AbstractCodeGenerator {

    private static final Identifier TYPE_POINTERS_START = new Identifier("_gc_type_pointers_start", I64.INSTANCE);
    private static final Identifier TYPE_POINTERS_STOP = new Identifier("_gc_type_pointers_stop", I64.INSTANCE);

    protected AbstractGarbageCollectingCodeGenerator(TypeManager typeManager) {
        super(typeManager);
    }

    /**
     * Adds data definitions for all identifiers to the given code section. This method also adds a second
     * sequence of data definitions for all identifiers that can identify any type of dynamic memory. These
     * type flags contain pointers to corresponding nodes in the memory allocation list. Initially, all
     * type flags are 0.
     * <p>
     * The only type of dynamic memory that can be managed at the moment, is string memory.
     */
    @Override
    protected void addDataDefinitions(List<Identifier> identifiers, SymbolTable symbols, Section section) {
        // First, add definitions for all real identifiers
        identifiers.forEach(identifier -> section.add(
                new DataDefinition(identifier, (String) symbols.getValue(identifier.getName()), symbols.isConstant(identifier.getName())))
        );

        // Second, add definitions for "type identifiers" that specify type of dynamic memory for the corresponding identifier
        if (identifiers.stream().anyMatch(identifier -> storesDynamicMemory(identifier, symbols.isConstant(identifier.getName())))) {
            section.add(new Comment("--- Dynamic memory type pointers ---"));

            section.add(new DataDefinition(TYPE_POINTERS_START, NOT_MANAGED, true));
            identifiers
                    .stream()
                    .filter(identifier -> storesDynamicMemory(identifier, symbols.isConstant(identifier.getName())))
                    .forEach(identifier -> section.add(new DataDefinition(getMatchingTypeIdent(identifier), NOT_MANAGED, true)));
            section.add(new DataDefinition(TYPE_POINTERS_STOP, NOT_MANAGED, true));

            section.add(new Comment("--- Dynamic memory type pointers ---"));
        }
    }

    /**
     * Extends the generic code generation for assign statements with functionality for managing dynamic memory.
     */
    @Override
    protected void assignStatement(AssignStatement statement) {
        super.assignStatement(statement);

        if (allocatesDynamicMemory(statement.getExpression())) {
            registerDynamicMemory(statement.getIdentifier());
        } else if (throwsDynamicMemory(statement.getExpression())) {
            stopDynamicMemory(statement.getIdentifier());
        } else if (reassignsDynamicMemory(statement.getExpression())) {
            copyDynamicMemory(statement.getIdentifier(), ((IdentifierDerefExpression) statement.getExpression()).getIdentifier());
        }
    }

    /**
     * Extends the generic code generation for additions with functionality for adding strings.
     */
    @Override
    protected void addExpression(AddExpression expression, StorageLocation leftLocation) {
        Expression left = expression.getLeft();
        Expression right = expression.getRight();

        Type leftType = typeManager.getType(left);
        Type rightType = typeManager.getType(right);

        // If this is a string addition (concatenation)
        if (leftType instanceof Str && rightType instanceof Str) {
            // Generate code for left sub expression, and store result in leftLocation
            expression(expression.getLeft(), leftLocation);

            try (StorageLocation rightLocation = storageFactory.allocateNonVolatile(rightType);
                 StorageLocation tmpLocation = storageFactory.allocateNonVolatile(I64.INSTANCE)) {
                // Generate code for right sub expression, and store result in rightLocation
                expression(expression.getRight(), rightLocation);

                addFormattedComment(expression);

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
    private void registerDynamicMemory(Identifier identifier) {
        add(Blank.INSTANCE);
        add(new Comment("Register dynamic memory assigned to " + identifier.getName()));

        add(new MoveImmToReg(identifier.getMappedName(), RCX));
        add(new MoveImmToReg(getMatchingTypeIdent(identifier).getMappedName(), RDX));
        add(new MoveImmToReg(TYPE_POINTERS_START.getMappedName(), R8));
        add(new MoveImmToReg(TYPE_POINTERS_STOP.getMappedName(), R9));
        add(new SubImmFromReg(SHADOW_SPACE, RSP));
        add(new CallDirect(new Label(FUN_MEMORY_REGISTER.getMappedName())));
        add(new AddImmToReg(SHADOW_SPACE, RSP));

        addUsedBuiltInFunction(FUN_MEMORY_REGISTER);
        addAllFunctionDependencies(FUN_MEMORY_REGISTER.getDependencies());
        addAllConstantDependencies(FUN_MEMORY_REGISTER.getConstants());
    }

    /**
     * Generates code that copies the type pointer from the {@code expressionIdentifier}
     * to the {@code identifier}.
     */
    private void copyDynamicMemory(Identifier identifier, Identifier expressionIdentifier) {
        add(Blank.INSTANCE);
        add(new Comment("Make " + identifier.getName() + " refer to the same memory as " + expressionIdentifier.getName()));
        add(new MoveMemToReg(getMatchingTypeIdent(expressionIdentifier).getMappedName(), RCX));
        add(new MoveRegToMem(RCX, getMatchingTypeIdent(identifier).getMappedName()));
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
     * Returns the "type identifier" that matches the given identifier.
     */
    private Identifier getMatchingTypeIdent(Identifier identifier) {
        return new Identifier(identifier.getMappedName() + "_type", I64.INSTANCE);
    }

    /**
     * Returns true if the given identifier stores any type of dynamic memory.
     */
    private boolean storesDynamicMemory(Identifier identifier, boolean constant) {
        // Strings that are not constants are dynamic
        return (identifier.getType() instanceof Str) && !constant;
    }

    /**
     * Returns {@code true} if evaluating the given expression will allocate dynamic memory
     * that needs to be managed. Examples:
     *
     * - using the value of a string literal does not allocate memory
     * - de-referencing a string variable does not allocate memory
     * - adding two strings _does_ allocate memory
     * - calling a function that returns a string _does_ allocate memory
     *
     * @param expression The expression to check.
     * @return True if {@code expression} allocates dynamic memory.
     */
    private boolean allocatesDynamicMemory(Expression expression) {
        return (typeManager.getType(expression) instanceof Str) &&
                !(expression instanceof StringLiteral) &&
                !(expression instanceof IdentifierDerefExpression);
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
        return (typeManager.getType(expression) instanceof Str) && (expression instanceof IdentifierDerefExpression);
    }

    /**
     * Returns {@code true} if assigning the value of the given expression might lead to
     * throwing away dynamic memory. Examples:
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
