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
   | unsupportedStmt
   | whileStmt
   ;

assignStmt
   : LET? identExpr EQ expr
   | LET? reservedWord EQ expr
   ;

/*
 * The reserved words that cannot be used as variable names, as in QuickBASIC 4.5. They are
 * accepted in the two places a variable is named, an assignment and a DIM, only so that
 * BasicSyntaxVisitor can say so: the parser otherwise reports whatever the keyword's own statement
 * wanted next, which names a construct the program does not contain.
 *
 * ELSE, ELSEIF, END and WEND are left out. Making them start a statement changes what the parser
 * expects at a block boundary, which is where BasicErrorStrategy diagnoses an unterminated block
 * and an orphaned terminator. Those two already name the mistake for these four words.
 *
 * LET is left out because of the optional LET in front: with it here, LET = 7 reads as an
 * assignment to a variable named LET, which names the wrong mistake for a missing variable.
 */
reservedWord
   : AND
   | AS
   | BASE
   | CLS
   | CONST
   | DEF
   | DEFDBL
   | DEFINT
   | DEFSTR
   | DIM
   | EQV
   | GOSUB
   | GOTO
   | IF
   | IMP
   | INPUT
   | LINE
   | MOD
   | NOT
   | ON
   | OPTION
   | OR
   | PRINT
   | RANDOMIZE
   | RETURN
   | SLEEP
   | SWAP
   | SYSTEM
   | THEN
   | WHILE
   | XOR
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

/*
 * Any identifier is accepted as the function name, and the FN prefix is required in the syntax
 * visitor instead. A grammar predicate could only fail, and its failure printed the predicate's
 * own source code at the user.
 */
defFnStmt
   : DEF ident (LPAREN (paramDecl (COMMA paramDecl)*)? RPAREN)? EQ expr
   ;

paramDecl
   : ident (AS typeName)?
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

/* Any identifier is accepted here too; the syntax visitor requires a single letter. */
letterInterval
   : ident MINUS ident
   | ident
   ;

dimStmt
   : DIM varDecl (COMMA varDecl)*
   ;

varDecl
   /* Without an AS clause, the type comes from the type specifier, DEFtype, or the default type. */
   : ident (LPAREN subscriptBounds (COMMA subscriptBounds)* RPAREN)? (AS typeName)?
   | reservedWord (LPAREN subscriptBounds (COMMA subscriptBounds)* RPAREN)? (AS typeName)?
   ;

/*
 * QuickBASIC's explicit lower bound, 'DIM a(1 TO 10)', is parsed here only so that
 * BasicSyntaxVisitor can name it: JCC gives every dimension the lower bound of OPTION BASE.
 * The bound belongs to a declaration, so an array access still takes a plain subscriptDecl.
 */
subscriptBounds
   : subscriptDecl (TO subscriptDecl)?
   ;

/*
 * Any identifier is accepted as a type name here, and the name is resolved during semantic
 * analysis. That way an unknown or unsupported type name is named in the error message, and
 * DOUBLE, INTEGER and STRING are not reserved words.
 */
typeName
   : ident
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
   : IF expr THEN? (labelOrNumber | stmtList) elseSingle?
   ;

elseSingle
   : ELSE (labelOrNumber | stmtList)
   ;

/*
 * THEN is optional in the two IF rules and in elseIfBlock only so that a missing one can be
 * named. It is required in QuickBASIC, and BasicSyntaxVisitor reports it. Rejecting it in the
 * grammar instead costs the whole block, exactly as it does for ELSE IF below: the parser gives
 * up on the block rule at its header line and orphans the END IF, which is then reported as a
 * second mistake.
 */
ifThenBlock
   : IF expr THEN? commentStmt? NEWLINE line* elseIfBlock* elseBlock? endIf
   ;

/*
 * ELSE IF written as two words is accepted here only so that the mistake can be named. It
 * does not mean ELSEIF in QuickBASIC, which reads it as an ELSE holding a nested block IF and
 * so wants a second END IF; either way the program does not mean what it looks like.
 * BasicSyntaxVisitor reports it. Rejecting it in the grammar instead costs the whole block:
 * the parser gives up on elseIfBlock, and every ELSEIF, ELSE and END IF after it is orphaned.
 */
