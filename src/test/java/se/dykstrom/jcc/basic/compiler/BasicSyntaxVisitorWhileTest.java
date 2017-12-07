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
import se.dykstrom.jcc.common.ast.*;

/**
 * Tests class {@code BasicSyntaxVisitor}, especially functionality related to WHILE statements.
 * 
 * @author Johan Dykstrom
 * @see BasicSyntaxVisitor
 */
public class BasicSyntaxVisitorWhileTest extends AbstractBasicSyntaxVisitorTest {

    @Test
    public void shouldParseEmptyWhile() throws Exception {
        Statement ws = new WhileStatement(0, 0, IL_5, emptyList());
        List<Statement> expectedStatements = singletonList(ws);

        parseAndAssert("while 5 wend", expectedStatements);
    }

    @Test
    public void shouldParseSimpleWhile() throws Exception {
        Statement ps = new PrintStatement(0, 0, singletonList(BL_TRUE));
        Statement ws = new WhileStatement(0, 0, BL_TRUE, singletonList(ps));
        List<Statement> expectedStatements = singletonList(ws);

        parseAndAssert("while true print true wend", expectedStatements);
    }

    @Test
    public void shouldParseWhileWend() throws Exception {
        Statement cs = new CommentStatement(0, 0, "WEND", "30");
        Statement ps = new PrintStatement(0, 0, singletonList(BL_TRUE), "20");
        Statement ws = new WhileStatement(0, 0, BL_TRUE, asList(ps, cs), "10");
        List<Statement> expectedStatements = singletonList(ws);

        parseAndAssert("10 while true " +
                       "20   print true " +
                       "30 wend", expectedStatements);
    }

    @Test
    public void shouldParseNestedWhile() throws Exception {
        Expression equalExpr = new EqualExpression(0, 0, IDE_B, IL_3);
        Statement ps = new PrintStatement(0, 0, singletonList(BL_TRUE));
        Statement innerWhile = new WhileStatement(0, 0, equalExpr, singletonList(ps));
        Expression notEqualExpr = new NotEqualExpression(0, 0, IDE_A, IL_4);
        Statement outerWhile = new WhileStatement(0, 0, notEqualExpr, singletonList(innerWhile));
        List<Statement> expectedStatements = singletonList(outerWhile);

        parseAndAssert("while a% <> 4 " +
                       "  while b% = 3 " +
                       "    print true " +
                       "  wend " +
                       "wend", expectedStatements);
    }
}
