;;; JCC version: 0.10.0
;;; Date & time: 2025-08-09T14:06:46.009447
;;; Source file: hypotenuse.bas
format PE64 console
entry __main
include 'win64a.inc'

section '.idata' import data readable

library msvcrt,'msvcrt.dll'

import msvcrt,\
_exit_lib,'exit',\
_printf_lib,'printf'

section '.data' data readable writeable

__empty db "",0
__float_0 dq 3.0
__float_1 dq 4.0
__fmt_Str_F64 db "%s%f",10,0
__string_0 db "When the sides are 3.0 and 4.0, the hypotenuse is ",0
__tmp_location_0 dq 0

;; --- Dynamic memory type pointers -->
__gc_type_pointers_start dq 0h
__gc_type_pointers_stop dq 0h
;; <-- Dynamic memory type pointers ---

section '.code' code readable executable

__main:
;; Save base pointer
push rbp
mov rbp, rsp
;; Save float registers
sub rsp, 10h
movdqu [rsp], xmm6

;; 1: REM 


;; --- 4: PRINT "When the sides are 3.0 and 4.0, the hypoten... -->
;; Evaluate arguments (_printf_lib)
;; Defer evaluation of argument 0: _fmt_Str_F64
;; Defer evaluation of argument 1: "When the sides are 3.0 and 4.0, the hypotenuse is "

;; --- 4: FNhypotenuse(3.0, 4.0) -->
;; Evaluate arguments (_FNhypotenuse_F64_F64)
;; Defer evaluation of argument 0: 3.0
;; Defer evaluation of argument 1: 4.0
;; Move arguments to argument passing registers (_FNhypotenuse_F64_F64)
;; 4: 3.0
movsd xmm0, [__float_0]
;; 4: 4.0
movsd xmm1, [__float_1]
;; Allocate shadow space (_FNhypotenuse_F64_F64)
sub rsp, 20h
call __FNhypotenuse_F64_F64
;; Clean up shadow space (_FNhypotenuse_F64_F64)
add rsp, 20h
;; Move return value (xmm0) to storage location (xmm6)
movsd xmm6, xmm0
;; <-- 4: FNhypotenuse(3.0, 4.0) ---

;; Move arguments to argument passing registers (_printf_lib)
;; 4: _fmt_Str_F64
mov rcx, __fmt_Str_F64
;; 4: "When the sides are 3.0 and 4.0, the hypotenuse is "
mov rdx, __string_0
movsd [__tmp_location_0], xmm6
mov r8, [__tmp_location_0]
movsd xmm2, xmm6
;; Allocate shadow space (_printf_lib)
sub rsp, 20h
call [_printf_lib]
;; Clean up shadow space (_printf_lib)
add rsp, 20h
;; Ignore return value
;; <-- 4: PRINT "When the sides are 3.0 and 4.0, the hypoten... ---

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


;; --- User-defined functions -->

;; Definition of: FNhypotenuse(F64, F64) -> F64
__FNhypotenuse_F64_F64:
;; Save base pointer
push rbp
mov rbp, rsp
;; Save float registers
sub rsp, 10h
movdqu [rsp], xmm6
sub rsp, 10h
movdqu [rsp], xmm7
sub rsp, 10h
movdqu [rsp], xmm8

;; Save 2 argument(s) in home location(s)
movsd [rbp+10h], xmm0
movsd [rbp+18h], xmm1


;; sqrt(a * a + b * b)
;; 2: a
movsd xmm6, [rbp+10h]
;; 2: a
movsd xmm7, [rbp+10h]
;; 2: a * a
mulsd xmm6, xmm7
;; 2: b
movsd xmm7, [rbp+18h]
;; 2: b
movsd xmm8, [rbp+18h]
;; 2: b * b
mulsd xmm7, xmm8
;; 2: a * a + b * b
addsd xmm6, xmm7
sqrtsd xmm6, xmm6

;; Move result (xmm6) to return value (xmm0)
movsd xmm0, xmm6

;; Restore float registers
movdqu xmm8, [rsp]
add rsp, 10h
movdqu xmm7, [rsp]
add rsp, 10h
movdqu xmm6, [rsp]
add rsp, 10h
;; Restore base pointer
pop rbp
ret

;; <-- User-defined functions ---
