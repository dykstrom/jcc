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

import se.dykstrom.jcc.common.assembly.base.AssemblyComment;
import se.dykstrom.jcc.common.assembly.base.FixedLabel;
import se.dykstrom.jcc.common.assembly.base.FloatRegister;
import se.dykstrom.jcc.common.assembly.base.Register;
import se.dykstrom.jcc.common.assembly.instruction.*;
import se.dykstrom.jcc.common.assembly.instruction.floating.MoveFloatRegToMem;
import se.dykstrom.jcc.common.intermediate.Blank;
import se.dykstrom.jcc.common.intermediate.Line;
import se.dykstrom.jcc.common.types.F64;
import se.dykstrom.jcc.common.types.Type;

import java.util.ArrayList;
import java.util.List;

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

    public static List<Line> enter(int numberOfArgs) {
        List<Line> lines = new ArrayList<>();

        // TODO: Remove the "enter function" part below and save base
        //  pointer in each function instead, to make this method work
        //  more like the one below.

        lines.add(new AssemblyComment("Enter function"));
        lines.add(new PushReg(RBP));
        lines.add(new MoveRegToReg(RSP, RBP));

        lines.add(new AssemblyComment("Save " + numberOfArgs + " argument(s) in home location(s)"));
        if (numberOfArgs > 0) lines.add(new MoveRegToMem(RCX, RBP, "10h"));
        if (numberOfArgs > 1) lines.add(new MoveRegToMem(RDX, RBP, "18h"));
        if (numberOfArgs > 2) lines.add(new MoveRegToMem(R8, RBP, "20h"));
        if (numberOfArgs > 3) lines.add(new MoveRegToMem(R9, RBP, "28h"));
        return lines;
    }

    public static List<Line> enter(final List<Type> argTypes) {
        final var types = argTypes.stream().limit(4).toList();

        final List<Line> lines = new ArrayList<>();
        if (!types.isEmpty()) {
            lines.add(new AssemblyComment("Save " + types.size() + " argument(s) in home location(s)"));
            for (int i = 0; i < types.size(); i++) {
                if (types.get(i) instanceof F64) {
                    lines.add(new MoveFloatRegToMem(getFloatRegister(i), String.format("%s+%xh", RBP, 0x10 + i * 0x8)));
                } else {
                    lines.add(new MoveRegToMem(getIntRegister(i), String.format("%s+%xh", RBP, 0x10 + i * 0x8)));
                }
            }
            lines.add(Blank.INSTANCE);
        }
        return lines;
    }

    private static FloatRegister getFloatRegister(final int index) {
        return FloatRegister.values()[index];
    }

    private static Register getIntRegister(final int index) {
        return switch (index) {
            case 0 -> RCX;
            case 1 -> RDX;
            case 2 -> R8;
            case 3 -> R9;
            default -> throw new IllegalStateException("Unexpected value: " + index);
        };
    }

    public static List<Line> exit(String exitCode) {
        return List.of(
                new MoveImmToReg(exitCode, RCX),
                new SubImmFromReg(SHADOW_SPACE, RSP),
                new CallIndirect(new FixedLabel(FUN_EXIT.getMappedName())),
                new AddImmToReg(SHADOW_SPACE, RSP)
        );
    }

    public static List<Line> malloc(String size) {
        return List.of(
                new MoveImmToReg(size, RCX),
                new SubImmFromReg(SHADOW_SPACE, RSP),
                new CallIndirect(new FixedLabel(FUN_MALLOC.getMappedName())),
                new AddImmToReg(SHADOW_SPACE, RSP)
        );
    }

    public static List<Line> malloc(Register size) {
        return List.of(
                (size != RCX) ? new MoveRegToReg(size, RCX) : new AssemblyComment("malloc size already in rcx"),
                new SubImmFromReg(SHADOW_SPACE, RSP),
                new CallIndirect(new FixedLabel(FUN_MALLOC.getMappedName())),
                new AddImmToReg(SHADOW_SPACE, RSP)
        );
    }

    public static List<Line> realloc(Register buffer, Register size) {
        return List.of(
                (buffer != RCX) ? new MoveRegToReg(buffer, RCX) : new AssemblyComment("realloc buffer already in rcx"),
                (size != RDX) ? new MoveRegToReg(size, RDX) : new AssemblyComment("realloc size already in rdx"),
                new SubImmFromReg(SHADOW_SPACE, RSP),
                new CallIndirect(new FixedLabel(FUN_REALLOC.getMappedName())),
                new AddImmToReg(SHADOW_SPACE, RSP)
        );
    }

    public static List<Line> free(Register address) {
        return List.of(
                (address != RCX) ? new MoveRegToReg(address, RCX) : new AssemblyComment("free address already in rcx"),
                new SubImmFromReg(SHADOW_SPACE, RSP),
                new CallIndirect(new FixedLabel(FUN_FREE.getMappedName())),
                new AddImmToReg(SHADOW_SPACE, RSP)
        );
    }

    public static List<Line> memset(Register address, Register character, Register size) {
        return List.of(
                (address != RCX) ? new MoveRegToReg(address, RCX) : new AssemblyComment("memset address already in rcx"),
                (character != RDX) ? new MoveRegToReg(character, RDX) : new AssemblyComment("memset character already in rdx"),
                (size != R8) ? new MoveRegToReg(size, R8) : new AssemblyComment("memset size already in r8"),
                new SubImmFromReg(SHADOW_SPACE, RSP),
                new CallIndirect(new FixedLabel(FUN_MEMSET.getMappedName())),
                new AddImmToReg(SHADOW_SPACE, RSP)
        );
    }

    public static List<Line> printf(String formatString) {
        return List.of(
                new MoveImmToReg(formatString, RCX),
                new SubImmFromReg(SHADOW_SPACE, RSP),
                new CallIndirect(new FixedLabel(FUN_PRINTF.getMappedName())),
                new AddImmToReg(SHADOW_SPACE, RSP)
        );
    }

    public static List<Line> printf(String formatString, Register arg0) {
        return List.of(
                (arg0 != RDX) ? new MoveRegToReg(arg0, RDX) : new AssemblyComment("printf arg0 already in rdx"),
                new MoveImmToReg(formatString, RCX),
                new SubImmFromReg(SHADOW_SPACE, RSP),
                new CallIndirect(new FixedLabel(FUN_PRINTF.getMappedName())),
                new AddImmToReg(SHADOW_SPACE, RSP)
        );
    }

    public static List<Line> fflush(String stream) {
        return List.of(
                new MoveImmToReg(stream, RCX),
                new SubImmFromReg(SHADOW_SPACE, RSP),
                new CallIndirect(new FixedLabel(FUN_FFLUSH.getMappedName())),
                new AddImmToReg(SHADOW_SPACE, RSP)
        );
    }

    public static List<Line> strlen(Register address) {
        return List.of(
                (address != RCX) ? new MoveRegToReg(address, RCX) : new AssemblyComment("strlen address already in rcx"),
                new SubImmFromReg(SHADOW_SPACE, RSP),
                new CallIndirect(new FixedLabel(FUN_STRLEN.getMappedName())),
                new AddImmToReg(SHADOW_SPACE, RSP)
        );
    }

    public static List<Line> strcpy(Register destination, Register source) {
        return List.of(
                (destination != RCX) ? new MoveRegToReg(destination, RCX) : new AssemblyComment("strcpy destination already in rcx"),
                (source != RDX) ? new MoveRegToReg(source, RDX) : new AssemblyComment("strcpy source already in rdx"),
                new SubImmFromReg(SHADOW_SPACE, RSP),
                new CallIndirect(new FixedLabel(FUN_STRCPY.getMappedName())),
                new AddImmToReg(SHADOW_SPACE, RSP)
        );
    }

    public static List<Line> strncpy(Register destination, Register source, Register length) {
        return List.of(
                (destination != RCX) ? new MoveRegToReg(destination, RCX) : new AssemblyComment("strncpy destination already in rcx"),
                (source != RDX) ? new MoveRegToReg(source, RDX) : new AssemblyComment("strncpy source already in rdx"),
                (length != R8) ? new MoveRegToReg(length, R8) : new AssemblyComment("strncpy length already in r8"),
                new SubImmFromReg(SHADOW_SPACE, RSP),
                new CallIndirect(new FixedLabel(FUN_STRNCPY.getMappedName())),
                new AddImmToReg(SHADOW_SPACE, RSP)
        );
    }

    public static List<Line> strcat(Register destination, Register source) {
        return List.of(
                (destination != RCX) ? new MoveRegToReg(destination, RCX) : new AssemblyComment("strcat destination already in rcx"),
                (source != RDX) ? new MoveRegToReg(source, RDX) : new AssemblyComment("strcat source already in rdx"),
                new SubImmFromReg(SHADOW_SPACE, RSP),
                new CallIndirect(new FixedLabel(FUN_STRCAT.getMappedName())),
                new AddImmToReg(SHADOW_SPACE, RSP)
        );
    }

    public static List<Line> strdup(Register source) {
        return List.of(
                (source != RCX) ? new MoveRegToReg(source, RCX) : new AssemblyComment("strdup source already in rcx"),
                new SubImmFromReg(SHADOW_SPACE, RSP),
                new CallIndirect(new FixedLabel(FUN_STRDUP.getMappedName())),
                new AddImmToReg(SHADOW_SPACE, RSP)
        );
    }

    public static List<Line> getchar() {
        return List.of(
                new SubImmFromReg(SHADOW_SPACE, RSP),
                new CallIndirect(new FixedLabel(FUN_GETCHAR.getMappedName())),
                new AddImmToReg(SHADOW_SPACE, RSP)
        );
    }
}
