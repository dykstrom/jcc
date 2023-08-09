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

package se.dykstrom.jcc.common.assembly.base

import io.kotlintest.matchers.should
import io.kotlintest.matchers.shouldThrow
import io.kotlintest.specs.BehaviorSpec
import se.dykstrom.jcc.common.assembly.base.OperandSize.BYTE

class OperandSizeTests : BehaviorSpec() {

    init {
        Given("operand size byte") {
            When("you validate a valid value") {
                BYTE.validate("0")
                BYTE.validate(Byte.MIN_VALUE.toString())
                BYTE.validate(Byte.MAX_VALUE.toString())

                Then("no exception is thrown") {
                }
            }

            When("you validate a hexadecimal value") {
                BYTE.validate("0h")
                BYTE.validate("7fh")

                Then("no exception is thrown") {
                }
            }

            When("you validate an invalid value") {
                val exception = shouldThrow<NumberFormatException> {
                    BYTE.validate("128")
                }

                Then("a NumberFormatException is thrown") {
                    exception.message should {it?.startsWith("Value out of range.")}
                }
            }
        }
    }
}
