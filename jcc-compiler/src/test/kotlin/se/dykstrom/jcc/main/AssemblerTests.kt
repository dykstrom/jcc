/*
 * Copyright (C) 2026 Johan Dykstrom
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

package se.dykstrom.jcc.main

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.OptionalInt

class AssemblerTests {

    @Test
    fun shouldParseMajorVersion() {
        listOf(
            "clang version 22.1.8 (https://github.com/msys2/MINGW-packages d34feaaa1d0aff624c870910b6f2589aa40d20de)\nTarget: x86_64-w64-windows-gnu",
            "Homebrew clang version 22.1.8\nTarget: arm64-apple-darwin25.5.0",
            "Ubuntu clang version 22.1.8 (++20261001083015+7a941dfb2877-1~exp1~20261001083031.30)\nTarget: x86_64-pc-linux-gnu",
        ).forEach { assertEquals(OptionalInt.of(22), Assembler.parseClangMajorVersion(it), it) }
    }

    @Test
    fun shouldNotParseMajorVersion() {
        listOf(
            "Apple clang version 17.0.0 (clang-1700.3.19.1)\nTarget: arm64-apple-darwin25.5.0",
            "gcc (GCC) 16.2.0",
            "",
        ).forEach { assertEquals(OptionalInt.empty(), Assembler.parseClangMajorVersion(it), it) }
    }
}
