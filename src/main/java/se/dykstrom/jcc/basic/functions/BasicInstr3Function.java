/*
 * Copyright (C) 2018 Johan Dykstrom
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

package se.dykstrom.jcc.basic.functions;

import se.dykstrom.jcc.common.assembly.base.*;
import se.dykstrom.jcc.common.assembly.instruction.*;
import se.dykstrom.jcc.common.functions.AssemblyFunction;
import se.dykstrom.jcc.common.types.I64;
import se.dykstrom.jcc.common.types.Str;
import se.dykstrom.jcc.common.utils.MapUtils;
import se.dykstrom.jcc.common.utils.SetUtils;

import java.util.List;

import static java.util.Arrays.asList;
import static se.dykstrom.jcc.common.assembly.base.Register.*;
import static se.dykstrom.jcc.common.functions.FunctionUtils.LIB_LIBC;
import static se.dykstrom.jcc.common.functions.BuiltInFunctions.FUN_STRLEN;
import static se.dykstrom.jcc.common.functions.BuiltInFunctions.FUN_STRSTR;

/**
 * Implements the three-argument "instr" function. This function returns the position
 * of the first occurrence of the search string in the base string. The search starts
 * at the given start index. The start index, and the returned position are 1-based. 
 * If the search string is not found, the function returns 0.
 * 
 * Signature: instr(startIndex : I64, baseString : Str, searchString : Str) : I64
 * 
 * @author Johan Dykstrom
 */
public class BasicInstr3Function extends AssemblyFunction {

    public static final String NAME = "instr";

    private static final String START_INDEX_OFFSET = "10h";
    private static final String BASE_STRING_OFFSET = "18h";
    private static final String SEARCH_STRING_OFFSET = "20h";

    public BasicInstr3Function() {
        super(NAME, asList(I64.INSTANCE, Str.INSTANCE, Str.INSTANCE), I64.INSTANCE, MapUtils.of(LIB_LIBC, SetUtils.of(FUN_STRLEN, FUN_STRSTR)));
    }

    @Override
    public List<Line> lines() {
        CodeContainer codeContainer = new CodeContainer();

        Label indexOverflowLabel = new Label("_instr3_index_overflow");
        Label indexUnderflowLabel = new Label("_instr3_index_underflow");
        Label indexOkLabel = new Label("_instr3_index_ok");
        Label indexValidLabel = new Label("_instr3_index_valid");
        Label doneLabel = new Label("_instr3_done");
        
        codeContainer.add(new PushReg(RBP));
        codeContainer.add(new MoveRegToReg(RSP, RBP));
        
        // Make start index 0-based
        codeContainer.add(new DecReg(RCX));
        
        // If start index is < 0, return 0
        codeContainer.add(new CmpRegWithImm(RCX, "0"));
        codeContainer.add(new Jl(indexUnderflowLabel));
        codeContainer.add(new Jmp(indexValidLabel));
        codeContainer.add(indexUnderflowLabel);
        codeContainer.add(new MoveImmToReg("0", RAX));
        codeContainer.add(new Jmp(doneLabel));
        codeContainer.add(indexValidLabel);
        
        // Save argument registers in their home locations
        codeContainer.add(new MoveRegToMem(RCX, RBP, START_INDEX_OFFSET));
        codeContainer.add(new MoveRegToMem(RDX, RBP, BASE_STRING_OFFSET));
        codeContainer.add(new MoveRegToMem(R8, RBP, SEARCH_STRING_OFFSET));
        codeContainer.add(new MoveRegToReg(RDX, RCX));
        // RAX = strlen(base string)
        codeContainer.add(new SubImmFromReg("20h", RSP));
        codeContainer.add(new CallIndirect(new FixedLabel(FUN_STRLEN.getMappedName())));
        codeContainer.add(new AddImmToReg("20h", RSP));
        
        // If length < start index, return 0
        codeContainer.add(new CmpRegWithMem(RAX, RBP, START_INDEX_OFFSET));
        codeContainer.add(new Jl(indexOverflowLabel));
        codeContainer.add(new Jmp(indexOkLabel));
        codeContainer.add(indexOverflowLabel);
        codeContainer.add(new MoveImmToReg("0", RAX));
        codeContainer.add(new Jmp(doneLabel));
        codeContainer.add(indexOkLabel);
        
        // Move address of base string to RCX
        codeContainer.add(new MoveMemToReg(RBP, BASE_STRING_OFFSET, RCX));
        // Add start index to RCX
        codeContainer.add(new AddMemToReg(RBP, START_INDEX_OFFSET, RCX));
        // Move address of search string to RDX
        codeContainer.add(new MoveMemToReg(RBP, SEARCH_STRING_OFFSET, RDX));
        // RAX = strstr(base string, search string)
        codeContainer.add(new SubImmFromReg("20h", RSP));
        codeContainer.add(new CallIndirect(new FixedLabel(FUN_STRSTR.getMappedName())));
        codeContainer.add(new AddImmToReg("20h", RSP));
        
        // If not found, we are done
        codeContainer.add(new CmpRegWithImm(RAX, "0"));
        codeContainer.add(new Je(doneLabel));
        
        // Calculate 1-based result index
        codeContainer.add(new SubMemFromReg(RBP, BASE_STRING_OFFSET, RAX));
        codeContainer.add(new IncReg(RAX));

        codeContainer.add(Blank.INSTANCE);
        codeContainer.add(doneLabel);
        codeContainer.add(new PopReg(RBP));
        codeContainer.add(new Ret());
        
        return codeContainer.lines();
    }
}
