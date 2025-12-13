package se.dykstrom.jcc.basic.compiler

import org.junit.jupiter.api.Test
import se.dykstrom.jcc.basic.BasicTests.Companion.FL_0_5
import se.dykstrom.jcc.basic.BasicTests.Companion.IL_0
import se.dykstrom.jcc.basic.BasicTests.Companion.IL_3
import se.dykstrom.jcc.basic.BasicTests.Companion.IL_5
import se.dykstrom.jcc.basic.ast.statement.*
import se.dykstrom.jcc.common.ast.*
import se.dykstrom.jcc.common.ast.BooleanLiteral.FALSE

internal class BasicLlvmCodeGeneratorControlStructuresTests : AbstractBasicCodeGeneratorTests() {

    private val cg = BasicLlvmCodeGenerator(typeManager, symbols, optimizer)

    @Test
    fun whileLoopWithIntCondition() {
        val result = assembleProgram(cg, listOf(
            WhileStatement(IL_5, listOf(PrintStatement(listOf(IL_3))))
        ))
        assertContains(result, listOf(
            "L0:",
            "%0 = icmp ne i64 5, 0",
            "br i1 %0, label %L1, label %L2",
            "L1:",
            "L2:",
        ))
    }

    @Test
    fun whileLoopWithFloatCondition() {
        val result = assembleProgram(cg, listOf(
            WhileStatement(FL_0_5, listOf(PrintStatement(listOf(IL_3))))
        ))
        assertContains(result, listOf(
            "%0 = fcmp one double 0.5, 0.0",
        ))
    }

    @Test
    fun nestedWhileLoops() {
        val result = assembleProgram(cg, listOf(
            WhileStatement(FL_0_5, listOf(
                WhileStatement(IL_5, listOf(
                    PrintStatement(listOf(IL_3))
                ))
            ))
        ))
        assertContains(result, listOf(
            "%0 = fcmp one double 0.5, 0.0",
            "%1 = icmp ne i64 5, 0",
        ))
    }

    @Test
    fun ifWithIntCondition() {
        val result = assembleProgram(cg, listOf(
            IfStatement.builder(IL_5, PrintStatement(listOf(IL_3)))
                .elseStatements(PrintStatement(listOf(IL_0)))
                .build(),
        ))
        assertContains(result, listOf(
            "%0 = icmp ne i64 5, 0",
            "br i1 %0, label %L1, label %L2",
        ))
    }

    @Test
    fun gotoLabel() {
        val result = assembleProgram(cg, listOf(
            LabelledStatement("foo", GotoStatement("bar"))
        ))
        assertContains(result, listOf(
            "_foo:",
            "br label %_bar",
        ))
    }

    @Test
    fun gotoInWhile() {
        val ws = WhileStatement(FALSE, listOf(GotoStatement("foo")))
        val result = assembleProgram(cg, listOf(
            ws,
            EndStatement(),
            LabelledStatement("foo", EndStatement()),
        ))
        // What we really want to test is that the branch to label %_foo
        // is not directly followed by a branch to label %L0...
        assertContains(result, listOf(
            "br label %L0",
            "br label %_foo",
        ))
    }

    @Test
    fun onGoto() {
        val result = assembleProgram(cg, listOf(
            OnGotoStatement(AddExpression(IL_3, IL_0), listOf("10", "20", "30")),
            LabelledStatement("10", EndStatement()),
            LabelledStatement("20", EndStatement()),
            LabelledStatement("30", EndStatement()),
        ))
        assertContains(result, listOf(
            "%0 = add i64 3, 0",
            "%1 = icmp eq i64 %0, 0",
            "br i1 %1, label %_10, label %_L0",
            "_L0:",
        ))
    }

    @Test
    fun gosubLabel() {
        val result = assembleProgram(cg, listOf(
            GosubStatement("sub"),
            EndStatement(),
            LabelledStatement("sub", ReturnFromGosubStatement()),
        ))
        assertContains(result, listOf(
            "call void @gosub_push(ptr blockaddress(@main, %_after.gosub.0))",
            "br label %_sub",
            "_after.gosub.0:",
            "_sub:",
            "%1 = call ptr @gosub_pop()",
            "indirectbr ptr %1, [label %_after.gosub.0]",
        ))
    }

