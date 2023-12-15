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

package se.dykstrom.jcc.basic.compiler

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class BasicParserArrayTests : AbstractBasicParserTests() {

    @Test
    fun shouldParseDimStatement() {
        parse("dim foo(1, 2, 3) as integer")
        parse("dim q as string, w(1) as integer, e as double")
        parse("dim a(4, 4, 4, 4, 4, 4, 77 + 99) as double")
    }

    @Test
    fun shouldRejectMissingIdent() {
        assertThrows<IllegalStateException> { parse("dim (1) as integer") }
    }

    @Test
    fun shouldRejectMissingType() {
        // This is allowed in QuickBasic, but not implemented here yet
        assertThrows<IllegalStateException> { parse("dim a(6)") }
    }

    @Test
    fun shouldParseArrayAccess() {
        parse("print a(10)")
        parse("print f#(0, 0, 0)")
        parse("print foo$(3, 14)")
        parse("print a(0, 5) * b(1, 7)")
    }
}
