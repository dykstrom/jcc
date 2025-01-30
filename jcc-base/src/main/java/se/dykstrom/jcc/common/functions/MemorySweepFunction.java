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

import se.dykstrom.jcc.common.code.CodeContainer;
import se.dykstrom.jcc.common.assembly.base.AssemblyComment;
import se.dykstrom.jcc.common.code.Label;
import se.dykstrom.jcc.common.code.Line;
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
import static se.dykstrom.jcc.common.functions.LibcBuiltIns.FUN_FREE;
import static se.dykstrom.jcc.common.functions.FunctionUtils.LIB_LIBC;
import static se.dykstrom.jcc.common.functions.LibcBuiltIns.FUN_PRINTF_STR_VAR;
import static se.dykstrom.jcc.common.functions.MemoryManagementUtils.*;

/**
 * Implements the "memory_sweep" memory management function. This function sweeps (frees)
 * the memory nodes that are not in use.
 * <p>
 * Signature: memory_sweep(start : I64, end : I64) : I64
 *
 * @author Johan Dykstrom
 */
public class MemorySweepFunction extends AssemblyFunction {

    public static final String NAME = "memory_sweep";

    private static final String END_OF_LIST = "0";

    private static final Constant MSG_SWEEPING = new Constant(new Identifier("_gc_sweeping_msg", Str.INSTANCE), "\"GC: Sweeping memory: %x\",10,0");

    MemorySweepFunction() {
        super(NAME,
                asList(I64.INSTANCE, I64.INSTANCE),
                I64.INSTANCE,
                Map.of(LIB_LIBC, Set.of(FUN_FREE, FUN_PRINTF_STR_VAR)),
                Set.of(ALLOCATION_COUNT, ALLOCATION_LIST, MSG_SWEEPING));
    }

    @Override
    public List<Line> lines() {
        CodeContainer codeContainer = new CodeContainer();

        // Create jump labels
        Label loopLabel = new Label("_mem_sweep_loop");
        Label unmarkedRootLabel = new Label("_mem_sweep_unmarked_root");
        Label freeNodeLabel = new Label("_mem_sweep_free_node");
        Label rootAgainLabel = new Label("_mem_sweep_root_again");
        Label markedLabel = new Label("_mem_sweep_marked");
        Label doneLabel = new Label("_mem_sweep_done");

        codeContainer.add(new PushReg(RDI)); // RDI will point to the previous node
        codeContainer.add(new PushReg(RBX)); // RBX will point to the current node
        codeContainer.add(new MoveImmToReg("0", RDI));
        codeContainer.add(new MoveMemToReg(ALLOCATION_LIST.getIdentifier().getMappedName(), RBX));

        // LOOP
        codeContainer.add(loopLabel);

        codeContainer.add(new CmpRegWithImm(RBX, END_OF_LIST));
        codeContainer.add(new Je(doneLabel));

        // Is node marked?
        codeContainer.add(new CmpByteMemWithImm(RBX, NODE_TYPE_OFFSET, MARKED));
        codeContainer.add(new Je(markedLabel));

        // Are we looking at the root of the allocation list?
        codeContainer.add(new CmpRegWithImm(RDI, "0"));
        codeContainer.add(new Je(unmarkedRootLabel));

        // Set prev->next to current->next
        codeContainer.add(new AssemblyComment("previous->next = current->next"));
        codeContainer.add(new MoveMemToReg(RBX, RCX));
        codeContainer.add(new MoveRegToMem(RCX, RDI));
        codeContainer.add(new Jmp(freeNodeLabel));

        // REMOVE ROOT
        codeContainer.add(unmarkedRootLabel);
        codeContainer.add(new AssemblyComment("root->next = current->next"));
        codeContainer.add(new MoveMemToReg(RBX, RCX));
        codeContainer.add(new MoveRegToMem(RCX, ALLOCATION_LIST.getIdentifier().getMappedName()));

        // FREE NODE
        codeContainer.add(freeNodeLabel);

        // Debug output
        debug(() -> {
            codeContainer.add(new MoveMemToReg(RBX, NODE_DATA_OFFSET, RDX));
            codeContainer.addAll(Snippets.printf(MSG_SWEEPING.getIdentifier().getMappedName(), RDX));
        });

        // Free managed memory
        codeContainer.add(new AssemblyComment("Free managed memory"));
        codeContainer.add(new MoveMemToReg(RBX, NODE_DATA_OFFSET, RCX));
        codeContainer.addAll(Snippets.free(RCX));

        // Free swept node
        codeContainer.add(new AssemblyComment("Free swept node"));
        codeContainer.addAll(Snippets.free(RBX));

        // Decrease allocation count
        codeContainer.add(new DecMem(ALLOCATION_COUNT.getIdentifier().getMappedName()));

        // Did we remove the root of the allocation list?
        codeContainer.add(new CmpRegWithImm(RDI, "0"));
        codeContainer.add(new Je(rootAgainLabel));

        // Look at next node
        codeContainer.add(new AssemblyComment("Look at next node"));
        codeContainer.add(new MoveMemToReg(RDI, RBX));
        codeContainer.add(new Jmp(loopLabel));

        // NEXT ROOT
        // Look at root again
        codeContainer.add(rootAgainLabel);
        codeContainer.add(new AssemblyComment("Look at root again"));
        codeContainer.add(new MoveMemToReg(ALLOCATION_LIST.getIdentifier().getMappedName(), RBX));
        codeContainer.add(new Jmp(loopLabel));

        // MARKED
        codeContainer.add(markedLabel);
        codeContainer.add(new AssemblyComment("Unmark node"));
        codeContainer.add(new MoveByteImmToMem(UNMARKED, RBX, NODE_TYPE_OFFSET));      // Unmark node
        codeContainer.add(new AssemblyComment("Look at next node"));
        codeContainer.add(new MoveRegToReg(RBX, RDI));                                 // Go to next node
        codeContainer.add(new MoveMemToReg(RBX, RBX));
        codeContainer.add(new Jmp(loopLabel));

        // DONE
        codeContainer.add(doneLabel);
        codeContainer.add(new PopReg(RBX));
        codeContainer.add(new PopReg(RDI));
        codeContainer.add(new Ret());

        return codeContainer.lines();
    }
}
