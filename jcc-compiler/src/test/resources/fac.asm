;;; JCC version: 0.8.1
;;; Date & time: 2023-12-02T15:00:47.585282
;;; Source file: fac.bas
format PE64 console
entry __main
include 'win64a.inc'

section '.idata' import data readable

library msvcrt,'msvcrt.dll'

import msvcrt,\
_exit_lib,'exit',\
_printf_lib,'printf'

section '.data' data readable writeable

_N dq 5
__empty db "",0
__fmt_Str_I64_Str_I64 db "%s%lld%s%lld",10,0
__string_0 db "fac(",0
__string_1 db ")=",0
_i dq 0
_result dq 0

;; --- Dynamic memory type pointers -->
__gc_type_pointers_start dq 0h
__gc_type_pointers_stop dq 0h
;; <-- Dynamic memory type pointers ---

section '.code' code readable executable

__main:
;; Save used non-volatile registers
push rbx
push rdi
push rsi
push r12
push r13

__line_10:
;; 1: REM 


__line_20:
;; 2: CONST N : I64 = 5


__line_30:


__line_40:
;; 4: 1
mov rbx, 1
;; 4: result = 1
mov [_result], rbx


__line_50:
;; 5: N
mov rbx, [_N]
;; 5: i = N
mov [_i], rbx


__line_60:
;; 6: i
mov rbx, [_i]
;; 6: 0
mov rdi, 0
;; 6: i == 0
cmp rbx, rdi
je @f
mov rbx, 0
jmp _after_cmp_2
@@:
mov rbx, -1
_after_cmp_2:

;; 6: IF i == 0 THEN GOTO 100
cmp rbx, 0
je _after_then_0

;; 6: GOTO 100
jmp __line_100

_after_then_0:

_after_else_1:


__line_70:
;; 7: result
mov rbx, [_result]
;; 7: i
mov rdi, [_i]
;; 7: result * i
imul rbx, rdi
;; 7: result = result * i
mov [_result], rbx


__line_80:
;; 8: i
mov rbx, [_i]
;; 8: 1
mov rdi, 1
;; 8: i - 1
sub rbx, rdi
;; 8: i = i - 1
mov [_i], rbx


__line_90:
;; 9: GOTO 60
jmp __line_60


__line_100:
;; --- 10: PRINT "fac(", N, ")=", result -->
;; Evaluate arguments (_printf_lib)
;; 10: _fmt_Str_I64_Str_I64
mov rbx, __fmt_Str_I64_Str_I64
;; 10: "fac("
mov rdi, __string_0
;; 10: N
mov rsi, [_N]
;; 10: ")="
mov r12, __string_1
;; Push 1 additional argument(s) to stack
;; 10: result
mov r13, [_result]
push r13
;; Move arguments to argument passing registers (_printf_lib)
mov rcx, rbx
mov rdx, rdi
mov r8, rsi
mov r9, r12
;; Allocate shadow space (_printf_lib)
sub rsp, 20h
call [_printf_lib]
;; Clean up shadow space (_printf_lib)
add rsp, 20h
;; Ignore return value
;; Clean up 1 pushed argument(s)
add rsp, 8h
;; <-- 10: PRINT "fac(", N, ")=", result ---


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

