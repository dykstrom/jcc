package se.dykstrom.jcc.llvm

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import se.dykstrom.jcc.common.types.F64
import se.dykstrom.jcc.common.types.I32
import se.dykstrom.jcc.common.types.I64
import se.dykstrom.jcc.common.types.Ptr
import se.dykstrom.jcc.llvm.LlvmTests.Companion.LABEL_L0
import se.dykstrom.jcc.llvm.LlvmTests.Companion.LABEL_L1
import se.dykstrom.jcc.llvm.LlvmUtils.endsWithTerminator
import se.dykstrom.jcc.llvm.LlvmUtils.typeToOperator
import se.dykstrom.jcc.llvm.code.Comment
import se.dykstrom.jcc.llvm.code.Line
import se.dykstrom.jcc.llvm.operand.TempOperand
import se.dykstrom.jcc.llvm.operation.BranchOperation
import se.dykstrom.jcc.llvm.operation.IndirectBranchOperation
import se.dykstrom.jcc.llvm.operation.ReturnOperation

class LlvmUtilsTests {

    @Test
    fun shouldGetOperatorByType() {
        assertEquals(LlvmOperator.ADD, typeToOperator(I32.INSTANCE, LlvmOperator.FADD, LlvmOperator.ADD))
        assertEquals(LlvmOperator.ADD, typeToOperator(I64.INSTANCE, LlvmOperator.FADD, LlvmOperator.ADD))
        assertEquals(LlvmOperator.FADD, typeToOperator(F64.INSTANCE, LlvmOperator.FADD, LlvmOperator.ADD))
    }

    @Test
    fun shouldDetectTerminator() {
        assertTrue(endsWithTerminator(listOf(BranchOperation(LABEL_L0))))
        assertTrue(endsWithTerminator(listOf(IndirectBranchOperation(TempOperand("%0", Ptr.INSTANCE), listOf(LABEL_L0)))))
        assertTrue(endsWithTerminator(listOf(ReturnOperation())))
        assertTrue(endsWithTerminator(listOf(BranchOperation(LABEL_L0), Comment("after"))))
    }

    @Test
    fun shouldDetectNoTerminator() {
        assertFalse(endsWithTerminator(listOf<Line>()))
        assertFalse(endsWithTerminator(listOf(Comment("only"))))
        assertFalse(endsWithTerminator(listOf(BranchOperation(LABEL_L0), LABEL_L1)))
    }
}
