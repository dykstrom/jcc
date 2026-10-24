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

package se.dykstrom.jcc.llvm.code

import se.dykstrom.jcc.common.ast.AssignStatement
import se.dykstrom.jcc.common.ast.AstProgram
import se.dykstrom.jcc.common.ast.IdentifierDerefExpression
import se.dykstrom.jcc.common.ast.ReturnStatement
import se.dykstrom.jcc.common.compiler.DefaultTypeManager
import se.dykstrom.jcc.common.optimization.DefaultAstOptimizer
import se.dykstrom.jcc.common.symbols.Scope.GLOBAL
import se.dykstrom.jcc.common.symbols.SymbolTable
import se.dykstrom.jcc.llvm.code.expression.IdentDerefCodeGenerator
import se.dykstrom.jcc.llvm.code.statement.AssignCodeGenerator

/**
 * A minimal code generator for testing the language-neutral code generators in jcc-llvm.
 * Variables are global, and all statements are wrapped in a main function. If the last
 * statement is a return statement, it replaces the default "return 0".
 */
class TestLlvmCodeGenerator(symbolTable: SymbolTable = SymbolTable()) :
    AbstractLlvmCodeGenerator(TYPE_MANAGER, symbolTable, DefaultAstOptimizer(TYPE_MANAGER, symbolTable)) {

    init {
        statementDictionary[AssignStatement::class.java] = AssignCodeGenerator(this, GLOBAL)
        expressionDictionary[IdentifierDerefExpression::class.java] = IdentDerefCodeGenerator(GLOBAL)
    }

    override fun generate(program: AstProgram): TargetProgram {
        val statements = program.statements
        val trailing = if (statements.lastOrNull() is ReturnStatement) listOf() else listOf(RETURN_I32_ZERO)
        val lines = mutableListOf<Line>()
        statement(generateMainFunction(statements, trailing), lines, symbolTable())
        lines.addAll(0, generateGlobals(symbolTable()))
        return TargetProgram(lines)
    }

    companion object {
        private val TYPE_MANAGER = DefaultTypeManager()
    }
}
