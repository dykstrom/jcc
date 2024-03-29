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
   : FUN ident OPEN (ident AS type (COMMA ident AS type)*)? CLOSE ARROW returnType EQ expr
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
   : addSubExpr
   ;

addSubExpr
   : addSubExpr PLUS term
   | addSubExpr MINUS term
   | term
   ;

term
   : term STAR factor
   | term SLASH factor
   | term DIV factor
   | term MOD factor
   | factor
   ;

factor
   : MINUS factor
   | OPEN expr CLOSE
   | ident
   | functionCall
   | integerLiteral
   | floatLiteral
   ;

functionCall
   : ident OPEN (expr (COMMA expr)*)? CLOSE
   ;

integerLiteral
   : NUMBER
   ;

floatLiteral
   : FLOAT_NUMBER
   ;

ident
   : ID
   ;

libFunIdent
   : LIB_FUN_ID
   ;

/* Reserved words */

ALIAS : 'alias' ;

AS : 'as' ;

DIV : 'div' ;

FUN : 'fun' ;

IMPORT : 'import' ;

MOD : 'mod' ;

PRINTLN : 'println' ;

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
   : NUMBER? '.' NUMBER EXPONENT?
   | NUMBER '.' EXPONENT?
   | NUMBER EXPONENT
   ;

fragment
EXPONENT
   : 'E' SIGN? NUMBER
   ;

fragment
SIGN
   : '+' | '-'
   ;

/* Symbols */

ARROW : '-' '>' ;

CLOSE : ')' ;

COLON : ':' ;

COMMA : ',' ;

DOT : '.' ;

EQ : '=' ;

MINUS : '-' ;

OPEN : '(' ;

PLUS : '+' ;

SLASH : '/' ;

STAR : '*' ;

UNDERSCORE : '_' ;

COMMENT
   : '//' ~[\r\n]* -> skip
   ;

WS
   : [ \r\n] -> skip
   ;
