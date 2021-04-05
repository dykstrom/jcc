/*
 * Copyright (C) 2018 Johan Dykstrom
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

package se.dykstrom.jcc.basic.functions

import io.kotlintest.matchers.contain
import io.kotlintest.matchers.should
import io.kotlintest.matchers.shouldBe
import io.kotlintest.specs.BehaviorSpec
import se.dykstrom.jcc.common.assembly.instruction.CallIndirect
import se.dykstrom.jcc.common.assembly.instruction.Ret
import se.dykstrom.jcc.common.assembly.instruction.floating.RoundFloatRegToIntReg
import se.dykstrom.jcc.common.functions.BuiltInFunctions.FUN_FLOOR

class BasicIntFunctionTests : BehaviorSpec() {
    init {
        Given("function int") {
            val function = BasicIntFunction()

            When("you get all code lines") {
                val codeLines = function.lines()

                Then("one should be a function call to floor") {
                    val floor = "[" + FUN_FLOOR.mappedName + "]"
                    codeLines.filterIsInstance(CallIndirect::class.java).map { call -> call.target } should contain(floor)
                }

                Then("one should be a rounding instruction") {
                    codeLines should containType(RoundFloatRegToIntReg::class)
                }

                Then("the last should be Ret") {
                    codeLines.last() shouldBe Ret()
                }
            }
        }
    }
}
