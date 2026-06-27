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
   | valStmt
   | whileStmt
   ;

aliasStmt
   : ALIAS ident AS type
   ;

functionCallStmt
   : CALL functionCall
   | functionCall
   ;

functionDefinitionStmt
   : FUN ident OPEN (ident (AS type)? (COMMA ident (AS type)?)*)? CLOSE (ARROW returnType)? ASSIGN expr
   ;

importStmt
   : IMPORT libFunIdent funType (AS ident)?
   ;

valStmt
   : VAL ident (AS type)? (ASSIGN expr)?
   ;

whileStmt
   : WHILE expr DO stmt* END
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
   : relExpr EQ addExpr
   | relExpr GE addExpr
   | relExpr GT addExpr
   | relExpr LE addExpr
   | relExpr LT addExpr
   | relExpr NE addExpr
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
   | BECOME functionCall
   | OPEN expr CLOSE
   | booleanLiteral
   | floatLiteral
   | integerLiteral
   | ident
   | functionCall
   | ifExpr
   ;

booleanLiteral
   : TRUE
   | FALSE
   ;

floatLiteral
   : FLOAT_NUMBER
   | DEC_NUMBER_FLOAT_TYPED
   | MALFORMED_FLOAT
   ;

integerLiteral
   : BIN_NUMBER
   | HEX_NUMBER
   | DEC_NUMBER
   | DEC_NUMBER_TYPED
   ;

ident
   : ID
   ;

functionCall
   : ident OPEN (expr (COMMA expr)*)? CLOSE
   ;

ifExpr
   : IF expr THEN expr (ELSE expr)?
   ;

libFunIdent
   : LIB_FUN_ID
   ;

/* Reserved words */

ALIAS : 'alias' ;

AND : 'and' ;

AS : 'as' ;

BECOME : 'become' ;

CALL : 'call' ;

DIV : 'div' ;

DO : 'do' ;

ELSE : 'else' ;

END : 'end' ;

FALSE : 'false' ;

FUN : 'fun' ;

IF : 'if' ;

IMPORT : 'import' ;

MOD : 'mod' ;

NOT : 'not' ;

OR : 'or' ;

THEN : 'then' ;

TRUE : 'true' ;

VAL : 'val' ;

WHILE : 'while' ;

XOR : 'xor' ;

/* Literals */

ID
   : LETTERS (LETTERS | DEC_NUMBER | UNDERSCORE)*
   ;

LIB_FUN_ID
   : LETTERS (LETTERS | DEC_NUMBER | UNDERSCORE)* DOT (LETTERS | DEC_NUMBER | UNDERSCORE)+
   ;

BIN_NUMBER
   : '0' 'b' [01_]+
   ;

DEC_NUMBER
   : [0-9_]+
   ;

DEC_NUMBER_TYPED
   : DEC_NUMBER INT_SUFFIX
   ;

DEC_NUMBER_FLOAT_TYPED
   : DEC_NUMBER FLOAT_SUFFIX
   ;

HEX_NUMBER
   : '0' 'x' [0-9a-fA-F_]+
   ;

LETTERS
   : [a-zA-Z]+
   ;

FLOAT_NUMBER
   : DEC_NUMBER DOT DEC_NUMBER EXPONENT? FLOAT_SUFFIX?
   | DEC_NUMBER EXPONENT FLOAT_SUFFIX?
   ;

// A decimal point with digits on only one side, e.g. '.99' or '17.'. Rejected in semantic
// analysis with a message naming the rule; valid floats match the longer FLOAT_NUMBER first.
MALFORMED_FLOAT
   : DOT DEC_NUMBER
   | DEC_NUMBER DOT
   ;

fragment
EXPONENT
   : [eE] SIGN? DEC_NUMBER
   ;

fragment
FLOAT_SUFFIX
   : 'f32' | 'f64'
   ;

fragment
INT_SUFFIX
   : 'i32' | 'i64'
   ;

fragment
SIGN
   : PLUS | MINUS
   ;

/* Symbols */

AMPERSAND : '&' ;

ARROW : '->' ;

ASSIGN : ':=' ;

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
   : [ \t\r\n] -> skip
   ;
