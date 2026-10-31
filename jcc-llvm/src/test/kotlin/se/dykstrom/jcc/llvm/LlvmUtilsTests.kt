package se.dykstrom.jcc.llvm

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import se.dykstrom.jcc.common.types.F32
import se.dykstrom.jcc.common.types.F64
import se.dykstrom.jcc.common.types.I32
import se.dykstrom.jcc.common.types.I64
import se.dykstrom.jcc.common.types.Ptr
import se.dykstrom.jcc.llvm.LlvmTests.Companion.LABEL_L0
import se.dykstrom.jcc.llvm.LlvmTests.Companion.LABEL_L1
import se.dykstrom.jcc.llvm.LlvmUtils.endsWithTerminator
import se.dykstrom.jcc.llvm.LlvmUtils.floatToLlvm
import se.dykstrom.jcc.llvm.LlvmUtils.typeToOperator
import se.dykstrom.jcc.llvm.code.Comment
import se.dykstrom.jcc.llvm.code.Line
import se.dykstrom.jcc.llvm.operand.TempOperand
import se.dykstrom.jcc.llvm.operation.BranchOperation
import se.dykstrom.jcc.llvm.operation.IndirectBranchOperation
import se.dykstrom.jcc.llvm.operation.ReturnOperation

class LlvmUtilsTests {

    @Test
    fun shouldFormatFloatConstants() {
        assertEquals("1.5", floatToLlvm("1.5", F64.INSTANCE))
        assertEquals("0.1", floatToLlvm("0.1", F64.INSTANCE))
        // A single precision value must be exactly representable in single precision
        assertEquals("0.10000000149011612", floatToLlvm("0.1", F32.INSTANCE))
        // Infinity and NaN have no decimal form in LLVM
        assertEquals("0x7FF0000000000000", floatToLlvm("Infinity", F64.INSTANCE))
        assertEquals("0xFFF0000000000000", floatToLlvm("-Infinity", F32.INSTANCE))
        assertEquals("0x7FF8000000000000", floatToLlvm("NaN", F64.INSTANCE))
    }

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
