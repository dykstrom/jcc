package se.dykstrom.jcc.common.compiler

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import se.dykstrom.jcc.common.ast.FunctionCallExpression
import se.dykstrom.jcc.common.ast.IdentifierDerefExpression
import se.dykstrom.jcc.common.ast.IdentifierNameExpression
import se.dykstrom.jcc.common.ast.IntegerLiteral.ONE
import se.dykstrom.jcc.common.ast.IntegerLiteral.ZERO
import se.dykstrom.jcc.common.compiler.DefaultFunctionCallHelper.canBeEvaluatedLater
import se.dykstrom.jcc.common.functions.ExternalFunction
import se.dykstrom.jcc.common.functions.LibraryFunction
import se.dykstrom.jcc.common.functions.UserDefinedFunction
import se.dykstrom.jcc.common.symbols.SymbolTable
import se.dykstrom.jcc.common.types.I64
import se.dykstrom.jcc.common.types.Identifier
import se.dykstrom.jcc.common.types.Parameter
import se.dykstrom.jcc.common.types.Str

internal class DefaultFunctionCallHelperTests {

    private val symbolTable: SymbolTable = SymbolTable()

    private val functionAbs = LibraryFunction("abs", listOf(I64.INSTANCE), I64.INSTANCE, "N/A", ExternalFunction("N/A"))
    private val functionSum = UserDefinedFunction("sum", listOf("a", "b"), listOf(I64.INSTANCE, I64.INSTANCE), I64.INSTANCE)

    private val identifierFoo = Identifier("foo", I64.INSTANCE)
    private val identifierBar = Parameter("bar", I64.INSTANCE, "N/A")
    private val identifierTee = Identifier("tee", Str.INSTANCE)

    @BeforeEach
    fun setUp() {
        symbolTable.addVariable(identifierFoo)
        symbolTable.addVariable(identifierBar)
        symbolTable.addConstant(identifierTee, "N/A")
        symbolTable.addFunction(functionAbs)
        symbolTable.addFunction(functionSum)
    }

    @Test
    fun functionArgsCanBeEvaluatedLater() {
        // Given
        val ideFoo = IdentifierDerefExpression(0, 0, identifierFoo)
        val ideBar = IdentifierDerefExpression(0, 0, identifierBar)
        val ineTee = IdentifierNameExpression(0, 0, identifierTee)
        val fceAbs = FunctionCallExpression(0, 0, functionAbs.identifier, listOf(ZERO))
        val fceSum = FunctionCallExpression(0, 0, functionSum.identifier, listOf(ZERO, ONE))

        val args = listOf(
            ZERO,
            ideFoo,
            ideBar,
            fceSum, // 3: Call to user-defined function
            ZERO,
            ideFoo,
            ideBar,
            fceAbs, // 7: Call to built-in function
            ZERO,
            ideFoo,
            ideBar,
            ineTee,
        )

        // When
        val result0 = canBeEvaluatedLater(0, args, symbolTable)
        val result1 = canBeEvaluatedLater(1, args, symbolTable)
        val result2 = canBeEvaluatedLater(2, args, symbolTable)
        val result3 = canBeEvaluatedLater(3, args, symbolTable) // fceSum
        val result4 = canBeEvaluatedLater(4, args, symbolTable)
        val result5 = canBeEvaluatedLater(5, args, symbolTable)
        val result6 = canBeEvaluatedLater(6, args, symbolTable)
        val result7 = canBeEvaluatedLater(7, args, symbolTable) // fceAbs
        val result8 = canBeEvaluatedLater(8, args, symbolTable)
        val result9 = canBeEvaluatedLater(9, args, symbolTable)
        val result10 = canBeEvaluatedLater(10, args, symbolTable)
        val result11 = canBeEvaluatedLater(11, args, symbolTable)

        // Then
        assertTrue(result0)
        assertFalse(result1)
        assertTrue(result2)
        assertFalse(result3)
        assertTrue(result4)
        assertTrue(result5)
        assertTrue(result6)
        assertFalse(result7)
        assertTrue(result8)
        assertTrue(result9)
        assertTrue(result10)
        assertTrue(result11)
    }
}
