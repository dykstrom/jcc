/*
 * Copyright (C) 2023 Johan Dykstrom
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

grammar Col;

/* Top rule */

program
   : stmt*
   ;

/* Statements */

stmt
   : aliasStmt
   | functionCallStmt
   | functionDefinitionStmt
   | importStmt
   | printlnStmt
   ;

aliasStmt
   : ALIAS ident AS type
   ;

functionCallStmt
   : functionCall
   ;

functionDefinitionStmt
   : FUN ident OPEN (ident AS type (COMMA ident AS type)*)? CLOSE ARROW returnType ASSIGN expr
   ;

importStmt
   : IMPORT libFunIdent funType (AS ident)?
   ;

printlnStmt
   : PRINTLN expr?
   ;

/* Types */

returnType
   : type
   ;

type
   : funType
   | ident
   ;

funType
   : OPEN (type (COMMA type)*)? CLOSE (ARROW returnType)?
   ;

/* Expressions */

expr
   : orExpr
   ;

orExpr
   : orExpr BAR andExpr
   | orExpr CIRCUMFLEX andExpr
   | orExpr OR andExpr
   | orExpr XOR andExpr
   | andExpr
   ;

andExpr
   : andExpr AMPERSAND relExpr
   | andExpr AND relExpr
   | relExpr
   ;

relExpr
   : addExpr EQ addExpr
   | addExpr GE addExpr
   | addExpr GT addExpr
   | addExpr LE addExpr
   | addExpr LT addExpr
   | addExpr NE addExpr
   | addExpr
   ;

addExpr
   : addExpr PLUS term
   | addExpr MINUS term
   | term
   ;

term
   : term ASTERISK factor
   | term SLASH factor
   | term DIV factor
   | term MOD factor
   | factor
   ;

factor
   : MINUS factor
   | TILDE factor
   | NOT factor
   | OPEN expr CLOSE
   | booleanLiteral
   | floatLiteral
   | integerLiteral
   | ident
   | functionCall
   ;

booleanLiteral
   : TRUE
   | FALSE
   ;

floatLiteral
   : FLOAT_NUMBER
   ;

integerLiteral
   : NUMBER
   ;

ident
   : ID
   ;

functionCall
   : ident OPEN (expr (COMMA expr)*)? CLOSE
   ;

libFunIdent
   : LIB_FUN_ID
   ;

/* Reserved words */

ALIAS : 'alias' ;

AND : 'and' ;

AS : 'as' ;

DIV : 'div' ;

FALSE : 'false' ;

FUN : 'fun' ;

IMPORT : 'import' ;

MOD : 'mod' ;

NOT : 'not' ;

OR : 'or' ;

PRINTLN : 'println' ;

TRUE : 'true' ;

XOR : 'xor' ;

/* Literals */

ID
   : LETTERS (LETTERS | NUMBER | UNDERSCORE)*
   ;

LIB_FUN_ID
   : LETTERS (LETTERS | NUMBER | UNDERSCORE)* DOT (LETTERS | NUMBER | UNDERSCORE)+
   ;

NUMBER
   : [0-9_]+
   ;

LETTERS
   : [a-zA-Z]+
   ;

FLOAT_NUMBER
   : NUMBER? DOT NUMBER EXPONENT?
   | NUMBER DOT EXPONENT?
   | NUMBER EXPONENT
   ;

fragment
EXPONENT
   : 'E' SIGN? NUMBER
   ;

fragment
SIGN
   : PLUS | MINUS
   ;

/* Symbols */

AMPERSAND : '&' ;

ARROW : '->' ;

ASSIGN : '=' ;

ASTERISK : '*' ;

BAR : '|' ;

CIRCUMFLEX : '^' ;

CLOSE : ')' ;

COLON : ':' ;

COMMA : ',' ;

DOT : '.' ;

EQ : '==' ;

GE : '>=' ;

GT : '>' ;

LE : '<=' ;

LT : '<' ;

MINUS : '-' ;

NE : '!=' ;

OPEN : '(' ;

PLUS : '+' ;

SLASH : '/' ;

TILDE : '~' ;

UNDERSCORE : '_' ;

COMMENT
   : '//' ~[\r\n]* -> skip
   ;

WS
   : [ \r\n] -> skip
   ;
