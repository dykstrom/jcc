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

package se.dykstrom.jcc.col.optimization

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import se.dykstrom.jcc.col.ast.expression.BecomeExpression
import se.dykstrom.jcc.col.ast.statement.FunCallStatement
import se.dykstrom.jcc.col.ast.statement.ValDeclarationStatement
import se.dykstrom.jcc.col.compiler.ColSymbols
import se.dykstrom.jcc.col.compiler.OpaqueConversionFunction
import se.dykstrom.jcc.col.compiler.OpaqueStringFunction
import se.dykstrom.jcc.col.type.ColTypeManager
import se.dykstrom.jcc.common.ast.AddExpression
import se.dykstrom.jcc.common.ast.AstProgram
import se.dykstrom.jcc.common.ast.DeclarationAssignment
import se.dykstrom.jcc.common.ast.Expression
import se.dykstrom.jcc.common.ast.FloatLiteral
import se.dykstrom.jcc.common.ast.FunctionCallExpression
import se.dykstrom.jcc.common.ast.IdentifierDerefExpression
import se.dykstrom.jcc.common.ast.IntegerLiteral
import se.dykstrom.jcc.common.ast.IntegerLiteral.ONE_I32
import se.dykstrom.jcc.common.ast.IntegerLiteral.TWO_I32
import se.dykstrom.jcc.common.ast.Statement
import se.dykstrom.jcc.common.ast.StringLiteral
import se.dykstrom.jcc.common.functions.Function
import se.dykstrom.jcc.common.functions.UserDefinedFunction
import se.dykstrom.jcc.common.types.F64
import se.dykstrom.jcc.common.types.Fun
import se.dykstrom.jcc.common.types.I32
import se.dykstrom.jcc.common.types.Identifier
import se.dykstrom.jcc.common.types.Opaque
import se.dykstrom.jcc.common.types.Str
import se.dykstrom.jcc.common.utils.OptimizationOptions

class ColAstOptimizerTests {

    private val symbols = ColSymbols()
    private val astOptimizer = ColAstOptimizer(ColTypeManager(), symbols)
    private val optimizer = astOptimizer.expressionOptimizer()

    @AfterEach
    fun tearDown() {
        OptimizationOptions.INSTANCE.level = 0
    }

    @Test
    fun shouldKeepConstantReference() {
        val identifier = Identifier("N", I32.INSTANCE)
        symbols.addConstant(identifier, "5")
        val expression = AddExpression(0, 0, IdentifierDerefExpression(0, 0, identifier), ONE_I32)

        assertEquals(expression, optimizer.expression(expression, symbols))
    }

    @Test
    fun shouldFoldLiterals() {
        val expression = AddExpression(0, 0, ONE_I32, TWO_I32)

        assertEquals(IntegerLiteral(0, 0, "3", I32.INSTANCE), optimizer.expression(expression, symbols))
    }

    @Test
    fun shouldNotFoldOpaqueConversionOfLiteral() {
        // Folding Meters(3.3) to 3.3 would change the expression's type from Meters to f64
        val toMeters = OpaqueConversionFunction("Meters", F64.INSTANCE, METERS)
        val expression = call(toMeters, FloatLiteral(0, 0, "3.3"))

        assertEquals(expression, optimizer.expression(expression, symbols))
    }

    @Test
    fun shouldNotFoldOtherFunctionCalls() {
        val userFunction = UserDefinedFunction("f", listOf("x"), listOf(Str.INSTANCE), NAME)
        val userCall = call(userFunction, StringLiteral(0, 0, "x"))
        val stringCall = call(OpaqueStringFunction(METERS), FloatLiteral(0, 0, "3.3"))

        assertEquals(userCall, optimizer.expression(userCall, symbols))
        assertEquals(stringCall, optimizer.expression(stringCall, symbols))
    }

    @Test
    fun shouldFoldValInitializer() {
        val declaration = DeclarationAssignment("v", Str.INSTANCE, AddExpression(0, 0, SL_A, SL_B))

        val statement = optimize(ValDeclarationStatement(declaration))

        assertEquals(ValDeclarationStatement(declaration.withExpression(SL_AB)), statement)
    }

    @Test
    fun shouldFoldCallArguments() {
        val userFunction = UserDefinedFunction("f", listOf("x"), listOf(Str.INSTANCE), Str.INSTANCE)

        val userCall = optimize(FunCallStatement(call(userFunction, AddExpression(0, 0, SL_A, SL_B))))

        assertEquals(FunCallStatement(call(userFunction, SL_AB)), userCall)
    }

    @Test
    fun shouldFoldBecomeArguments() {
        val userFunction = UserDefinedFunction("f", listOf("x"), listOf(Str.INSTANCE), Str.INSTANCE)
        val become = BecomeExpression(call(userFunction, AddExpression(0, 0, SL_A, SL_B)))

        assertEquals(BecomeExpression(call(userFunction, SL_AB)), optimizer.expression(become, symbols))
    }

    private fun optimize(statement: Statement): Statement {
        OptimizationOptions.INSTANCE.level = 1
        return astOptimizer.program(AstProgram(0, 0, listOf(statement))).statements.single()
    }

    private fun call(function: Function, arg: Expression): FunctionCallExpression {
        val identifier = Identifier(function.name, Fun.from(function.argTypes, function.returnType))
        return FunctionCallExpression(0, 0, identifier, listOf(arg), function)
    }

    private companion object {
        val METERS = Opaque("Meters", F64.INSTANCE)
        val NAME = Opaque("Name", Str.INSTANCE)
        val SL_A = StringLiteral(0, 0, "a")
        val SL_B = StringLiteral(0, 0, "b")
        val SL_AB = StringLiteral(0, 0, "ab")
    }
}
