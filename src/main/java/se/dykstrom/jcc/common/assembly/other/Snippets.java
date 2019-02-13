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

    public static List<Code> malloc(Register size) {
        return asList(
                (size != RCX) ? new MoveRegToReg(size, RCX) : new Comment("Note: malloc size already in rcx"),
                new SubImmFromReg(SHADOW_SPACE, RSP),
                new CallIndirect(new FixedLabel(FUN_MALLOC.getMappedName())),
                new AddImmToReg(SHADOW_SPACE, RSP)
        );
    }

    public static List<Code> free(Register address) {
        return asList(
                (address != RCX) ? new MoveRegToReg(address, RCX) : new Comment("Note: free address already in rcx"),
                new SubImmFromReg(SHADOW_SPACE, RSP),
                new CallIndirect(new FixedLabel(FUN_FREE.getMappedName())),
                new AddImmToReg(SHADOW_SPACE, RSP)
        );
    }

    public static List<Code> printf(String formatString) {
        return asList(
                new MoveImmToReg(formatString, RCX),
                new SubImmFromReg(SHADOW_SPACE, RSP),
                new CallIndirect(new FixedLabel(FUN_PRINTF.getMappedName())),
                new AddImmToReg(SHADOW_SPACE, RSP)
        );
    }

    public static List<Code> printf(String formatString, Register arg0) {
        return asList(
                (arg0 != RDX) ? new MoveRegToReg(arg0, RDX) : new Comment("Note: printf arg0 already in rdx"),
                new MoveImmToReg(formatString, RCX),
                new SubImmFromReg(SHADOW_SPACE, RSP),
                new CallIndirect(new FixedLabel(FUN_PRINTF.getMappedName())),
                new AddImmToReg(SHADOW_SPACE, RSP)
        );
    }

    public static List<Code> strlen(Register address) {
        return asList(
                (address != RCX) ? new MoveRegToReg(address, RCX) : new Comment("Note: strlen address already in rcx"),
                new SubImmFromReg(SHADOW_SPACE, RSP),
                new CallIndirect(new FixedLabel(FUN_STRLEN.getMappedName())),
                new AddImmToReg(SHADOW_SPACE, RSP)
        );
    }

    public static List<Code> strcpy(Register destination, Register source) {
        return asList(
                (destination != RCX) ? new MoveRegToReg(destination, RCX) : new Comment("Note: strcpy destination already in rcx"),
                (source != RDX) ? new MoveRegToReg(source, RDX) : new Comment("Note: strcpy source already in rdx"),
                new SubImmFromReg(SHADOW_SPACE, RSP),
                new CallIndirect(new FixedLabel(FUN_STRCPY.getMappedName())),
                new AddImmToReg(SHADOW_SPACE, RSP)
        );
    }

    public static List<Code> strcat(Register destination, Register source) {
        return asList(
                (destination != RCX) ? new MoveRegToReg(destination, RCX) : new Comment("Note: strcat destination already in rcx"),
                (source != RDX) ? new MoveRegToReg(source, RDX) : new Comment("Note: strcat source already in rdx"),
                new SubImmFromReg(SHADOW_SPACE, RSP),
                new CallIndirect(new FixedLabel(FUN_STRCAT.getMappedName())),
                new AddImmToReg(SHADOW_SPACE, RSP)
        );
    }
}
