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

/* Helper methods */

@members {
    public boolean isSingleLetter(String s) {
        return s.length() == 1;
    }
}

/* Top rule */

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
   | defStmt
   | dimStmt
   | endStmt
   | gosubStmt
   | gotoStmt
   | ifStmt
   | onGosubStmt
   | onGotoStmt
   | printStmt
   | returnStmt
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

defStmt
   : DEFBOOL letterList
   | DEFDBL letterList
   | DEFINT letterList
   | DEFSTR letterList
   ;

letterList
   : letterList COMMA letterInterval
   | letterInterval
   ;

letterInterval
   : ident { isSingleLetter($ident.text) }? MINUS ident { isSingleLetter($ident.text) }?
   | ident { isSingleLetter($ident.text) }?
   ;

dimStmt
   : DIM varDeclList
   ;

varDeclList
   : varDeclList COMMA varDecl
   | varDecl
   ;

varDecl
   : ident AS (TYPE_BOOLEAN | TYPE_DOUBLE | TYPE_INTEGER | TYPE_STRING)
   ;

endStmt
   : END
   ;

gosubStmt
   : GOSUB NUMBER
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

onGosubStmt
   : ON expr GOSUB numberList
   ;

onGotoStmt
   : ON expr GOTO numberList
   ;

numberList
   : numberList ',' NUMBER
   | NUMBER
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

returnStmt
   : RETURN
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
   | orExpr XOR andExpr
   | andExpr
   ;

andExpr
   : andExpr AND notExpr
   | notExpr
   ;

notExpr
   : NOT relExpr
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
   : MINUS expr
   | OPEN expr CLOSE
   | functionCall
   | ident
   | string
   | floating
   | integer
   | bool
   ;

functionCall
   : ident OPEN exprList CLOSE
   | ident OPEN CLOSE
   ;

exprList
   : exprList COMMA expr
   | expr
   ;

string
   : STRING
   ;

floating
   : FLOATNUMBER
   ;

integer
   : HEXNUMBER
   | OCTNUMBER
   | BINNUMBER
   | NUMBER
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
   : 'AND' | 'And' | 'and'
   ;

AS
   : 'AS' | 'As' | 'as'
   ;

DEFBOOL
   : 'DEFBOOL' | 'Defbool' | 'defbool'
   ;

DEFDBL
   : 'DEFDBL' | 'Defdbl' | 'defdbl'
   ;

DEFINT
   : 'DEFINT' | 'Defint' | 'defint'
   ;

DEFSTR
   : 'DEFSTR' | 'Defstr' | 'defstr'
   ;

DIM
   : 'DIM' | 'Dim' | 'dim'
   ;

ELSE
   : 'ELSE' | 'Else' | 'else'
   ;

ELSEIF
   : 'ELSEIF' | 'Elseif' | 'elseif'
   ;

END
   : 'END' | 'End' | 'end'
   ;

ENDIF
   : 'ENDIF' | 'Endif' | 'endif'
   ;

FALSE
   : 'FALSE' | 'False' | 'false'
   ;

GOSUB
   : 'GOSUB' | 'Gosub' | 'gosub'
   ;

GOTO
   : 'GOTO' | 'Goto' | 'goto'
   ;

IF
   : 'IF' | 'If' | 'if'
   ;

LET
   : 'LET' | 'Let' | 'let'
   ;

MOD
   : 'MOD' | 'Mod' | 'mod'
   ;

NOT
   : 'NOT' | 'Not' | 'not'
   ;

ON
   : 'ON' | 'On' | 'on'
   ;

OR
   : 'OR' | 'Or' | 'or'
   ;

PRINT
   : 'PRINT' | 'Print' | 'print'
   ;

REM
   : 'REM' | 'Rem' | 'rem'
   ;

RETURN
   : 'RETURN' | 'Return' | 'return'
   ;

THEN
   : 'THEN' | 'Then' | 'then'
   ;

TRUE
   : 'TRUE' | 'True' | 'true'
   ;

TYPE_BOOLEAN
   : 'BOOLEAN' | 'Boolean' | 'boolean'
   ;

TYPE_DOUBLE
   : 'DOUBLE' | 'Double' | 'double'
   ;

TYPE_INTEGER
   : 'INTEGER' | 'Integer' | 'integer'
   ;

TYPE_STRING
   : 'STRING' | 'String' | 'string'
   ;

WHILE
   : 'WHILE' | 'While' | 'while'
   ;

WEND
   : 'WEND' | 'Wend' | 'wend'
   ;

XOR
   : 'XOR' | 'Xor' | 'xor'
   ;

/* Literals */

ID
   : LETTERS (LETTERS | NUMBER | DOT)* (PERCENT | DOLLAR | HASH)?
   ;

NUMBER
   : [0-9]+
   ;

HEXNUMBER
   : AMPERSAND 'H' [0-9A-F]+
   ;

OCTNUMBER
   : AMPERSAND 'O' [0-7]+
   ;

BINNUMBER
   : AMPERSAND 'B' [0-1]+
   ;

FLOATNUMBER
   : FRACTNUMBER EXPONENT? FLOATSUFFIX?
   | NUMBER EXPONENT FLOATSUFFIX?
   | NUMBER FLOATSUFFIX
   ;

FRACTNUMBER
   : NUMBER? '.' NUMBER
   | NUMBER '.'
   ;

fragment
EXPONENT
   : [deDE]+ SIGN? NUMBER
   ;

fragment
SIGN
   : '+' | '-'
   ;

fragment
FLOATSUFFIX
   : '#'
   ;

LETTERS
   : [a-zA-Z]+
   ;

STRING
   : '"' ~ ["\r\n]* '"'
   ;

/* Comments */

COMMENT
   : (APOSTROPHE | REM) ~[\r\n]*
   ;

/* Symbols */

AMPERSAND
   : '&'
   ;

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

COMMA
   : ','
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

HASH
   : '#'
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
