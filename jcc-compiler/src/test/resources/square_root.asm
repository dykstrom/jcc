;;; JCC version: 0.8.2-SNAPSHOT
;;; Date & time: 2024-03-16T18:00:19.612951
;;; Source file: square_root.bas
format PE64 console
entry __main
include 'win64a.inc'

section '.idata' import data readable

library msvcrt,'msvcrt.dll'

import msvcrt,\
_exit_lib,'exit',\
_fabs_lib,'fabs',\
_printf_lib,'printf',\
_sqrt_lib,'sqrt'

section '.data' data readable writeable

_N dq 2.0
__empty db "",0
__float_0 dq 0.001
__fmt_ db "",10,0
__fmt_Str_F64_Str_F64 db "%s%f%s%f",10,0
__string_0 db "Guess=",0
__string_1 db ", next guess=",0
__string_2 db "The square root of ",0
__string_3 db " = ",0
__string_4 db "Calling sqr(",0
__string_5 db ") returns ",0
__tmp_location_0 dq 0
_dividend dq 0.0
_divisor dq 0.0
_guess dq 0.0
_result dq 0.0

;; --- Dynamic memory type pointers -->
__gc_type_pointers_start dq 0h
__gc_type_pointers_stop dq 0h
;; <-- Dynamic memory type pointers ---

section '.code' code readable executable

__main:
;; Save base pointer
push rbp
mov rbp, rsp
;; Save g.p. registers
push rbx
;; Align stack
sub rsp, 8h
;; Save float registers
sub rsp, 10h
movdqu [rsp], xmm6
sub rsp, 10h
movdqu [rsp], xmm7
sub rsp, 10h
movdqu [rsp], xmm8
sub rsp, 10h
movdqu [rsp], xmm9

;; 1: REM 

;; 2: CONST N : F64 = 2.0



;; 7: REM 

;; 8: N
movsd xmm6, [_N]
;; 8: guess = N
movsd [_guess], xmm6

;; 9: N
movsd xmm6, [_N]
;; 9: 2.0
movsd xmm7, [_N]
;; 9: N / 2.0
divsd xmm6, xmm7
;; 9: result = N / 2.0
movsd [_result], xmm6

;; 11: REM 

_before_while_0:

;; --- 12: abs(guess - result) -->
;; Evaluate arguments (_fabs_lib)
;; 12: guess
movsd xmm8, [_guess]
;; 12: result
movsd xmm9, [_result]
;; 12: guess - result
subsd xmm8, xmm9
;; Move arguments to argument passing registers (_fabs_lib)
movsd xmm0, xmm8
;; Allocate shadow space (_fabs_lib)
sub rsp, 20h
call [_fabs_lib]
;; Clean up shadow space (_fabs_lib)
add rsp, 20h
;; Move return value (xmm0) to storage location (xmm6)
movsd xmm6, xmm0
;; <-- 12: abs(guess - result) ---

;; 12: 0.001
movsd xmm7, [__float_0]
;; 12: abs(guess - result) > 0.001
ucomisd xmm6, xmm7
ja @f
mov rbx, 0
jmp _after_cmp_2
@@:
mov rbx, -1
_after_cmp_2:

;; 12: WHILE abs(guess - result) > 0.001 guess = result :...
cmp rbx, 0
je _after_while_1

;; 13: result
movsd xmm6, [_result]
;; 13: guess = result
movsd [_guess], xmm6

;; 15: REM 

;; 16: guess
movsd xmm6, [_guess]
;; 16: guess
movsd xmm7, [_guess]
;; 16: guess * guess
mulsd xmm6, xmm7
;; 16: N
movsd xmm7, [_N]
;; 16: guess * guess - N
subsd xmm6, xmm7
;; 16: divisor = guess * guess - N
movsd [_divisor], xmm6

;; 17: 2.0
movsd xmm6, [_N]
;; 17: guess
movsd xmm7, [_guess]
;; 17: 2.0 * guess
mulsd xmm6, xmm7
;; 17: dividend = 2.0 * guess
movsd [_dividend], xmm6

;; 18: guess
movsd xmm6, [_guess]
;; 18: divisor
movsd xmm7, [_divisor]
;; 18: dividend
movsd xmm8, [_dividend]
;; 18: divisor / dividend
divsd xmm7, xmm8
;; 18: guess - divisor / dividend
subsd xmm6, xmm7
;; 18: result = guess - divisor / dividend
movsd [_result], xmm6

