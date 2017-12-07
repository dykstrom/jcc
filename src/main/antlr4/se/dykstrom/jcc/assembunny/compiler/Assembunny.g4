/*
 * Copyright (C) 2017 Johan Dykstrom
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

grammar Assembunny;

program
   : statement*
   ;

statement
   : inc
   | dec
   | cpy
   | jnz
   | outn
   ;

inc
   : INC register
   ;

dec
   : DEC register
   ;

cpy
   : cpyFromRegister
   | cpyFromInteger
   ;

cpyFromRegister
   : CPY register register
   ;

cpyFromInteger
   : CPY integer register
   ;

jnz
   : jnzOnRegister
   | jnzOnInteger
   ;

jnzOnRegister
   : JNZ register integer
   ;

jnzOnInteger
   : JNZ integer integer
   ;

outn
   : OUTN register
   ;

integer
   : MINUS? NUMBER
   ;

register
   : CHARACTER
   ;

INC
   : 'inc'
   ;

DEC
   : 'dec'
   ;

CPY
   : 'cpy'
   ;

JNZ
   : 'jnz'
   ;

OUTN
   : 'outn'
   ;

NUMBER
   : ('0' .. '9')+
   ;

CHARACTER
   : 'a' | 'b' | 'c' | 'd'
   ;

MINUS
   : '-'
   ;

WS
   : [ \r\n] -> skip
   ;
