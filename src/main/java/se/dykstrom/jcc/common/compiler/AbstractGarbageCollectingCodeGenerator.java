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
import se.dykstrom.jcc.common.assembly.instruction.AddImmToReg;
import se.dykstrom.jcc.common.assembly.instruction.CallDirect;
import se.dykstrom.jcc.common.assembly.instruction.MoveImmToReg;
import se.dykstrom.jcc.common.assembly.instruction.SubImmFromReg;
import se.dykstrom.jcc.common.assembly.other.DataDefinition;
import se.dykstrom.jcc.common.assembly.section.Section;
import se.dykstrom.jcc.common.ast.AddExpression;
import se.dykstrom.jcc.common.ast.AssignStatement;
import se.dykstrom.jcc.common.ast.Expression;
import se.dykstrom.jcc.common.ast.StringLiteral;
import se.dykstrom.jcc.common.storage.StorageLocation;
import se.dykstrom.jcc.common.symbols.SymbolTable;
import se.dykstrom.jcc.common.types.I64;
import se.dykstrom.jcc.common.types.Identifier;
import se.dykstrom.jcc.common.types.Str;

import java.util.List;

import static se.dykstrom.jcc.common.assembly.base.Register.*;
import static se.dykstrom.jcc.common.functions.BuiltInFunctions.FUN_MEMORY_REGISTER;

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

            section.add(new DataDefinition(TYPE_POINTERS_START, "0", true));
            identifiers
                    .stream()
                    .filter(identifier -> storesDynamicMemory(identifier, symbols.isConstant(identifier.getName())))
                    .forEach(identifier -> section.add(new DataDefinition(getMatchingTypeIdent(identifier), "0", true)));
            section.add(new DataDefinition(TYPE_POINTERS_STOP, "0", true));

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
        }
    }

    /**
     *
     */
    @Override
    protected void addExpression(AddExpression expression, StorageLocation leftLocation) {

        // TODO: Check if left and right are strings, and if so, perform string addition here.


        if (allocatesDynamicMemory(expression.getLeft())) {
            // TODO: Free memory that we don't need anymore.
        }
        if (allocatesDynamicMemory(expression.getRight())) {
            // TODO: Free memory that we don't need anymore.
        }

        // TODO: No string addition, call super.

        super.addExpression(expression, leftLocation);
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
     * Returns true if the given expression will allocate dynamic memory that needs to be managed.
     */
    private boolean allocatesDynamicMemory(Expression expression) {
        // String expressions that are not string literals are dynamic
        return (typeManager.getType(expression) instanceof Str) && !(expression instanceof StringLiteral);
    }
}