elseIfBlock
   : labelOrNumberDef? (ELSEIF | ELSE IF) expr THEN? commentStmt? NEWLINE line*
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

/*
 * QuickBASIC statements JCC does not implement, parsed only so that BasicSyntaxVisitor can
 * name them. Their keywords are soft keywords: tokens of their own, and alternatives of ident,
 * so they keep working as variable names. Without a token the parser sees a plain ID and can
 * report nothing better than a smushed-together token pair the programmer never wrote.
 *
 * The rest of the line is consumed unparsed. There is nothing to do with it, and swallowing it
 * keeps one unsupported statement to one diagnostic. COLON is left alone so that the statements
 * after it on the same line are still parsed.
 */
unsupportedStmt
   : unsupportedKeyword ~(NEWLINE | COLON)*
   ;

unsupportedKeyword
   : CASE
   | CLOSE
   | COLOR
   | DATA
   | DO
   | END (FUNCTION | SELECT | SUB | TYPE)
   | ERASE
   | EXIT
   | FOR
   | FUNCTION
   | INPUT
   | LOCATE
   | LOOP
   | NEXT
   | OPEN
   | PRINT USING
   | READ
   | REDIM
   | RESTORE
   | SELECT
   | STEP
   | SUB
   | TO
   | TYPE
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

/* The last alternative of each rule is the C-style spelling, see relExpr below. */
orExpr
   : orExpr OR andExpr
   | orExpr PIPE_PIPE andExpr
   | andExpr
   ;

andExpr
   : andExpr AND notExpr
   | andExpr AMP_AMP notExpr
   | notExpr
   ;

notExpr
   : NOT relExpr
   | relExpr
   ;

/*
 * The last two alternatives are the C-style operators BASIC does not have, parsed only so that
 * BasicSyntaxVisitor can name the operator to write instead. The same is done for '&&' and '||'
 * in andExpr and orExpr above. No BASIC program spells any of them, so the tokens are pure
 * additions.
 */
relExpr
   : relExpr EQ addSubExpr
   | relExpr GE addSubExpr
   | relExpr GT addSubExpr
   | relExpr LE addSubExpr
   | relExpr LT addSubExpr
   | relExpr NE addSubExpr
   | relExpr EQ_EQ addSubExpr
   | relExpr BANG_EQ addSubExpr
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
   | LPAREN expr RPAREN
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
   : ident LPAREN (expr (COMMA expr)*)? RPAREN
   ;

identExpr
   : ident
   | arrayElement
   ;

arrayElement
   : ident LPAREN subscriptDecl (COMMA subscriptDecl)* RPAREN
   ;

/*
 * The second alternative is a string literal whose closing quote is missing, parsed only so
 * that BasicSyntaxVisitor can name it. Letting the lexer reject it instead costs a second
 * error on the line after.
 */
string
   : STRING
   | UNTERMINATED_STRING
   ;

floating
   : FLOATNUMBER
   ;

/*
 * The last alternative is a radix literal with a missing or invalid digit, parsed only so that
 * BasicSyntaxVisitor can name the radix and the digits it takes.
 */
integer
   : HEXNUMBER
   | OCTNUMBER
   | BINNUMBER
   | NUMBER
   | MALFORMED_RADIXNUMBER
   ;

ident
   : ID unsupportedSuffix?
   | softKeyword
   ;

/*
 * QuickBASIC's type suffixes for the two types JCC does not have: '!' for single precision and
 * '&' for long. They are as common in real QuickBASIC source as the '%', '$' and '#' that ID
 * accepts, so they are parsed here and named in the syntax visitor.
 *
 * They are tokens of their own rather than part of ID's suffix position: inside ID, the lexer's
 * longest match would take 'a!' out of 'a!=3' and the '!=' of relExpr would never be reached.
 * As a token, '!=' wins over '!' at the same position, and '&&' and '&H10' over '&'.
 */
unsupportedSuffix
   : BANG
   | AMPERSAND
   ;

/*
 * The keywords of the unsupported statements. They were plain identifiers before they became
 * tokens, and words like DATA, TYPE and NEXT are common variable names, so every rule that
 * accepts an identifier accepts them too. Every other keyword is reserved, as in QuickBASIC 4.5;
 * see reservedWord.
 */