;; --- 20: PRINT "Guess=", guess, ", next guess=", result -->
;; Evaluate arguments (_printf_lib)
;; Defer evaluation of argument 0: _fmt_Str_F64_Str_F64
;; Defer evaluation of argument 1: "Guess="
;; Defer evaluation of argument 2: guess
;; Defer evaluation of argument 3: ", next guess="
;; Push 1 additional argument(s) to stack
;; 20: result
movsd xmm6, [_result]
movsd [__tmp_location_0], xmm6
push qword [__tmp_location_0]
;; Move arguments to argument passing registers (_printf_lib)
;; 20: _fmt_Str_F64_Str_F64
mov rcx, __fmt_Str_F64_Str_F64
;; 20: "Guess="
mov rdx, __string_0
;; 20: guess
movsd xmm2, [_guess]
movsd [__tmp_location_0], xmm2
mov r8, [__tmp_location_0]
;; 20: ", next guess="
mov r9, __string_1
;; Allocate shadow space (_printf_lib)
sub rsp, 20h
call [_printf_lib]
;; Clean up shadow space (_printf_lib)
add rsp, 20h
;; Ignore return value
;; Clean up 1 pushed argument(s)
add rsp, 8h
;; <-- 20: PRINT "Guess=", guess, ", next guess=", result ---

jmp _before_while_0
_after_while_1:

;; --- 23: PRINT  -->
;; Evaluate arguments (_printf_lib)
;; Defer evaluation of argument 0: _fmt_
;; Move arguments to argument passing registers (_printf_lib)
;; 23: _fmt_
mov rcx, __fmt_
;; Allocate shadow space (_printf_lib)
sub rsp, 20h
call [_printf_lib]
;; Clean up shadow space (_printf_lib)
add rsp, 20h
;; Ignore return value
;; <-- 23: PRINT  ---

;; --- 24: PRINT "The square root of ", N, " = ", result -->
;; Evaluate arguments (_printf_lib)
;; Defer evaluation of argument 0: _fmt_Str_F64_Str_F64
;; Defer evaluation of argument 1: "The square root of "
;; Defer evaluation of argument 2: N
;; Defer evaluation of argument 3: " = "
;; Push 1 additional argument(s) to stack
;; 24: result
movsd xmm6, [_result]
movsd [__tmp_location_0], xmm6
push qword [__tmp_location_0]
;; Move arguments to argument passing registers (_printf_lib)
;; 24: _fmt_Str_F64_Str_F64
mov rcx, __fmt_Str_F64_Str_F64
;; 24: "The square root of "
mov rdx, __string_2
;; 24: N
movsd xmm2, [_N]
movsd [__tmp_location_0], xmm2
mov r8, [__tmp_location_0]
;; 24: " = "
mov r9, __string_3
;; Allocate shadow space (_printf_lib)
sub rsp, 20h
call [_printf_lib]
;; Clean up shadow space (_printf_lib)
add rsp, 20h
;; Ignore return value
;; Clean up 1 pushed argument(s)
add rsp, 8h
;; <-- 24: PRINT "The square root of ", N, " = ", result ---

;; --- 25: PRINT "Calling sqr(", N, ") returns ", sqr(N) -->
;; Evaluate arguments (_printf_lib)
;; Defer evaluation of argument 0: _fmt_Str_F64_Str_F64
;; Defer evaluation of argument 1: "Calling sqr("
;; Defer evaluation of argument 2: N
;; Defer evaluation of argument 3: ") returns "
;; Push 1 additional argument(s) to stack

;; --- 25: sqr(N) -->
;; Evaluate arguments (_sqrt_lib)
;; Defer evaluation of argument 0: N
;; Move arguments to argument passing registers (_sqrt_lib)
;; 25: N
movsd xmm0, [_N]
;; Allocate shadow space (_sqrt_lib)
sub rsp, 20h
call [_sqrt_lib]
;; Clean up shadow space (_sqrt_lib)
add rsp, 20h
;; Move return value (xmm0) to storage location (xmm6)
movsd xmm6, xmm0
;; <-- 25: sqr(N) ---

movsd [__tmp_location_0], xmm6
push qword [__tmp_location_0]
;; Move arguments to argument passing registers (_printf_lib)
;; 25: _fmt_Str_F64_Str_F64
mov rcx, __fmt_Str_F64_Str_F64
;; 25: "Calling sqr("
mov rdx, __string_4
;; 25: N
movsd xmm2, [_N]
movsd [__tmp_location_0], xmm2
mov r8, [__tmp_location_0]
;; 25: ") returns "
mov r9, __string_5
;; Allocate shadow space (_printf_lib)
sub rsp, 20h
call [_printf_lib]
;; Clean up shadow space (_printf_lib)
add rsp, 20h
;; Ignore return value
;; Clean up 1 pushed argument(s)
add rsp, 8h
;; <-- 25: PRINT "Calling sqr(", N, ") returns ", sqr(N) ---

;; --- exit(0) -->
;; Evaluate arguments (_exit_lib)
;; Defer evaluation of argument 0: 0
;; Move arguments to argument passing registers (_exit_lib)
;; 0
mov rcx, 0
;; Allocate shadow space (_exit_lib)
sub rsp, 20h
call [_exit_lib]
;; Clean up shadow space (_exit_lib)
add rsp, 20h
;; Ignore return value
;; <-- exit(0) ---

