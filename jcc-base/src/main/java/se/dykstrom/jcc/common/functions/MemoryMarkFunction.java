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
import static se.dykstrom.jcc.common.functions.FunctionUtils.LIB_LIBC;
import static se.dykstrom.jcc.common.functions.LibcBuiltIns.CF_PRINTF_STR_VAR;
import static se.dykstrom.jcc.common.functions.MemoryManagementUtils.*;

/**
 * Implements the "memory_mark" memory management function. This function marks memory nodes
 * that are still in use, starting at the end address given by RDX, working backwards to the
 * start address given by RCX.
 * <p>
 * Signature: memory_mark(start : I64, end : I64) : I64
 *
 * @author Johan Dykstrom
 */
public class MemoryMarkFunction extends AssemblyFunction {

    public static final String NAME = "memory_mark";

    private static final Constant MSG_MARKED = new Constant(new Identifier("_gc_marked_msg", Str.INSTANCE), "\"GC: Marking memory: %x\",10,0");

    MemoryMarkFunction() {
        super(NAME, asList(I64.INSTANCE, I64.INSTANCE), I64.INSTANCE, Map.of(LIB_LIBC, Set.of(CF_PRINTF_STR_VAR)), Set.of(MSG_MARKED));
    }

    @Override
    public List<Line> lines() {
        CodeContainer codeContainer = new CodeContainer();

        // Create jump labels
        Label loopLabel = new Label("_mem_mark_loop");
        Label doneLabel = new Label("_mem_mark_done");

        // LOOP
        codeContainer.add(loopLabel);
        // Move backwards to previous memory slot
        codeContainer.add(new SubImmFromReg(PTR_SIZE, RDX));
        // If we have reached the start, we are done
        codeContainer.add(new CmpRegWithReg(RDX, RCX));
        codeContainer.add(new Je(doneLabel));

        // Load variable type pointer into RAX
        codeContainer.add(new MoveMemToReg(RDX, RAX));

        // If this type pointer says memory is not managed, just continue
        codeContainer.add(new CmpRegWithImm(RAX, NOT_MANAGED));
        codeContainer.add(new Je(loopLabel));

        // Mark this memory node
        codeContainer.add(new MoveByteImmToMem(MARKED, RAX, NODE_TYPE_OFFSET));

        // Debug output
        debug(() -> {
            codeContainer.add(new PushReg(RCX));
            codeContainer.add(new PushReg(RDX));
            codeContainer.add(new MoveMemToReg(RAX, NODE_DATA_OFFSET, RDX));
            codeContainer.addAll(Snippets.printf(MSG_MARKED.getIdentifier().getMappedName(), RDX));
            codeContainer.add(new PopReg(RDX));
            codeContainer.add(new PopReg(RCX));
        });

        codeContainer.add(new Jmp(loopLabel));

        // DONE
        codeContainer.add(doneLabel);
        codeContainer.add(new Ret());

        return codeContainer.lines();
    }
}
