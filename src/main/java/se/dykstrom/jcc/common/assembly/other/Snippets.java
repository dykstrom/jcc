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

import se.dykstrom.jcc.common.assembly.base.Line;
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
public final class Snippets {

    private Snippets() { }

    private static final String SHADOW_SPACE = "20h";

    private static final String HOME_LOCATION_RCX = "10h";
    private static final String HOME_LOCATION_RDX = "18h";
    private static final String HOME_LOCATION_R8 = "20h";
    private static final String HOME_LOCATION_R9 = "28h";

    public static List<Line> enter(int numberOfArgs) {
        List<Line> lines = new ArrayList<>();
        lines.add(new Comment("Save " + numberOfArgs + " arguments in home locations"));
        lines.add(new PushReg(RBP));
        lines.add(new MoveRegToReg(RSP, RBP));
        if (numberOfArgs > 0) lines.add(new MoveRegToMem(RCX, RBP, HOME_LOCATION_RCX));
        if (numberOfArgs > 1) lines.add(new MoveRegToMem(RDX, RBP, HOME_LOCATION_RDX));
        if (numberOfArgs > 2) lines.add(new MoveRegToMem(R8, RBP, HOME_LOCATION_R8));
        if (numberOfArgs > 3) lines.add(new MoveRegToMem(R9, RBP, HOME_LOCATION_R9));
        return lines;
    }

    public static List<Line> exit(String exitCode) {
        return asList(
                new MoveImmToReg(exitCode, RCX),
                new SubImmFromReg(SHADOW_SPACE, RSP),
                new CallIndirect(new FixedLabel(FUN_EXIT.getMappedName())),
                new AddImmToReg(SHADOW_SPACE, RSP)
        );
    }

    public static List<Line> malloc(String size) {
        return asList(
                new MoveImmToReg(size, RCX),
                new SubImmFromReg(SHADOW_SPACE, RSP),
                new CallIndirect(new FixedLabel(FUN_MALLOC.getMappedName())),
                new AddImmToReg(SHADOW_SPACE, RSP)
        );
    }

    public static List<Line> malloc(Register size) {
        return asList(
                (size != RCX) ? new MoveRegToReg(size, RCX) : new Comment("malloc size already in rcx"),
                new SubImmFromReg(SHADOW_SPACE, RSP),
                new CallIndirect(new FixedLabel(FUN_MALLOC.getMappedName())),
                new AddImmToReg(SHADOW_SPACE, RSP)
        );
    }

    public static List<Line> realloc(Register buffer, Register size) {
        return asList(
                (buffer != RCX) ? new MoveRegToReg(buffer, RCX) : new Comment("realloc buffer already in rcx"),
                (size != RDX) ? new MoveRegToReg(size, RDX) : new Comment("realloc size already in rdx"),
                new SubImmFromReg(SHADOW_SPACE, RSP),
                new CallIndirect(new FixedLabel(FUN_REALLOC.getMappedName())),
                new AddImmToReg(SHADOW_SPACE, RSP)
        );
    }

    public static List<Line> free(Register address) {
        return asList(
                (address != RCX) ? new MoveRegToReg(address, RCX) : new Comment("free address already in rcx"),
                new SubImmFromReg(SHADOW_SPACE, RSP),
                new CallIndirect(new FixedLabel(FUN_FREE.getMappedName())),
                new AddImmToReg(SHADOW_SPACE, RSP)
        );
    }

    public static List<Line> memset(Register address, Register character, Register size) {
        return asList(
                (address != RCX) ? new MoveRegToReg(address, RCX) : new Comment("memset address already in rcx"),
                (character != RDX) ? new MoveRegToReg(character, RDX) : new Comment("memset character already in rdx"),
                (size != R8) ? new MoveRegToReg(size, R8) : new Comment("memset size already in r8"),
                new SubImmFromReg(SHADOW_SPACE, RSP),
                new CallIndirect(new FixedLabel(FUN_MEMSET.getMappedName())),
                new AddImmToReg(SHADOW_SPACE, RSP)
        );
    }

    public static List<Line> printf(String formatString) {
        return asList(
                new MoveImmToReg(formatString, RCX),
                new SubImmFromReg(SHADOW_SPACE, RSP),
                new CallIndirect(new FixedLabel(FUN_PRINTF.getMappedName())),
                new AddImmToReg(SHADOW_SPACE, RSP)
        );
    }

    public static List<Line> printf(String formatString, Register arg0) {
        return asList(
                (arg0 != RDX) ? new MoveRegToReg(arg0, RDX) : new Comment("printf arg0 already in rdx"),
                new MoveImmToReg(formatString, RCX),
                new SubImmFromReg(SHADOW_SPACE, RSP),
                new CallIndirect(new FixedLabel(FUN_PRINTF.getMappedName())),
                new AddImmToReg(SHADOW_SPACE, RSP)
        );
    }

    public static List<Line> fflush(String stream) {
        return asList(
                new MoveImmToReg(stream, RCX),
                new SubImmFromReg(SHADOW_SPACE, RSP),
                new CallIndirect(new FixedLabel(FUN_FFLUSH.getMappedName())),
                new AddImmToReg(SHADOW_SPACE, RSP)
        );
    }

    public static List<Line> strlen(Register address) {
        return asList(
                (address != RCX) ? new MoveRegToReg(address, RCX) : new Comment("strlen address already in rcx"),
                new SubImmFromReg(SHADOW_SPACE, RSP),
                new CallIndirect(new FixedLabel(FUN_STRLEN.getMappedName())),
                new AddImmToReg(SHADOW_SPACE, RSP)
        );
    }

    public static List<Line> strcpy(Register destination, Register source) {
        return asList(
                (destination != RCX) ? new MoveRegToReg(destination, RCX) : new Comment("strcpy destination already in rcx"),
                (source != RDX) ? new MoveRegToReg(source, RDX) : new Comment("strcpy source already in rdx"),
                new SubImmFromReg(SHADOW_SPACE, RSP),
                new CallIndirect(new FixedLabel(FUN_STRCPY.getMappedName())),
                new AddImmToReg(SHADOW_SPACE, RSP)
        );
    }

    public static List<Line> strncpy(Register destination, Register source, Register length) {
        return asList(
                (destination != RCX) ? new MoveRegToReg(destination, RCX) : new Comment("strncpy destination already in rcx"),
                (source != RDX) ? new MoveRegToReg(source, RDX) : new Comment("strncpy source already in rdx"),
                (length != R8) ? new MoveRegToReg(length, R8) : new Comment("strncpy length already in r8"),
                new SubImmFromReg(SHADOW_SPACE, RSP),
                new CallIndirect(new FixedLabel(FUN_STRNCPY.getMappedName())),
                new AddImmToReg(SHADOW_SPACE, RSP)
        );
    }

    public static List<Line> strcat(Register destination, Register source) {
        return asList(
                (destination != RCX) ? new MoveRegToReg(destination, RCX) : new Comment("strcat destination already in rcx"),
                (source != RDX) ? new MoveRegToReg(source, RDX) : new Comment("strcat source already in rdx"),
                new SubImmFromReg(SHADOW_SPACE, RSP),
                new CallIndirect(new FixedLabel(FUN_STRCAT.getMappedName())),
                new AddImmToReg(SHADOW_SPACE, RSP)
        );
    }

    public static List<Line> getchar() {
        return asList(
                new SubImmFromReg(SHADOW_SPACE, RSP),
                new CallIndirect(new FixedLabel(FUN_GETCHAR.getMappedName())),
                new AddImmToReg(SHADOW_SPACE, RSP)
        );
    }
}
