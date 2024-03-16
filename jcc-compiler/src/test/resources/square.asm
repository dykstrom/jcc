;;; JCC version: 0.8.2-SNAPSHOT
;;; Date & time: 2024-03-16T18:00:19.259116
;;; Source file: square.bas
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
__fmt_Str_I64 db "%s%lld",10,0
__string_0 db "The square of 5 is ",0
__string_1 db "The square of -1 is ",0

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

;; 1: REM 


;; --- 4: PRINT "The square of 5 is ", FNsquare%(5) -->
;; Evaluate arguments (_printf_lib)
;; Defer evaluation of argument 0: _fmt_Str_I64
;; Defer evaluation of argument 1: "The square of 5 is "

;; --- 4: FNsquare%(5) -->
;; Evaluate arguments (_FNsquare%_I64)
;; Defer evaluation of argument 0: 5
;; Move arguments to argument passing registers (_FNsquare%_I64)
;; 4: 5
mov rcx, 5
;; Allocate shadow space (_FNsquare%_I64)
sub rsp, 20h
call __FNsquare%_I64
;; Clean up shadow space (_FNsquare%_I64)
add rsp, 20h
;; Move return value (rax) to storage location (rbx)
mov rbx, rax
;; <-- 4: FNsquare%(5) ---

;; Move arguments to argument passing registers (_printf_lib)
;; 4: _fmt_Str_I64
mov rcx, __fmt_Str_I64
;; 4: "The square of 5 is "
mov rdx, __string_0
mov r8, rbx
;; Allocate shadow space (_printf_lib)
sub rsp, 20h
call [_printf_lib]
;; Clean up shadow space (_printf_lib)
add rsp, 20h
;; Ignore return value
;; <-- 4: PRINT "The square of 5 is ", FNsquare%(5) ---

;; --- 5: PRINT "The square of -1 is ", FNsquare%(-1) -->
;; Evaluate arguments (_printf_lib)
;; Defer evaluation of argument 0: _fmt_Str_I64
;; Defer evaluation of argument 1: "The square of -1 is "

;; --- 5: FNsquare%(-1) -->
;; Evaluate arguments (_FNsquare%_I64)
;; Defer evaluation of argument 0: -1
;; Move arguments to argument passing registers (_FNsquare%_I64)
;; 5: -1
mov rcx, -1
;; Allocate shadow space (_FNsquare%_I64)
sub rsp, 20h
call __FNsquare%_I64
;; Clean up shadow space (_FNsquare%_I64)
add rsp, 20h
;; Move return value (rax) to storage location (rbx)
mov rbx, rax
;; <-- 5: FNsquare%(-1) ---

;; Move arguments to argument passing registers (_printf_lib)
;; 5: _fmt_Str_I64
mov rcx, __fmt_Str_I64
;; 5: "The square of -1 is "
mov rdx, __string_1
mov r8, rbx
;; Allocate shadow space (_printf_lib)
sub rsp, 20h
call [_printf_lib]
;; Clean up shadow space (_printf_lib)
add rsp, 20h
;; Ignore return value
;; <-- 5: PRINT "The square of -1 is ", FNsquare%(-1) ---

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

;; Definition of: FNsquare%(I64) -> I64
__FNsquare%_I64:
;; Save base pointer
push rbp
mov rbp, rsp
;; Save g.p. registers
push rbx
push rdi

;; Save 1 argument(s) in home location(s)
mov [rbp+10h], rcx

;; 2: x
mov rbx, [rbp+10h]
;; 2: x
mov rdi, [rbp+10h]
;; 2: x * x
imul rbx, rdi
;; Move result (rbx) to return value (rax)
mov rax, rbx

;; Restore g.p. registers
pop rdi
pop rbx
;; Restore base pointer
pop rbp
ret

;; <-- User-defined functions ---
