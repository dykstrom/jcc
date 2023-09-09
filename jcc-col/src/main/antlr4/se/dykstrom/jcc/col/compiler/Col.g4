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
   | printlnStmt
   ;

aliasStmt
   : ALIAS ident EQ type
   ;

printlnStmt
   : PRINTLN expr?
   ;

/* Types */

type
   : varType
   ;

varType
   : ident
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
   : factor
   ;

factor
   : functionCall
   | integerLiteral
   ;

functionCall
   : ident OPEN (expr (COMMA expr)*)? CLOSE
   ;

integerLiteral
   : NUMBER
   ;

ident
   : ID
   ;

/* Reserved words */

ALIAS
   : 'alias'
   ;

PRINTLN
   : 'println'
   ;

/* Literals */

ID
   : LETTERS (LETTERS | NUMBER)*
   ;

NUMBER
   : [0-9]+
   ;

LETTERS
   : [a-zA-Z]+
   ;

/* Symbols */

CLOSE
   : ')'
   ;

EQ
   : '='
   ;

COLON
   : ':'
   ;

COMMA
   : ','
   ;

MINUS
   : '-'
   ;

OPEN
   : '('
   ;

PLUS
   : '+'
   ;

COMMENT
   : '//' ~[\r\n]* -> skip
   ;

WS
   : [ \r\n] -> skip
   ;
