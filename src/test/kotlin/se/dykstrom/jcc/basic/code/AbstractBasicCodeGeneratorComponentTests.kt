package se.dykstrom.jcc.basic.code

import se.dykstrom.jcc.basic.compiler.BasicCodeGenerator
import se.dykstrom.jcc.basic.compiler.BasicTypeManager
import se.dykstrom.jcc.common.code.Context
import se.dykstrom.jcc.common.optimization.DefaultAstOptimizer
import se.dykstrom.jcc.common.storage.StorageFactory
import se.dykstrom.jcc.common.types.Arr
import se.dykstrom.jcc.common.types.I64
import se.dykstrom.jcc.common.types.Identifier
import kotlin.test.assertTrue

open class AbstractBasicCodeGeneratorComponentTests {

    protected val types = BasicTypeManager()
    protected val storageFactory = StorageFactory()
    protected val astOptimizer = DefaultAstOptimizer(types)
    protected val codeGenerator = BasicCodeGenerator(types, astOptimizer)
    protected val context = Context(codeGenerator.symbols, types, storageFactory, codeGenerator)

    protected fun assertRegexMatches(expected: Regex, actual : String) {
        assertTrue(expected.matches(actual), "\nExpected (regex) :${expected}\nActual (string)  :${actual}")
    }

    protected fun address(identifier: Identifier) =
        if (identifier.type is Arr) """${identifier.mappedName}_arr\+8\*r[a-z0-9]+"""
        else identifier.mappedName

    companion object {
        val IDENT_I64_FOO = Identifier("foo", I64.INSTANCE)
    }
}
