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

import org.junit.Test

class BasicParserArrayTests : AbstractBasicParserTest() {

    @Test
    fun shouldParseDimStatement() {
        parse("dim foo(1, 2, 3) as integer")
        parse("dim q as string, w(1) as boolean, e as boolean")
        parse("dim a(4, 4, 4, 4, 4, 4, 77 + 99) as double")
    }

    @Test(expected = IllegalStateException::class)
    fun shouldRejectMissingIdent() {
        parse("dim (1) as integer")
    }

    @Test(expected = IllegalStateException::class)
    fun shouldRejectMissingType() {
        // This is allowed in QuickBasic, but not implemented here yet
        parse("dim a(6)")
    }
}
