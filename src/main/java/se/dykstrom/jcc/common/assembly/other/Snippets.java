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

package se.dykstrom.jcc.common.assembly.other;

import se.dykstrom.jcc.common.assembly.base.Code;
import se.dykstrom.jcc.common.assembly.base.Comment;
import se.dykstrom.jcc.common.assembly.base.FixedLabel;
import se.dykstrom.jcc.common.assembly.base.Register;
import se.dykstrom.jcc.common.assembly.instruction.*;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static se.dykstrom.jcc.common.assembly.base.Register.*;
import static se.dykstrom.jcc.common.functions.BuiltInFunctions.*;

/**
 * Contains snippets of assembly code.
 *
 * @author Johan Dykstrom
 */
public class Snippets {

    private static final String SHADOW_SPACE = "20h";

    public static List<Code> exit(String exitCode) {
        return asList(
                new MoveImmToReg(exitCode, RCX),
                new SubImmFromReg(SHADOW_SPACE, RSP),
                new CallIndirect(new FixedLabel(FUN_EXIT.getMappedName())),
                new AddImmToReg(SHADOW_SPACE, RSP)
        );
    }

    public static List<Code> malloc(String size) {
        return asList(
                new MoveImmToReg(size, RCX),
                new SubImmFromReg(SHADOW_SPACE, RSP),
                new CallIndirect(new FixedLabel(FUN_MALLOC.getMappedName())),
                new AddImmToReg(SHADOW_SPACE, RSP)
        );
    }

    public static List<Code> malloc(Register register) {
        List<Code> codeLines = new ArrayList<>();
        if (register != RCX) {
            codeLines.add(new MoveRegToReg(register, RCX));
        } else {
            codeLines.add(new Comment("Size already in rcx"));
        }
        codeLines.addAll(asList(
                new SubImmFromReg(SHADOW_SPACE, RSP),
                new CallIndirect(new FixedLabel(FUN_MALLOC.getMappedName())),
                new AddImmToReg(SHADOW_SPACE, RSP)
        ));
        return codeLines;
    }

    public static List<Code> printf(String formatString) {
        return asList(
                new MoveImmToReg(formatString, RCX),
                new SubImmFromReg(SHADOW_SPACE, RSP),
                new CallIndirect(new FixedLabel(FUN_PRINTF.getMappedName())),
                new AddImmToReg(SHADOW_SPACE, RSP)
        );
    }
}
