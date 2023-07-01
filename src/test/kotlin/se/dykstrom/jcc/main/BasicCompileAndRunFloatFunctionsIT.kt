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

package se.dykstrom.jcc.main

import io.kotlintest.specs.BehaviorSpec
import se.dykstrom.jcc.main.AbstractIntegrationTest.*

/**
 * Compile-and-run integration tests for Basic, specific for testing floating point functions.
 *
 * @author Johan Dykstrom
 */
class BasicCompileAndRunFloatFunctionsIT : BehaviorSpec() {
    init {
        Given("calls to function abs") {
            val source = listOf(
                    "print abs(1.0)",
                    "print abs(-1.0)",
                    "print abs(4711.4711)",
                    "print abs(-4711.4711)",
                    "print abs(3.14E-1)",
                    "print abs(-3.14E-1)"
            )
            val sourceFile = createSourceFile(source, BASIC)

            When("you compile and run") {

                Then("the output matches") {
                    compileAndAssertSuccess(sourceFile)
                    runAndAssertSuccess(sourceFile, "1.000000\n1.000000\n4711.471100\n4711.471100\n0.314000\n0.314000\n", 0)
                }
            }
        }

        Given("calls to some trigonometric functions") {
            val source = listOf(
                    "print atn(0.577350)",
                    "print atn(tan(0.5))", // atn(tan(0.5)) -> 0.5
                    "print 4 * atn(1)", // 4 * atn(1) -> PI
                    "print cos(3.1415926535897932384626433832795)",
                    "print cos(0)",
                    "print sin(1.5707963267948966192313216916398)",
                    "print sin(0)",
                    "print tan(3.1415926535897932384626433832795)",
                    "print tan(0.577350)"
            )
            val sourceFile = createSourceFile(source, BASIC)

            When("you compile and run") {

                Then("the output matches") {
                    compileAndAssertSuccess(sourceFile)
                    runAndAssertSuccess(sourceFile, """
                        |0.523599
                        |0.500000
                        |3.141593
                        |-1.000000
                        |1.000000
                        |1.000000
                        |0.000000
                        |-0.000000
                        |0.651388
                        |""".trimMargin(), 0)
                }
            }
        }

        Given("calls to function cdbl") {
            val source = listOf(
                    "print cdbl(1.0)",
                    "print cdbl(-7E3)",
                    "print cdbl(.345)",
                    "print cdbl(4711)",
                    "print cdbl(10 + 7)"
            )
            val sourceFile = createSourceFile(source, BASIC)

            When("you compile and run") {

                Then("the output matches") {
                    compileAndAssertSuccess(sourceFile)
                    runAndAssertSuccess(sourceFile, "1.000000\n-7000.000000\n0.345000\n4711.000000\n17.000000\n", 0)
                }
            }
        }

        Given("calls to function cint") {
            val source = listOf(
                    "print cint(1.49)",
                    "print cint(1.50)",
                    "print cint(17)",
                    "print cint(-5.7)",
                    "print cint(-0.49)",
                    "print cint(-0.50)",
                    "print cint(-0.51)",
                    "print cint(1234567890)"
            )
            val sourceFile = createSourceFile(source, BASIC)

            When("you compile and run") {

                Then("the output matches") {
                    compileAndAssertSuccess(sourceFile)
                    runAndAssertSuccess(sourceFile, "1\n2\n17\n-6\n0\n-1\n-1\n1234567890\n", 0)
                }
            }
        }

        Given("calls to function exp") {
            val source = listOf(
                    "print exp(1)",
                    "print exp(2.0)"
            )
            val sourceFile = createSourceFile(source, BASIC)

            When("you compile and run") {

                Then("the output matches") {
                    compileAndAssertSuccess(sourceFile)
                    runAndAssertSuccess(sourceFile, "2.718282\n7.389056\n", 0)
                }
            }
        }

        Given("calls to function fix") {
            val source = listOf(
                    "print fix(2.5)",
                    "print fix(-2.5)",
                    "print fix(17.12345)",
                    "print fix(-5.7)",
                    "print fix(-0.50)"
            )
            val sourceFile = createSourceFile(source, BASIC)

            When("you compile and run") {

                Then("the output matches") {
                    compileAndAssertSuccess(sourceFile)
                    runAndAssertSuccess(sourceFile, "2\n-2\n17\n-5\n0\n", 0)
                }
            }
        }

        Given("calls to function int") {
            val source = listOf(
                    "print int(2.5)",
                    "print int(-2.5)",
                    "print int(17.12345)",
                    "print int(-5.7)",
                    "print int(-0.50)"
            )
            val sourceFile = createSourceFile(source, BASIC)

            When("you compile and run") {

                Then("the output matches") {
                    compileAndAssertSuccess(sourceFile)
                    runAndAssertSuccess(sourceFile, "2\n-3\n17\n-6\n-1\n", 0)
                }
            }
        }

        Given("calls to function log") {
            val source = listOf(
                    "print log(10)",
                    "print log(100)"
            )
            val sourceFile = createSourceFile(source, BASIC)

            When("you compile and run") {

                Then("the output matches") {
                    compileAndAssertSuccess(sourceFile)
                    runAndAssertSuccess(sourceFile, "2.302585\n4.605170\n", 0)
                }
            }
        }

        Given("calls to function sqr") {
            val source = listOf(
                    "print sqr(1.0)",
                    "print sqr(2)",
                    "print sqr(4.0)",
                    "print sqr(25)"
            )
            val sourceFile = createSourceFile(source, BASIC)

            When("you compile and run") {

                Then("the output matches") {
                    compileAndAssertSuccess(sourceFile)
                    runAndAssertSuccess(sourceFile, """
                        |1.000000
                        |1.414214
                        |2.000000
                        |5.000000
                        |""".trimMargin(), 0)
                }
            }
        }
    }
}
