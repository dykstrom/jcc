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

package se.dykstrom.jcc.common.functions;

import se.dykstrom.jcc.common.assembly.base.CodeContainer;
import se.dykstrom.jcc.common.assembly.base.Label;
import se.dykstrom.jcc.common.assembly.base.Line;
import se.dykstrom.jcc.common.assembly.instruction.*;
import se.dykstrom.jcc.common.assembly.other.Snippets;
import se.dykstrom.jcc.common.types.Constant;
import se.dykstrom.jcc.common.types.I64;
import se.dykstrom.jcc.common.types.Identifier;
import se.dykstrom.jcc.common.types.Str;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Arrays.asList;
import static se.dykstrom.jcc.common.assembly.base.Register.*;
import static se.dykstrom.jcc.common.functions.BuiltInFunctions.*;
import static se.dykstrom.jcc.common.functions.FunctionUtils.LIB_INTERNAL;
import static se.dykstrom.jcc.common.functions.FunctionUtils.LIB_LIBC;
import static se.dykstrom.jcc.common.functions.MemoryManagementUtils.*;

/**
 * Implements the "memory_register" memory management function. This function registers the dynamic memory
 * stored in the memory address (identifier) given by RCX in the memory allocation list, and adds a pointer
 * to the new list node in the memory address given by RDX.
 * <p>
 * Signature: memory_register(ident_address : I64, type_address : I64) : I64
 *
 * @author Johan Dykstrom
 */
public class MemoryRegisterFunction extends AssemblyFunction {

    public static final String NAME = "memory_register";

    private static final String VAR_IDENT_OFFSET = "10h";
    private static final String VAR_TYPE_OFFSET = "18h";

    private static final Constant MSG_REGISTER = new Constant(new Identifier("_gc_register_msg", Str.INSTANCE), "\"GC: Registering new memory: %x\",10,0");
    private static final Constant MSG_COUNT = new Constant(new Identifier("_gc_count_msg", Str.INSTANCE), "\"GC: Allocation count reached limit: %d - collecting\",10,0");
    private static final Constant MSG_LIMIT = new Constant(new Identifier("_gc_limit_msg", Str.INSTANCE), "\"GC: Collection finished with new limit: %d\",10,0");

    MemoryRegisterFunction() {
        super(NAME,
                asList(I64.INSTANCE, I64.INSTANCE),
                I64.INSTANCE,
                Map.of(LIB_LIBC, Set.of(FUN_MALLOC, FUN_PRINTF), LIB_INTERNAL, Set.of(FUN_MEMORY_MARK, FUN_MEMORY_SWEEP)),
                Set.of(ALLOCATION_LIST, ALLOCATION_COUNT, ALLOCATION_LIMIT, MSG_COUNT, MSG_LIMIT, MSG_REGISTER));
    }

    @Override
    public List<Line> lines() {
        CodeContainer codeContainer = new CodeContainer();

        // Create jump labels
        Label doneLabel = new Label("_mem_reg_done");

        // Save arguments in home locations
        codeContainer.addAll(Snippets.enter(2));

        // Debug output
        debug(() -> {
            codeContainer.add(new MoveMemToReg(RCX, RDX));
            codeContainer.addAll(Snippets.printf(MSG_REGISTER.getIdentifier().getMappedName(), RDX));
        });

        // Allocate memory for new node
        codeContainer.addAll(Snippets.malloc(NODE_SIZE));                // RAX now contains address to new node

        // Set node->data to point to variable->data
        {
            codeContainer.add(new MoveMemToReg(RBP, VAR_IDENT_OFFSET, RCX));
            codeContainer.add(new MoveMemToReg(RCX, R10));
            codeContainer.add(new MoveRegToMem(R10, RAX, NODE_DATA_OFFSET));
        }

        // Set node->type to UNMARKED
        {
            codeContainer.add(new MoveImmToReg(UNMARKED, RCX));
            codeContainer.add(new MoveRegToMem(RCX, RAX, NODE_TYPE_OFFSET));
        }

        // Set variable->type to &node
        {
            codeContainer.add(new MoveMemToReg(RBP, VAR_TYPE_OFFSET, RDX)); // Address to variable->type goes in RDX
            codeContainer.add(new MoveRegToMem(RAX, RDX));                  // Store address to node in variable->type
        }

        // Add new node first in memory allocation list
        {
            // node->next = root
            codeContainer.add(new MoveMemToReg(ALLOCATION_LIST.getIdentifier().getMappedName(), R10));
            codeContainer.add(new MoveRegToMem(R10, RAX));
            // root = node
            codeContainer.add(new MoveRegToMem(RAX, ALLOCATION_LIST.getIdentifier().getMappedName()));
        }

        // Increase allocation count, and check if it is time to GC
        {
            codeContainer.add(new IncMem(ALLOCATION_COUNT.getIdentifier().getMappedName()));
            codeContainer.add(new MoveMemToReg(ALLOCATION_COUNT.getIdentifier().getMappedName(), R10));
            codeContainer.add(new CmpRegWithMem(R10, ALLOCATION_LIMIT.getIdentifier().getMappedName()));
            codeContainer.add(new Jl(doneLabel));

            // Debug output
            debug(() -> codeContainer.addAll(Snippets.printf(MSG_COUNT.getIdentifier().getMappedName(), R10)));

            // Allocation count has reached the limit, call GC
            codeContainer.add(new MoveImmToReg(TYPE_POINTERS_START.getIdentifier().getMappedName(), RCX));
            codeContainer.add(new MoveImmToReg(TYPE_POINTERS_STOP.getIdentifier().getMappedName(), RDX));
            codeContainer.add(new SubImmFromReg(SHADOW_SPACE, RSP));
            codeContainer.add(new CallDirect(new Label(FUN_MEMORY_MARK.getMappedName())));
            codeContainer.add(new AddImmToReg(SHADOW_SPACE, RSP));

            codeContainer.add(new SubImmFromReg(SHADOW_SPACE, RSP));
            codeContainer.add(new CallDirect(new Label(FUN_MEMORY_SWEEP.getMappedName())));
            codeContainer.add(new AddImmToReg(SHADOW_SPACE, RSP));

            // Set new allocation limit to 2 * allocation count
            codeContainer.add(new MoveMemToReg(ALLOCATION_COUNT.getIdentifier().getMappedName(), R10));
            codeContainer.add(new IMulImmWithReg("2", R10));
            codeContainer.add(new MoveRegToMem(R10, ALLOCATION_LIMIT.getIdentifier().getMappedName()));

            // Debug output
            debug(() -> codeContainer.addAll(Snippets.printf(MSG_LIMIT.getIdentifier().getMappedName(), R10)));
        }

        // DONE
        codeContainer.add(doneLabel);

        codeContainer.add(new PopReg(RBP));
        codeContainer.add(new Ret());

        return codeContainer.lines();
    }
}
