/*
 * Copyright (C) 2016 Johan Dykstrom
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

grammar Basic;

program
   : line_list
   ;

/* Lines */

line_list
   : line_list line
   | line
   ;

line
   : NUMBER stmt_list
   ;

/* Statements */

stmt_list
   : stmt_list COLON stmt
   | stmt
   ;

stmt
   : end_stmt
   | goto_stmt
   | print_stmt
   ;

end_stmt
   : END
   ;

goto_stmt
   : GOTO NUMBER
   ;

print_stmt
   : PRINT print_list
   | PRINT
   ;

print_list
   : print_list print_sep expr
   | expr
   ;

print_sep
   : ','
   | ';'
   ;

/* Expressions */

expr
   : expr PLUS term
   | expr MINUS term
   | term
   ;

term
   : term STAR factor
   | term SLASH factor
   | factor
   ;

factor
   : string
   | integer
   ;

string
   : STRING
   ;

integer
   : MINUS? NUMBER
   ;

/* Reserved words */

END
   : 'END' | 'end'
   ;

GOTO
   : 'GOTO' | 'goto'
   ;

PRINT
   : 'PRINT' | 'print'
   ;

/* Literals */

NUMBER
   : ('0' .. '9')+
   ;

STRING
   : '"' ~ ["\r\n]* '"'
   ;

/* Symbols */

COLON
   : ':'
   ;

MINUS
   : '-'
   ;

PLUS
   : '+'
   ;

SLASH
   : '/'
   ;

STAR
   : '*'
   ;

WS
   : [ \r\n] -> skip
   ;
