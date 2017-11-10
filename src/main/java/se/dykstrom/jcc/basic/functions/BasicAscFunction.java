package se.dykstrom.jcc.basic.functions;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static se.dykstrom.jcc.common.assembly.base.Register.RAX;
import static se.dykstrom.jcc.common.assembly.base.Register.RCX;

import java.util.List;

import se.dykstrom.jcc.common.assembly.base.Code;
import se.dykstrom.jcc.common.assembly.base.CodeContainer;
import se.dykstrom.jcc.common.assembly.instruction.MoveByteMemToReg;
import se.dykstrom.jcc.common.assembly.instruction.Ret;
import se.dykstrom.jcc.common.functions.AssemblyFunction;
import se.dykstrom.jcc.common.types.I64;
import se.dykstrom.jcc.common.types.Str;

/**
 * Implements the "asc" function. This function returns the ASCII value of the first character of
 * the given string. For an empty string, it returns 0.
 * 
 * Signature: asc(string : Str) : I64
 * 
 * @author Johan Dykstrom
 */
public class BasicAscFunction extends AssemblyFunction {

    public static final String NAME = "asc";

    public BasicAscFunction() {
        super(NAME, singletonList(Str.INSTANCE), I64.INSTANCE, emptyMap());
    }

    @Override
    public List<Code> codes() {
        CodeContainer codeContainer = new CodeContainer();

        // RCX contains the address to the (first character of the) string
        codeContainer.add(new MoveByteMemToReg(RCX, RAX));
        codeContainer.add(new Ret());
        
        return codeContainer.codes();
    }
}
