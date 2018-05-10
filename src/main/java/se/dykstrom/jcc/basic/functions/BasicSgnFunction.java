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

import se.dykstrom.jcc.common.assembly.base.Code;
import se.dykstrom.jcc.common.assembly.base.CodeContainer;
import se.dykstrom.jcc.common.assembly.base.Label;
import se.dykstrom.jcc.common.assembly.instruction.*;
import se.dykstrom.jcc.common.functions.AssemblyFunction;
import se.dykstrom.jcc.common.types.I64;

import java.util.List;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static se.dykstrom.jcc.common.assembly.base.Register.RAX;
import static se.dykstrom.jcc.common.assembly.base.Register.RCX;

/**
 * Implements the "sgn" function. This function returns -1, 0, or 1 depending on 
 * if the argument is less than, equal to, or greater than 0.
 * 
 * Signature: sgn(number : I64) : I64
 * 
 * @author Johan Dykstrom
 */
public class BasicSgnFunction extends AssemblyFunction {

    public static final String NAME = "sgn";

    public BasicSgnFunction() {
        super(NAME, singletonList(I64.INSTANCE), I64.INSTANCE, emptyMap());
    }

    @Override
    public List<Code> codes() {
        CodeContainer codeContainer = new CodeContainer();

        // Create jump labels
        Label lessThanLabel = new Label("_sgn_less_than");
        Label greaterThanLabel = new Label("_sgn_greater_than");
        Label doneLabel = new Label("_sgn_done");

        // RCX contains the argument to compare with 0
        codeContainer.add(new CmpRegWithImm(RCX, "0"));
        codeContainer.add(new Jl(lessThanLabel));
        codeContainer.add(new Jg(greaterThanLabel));
        // Equal to 0
        codeContainer.add(new MoveImmToReg("0", RAX));
        codeContainer.add(new Jmp(doneLabel));
        // Less than 0
        codeContainer.add(lessThanLabel);
        codeContainer.add(new MoveImmToReg("-1", RAX));
        codeContainer.add(new Jmp(doneLabel));
        // Greater than 0
        codeContainer.add(greaterThanLabel);
        codeContainer.add(new MoveImmToReg("1", RAX));
        // Done
        codeContainer.add(doneLabel);
        codeContainer.add(new Ret());
        
        return codeContainer.codes();
    }
}
