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

@parser::members {
    public boolean isSingleLetter(String s) {
        return s.length() == 1;
    }

    public boolean isFnIdent(String s) {
        return s.startsWith("FN") || s.startsWith("Fn") || s.startsWith("fn");
    }
}

@lexer::members {
    private int previousType = -1;

    /**
     * A source file need not end with a line break, but every grammar rule relies on
     * NEWLINE as its terminator. Synthesize one before EOF when the input does not
     * already end with a line break.
     */
    @Override
    public Token nextToken() {
        final Token token = super.nextToken();
        if (token.getType() == Token.EOF && previousType != NEWLINE && previousType != -1) {
            previousType = NEWLINE;
            final CommonToken newline = new CommonToken(token);
            newline.setType(NEWLINE);
            newline.setText("\n");
            return newline;
        }
        previousType = token.getType();
        return token;
    }
}

/* Top rule */

program
   : NEWLINE? line* EOF
   ;

/* Statements */

/*
 * A statement ends at the end of its line, as in QuickBASIC 4.5. A line may hold
 * several statements separated by COLON, and a label may stand alone on its own line.
 * A comment may trail the last statement without a COLON in front of it.
 */
line
   : labelOrNumberDef stmtList? commentStmt? NEWLINE
   | stmtList commentStmt? NEWLINE
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
   | sleepStmt
   | swapStmt
   | systemStmt
   | whileStmt
   ;

assignStmt
   : LET? identExpr EQ expr
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
   : CONST constDecl (COMMA constDecl)*
   ;

constDecl
   : ident EQ expr
   ;

defFnStmt
   : DEF ident { isFnIdent($ident.text) }? (OPEN (paramDecl (COMMA paramDecl)*)? CLOSE)? EQ expr
   ;

paramDecl
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
   : DIM varDecl (COMMA varDecl)*
   ;

varDecl
   /* Without an AS clause, the type comes from the type specifier, DEFtype, or the default type. */
   : ident (OPEN subscriptDecl (COMMA subscriptDecl)* CLOSE)? (AS (TYPE_DOUBLE | TYPE_INTEGER | TYPE_STRING))?
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

/*
 * The block form is listed before the single-line form so that THEN followed by a
 * comment resolves to the block form, as in QuickBASIC 4.5.
 */
ifStmt
   : ifGoto
   | ifThenBlock
   | ifThenSingle
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
   : IF expr THEN commentStmt? NEWLINE line* elseIfBlock* elseBlock? endIf
   ;

elseIfBlock
   : labelOrNumberDef? ELSEIF expr THEN commentStmt? NEWLINE line*
   ;

elseBlock
   : labelOrNumberDef? ELSE commentStmt? NEWLINE line*
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
   | PRINT printList printSep
   | PRINT
   ;

printList
   : printList printSep expr
   | expr
   ;

printSep
   : COMMA
   | SEMICOLON
   ;

/*
 * The QuickBASIC documentation states that expression that follows RANDOMIZE may
 * be any type of expression. But to simplify parsing we restrict the expression to
 * be an arithmetic expression.
 */
randomizeStmt
   : RANDOMIZE addSubExpr?
   ;

returnStmt
   : RETURN
   ;

sleepStmt
   : SLEEP addSubExpr?
   ;

swapStmt
   : SWAP identExpr COMMA identExpr
   ;

systemStmt
   : SYSTEM
   ;

whileStmt
   : WHILE expr commentStmt? NEWLINE line* labelOrNumberDef? WEND
   ;

/* Expressions */

expr
   : impExpr
   ;

impExpr
   : impExpr IMP eqvExpr
   | eqvExpr
   ;

eqvExpr
   : eqvExpr EQV xorExpr
   | xorExpr
   ;

xorExpr
   : xorExpr XOR orExpr
   | orExpr
   ;

orExpr
   : orExpr OR andExpr
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
   : relExpr EQ addSubExpr
   | relExpr GE addSubExpr
   | relExpr GT addSubExpr
   | relExpr LE addSubExpr
   | relExpr LT addSubExpr
   | relExpr NE addSubExpr
   | addSubExpr
   ;

addSubExpr
   : addSubExpr PLUS modExpr
   | addSubExpr MINUS modExpr
   | modExpr
   ;

modExpr
   : modExpr MOD iDivExpr
   | iDivExpr
   ;

iDivExpr
   : iDivExpr BACKSLASH mulDivExpr
   | mulDivExpr
   ;

mulDivExpr
   : mulDivExpr STAR factor
   | mulDivExpr SLASH factor
   | factor
   ;

factor
   : factor CIRCUMFLEX factor
   | MINUS factor
   | OPEN expr CLOSE
   | functionCall
   | ident
   | literal
   ;

literal
   : string
   | floating
   | integer
   ;

functionCall
   : ident OPEN (expr (COMMA expr)*)? CLOSE
   ;

identExpr
   : ident
   | arrayElement
   ;

arrayElement
   : ident OPEN subscriptDecl (COMMA subscriptDecl)* CLOSE
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

EQV
   : 'EQV' | 'Eqv' | 'eqv'
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

IMP
   : 'IMP' | 'Imp' | 'imp'
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

SLEEP
   : 'SLEEP' | 'Sleep' | 'sleep'
   ;

SWAP
   : 'SWAP' | 'Swap' | 'swap'
   ;

SYSTEM
   : 'SYSTEM' | 'System' | 'system'
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
   : NUMBER? DOT NUMBER
   | NUMBER DOT
   ;

fragment
EXPONENT
   : [deDE]+ SIGN? NUMBER
   ;

fragment
SIGN
   : PLUS | MINUS
   ;

fragment
FLOATSUFFIX
   : HASH
   ;

LETTERS
   : [a-zA-Z]+
   ;

STRING
   : '"' ~ ["\r\n]* '"'
   ;

/* Comments */

COMMENT
   : APOSTROPHE ~[\r\n]*
   | REM (' ' ~[\r\n]*)?
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

CIRCUMFLEX
   : '^'
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

/* Whitespace and line breaks */

/*
 * An underscore as the last character on a line continues the statement onto the next
 * physical line. Skipping the line break together with the underscore joins the two
 * lines. COMMENT and STRING match the underscore first, so neither can be continued.
 */
CONTINUATION
   : '_' [ \t]* LINEBREAK -> skip
   ;

/*
 * A line break, together with any blank lines that follow it, is one token, so blank lines
 * need no grammar rule of their own. The blank lines must be matched here rather than left
 * to WS: a line holding nothing but spaces would otherwise split this into two tokens, and
 * the second one would have no statement in front of it.
 */
NEWLINE
   : LINEBREAK ([ \t]* LINEBREAK)*
   ;

fragment
LINEBREAK
   : '\r' '\n'? | '\n'
   ;

WS
   : [ \t] -> skip
   ;
