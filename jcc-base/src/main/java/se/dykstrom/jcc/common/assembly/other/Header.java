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

import se.dykstrom.jcc.common.assembly.directive.Entry;
import se.dykstrom.jcc.common.assembly.directive.Format;
import se.dykstrom.jcc.common.assembly.directive.Include;
import se.dykstrom.jcc.common.code.Blank;
import se.dykstrom.jcc.common.code.CodeContainer;
import se.dykstrom.jcc.common.assembly.base.AssemblyComment;
import se.dykstrom.jcc.common.assembly.directive.Label;
import se.dykstrom.jcc.common.utils.Version;

import java.nio.file.Path;
import java.time.LocalDateTime;

import static java.time.format.DateTimeFormatter.ISO_DATE_TIME;

/**
 * Represents a FASM source file header, with comments, format directive, and entry point.
 *
 * @author Johan Dykstrom
 */
public class Header extends CodeContainer {

    public Header(final Path sourcePath, final Label entryLabel) {
        add(new AssemblyComment("JCC version: " + Version.instance(), 3));
        add(new AssemblyComment("Date & time: " + ISO_DATE_TIME.format(LocalDateTime.now()), 3));
        add(new AssemblyComment("Source file: " + sourcePath, 3));
        add(new Format());
        add(new Entry(entryLabel));
        add(new Include("win64a.inc"));
        add(Blank.INSTANCE);
    }
}
