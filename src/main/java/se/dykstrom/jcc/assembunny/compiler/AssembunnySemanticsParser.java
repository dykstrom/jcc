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

import static java.util.stream.Collectors.toList;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import se.dykstrom.jcc.assembunny.ast.JnzStatement;
import se.dykstrom.jcc.common.ast.Program;
import se.dykstrom.jcc.common.ast.Statement;
import se.dykstrom.jcc.common.compiler.AbstractSemanticsParser;

/**
 * The semantics parser for the Assembunny language. This parser makes sure that the program is semantically correct.
 * The parser returns a copy of the program, where all jumps are guaranteed to target existing lines.
 *
 * @author Johan Dykstrom
 */
class AssembunnySemanticsParser extends AbstractSemanticsParser {

    /** A set of all line numbers used in the program. */
    private final Set<String> lineNumbers = new HashSet<>();

    public Program program(Program program) {
        program.getStatements().forEach(this::lineNumber);
        List<Statement> statements = program.getStatements().stream().map(this::statement).collect(toList());
        return program.withStatements(statements);
    }

    /**
     * Save line number of statement to the set of line numbers.
     */
    private void lineNumber(Statement statement) {
        lineNumbers.add(statement.getLabel());
    }

    private Statement statement(Statement statement) {
        if (statement instanceof JnzStatement) {
            return jnzStatement((JnzStatement) statement);
        } else {
            return statement;
        }
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
