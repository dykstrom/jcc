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

package se.dykstrom.jcc.common.compiler

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import se.dykstrom.jcc.common.ast.AstProgram
import se.dykstrom.jcc.common.error.CompilationErrorListener
import se.dykstrom.jcc.common.symbols.SymbolTable
import se.dykstrom.jcc.common.types.I64
import se.dykstrom.jcc.common.types.Identifier

class AbstractSemanticsParserTests {

    private val rootSymbolTable = SymbolTable()

    private val parser = object : AbstractSemanticsParser<DefaultTypeManager>(
        CompilationErrorListener(),
        rootSymbolTable,
        DefaultTypeManager()
    ) {
        override fun parse(program: AstProgram): AstProgram = program
    }

    @Test
    fun localSymbolTableShouldBeChildOfCurrent() {
        parser.withLocalSymbolTable {
            assertNotSame(rootSymbolTable, parser.symbols())
            assertSame(rootSymbolTable, parser.symbols().pop())
        }
        assertSame(rootSymbolTable, parser.symbols())
    }

    @Test
    fun localSymbolTableShouldBeDiscarded() {
        parser.withLocalSymbolTable {
            parser.symbols().addVariable(IDENT_I64_A)
            assertTrue(parser.symbols().contains(IDENT_I64_A.name()))
        }
        assertFalse(parser.symbols().contains(IDENT_I64_A.name()))
    }

    @Test
    fun localSymbolTableShouldSeeOuterScopes() {
        rootSymbolTable.addVariable(IDENT_I64_A)
        parser.withLocalSymbolTable {
            assertTrue(parser.symbols().contains(IDENT_I64_A.name()))
        }
    }

    @Test
    fun localSymbolTableShouldBeRestoredOnException() {
        assertThrows<IllegalStateException> {
            parser.withLocalSymbolTable { throw IllegalStateException() }
        }
        assertSame(rootSymbolTable, parser.symbols())
    }

    @Test
    fun localSymbolTableShouldReturnSupplierValue() {
        assertEquals(17, parser.withLocalSymbolTable { 17 })
    }

    @Test
    fun globalSymbolTableShouldBeChildOfRoot() {
        parser.withLocalSymbolTable {
            parser.withLocalSymbolTable {
                parser.withGlobalSymbolTable {
                    // The new scope bypasses the intermediate scopes
                    assertSame(rootSymbolTable, parser.symbols().pop())
                }
            }
        }
    }

    @Test
    fun globalSymbolTableShouldNotSeeIntermediateScopes() {
        rootSymbolTable.addVariable(IDENT_I64_A)
        parser.withLocalSymbolTable {
            parser.symbols().addVariable(IDENT_I64_B)
            parser.withGlobalSymbolTable {
                assertTrue(parser.symbols().contains(IDENT_I64_A.name()))
                assertFalse(parser.symbols().contains(IDENT_I64_B.name()))
            }
            // The intermediate scope is restored afterwards
            assertTrue(parser.symbols().contains(IDENT_I64_B.name()))
        }
    }

    @Test
    fun globalSymbolTableShouldBeRestoredOnException() {
        parser.withLocalSymbolTable {
            val localSymbolTable = parser.symbols()
            assertThrows<IllegalStateException> {
                parser.withGlobalSymbolTable { throw IllegalStateException() }
            }
            assertSame(localSymbolTable, parser.symbols())
        }
    }

    @Test
    fun globalSymbolTableShouldReturnSupplierValue() {
        assertEquals(17, parser.withGlobalSymbolTable { 17 })
    }

    companion object {
        private val IDENT_I64_A = Identifier("a", I64.INSTANCE)
        private val IDENT_I64_B = Identifier("b", I64.INSTANCE)
    }
}
