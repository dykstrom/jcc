package se.dykstrom.jcc.basic.functions;

import static java.util.Arrays.asList;
import static se.dykstrom.jcc.common.assembly.base.Register.RAX;
import static se.dykstrom.jcc.common.assembly.base.Register.RBP;
import static se.dykstrom.jcc.common.assembly.base.Register.RCX;
import static se.dykstrom.jcc.common.assembly.base.Register.RSP;
import static se.dykstrom.jcc.common.compiler.CompilerUtils.LIB_LIBC;

import java.util.List;

import se.dykstrom.jcc.common.assembly.base.Code;
import se.dykstrom.jcc.common.assembly.base.CodeContainer;
import se.dykstrom.jcc.common.assembly.base.FixedLabel;
import se.dykstrom.jcc.common.assembly.base.Label;
import se.dykstrom.jcc.common.assembly.instruction.*;
import se.dykstrom.jcc.common.functions.AssemblyFunction;
import se.dykstrom.jcc.common.types.I64;
import se.dykstrom.jcc.common.types.Str;
import se.dykstrom.jcc.common.utils.MapUtils;
import se.dykstrom.jcc.common.utils.SetUtils;

/**
 * Implements the two-argument "instr" function. This function returns the position
 * of the first occurrence of the search string in the base string. The returned 
 * position is 1-based. If the search string is not found, the function returns 0.
 * 
 * Signature: instr(baseString : Str, searchString : Str) : I64
 * 
 * @author Johan Dykstrom
 */
public class BasicInstr2Function extends AssemblyFunction {

    public static final String NAME = "instr";

    private static final String BASE_STRING_OFFSET = "10h";

    public BasicInstr2Function() {
        super(NAME, asList(Str.INSTANCE, Str.INSTANCE), I64.INSTANCE, MapUtils.of(LIB_LIBC, SetUtils.of("strstr")));
    }

    @Override
    public List<Code> codes() {
        CodeContainer codeContainer = new CodeContainer();

        Label doneLabel = new Label("_instr2_done");
        
        codeContainer.add(new PushReg(RBP));
        codeContainer.add(new MoveRegToReg(RSP, RBP));
        // Save base string (RCX) in home location
        codeContainer.add(new MoveRegToMem(RCX, RBP, BASE_STRING_OFFSET));
        
        // Allocate shadow space, call strstr, and free shadow space
        codeContainer.add(new SubImmFromReg("20h", RSP));
        codeContainer.add(new CallIndirect(new FixedLabel("strstr")));
        codeContainer.add(new AddImmToReg("20h", RSP));
        
        // If not found, we are done
        codeContainer.add(new CmpRegWithImm(RAX, "0"));
        codeContainer.add(new Je(doneLabel));
        
        // Calculate 1-based result index
        codeContainer.add(new SubMemFromReg(RBP, BASE_STRING_OFFSET, RAX));
        codeContainer.add(new IncReg(RAX));
        codeContainer.add(doneLabel);
        codeContainer.add(new PopReg(RBP));
        codeContainer.add(new Ret());
        
        return codeContainer.codes();
    }
}
