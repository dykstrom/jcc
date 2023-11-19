package se.dykstrom.jcc.basic.code

import se.dykstrom.jcc.basic.compiler.BasicCodeGenerator
import se.dykstrom.jcc.basic.compiler.BasicTypeManager
import se.dykstrom.jcc.common.ast.FloatLiteral
import se.dykstrom.jcc.common.ast.IntegerLiteral
import se.dykstrom.jcc.common.ast.StringLiteral
import se.dykstrom.jcc.common.optimization.DefaultAstOptimizer
import se.dykstrom.jcc.common.symbols.SymbolTable
import se.dykstrom.jcc.common.types.Arr
import se.dykstrom.jcc.common.types.I64
import se.dykstrom.jcc.common.types.Identifier
import kotlin.test.assertNotNull

open class AbstractBasicCodeGeneratorComponentTests {

    protected val types = BasicTypeManager()
    protected val symbols: SymbolTable = SymbolTable()
    protected val codeGenerator = BasicCodeGenerator(types, symbols, DefaultAstOptimizer(types, symbols))

    /**
     * Asserts that the expected regex matches the actual string. If the regex contains
     * a group, this method returns the text that was matched by the group, otherwise null.
     */
    protected fun assertRegexMatches(expected: Regex, actual : String): String? {
        val matchResult = expected.matchEntire(actual)
        assertNotNull(matchResult, "\nExpected (regex) :${expected}\nActual (string)  :${actual}")
        return if (matchResult.groups.size > 1) matchResult.groups[1]?.value else null
    }

    companion object {
        val IDENT_I64_FOO = Identifier("foo", I64.INSTANCE)

        val IDENT_ARR_I64_ONE = Identifier("one", Arr.from(1, I64.INSTANCE))
        val IDENT_ARR_I64_TWO = Identifier("two", Arr.from(2, I64.INSTANCE))

        val IL_4 = IntegerLiteral(0, 0, 4)
        val IL_53 = IntegerLiteral(0, 0, 53)
        val FL_1_0 = FloatLiteral(0, 0, 1.0)
        val FL_0_5 = FloatLiteral(0, 0, 0.5)
        val SL_A = StringLiteral(0, 0, "a")
        val SL_B = StringLiteral(0, 0, "b")
    }
}
