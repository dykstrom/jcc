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

package se.dykstrom.jcc.basic.compiler;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import java.util.List;

import org.junit.Test;

import se.dykstrom.jcc.basic.ast.PrintStatement;
import se.dykstrom.jcc.common.ast.Expression;
import se.dykstrom.jcc.common.ast.FunctionCallExpression;
import se.dykstrom.jcc.common.ast.IdentifierDerefExpression;
import se.dykstrom.jcc.common.ast.Statement;

/**
 * Tests class {@code BasicSyntaxVisitor}, especially functionality related to function calls.
 * 
 * @author Johan Dykstrom
 * @see BasicSyntaxVisitor
 */
public class BasicSyntaxVisitorFunctionTest extends AbstractBasicSyntaxVisitorTest {

    @Test
    public void shouldParseCall() throws Exception {
        Expression fe = new FunctionCallExpression(0, 0, IDENT_UNK_FOO, emptyList());
        Statement ps = new PrintStatement(0, 0, singletonList(fe));
        List<Statement> expectedStatements = singletonList(ps);

        parseAndAssert("print foo()", expectedStatements);
    }

    @Test
    public void shouldParseCallWithoutParens() throws Exception {
        Expression ie = new IdentifierDerefExpression(0, 0, IDENT_UNK_FOO);
        Statement ps = new PrintStatement(0, 0, singletonList(ie));
        List<Statement> expectedStatements = singletonList(ps);

        parseAndAssert("print foo", expectedStatements);
    }

    @Test
    public void shouldParseCallWithTypedFunc() throws Exception {
        Expression fe = new FunctionCallExpression(0, 0, IDENT_STR_COMMAND, emptyList());
        Statement ps = new PrintStatement(0, 0, singletonList(fe));
        List<Statement> expectedStatements = singletonList(ps);

        parseAndAssert("print command$()", expectedStatements);
    }

    @Test
    public void shouldParseCallWithArg() throws Exception {
        Expression fe = new FunctionCallExpression(0, 0, IDENT_UNK_FOO, singletonList(IL_1));
        Statement ps = new PrintStatement(0, 0, singletonList(fe));
        List<Statement> expectedStatements = singletonList(ps);

        parseAndAssert("print foo(1)", expectedStatements);
    }

    @Test
    public void shouldParseCallWithSeveralArgs() throws Exception {
        List<Expression> expressions = asList(IL_1, SL_A, BL_FALSE);
        Expression fe = new FunctionCallExpression(0, 0, IDENT_UNK_FOO, expressions);
        Statement ps = new PrintStatement(0, 0, singletonList(fe));
        List<Statement> expectedStatements = singletonList(ps);

        parseAndAssert("print foo(1, \"A\", false)", expectedStatements);
    }

    @Test
    public void shouldParseCallWithFunCallArgs() throws Exception {
        Expression feBar12 = new FunctionCallExpression(0, 0, IDENT_UNK_BAR, asList(IL_1, IL_2));
        Expression feBar34 = new FunctionCallExpression(0, 0, IDENT_UNK_BAR, asList(IL_3, IL_4));
        Expression feFoo = new FunctionCallExpression(0, 0, IDENT_UNK_FOO, asList(feBar12, feBar34));
        Statement ps = new PrintStatement(0, 0, singletonList(feFoo));
        List<Statement> expectedStatements = singletonList(ps);

        parseAndAssert("print foo(bar(1, 2), bar(3, 4))", expectedStatements);
    }
}
