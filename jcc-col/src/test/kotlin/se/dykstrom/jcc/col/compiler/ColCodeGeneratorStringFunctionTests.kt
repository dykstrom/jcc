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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import se.dykstrom.jcc.col.ColTests.Companion.SL_FOO
import se.dykstrom.jcc.col.ast.expression.BecomeExpression
import se.dykstrom.jcc.col.compiler.ColSymbols.BF_PRINTLN_STR
import se.dykstrom.jcc.common.ast.AddExpression
import se.dykstrom.jcc.common.ast.Declaration
import se.dykstrom.jcc.common.ast.EqualExpression
import se.dykstrom.jcc.common.ast.FunctionCallExpression
import se.dykstrom.jcc.common.ast.FunctionDefinitionStatement
import se.dykstrom.jcc.common.ast.IdentifierDerefExpression
import se.dykstrom.jcc.common.ast.IfExpression
import se.dykstrom.jcc.common.ast.IntegerLiteral.ONE
import se.dykstrom.jcc.common.ast.IntegerLiteral.ZERO
import se.dykstrom.jcc.common.ast.SubExpression
import se.dykstrom.jcc.common.functions.ReferenceFunction
import se.dykstrom.jcc.common.functions.UserDefinedFunction
import se.dykstrom.jcc.common.types.Fun
import se.dykstrom.jcc.common.types.I64
import se.dykstrom.jcc.common.types.Identifier
import se.dykstrom.jcc.common.types.Str

/**
 * Tests the garbage-collector plumbing COL emits for strings that cross a function boundary: a
 * string parameter rooted in the callee's own frame, a string result rooted by the caller, and a
 * `become` whose string argument is produced in the frame the tail call pops.
 *
 * Strings inside a single function are covered by [ColCodeGeneratorStringTests], and the frame
 * bookkeeping that is not string-specific by [ColCodeGeneratorGcTests].
 */
internal class ColCodeGeneratorStringFunctionTests : AbstractColCodeGeneratorTests() {

    @Test
    fun shouldRootStringParameterInCalleeOwnFrame() {
        // Given: fun echo(s as string) -> string := s
        val result = assembleProgram(cg, listOf(echoDefinition()))

        // Then: the parameter slot holds the incoming pointer before it is rooted, and it is rooted
        // in the callee's frame - that is what makes the argument reachable again after the caller
        // has popped its own frame, and what lets echo safely return an argument it does not own
        assertContains(result, listOf(
            "define tailcc ptr @echo_Str(ptr %0)",
            "%_s = alloca ptr",
            "store ptr %0, ptr %_s",
            "call void @jcc_gc_add_root(ptr %_s)",
        ))
        assertInOrder(result, listOf(
            "call void @jcc_gc_push_frame()",
            "store ptr %0, ptr %_s",
            "call void @jcc_gc_add_root(ptr %_s)",
            "call void @jcc_gc_pop_frame()",
            "ret ptr",
        ))
        // Returning an argument registers nothing: the value was already registered by whoever
        // allocated it, or it is a literal the collector does not own
        assertNotContains(result, listOf("jcc_gc_register"))
    }

    @Test
    fun shouldRootStringResultInCallerWithoutRegisteringItAgain() {
        // Given: fun echo(s as string) -> string := s / call println(echo("foo"))
        val udf = UserDefinedFunction("echo", listOf("s"), listOf(Str.INSTANCE), Str.INSTANCE)
        val call = FunctionCallExpression(0, 0, ECHO, listOf(SL_FOO), udf)
        val result = assembleProgram(cg, listOf(echoDefinition(), funCall(BF_PRINTLN_STR, call)))

        // Then: a user-defined function registers its own result inside the callee, so the caller
        // only roots it - registering here would be a double registration
        assertContains(result, listOf(
            "%0 = call tailcc ptr @echo_Str(ptr @_.str.0)",
            "store ptr %0, ptr %_.gc.slot.0",
        ))
        assertInOrder(result, listOf(
            "%0 = call tailcc ptr @echo_Str(ptr @_.str.0)",
            "store ptr %0, ptr %_.gc.slot.0",
            "call i32 (ptr, ...) @printf",
        ))
        assertNotContains(result, listOf("jcc_gc_register"))
    }

