package se.dykstrom.jcc.col

import org.junit.jupiter.api.Assertions
import se.dykstrom.jcc.col.compiler.ColSymbols
import se.dykstrom.jcc.common.ast.*
import se.dykstrom.jcc.common.functions.ExternalFunction
import se.dykstrom.jcc.common.functions.LibraryFunction
import se.dykstrom.jcc.common.types.*

@Suppress("MemberVisibilityCanBePrivate")
class ColTests {

    companion object {

        fun verify(program: AstProgram, vararg statements: Statement) {
            Assertions.assertEquals(statements.size, program.statements.size)
            for ((index, statement) in statements.withIndex()) {
                Assertions.assertEquals(statement, program.statements[index])
            }
        }

        // Literals
        val IL_5 = IntegerLiteral(0, 0, 5)
        val IL_17 = IntegerLiteral(0, 0, 17)
        val IL_18 = IntegerLiteral(0, 0, 18)
        val IL_1_000 = IntegerLiteral(0, 0, 1_000)
        val IL_M_1 = IntegerLiteral(0, 0, -1)
        val IL_17_I32 = IntegerLiteral(0, 0, "17", I32.INSTANCE)

        val FL_1_0 = FloatLiteral(0, 0, "1.0")
        val FL_1_5 = FloatLiteral(0, 0, "1.5")
        val FL_2_0 = FloatLiteral(0, 0, "2.0")
        val FL_1_5_F32 = FloatLiteral(0, 0, "1.5", F32.INSTANCE)
        val FL_17_0_F32 = FloatLiteral(0, 0, "17.0", F32.INSTANCE)

        val CAST_0_I32 = FunctionCallExpression(ColSymbols.BF_I32_I64.identifier, listOf(IntegerLiteral.ZERO))
        val CAST_1_I32 = FunctionCallExpression(ColSymbols.BF_I32_I64.identifier, listOf(IntegerLiteral.ONE))
        val CAST_5_I32 = FunctionCallExpression(ColSymbols.BF_I32_I64.identifier, listOf(IL_5))
        val CAST_1_0_F32 = FunctionCallExpression(ColSymbols.BF_F32_F64.identifier, listOf(FL_1_0))

        // Identifiers
        val IDENT_F64_F = Identifier("f", F64.INSTANCE)
        val IDENT_I64_A = Identifier("a", I64.INSTANCE)
        val IDENT_I64_B = Identifier("b", I64.INSTANCE)

        // Identifier references
        val IDE_F64_F = IdentifierDerefExpression(0, 0, IDENT_F64_F)
        val IDE_I64_A = IdentifierDerefExpression(0, 0, IDENT_I64_A)
        val IDE_I64_B = IdentifierDerefExpression(0, 0, IDENT_I64_B)
        val IDE_UNK_A = IdentifierDerefExpression(0, 0, Identifier("a", null))
        val IDE_UNK_B = IdentifierDerefExpression(0, 0, Identifier("b", null))

        // Types
        val NT_BOOL = NamedType("bool")
        val NT_F64 = NamedType("f64")
        val NT_I64 = NamedType("i64")
        val NT_VOID = NamedType("void")

        // Function types
        val FUN_F64_TO_I64: Fun = Fun.from(listOf(F64.INSTANCE), I64.INSTANCE)
        val FUN_I64_TO_I64: Fun = Fun.from(listOf(I64.INSTANCE), I64.INSTANCE)
        val FUN_I64_F64_TO_I64: Fun = Fun.from(listOf(I64.INSTANCE, F64.INSTANCE), I64.INSTANCE)
        val FUN_TO_F64: Fun = Fun.from(listOf(), F64.INSTANCE)
        val FUN_TO_I64: Fun = Fun.from(listOf(), I64.INSTANCE)

        // Functions
        val EXT_FUN_ABS64 = ExternalFunction("_abs64")
        val EXT_FUN_FOO = ExternalFunction("foo")
        val EXT_FUN_SUM = ExternalFunction("sum")

        val FUN_ABS = LibraryFunction("abs", listOf(I64.INSTANCE), I64.INSTANCE, "msvcrt.dll", EXT_FUN_ABS64)
        val FUN_SUM0 = LibraryFunction("sum", listOf(), I64.INSTANCE, "lib.dll", EXT_FUN_SUM)
        val FUN_SUM1 = LibraryFunction("sum", listOf(I64.INSTANCE), I64.INSTANCE, "lib.dll", EXT_FUN_SUM)
        val FUN_SUM2 = LibraryFunction("sum", listOf(I64.INSTANCE, I64.INSTANCE), I64.INSTANCE, "lib.dll", EXT_FUN_SUM)
    }
}
