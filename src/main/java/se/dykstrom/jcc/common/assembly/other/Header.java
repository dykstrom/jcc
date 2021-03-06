/*
 * Copyright (C) 2016 Johan Dykstrom
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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import se.dykstrom.jcc.common.assembly.base.Blank;
import se.dykstrom.jcc.common.assembly.base.CodeContainer;
import se.dykstrom.jcc.common.assembly.base.Comment;
import se.dykstrom.jcc.common.assembly.base.Label;
import se.dykstrom.jcc.common.utils.Version;

/**
 * Represents a FASM source file header, with comments, format directive, and entry point.
 *
 * @author Johan Dykstrom
 */
public class Header extends CodeContainer {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public Header(String sourceFilename, Label entry) {
        add(new Comment("JCC version: " + Version.instance()));
        add(new Comment("Date & time: " + FORMATTER.format(LocalDateTime.now())));
        add(new Comment("Source file: " + sourceFilename));
        add(new Format());
        add(new Entry(entry));
        add(new Include("win64a.inc"));
        add(Blank.INSTANCE);
    }
}