    @Test
    fun shouldRootStringParameterButNotFunctionTypedParameter() {
        // Given: fun apply(f as (string) -> string, s as string) -> string := f(s)
        val funType = Fun.from(listOf(Str.INSTANCE), Str.INSTANCE)
        val identifier = Identifier("apply", Fun.from(listOf(funType, Str.INSTANCE), Str.INSTANCE))
        val declarations = listOf(Declaration(0, 0, "f", funType), Declaration(0, 0, "s", Str.INSTANCE))
        val referenceCall = FunctionCallExpression(
            0, 0,
            Identifier("f", funType),
            listOf(IdentifierDerefExpression(0, 0, Identifier("s", Str.INSTANCE))),
            ReferenceFunction("f", listOf(Str.INSTANCE), Str.INSTANCE)
        )
        val fds = FunctionDefinitionStatement(0, 0, identifier, declarations, referenceCall)

        val result = assembleProgram(cg, listOf(fds))

        // Then: only the string parameter is a root - a function pointer is not a collected object
        assertContains(result, listOf(
            "%_f = alloca ptr",
            "%_s = alloca ptr",
            "call void @jcc_gc_add_root(ptr %_s)",
        ))
        assertNotContains(result, listOf("call void @jcc_gc_add_root(ptr %_f)"))
        // A call through a function value returns an already-registered string too, so it is rooted
        // rather than registered
        assertContains(result, listOf("store ptr %4, ptr %_.gc.slot.0"))
        assertNotContains(result, listOf("jcc_gc_register"))
    }

    @Test
    fun shouldRegisterAndStoreBecomeArgumentBeforePoppingFrame() {
        // Given: fun build(acc as string, n as i64) -> string :=
        //            if n == 0 then acc else become build(acc + "foo", n - 1)
        val identifier = Identifier("build", Fun.from(listOf(Str.INSTANCE, I64.INSTANCE), Str.INSTANCE))
        val udf = UserDefinedFunction("build", listOf("acc", "n"), listOf(Str.INSTANCE, I64.INSTANCE), Str.INSTANCE)
        val declarations = listOf(
            Declaration(0, 0, "acc", Str.INSTANCE),
            Declaration(0, 0, "n", I64.INSTANCE),
        )
        val ideAcc = IdentifierDerefExpression(0, 0, Identifier("acc", Str.INSTANCE))
        val ideN = IdentifierDerefExpression(0, 0, Identifier("n", I64.INSTANCE))
        val tailCall = FunctionCallExpression(
            0, 0, identifier,
            listOf(AddExpression(ideAcc, SL_FOO), SubExpression(ideN, ONE)),
            udf
        )
        val body = IfExpression(EqualExpression(ideN, ZERO), ideAcc, BecomeExpression(tailCall))
        val fds = FunctionDefinitionStatement(0, 0, identifier, declarations, body)

        val result = assembleProgram(cg, listOf(fds))

        // Then: the accumulator is registered and stored into a rooted slot of the frame that is
        // about to be popped, so a second string sub-expression could not collect it away; the pop
        // then immediately precedes the musttail call, which admits nothing after it. Between the
        // pop and the callee rooting its parameter the pointer lives only in a register - safe only
        // because pushing a frame and adding a root never collect (working-notes/become-strings-and-gc.md).
        assertInOrder(result, listOf(
            "call ptr @col_concat_str_str",
            "call ptr @jcc_gc_register",
            // The store of the registered pointer into the rooted slot; matched by its destination
            // so the assertion does not pin a temporary register number
            ", ptr %_.gc.slot.0",
            "call void @jcc_gc_pop_frame()",
            "musttail call tailcc ptr @build_Str_I64",
        ))
        // The value leaf pops its own frame too, so a deep become chain does not grow the shadow
        // stack: two pops in build, one in main
        assertEquals(3, result.toText().split(POP_FRAME).size - 1)
    }

    /** `fun echo(s as string) -> string := s` */
    private fun echoDefinition(): FunctionDefinitionStatement {
        val declarations = listOf(Declaration(0, 0, "s", Str.INSTANCE))
        val body = IdentifierDerefExpression(0, 0, Identifier("s", Str.INSTANCE))
        return FunctionDefinitionStatement(0, 0, ECHO, declarations, body)
    }

    companion object {
        private val ECHO = Identifier("echo", Fun.from(listOf(Str.INSTANCE), Str.INSTANCE))
        private const val POP_FRAME = "call void @jcc_gc_pop_frame()"
    }
}