    @Test
    fun gosubTwoTimes() {
        val result = assembleProgram(
            cg, listOf(
                GosubStatement("sub"),
                PrintStatement(listOf()),
                GosubStatement("sub"),
                LabelledStatement("lab", EndStatement()),
                LabelledStatement("sub", ReturnFromGosubStatement()),
            )
        )
        assertContains(result, listOf(
            "call void @gosub_push(ptr blockaddress(@main, %_after.gosub.0))",
            "br label %_sub",
            "_after.gosub.0:",
            "call void @gosub_push(ptr blockaddress(@main, %_lab))",
            "br label %_sub",
            "_lab:",
            "_sub:",
            "%2 = call ptr @gosub_pop()",
            "indirectbr ptr %2, [label %_after.gosub.0, label %_lab]",
        ))
    }

    @Test
    fun gosubInWhile() {
        val ws = WhileStatement(FALSE, listOf(GosubStatement("sub")))
        val result = assembleProgram(cg, listOf(
            ws,
            EndStatement(),
            LabelledStatement("sub", ReturnFromGosubStatement()),
        ))
        assertContains(result, listOf(
            "call void @gosub_push(ptr blockaddress(@main, %_after.gosub.0))",
            "br label %_sub",
            "_after.gosub.0:",
            "_sub:",
            "%2 = call ptr @gosub_pop()",
            "indirectbr ptr %2, [label %_after.gosub.0]",
        ))
    }

    @Test
    fun gosubInIfThen() {
        val ifs = IfStatement.builder(FALSE, listOf(GosubStatement("sub")))
            .build()
        val result = assembleProgram(
            cg, listOf(
                ifs,
                EndStatement(),
                LabelledStatement("sub", ReturnFromGosubStatement()),
            )
        )
        assertContains(
            result, listOf(
                "call void @gosub_push(ptr blockaddress(@main, %_after.gosub.0))",
                "br label %_sub",
                "_after.gosub.0:",
                "_sub:",
                "%2 = call ptr @gosub_pop()",
                "indirectbr ptr %2, [label %_after.gosub.0]",
            )
        )
    }

    @Test
    fun gosubInIfThenElse() {
        val ifs = IfStatement.builder(FALSE, listOf(GosubStatement("sub")))
            .elseStatements(GosubStatement("sub"))
            .build()
        val result = assembleProgram(
            cg, listOf(
                ifs,
                EndStatement(),
                LabelledStatement("sub", ReturnFromGosubStatement()),
            )
        )
        assertContains(
            result, listOf(
                "call void @gosub_push(ptr blockaddress(@main, %_after.gosub.0))",
                "br label %_sub",
                "_after.gosub.0:",
                "call void @gosub_push(ptr blockaddress(@main, %_after.gosub.1))",
                "br label %_sub",
                "_after.gosub.1:",
                "_sub:",
                "%2 = call ptr @gosub_pop()",
                "indirectbr ptr %2, [label %_after.gosub.0, label %_after.gosub.1]",
            )
        )
    }

    @Test
    fun gosubInIfThenElseWithLabelTargets() {
        val ifs = IfStatement.builder(
            FALSE,
            GosubStatement("sub"),
            LabelledStatement("thenRet", PrintStatement(listOf(IL_3))),
            )
            .elseStatements(
                GosubStatement("sub"),
                LabelledStatement("elseRet", PrintStatement(listOf(IL_5))),
            )
            .build()
        val result = assembleProgram(
            cg, listOf(
                ifs,
                EndStatement(),
                LabelledStatement("sub", ReturnFromGosubStatement()),
            )
        )
        assertContains(
            result, listOf(
                "call void @gosub_push(ptr blockaddress(@main, %_thenRet))",
                "br label %_sub",
                "_thenRet:",
                "call void @gosub_push(ptr blockaddress(@main, %_elseRet))",
                "br label %_sub",
                "_elseRet:",
                "_sub:",
                "indirectbr ptr %4, [label %_thenRet, label %_elseRet]",
            )
        )
    }
}
