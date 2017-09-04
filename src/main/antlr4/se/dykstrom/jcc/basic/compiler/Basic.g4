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
   : lineList
   ;

/* Lines */

lineList
   : lineList line
   | line
   ;

line
   : NUMBER stmtList
   ;

/* Statements */

stmtList
   : stmtList COLON stmt
   | stmt
   ;

stmt
   : assignStmt
   | commentStmt
   | endStmt
   | gotoStmt
   | printStmt
   ;

assignStmt
   : LET? ident '=' expr
   ;

commentStmt
   : COMMENT
   | APOSTROPHE
   | REM
   ;

endStmt
   : END
   ;

gotoStmt
   : GOTO NUMBER
   ;

printStmt
   : PRINT printList
   | PRINT
   ;

printList
   : printList printSep expr
   | expr
   ;

printSep
   : ','
   | ';'
   ;

/* Expressions */

expr
   : orExpr
   ;

orExpr
   : orExpr OR andExpr
   | andExpr
   ;

andExpr
   : andExpr AND relExpr
   | relExpr
   ;

relExpr
   : addSubExpr EQ addSubExpr
   | addSubExpr GE addSubExpr
   | addSubExpr GT addSubExpr
   | addSubExpr LE addSubExpr
   | addSubExpr LT addSubExpr
   | addSubExpr NE addSubExpr
   | addSubExpr
   ;

addSubExpr
   : addSubExpr PLUS term
   | addSubExpr MINUS term
   | term
   ;

term
   : term STAR factor
   | term SLASH factor
   | factor
   ;

factor
   : OPEN expr CLOSE
   | ident
   | string
   | integer
   | bool
   ;

string
   : STRING
   ;

integer
   : MINUS? NUMBER
   ;

bool
   : TRUE
   | FALSE
   ;

ident
   : ID
   ;

/* Reserved words */

AND
   : 'AND' | 'and'
   ;

END
   : 'END' | 'end'
   ;

FALSE
   : 'FALSE' | 'false'
   ;

GOTO
   : 'GOTO' | 'goto'
   ;

LET
   : 'LET' | 'let'
   ;

OR
   : 'OR' | 'or'
   ;

PRINT
   : 'PRINT' | 'print'
   ;

REM
   : 'REM' | 'rem'
   ;

TRUE
   : 'TRUE' | 'true'
   ;

/* Literals */

ID
   : LETTERS (LETTERS | NUMBER | DOT)* (PERCENT | DOLLAR)?
   ;

NUMBER
   : ('0' .. '9')+
   ;

LETTERS
   : ('a' .. 'z' | 'A' .. 'Z')+
   ;

STRING
   : '"' ~ ["\r\n]* '"'
   ;

/* Comments */

COMMENT
   : (APOSTROPHE | REM) ~[\r\n]*
   ;

/* Symbols */

APOSTROPHE
   : '\''
   ;

CLOSE
   : ')'
   ;

COLON
   : ':'
   ;

DOLLAR
   : '$'
   ;

DOT
   : '.'
   ;

EQ
   : '='
   ;

GE
   : '>='
   ;

GT
   : '>'
   ;

LE
   : '<='
   ;

LT
   : '<'
   ;

MINUS
   : '-'
   ;

NE
   : '<>'
   ;

OPEN
   : '('
   ;

PERCENT
   : '%'
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
