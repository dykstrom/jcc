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

package se.dykstrom.jcc.col.compiler

import org.junit.jupiter.api.Test
import se.dykstrom.jcc.common.ast.WhileStatement

class ColSemanticsParserWhileTests : AbstractColSemanticsParserTests() {

    @Test
    fun shouldParseSimpleWhile() {
        val program = parse(
            """
            while true do
                call println(1)
            end
            """.trimIndent()
        )
        assert(program.statements[0] is WhileStatement)
    }

    @Test
    fun shouldParseWhileWithBooleanConditionExpression() {
        parse(
            """
            val limit := 10
            while limit > 0 do
                call println(limit)
            end
            """.trimIndent()
        )
    }

    @Test
    fun shouldParseWhileWithLoopLocalVal() {
        // A val declared in the loop is visible within the loop body
        parse(
            """
            while true do
                val x := 17
                call println(x)
            end
            """.trimIndent()
        )
    }

    @Test
    fun shouldParseNestedWhile() {
        parse(
            """
            while true do
                while false do
                    call println(1)
                end
            end
            """.trimIndent()
        )
    }

    @Test
    fun shouldNotParseWhileWithIntegerCondition() {
        parseAndExpectError(
            """
            while 1 do
                call println(1)
            end
            """.trimIndent(),
            "while condition must be a boolean expression"
        )
    }

    @Test
    fun shouldNotParseWhileWithFloatCondition() {
        parseAndExpectError(
            """
            while 1.0 do
                call println(1)
            end
            """.trimIndent(),
            "while condition must be a boolean expression"
        )
    }

    @Test
    fun shouldNotParseFunDefInWhileBody() {
        parseAndExpectError(
            """
            while true do
                fun f() -> i64 := 1
            end
            """.trimIndent(),
            "statement not allowed in while body"
        )
    }

    @Test
    fun shouldNotParseAliasInWhileBody() {
        parseAndExpectError(
            """
            while true do
                alias Number as i64
            end
            """.trimIndent(),
            "statement not allowed in while body"
        )
    }

    @Test
    fun shouldNotParseValShadowingEnclosingVal() {
        // A val in the loop body may not shadow a name visible from the enclosing scope
        parseAndExpectError(
            """
            val x := 1
            while true do
                val x := 2
                call println(x)
            end
            """.trimIndent(),
            "value 'x' is already defined"
        )
    }

    @Test
    fun shouldNotSeeLoopLocalValAfterLoop() {
        // A val declared in the loop is invisible once the loop ends
        parseAndExpectError(
            """
            while true do
                val x := 17
                call println(x)
            end
            call println(x)
            """.trimIndent(),
            "undefined variable: x"
        )
    }
}
