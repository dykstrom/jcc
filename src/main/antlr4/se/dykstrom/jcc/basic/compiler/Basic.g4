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
   : line*
   ;

/* Statements */

line
   : NUMBER? stmtList
   ;

stmtList
   : stmtList COLON stmt
   | stmt
   ;

stmt
   : assignStmt
   | commentStmt
   | endStmt
   | gotoStmt
   | ifStmt
   | printStmt
   | whileStmt
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

ifStmt
   : ifGoto
   | ifThenSingle
   | ifThenBlock
   ;

ifGoto
   : IF expr GOTO NUMBER elseSingle?
   ;

ifThenSingle
   : IF expr THEN (NUMBER | stmtList) elseSingle?
   ;

elseSingle
   : ELSE (NUMBER | stmtList)
   ;

ifThenBlock
   : IF expr THEN line* elseIfBlock* elseBlock? endIf
   ;

elseIfBlock
   : NUMBER? ELSEIF expr THEN line*
   ;

elseBlock
   : NUMBER? ELSE line*
   ;

endIf
   : NUMBER? ENDIF
   | NUMBER? END IF
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

whileStmt
   : WHILE expr line* NUMBER? WEND
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
   | term BACKSLASH factor
   | term MOD factor
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

ELSE
   : 'ELSE' | 'else'
   ;

ELSEIF
   : 'ELSEIF' | 'elseif'
   ;

END
   : 'END' | 'end'
   ;

ENDIF
   : 'ENDIF' | 'endif'
   ;

FALSE
   : 'FALSE' | 'false'
   ;

GOTO
   : 'GOTO' | 'goto'
   ;

IF
   : 'IF' | 'if'
   ;

LET
   : 'LET' | 'let'
   ;

MOD
   : 'MOD' | 'mod'
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

THEN
   : 'THEN' | 'then'
   ;

TRUE
   : 'TRUE' | 'true'
   ;

WHILE
   : 'WHILE' | 'while'
   ;

WEND
   : 'WEND' | 'wend'
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

BACKSLASH
   : '\\'
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
