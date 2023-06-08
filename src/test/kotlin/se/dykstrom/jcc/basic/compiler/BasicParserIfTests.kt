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
package se.dykstrom.jcc.basic.compiler

import org.junit.Test
import se.dykstrom.jcc.common.utils.FormatUtils.EOL

class BasicParserIfTests : AbstractBasicParserTests() {

    @Test
    fun shouldParseIfGotoLine() {
        parse("10 if 5 goto 20")
    }

    @Test
    fun shouldParseIfThenLine() {
        parse("10 if -1 then 100")
    }

    @Test
    fun shouldParseIfThenStatements() {
        parse("10 if -1 then goto 17")
        parse("10 if -1 then print \"A\"")
        parse("10 if -1 then x = 5 : print x")
        parse("10 if -1 then print 17 : goto 10")
        parse("10 if -1 then let b = 1 <> 0 : print \"b = \"; b : goto 10")
    }

    @Test
    fun shouldParseIfThenNestedIfThen() {
        parse("10 if -1 then if 0 then print -1; 0")
        parse("10 if x > 0 then if y > 0 then print \"both > 0\"")
        parse("10 if x > 0 then print 1 : if y > 0 then print 2")
    }

    @Test
    fun shouldParseIfGotoLineElseLine() {
        parse("10 if 10 goto 20 else 30")
    }

    @Test
    fun shouldParseIfThenLineElseLine() {
        parse("10 if -1 then 100 else 123")
    }

    @Test
    fun shouldParseIfThenLineElseStatements() {
        parse("10 if -1 then 100 else print 1 : print 2 : end")
        parse("10 if 0 <> 0 then 100 else let x$ = \"X\" : print \"x$ = \", x$")
        parse("10 if 1 or 2 then 100 else a = 1 : b = 2 : c = 3 : d = a + b + c : print d")
    }

    @Test
    fun shouldParseIfThenStatementsElseStatements() {
        parse("10 if -1 then goto 17 else goto 71")
        parse("10 if -1 then print \"A\" else print \"B\"")
        parse("10 if -1 then x = 5 : print x else x = 7 : print x")
        parse("10 if -1 then print 17 : goto 10 else goto 20")
        parse("10 if -1 then let b = 1 <> 0 : print \"b = \"; b : goto 10 else let b = -1 : let c = 0 : print b and c : goto 10")
    }

    @Test
    fun shouldParseIfThenElseNestedIfThenElse() {
        parse("10 if -1 then if 0 then print -1; 0 else print 0; -1")
        parse("10 if x > 0 then if y > 0 then print \"both > 0\" else if y < 0 then print 0")
        parse("10 if x > 0 then print 1 : if x < 0 then print 2 : print 3 else print 4")
    }

    @Test
    fun shouldParseIfThenBlock() {
        parse("10 if -1 then" + EOL + "20 print 5" + EOL + "30 end if")
        parse("10 if x = 0 then" + EOL + "20 x = x + 1" + EOL + "30 print x" + EOL + "40 end if" + EOL + "50 print x")
        parse("if -1 then" + EOL + "print 5" + EOL + "end if")
        parse("if 1 <> 1 then" + EOL + "print 5" + EOL + "end if")
        parse("10 if -1 then" + EOL + "print 5" + EOL + "print 7" + EOL + "20 end if")
        parse("10 if -1 then" + EOL + "20 end if")
        parse("if 1<>1 then" + EOL + "end if")
    }

    @Test
    fun shouldParseIfThenBlockWithEndStatement() {
        parse("""
                if -1 then
                    print 1
                    end
                end if
                """)
    }

    @Test
    fun shouldParseIfThenBlockElseBlock() {
        parse("10 if -1 then" + EOL + "20 print 5" + EOL + "30 else" + EOL + "40 print 2" + EOL + "50 end if")
        parse("10 if 1 and 0 then" + EOL + "20 print 5 : goto 50" + EOL + "30 else" + EOL + "40 print 2" + EOL + "45 print 3" + EOL + "50 end if")
        parse("if 0 then" + EOL + "print 5" + EOL + "else" + EOL + "print 2" + EOL + "end if")
        parse("if 0 then" + EOL + "else" + EOL + "end if")
    }

    @Test
    fun shouldParseIfThenElseIfThen() {
        parse(
            "10 if -1 then" + EOL +
                    "20 print 5" + EOL +
                    "30 elseif 0 then" + EOL +
                    "40 print 2" + EOL +
                    "50 end if"
        )
        parse(
            "10 if 1 + 1 < 2 then" + EOL +
                    "20   print \"less\"" + EOL +
                    "30 elseif 1 + 1 > 2 then" + EOL +
                    "40   print \"more\"" + EOL +
                    "50 elseif 1 + 1 = 2 then" + EOL +
                    "60   print \"equal\"" + EOL +
                    "70 end if"
        )
        parse(
            "if 1 + 1 < 2 then" + EOL +
                    "  print 1" + EOL +
                    "  print 2" + EOL +
                    "  end" + EOL +
                    "elseif 1 + 1 > 2 then" + EOL +
                    "  print 3; 4; 5 : print 6; 7; 8 : end" + EOL +
                    "elseif 1 + 1 = 2 then" + EOL +
                    "  end" + EOL +
                    "end if"
        )
        parse(
            "if -1 then" + EOL +
                    "  rem -1" + EOL +
                    "  print 1" + EOL +
                    "  print 2" + EOL +
                    "  end" + EOL +
                    "elseif 0 then" + EOL +
                    "  rem 0" + EOL +
                    "  print 3; 4; 5 : print 6; 7; 8 : end" + EOL +
                    "else" + EOL +
                    "  rem this is the end" + EOL +
                    "  end" + EOL +
                    "end if"
        )
        parse(
            "10 if -1 then" + EOL +
                    "20 elseif 0 then" + EOL +
                    "30 else" + EOL +
                    "40 end if"
        )
    }

    // Negative tests:
    @Test(expected = IllegalStateException::class)
    fun shouldNotParseIfGotoWithoutLine() {
        parse("10 if -1 goto")
    }

    @Test(expected = IllegalStateException::class)
    fun shouldNotParseIfThenWithoutLine() {
        parse("10 if -1 then")
    }

    @Test(expected = IllegalStateException::class)
    fun shouldNotParseIfWithoutThen() {
        parse("10 if -1")
    }

    @Test(expected = IllegalStateException::class)
    fun shouldNotParseIfWithoutCondition() {
        parse("10 if then 10")
    }

    @Test(expected = IllegalStateException::class)
    fun shouldNotParseIfThenElseWithoutElseClause() {
        parse("10 if 5 then print 1 else")
    }

    @Test(expected = IllegalStateException::class)
    fun shouldNotParseIfThenElseWithoutThenClause() {
        parse("10 if 5 then else print 1")
    }

    @Test(expected = IllegalStateException::class)
    fun shouldNotParseElseBlockWithoutEnd() {
        parse("if 5 then print 1 print 2 else print 3")
    }

    @Test(expected = IllegalStateException::class)
    fun shouldNotParseElseIfBlockWithoutEnd() {
        parse("if 5 then print 1 elseif 8 then print 3")
    }

    @Test(expected = IllegalStateException::class)
    fun shouldNotParseElseIfBlockWithoutThen() {
        parse("if 5 then print 1 elseif 8 print 3 end if")
    }
}
