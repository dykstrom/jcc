;;; JCC version: 0.8.1
;;; Date & time: 2023-12-02T15:00:49.019569
;;; Source file: hypotenuse.bas
format PE64 console
entry __main
include 'win64a.inc'

section '.idata' import data readable

library msvcrt,'msvcrt.dll'

import msvcrt,\
_exit_lib,'exit',\
_printf_lib,'printf',\
_sqrt_lib,'sqrt'

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
;; Save used non-volatile registers
push rbx
push rdi
sub rsp, 16
movdqu [rsp], xmm6
sub rsp, 16
movdqu [rsp], xmm7
sub rsp, 16
movdqu [rsp], xmm8
;; Align stack
sub rsp, 8

;; 1: REM 


;; --- 4: PRINT "When the sides are 3.0 and 4.0, the hypoten... -->
;; Evaluate arguments (_printf_lib)
;; 4: _fmt_Str_F64
mov rbx, __fmt_Str_F64
;; 4: "When the sides are 3.0 and 4.0, the hypotenuse is "
mov rdi, __string_0

;; --- 4: FNhypotenuse(3.0, 4.0) -->
;; Evaluate arguments (_FNhypotenuse_F64_F64)
;; 4: 3.0
movsd xmm7, [__float_0]
;; 4: 4.0
movsd xmm8, [__float_1]
;; Move arguments to argument passing registers (_FNhypotenuse_F64_F64)
movsd xmm0, xmm7
movsd xmm1, xmm8
;; Allocate shadow space (_FNhypotenuse_F64_F64)
sub rsp, 20h
call __FNhypotenuse_F64_F64
;; Clean up shadow space (_FNhypotenuse_F64_F64)
add rsp, 20h
;; Move return value (xmm0) to storage location (xmm6)
movsd xmm6, xmm0
;; <-- 4: FNhypotenuse(3.0, 4.0) ---

;; Move arguments to argument passing registers (_printf_lib)
mov rcx, rbx
mov rdx, rdi
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
;; 0
mov rbx, 0
;; Move arguments to argument passing registers (_exit_lib)
mov rcx, rbx
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
;; Enter function
push rbp
mov rbp, rsp
;; Save 2 argument(s) in home location(s)
movsd [rbp+10h], xmm0
movsd [rbp+18h], xmm1
;; Save used non-volatile registers
sub rsp, 16
movdqu [rsp], xmm6
sub rsp, 16
movdqu [rsp], xmm7
sub rsp, 16
movdqu [rsp], xmm8
sub rsp, 16
movdqu [rsp], xmm9
;; Align stack
sub rsp, 8


;; --- 2: sqr(a * a + b * b) -->
;; Evaluate arguments (_sqrt_lib)
;; 2: a
movsd xmm7, [rbp+10h]
;; 2: a
movsd xmm8, [rbp+10h]
;; 2: a * a
mulsd xmm7, xmm8
;; 2: b
movsd xmm8, [rbp+18h]
;; 2: b
movsd xmm9, [rbp+18h]
;; 2: b * b
mulsd xmm8, xmm9
;; 2: a * a + b * b
addsd xmm7, xmm8
;; Move arguments to argument passing registers (_sqrt_lib)
movsd xmm0, xmm7
;; Allocate shadow space (_sqrt_lib)
sub rsp, 20h
call [_sqrt_lib]
;; Clean up shadow space (_sqrt_lib)
add rsp, 20h
;; Move return value (xmm0) to storage location (xmm6)
movsd xmm6, xmm0
;; <-- 2: sqr(a * a + b * b) ---

;; Move result (xmm6) to return value (xmm0)
movsd xmm0, xmm6

;; Undo align stack
add rsp, 8
;; Restore used non-volatile registers
movdqu xmm9, [rsp]
add rsp, 16
movdqu xmm8, [rsp]
add rsp, 16
movdqu xmm7, [rsp]
add rsp, 16
movdqu xmm6, [rsp]
add rsp, 16
;; Leave function
pop rbp
ret

;; <-- User-defined functions ---
