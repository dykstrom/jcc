package se.dykstrom.jcc.llvm

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import se.dykstrom.jcc.common.types.F64
import se.dykstrom.jcc.common.types.I32
import se.dykstrom.jcc.common.types.I64
import se.dykstrom.jcc.llvm.LlvmUtils.typeToOperator

class LlvmUtilsTests {

    @Test
    fun shouldPrintVersion() {
        assertEquals(LlvmOperator.ADD, typeToOperator(I32.INSTANCE, LlvmOperator.FADD, LlvmOperator.ADD))
        assertEquals(LlvmOperator.ADD, typeToOperator(I64.INSTANCE, LlvmOperator.FADD, LlvmOperator.ADD))
        assertEquals(LlvmOperator.FADD, typeToOperator(F64.INSTANCE, LlvmOperator.FADD, LlvmOperator.ADD))
    }
}
