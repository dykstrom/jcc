/*
 * Copyright (C) 2017 Johan Dykstrom
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

package se.dykstrom.jcc.assembunny.compiler;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import se.dykstrom.jcc.assembunny.ast.JnzStatement;
import se.dykstrom.jcc.common.ast.LabelledStatement;
import se.dykstrom.jcc.common.ast.AstProgram;
import se.dykstrom.jcc.common.ast.Statement;
import se.dykstrom.jcc.common.compiler.AbstractSemanticsParser;
import se.dykstrom.jcc.common.compiler.TypeManager;
import se.dykstrom.jcc.common.error.CompilationErrorListener;
import se.dykstrom.jcc.common.error.SemanticsException;
import se.dykstrom.jcc.common.symbols.SymbolTable;

/**
 * The semantics parser for the Assembunny language. This parser makes sure that the program is semantically correct.
 * The parser returns a copy of the program, where all jumps are guaranteed to target existing lines.
 *
 * @author Johan Dykstrom
 */
public class AssembunnySemanticsParser extends AbstractSemanticsParser<TypeManager> {

    /** A set of all line numbers used in the program. */
    private final Set<String> lineNumbers = new HashSet<>();

    public AssembunnySemanticsParser(final CompilationErrorListener errorListener,
                                     final SymbolTable symbolTable,
                                     final TypeManager typeManager) {
        super(errorListener, symbolTable, typeManager);
    }

    @Override
    public AstProgram parse(final AstProgram program) throws SemanticsException {
        program.getStatements().forEach(this::lineNumber);
        List<Statement> statements = program.getStatements().stream().map(this::statement).toList();
        return program.withStatements(statements);
    }

    /**
     * Save line number of statement to the set of line numbers.
     */
    private void lineNumber(Statement statement) {
        lineNumbers.add(((LabelledStatement) statement).label());
    }

    @Override
    public Statement statement(Statement statement) {
        if (statement instanceof JnzStatement jnzStatement) {
            return jnzStatement(jnzStatement);
        } else if (statement instanceof LabelledStatement labelledStatement) {
            return labelledStatement(labelledStatement);
        } else {
            return statement;
        }
    }

    private Statement labelledStatement(LabelledStatement labelledStatement) {
        return labelledStatement.withStatement(statement(labelledStatement.statement()));
    }

    private JnzStatement jnzStatement(JnzStatement statement) {
        String line = statement.getTarget();
        if (lineNumbers.contains(line)) {
            return statement;
        } else {
            return statement.withTarget(AssembunnyUtils.END_JUMP_TARGET);
        }
    }
}