softKeyword
   : CASE
   | CLOSE
   | COLOR
   | DATA
   | DO
   | ERASE
   | EXIT
   | FOR
   | FUNCTION
   | LOCATE
   | LOOP
   | NEXT
   | OPEN
   | READ
   | REDIM
   | RESTORE
   | SELECT
   | STEP
   | SUB
   | TO
   | TYPE
   | USING
   ;

labelOrNumber
   : ident
   | NUMBER
   ;

labelOrNumberDef
   : ident COLON
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

CASE
   : 'CASE' | 'Case' | 'case'
   ;

CLOSE
   : 'CLOSE' | 'Close' | 'close'
   ;

CLS
   : 'CLS' | 'Cls' | 'cls'
   ;

COLOR
   : 'COLOR' | 'Color' | 'color'
   ;

CONST
   : 'CONST' | 'Const' | 'const'
   ;

DATA
   : 'DATA' | 'Data' | 'data'
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

DO
   : 'DO' | 'Do' | 'do'
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

ERASE
   : 'ERASE' | 'Erase' | 'erase'
   ;

EXIT
   : 'EXIT' | 'Exit' | 'exit'
   ;

FOR
   : 'FOR' | 'For' | 'for'
   ;

FUNCTION
   : 'FUNCTION' | 'Function' | 'function'
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

LOCATE
   : 'LOCATE' | 'Locate' | 'locate'
   ;

LOOP
   : 'LOOP' | 'Loop' | 'loop'
   ;

MOD
   : 'MOD' | 'Mod' | 'mod'
   ;

NEXT
   : 'NEXT' | 'Next' | 'next'
   ;

NOT
   : 'NOT' | 'Not' | 'not'
   ;

ON
   : 'ON' | 'On' | 'on'
   ;

OPEN
   : 'OPEN' | 'Open' | 'open'
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

READ
   : 'READ' | 'Read' | 'read'
   ;

REDIM
   : 'REDIM' | 'Redim' | 'redim'
   ;

REM
   : 'REM' | 'Rem' | 'rem'
   ;

RESTORE
   : 'RESTORE' | 'Restore' | 'restore'
   ;

RETURN
   : 'RETURN' | 'Return' | 'return'
   ;

SELECT
   : 'SELECT' | 'Select' | 'select'
   ;

SLEEP
   : 'SLEEP' | 'Sleep' | 'sleep'
   ;

STEP
   : 'STEP' | 'Step' | 'step'
   ;

SUB
   : 'SUB' | 'Sub' | 'sub'
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

TO
   : 'TO' | 'To' | 'to'
   ;

TYPE
   : 'TYPE' | 'Type' | 'type'
   ;

USING
   : 'USING' | 'Using' | 'using'
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

/*
 * The radix letter and the hexadecimal digits are case insensitive, as in QuickBASIC.
 */
HEXNUMBER
   : AMPERSAND [Hh] [0-9A-Fa-f]+
   ;

OCTNUMBER
   : AMPERSAND [Oo] [0-7]+
   ;

BINNUMBER
   : AMPERSAND [Bb] [0-1]+
   ;

/*
 * A radix literal with a missing or invalid digit. It must come after the three valid rules,
 * so that they win the equal-length match on a literal they both accept; it wins on length
 * where a valid rule stops short of the end, as HEXNUMBER does at the 'G' of '&H1G'.
 */
MALFORMED_RADIXNUMBER
   : AMPERSAND [HhOoBb] [0-9A-Za-z]*
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

/*
 * A string literal the closing quote is missing from. It must come after STRING, which wins
 * on length wherever the quote is there.
 */
UNTERMINATED_STRING
   : '"' ~ ["\r\n]*
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

AMP_AMP
   : '&&'
   ;

APOSTROPHE
   : '\''
   ;

BACKSLASH
   : '\\'
   ;

BANG
   : '!'
   ;

BANG_EQ
   : '!='
   ;

CIRCUMFLEX
   : '^'
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

EQ_EQ
   : '=='
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

LPAREN
   : '('
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

PERCENT
   : '%'
   ;

PIPE_PIPE
   : '||'
   ;

PLUS
   : '+'
   ;

RPAREN
   : ')'
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
 * lines. COMMENT and the two string rules match the underscore first, so a comment cannot be
 * continued, and neither can a string literal, terminated or not.
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
