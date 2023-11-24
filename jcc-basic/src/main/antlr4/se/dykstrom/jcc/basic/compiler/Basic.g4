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

    public boolean isFnIdent(String s) {
        return s.startsWith("FN") || s.startsWith("Fn") || s.startsWith("fn");
    }
}

/* Top rule */

program
   : line*
   ;

/* Statements */

line
   : labelOrNumberDef? stmtList
   ;

stmtList
   : stmtList COLON stmt
   | stmt
   ;

stmt
   : assignStmt
   | clsStmt
   | commentStmt
   | constStmt
   | defFnStmt
   | defTypeStmt
   | dimStmt
   | endStmt
   | gosubStmt
   | gotoStmt
   | ifStmt
   | lineInputStmt
   | onGosubStmt
   | onGotoStmt
   | optionBaseStmt
   | printStmt
   | randomizeStmt
   | returnStmt
   | swapStmt
   | whileStmt
   ;

assignStmt
   : LET? identExpr '=' expr
   ;

clsStmt
   : CLS
   ;

commentStmt
   : COMMENT
   | APOSTROPHE
   | REM
   ;

constStmt
   : CONST constDeclList
   ;

constDeclList
   : constDeclList COMMA constDecl
   | constDecl
   ;

constDecl
   : ident '=' expr
   ;

defFnStmt
   : DEF ident { isFnIdent($ident.text) }? (OPEN (paramDecl (COMMA paramDecl)*)? CLOSE)? '=' expr
   ;

paramDecl
   /* Unlike in varDecl, the type is optional here. */
   : ident (AS (TYPE_DOUBLE | TYPE_INTEGER | TYPE_STRING))?
   ;

defTypeStmt
   : DEFDBL letterList
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
   : ident AS (TYPE_DOUBLE | TYPE_INTEGER | TYPE_STRING)
   | ident OPEN subscriptList CLOSE AS (TYPE_DOUBLE | TYPE_INTEGER | TYPE_STRING)
   ;

subscriptList
   : subscriptList COMMA subscriptDecl
   | subscriptDecl
   ;

subscriptDecl
   : addSubExpr
   ;

endStmt
   : END
   ;

gosubStmt
   : GOSUB labelOrNumber
   ;

gotoStmt
   : GOTO labelOrNumber
   ;

ifStmt
   : ifGoto
   | ifThenSingle
   | ifThenBlock
   ;

ifGoto
   : IF expr GOTO labelOrNumber elseSingle?
   ;

ifThenSingle
   : IF expr THEN (labelOrNumber | stmtList) elseSingle?
   ;

elseSingle
   : ELSE (labelOrNumber | stmtList)
   ;

ifThenBlock
   : IF expr THEN line* elseIfBlock* elseBlock? endIf
   ;

elseIfBlock
   : labelOrNumberDef? ELSEIF expr THEN line*
   ;

elseBlock
   : labelOrNumberDef? ELSE line*
   ;

endIf
   : labelOrNumberDef? END IF
   ;

lineInputStmt
   : LINE INPUT SEMICOLON? prompt? ident
   ;

prompt
   : STRING (SEMICOLON | COMMA)
   ;

onGosubStmt
   : ON expr GOSUB labelOrNumberList
   ;

onGotoStmt
   : ON expr GOTO labelOrNumberList
   ;

labelOrNumberList
   : labelOrNumberList COMMA labelOrNumber
   | labelOrNumber
   ;

optionBaseStmt
   : OPTION BASE NUMBER
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

randomizeStmt
   : RANDOMIZE expr?
   ;

returnStmt
   : RETURN
   ;

swapStmt
   : SWAP identExpr COMMA identExpr
   ;

whileStmt
   : WHILE expr line* labelOrNumberDef? WEND
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
   : MINUS factor
   | OPEN expr CLOSE
   | functionCall
   | ident
   | string
   | floating
   | integer
   ;

functionCall
   : ident OPEN exprList CLOSE
   | ident OPEN CLOSE
   ;

exprList
   : exprList COMMA expr
   | expr
   ;

identExpr
   : ident
   | arrayElement
   ;

arrayElement
   : ident OPEN subscriptList CLOSE
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

ident
   : ID
   ;

labelOrNumber
   : ID
   | NUMBER
   ;

labelOrNumberDef
   : ID COLON
   | NUMBER
   ;

/* Reserved words */

AND
   : 'AND' | 'And' | 'and'
   ;

AS
   : 'AS' | 'As' | 'as'
   ;

BASE
   : 'BASE' | 'Base' | 'base'
   ;

CLS
   : 'CLS' | 'Cls' | 'cls'
   ;

CONST
   : 'CONST' | 'Const' | 'const'
   ;

DEF
   : 'DEF' | 'Def' | 'def'
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

GOSUB
   : 'GOSUB' | 'Gosub' | 'gosub'
   ;

GOTO
   : 'GOTO' | 'Goto' | 'goto'
   ;

IF
   : 'IF' | 'If' | 'if'
   ;

INPUT
   : 'INPUT' | 'Input' | 'input'
   ;

LET
   : 'LET' | 'Let' | 'let'
   ;

LINE
   : 'LINE' | 'Line' | 'line'
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

OPTION
   : 'OPTION' | 'Option' | 'option'
   ;

OR
   : 'OR' | 'Or' | 'or'
   ;

PRINT
   : 'PRINT' | 'Print' | 'print'
   ;

RANDOMIZE
   : 'RANDOMIZE' | 'Randomize' | 'randomize'
   ;

REM
   : 'REM' | 'Rem' | 'rem'
   ;

RETURN
   : 'RETURN' | 'Return' | 'return'
   ;

SWAP
   : 'SWAP' | 'Swap' | 'swap'
   ;

THEN
   : 'THEN' | 'Then' | 'then'
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

SEMICOLON
   : ';'
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
