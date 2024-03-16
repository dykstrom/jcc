;;; JCC version: 0.8.2-SNAPSHOT
;;; Date & time: 2024-03-16T18:00:18.942068
;;; Source file: sin_cos.bas
format PE64 console
entry __main
include 'win64a.inc'

section '.idata' import data readable

library msvcrt,'msvcrt.dll'

import msvcrt,\
_atan_lib,'atan',\
_cos_lib,'cos',\
_exit_lib,'exit',\
_printf_lib,'printf',\
_sin_lib,'sin'

section '.data' data readable writeable

_PI dq 0.0
__empty db "",0
__float_0 dq 4.0
__fmt_Str_I64_Str_F64_Str_I64_Str_F64 db "%s%lld%s%f%s%lld%s%f",10,0
__string_0 db ")=",0
__string_1 db ", cos(",0
__string_2 db "sin(",0
__tmp_location_0 dq 0
_angle dq 0
_rad dq 0.0

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
push rdi
;; Save float registers
sub rsp, 10h
movdqu [rsp], xmm6
sub rsp, 10h
movdqu [rsp], xmm7

;; 1: REM 



;; 6: 4.0
movsd xmm6, [__float_0]

;; --- 6: atn(1) -->
;; Evaluate arguments (_atan_lib)
;; Defer evaluation of argument 0: 1
;; Move arguments to argument passing registers (_atan_lib)
;; 6: 1
mov rbx, 1
;; Cast temporary I64 expression: 1
cvtsi2sd xmm0, rbx
;; Allocate shadow space (_atan_lib)
sub rsp, 20h
call [_atan_lib]
;; Clean up shadow space (_atan_lib)
add rsp, 20h
;; Move return value (xmm0) to storage location (xmm7)
movsd xmm7, xmm0
;; <-- 6: atn(1) ---

;; 6: 4.0 * atn(1)
mulsd xmm6, xmm7
;; 6: PI = 4.0 * atn(1)
movsd [_PI], xmm6

;; 8: 0
mov rbx, 0
;; 8: angle = 0
mov [_angle], rbx

_before_while_0:
;; 9: angle
mov rbx, [_angle]
;; 9: 360
mov rdi, 360
;; 9: angle <= 360
cmp rbx, rdi
jle @f
mov rbx, 0
jmp _after_cmp_2
@@:
mov rbx, -1
_after_cmp_2:

;; 9: WHILE angle <= 360 REM  : rad = angle * PI / 180 :...
cmp rbx, 0
je _after_while_1

;; 10: REM 

;; 11: angle
mov rbx, [_angle]
;; Cast temporary I64 expression: angle
cvtsi2sd xmm6, rbx
;; 11: PI
movsd xmm7, [_PI]
;; 11: angle * PI
mulsd xmm6, xmm7
;; 11: 180
mov rbx, 180
;; 11: angle * PI / 180
cvtsi2sd xmm4, rbx
divsd xmm6, xmm4
;; 11: rad = angle * PI / 180
movsd [_rad], xmm6

;; --- 12: PRINT "sin(", angle, ")=", sin(rad), ", cos(", ang... -->
;; Evaluate arguments (_printf_lib)
;; Defer evaluation of argument 0: _fmt_Str_I64_Str_F64_Str_I64_Str_F64
;; Defer evaluation of argument 1: "sin("
;; Defer evaluation of argument 2: angle
;; Defer evaluation of argument 3: ")="
;; Push 5 additional argument(s) to stack

;; --- 12: cos(rad) -->
;; Evaluate arguments (_cos_lib)
;; Defer evaluation of argument 0: rad
;; Move arguments to argument passing registers (_cos_lib)
;; 12: rad
movsd xmm0, [_rad]
;; Allocate shadow space (_cos_lib)
sub rsp, 20h
call [_cos_lib]
;; Clean up shadow space (_cos_lib)
add rsp, 20h
;; Move return value (xmm0) to storage location (xmm6)
movsd xmm6, xmm0
;; <-- 12: cos(rad) ---

movsd [__tmp_location_0], xmm6
push qword [__tmp_location_0]
;; 12: ")="
mov rbx, __string_0
push rbx
;; 12: angle
mov rbx, [_angle]
push rbx
;; 12: ", cos("
mov rbx, __string_1
push rbx

;; --- 12: sin(rad) -->
;; Evaluate arguments (_sin_lib)
;; Defer evaluation of argument 0: rad
;; Move arguments to argument passing registers (_sin_lib)
;; 12: rad
movsd xmm0, [_rad]
;; Allocate shadow space (_sin_lib)
sub rsp, 20h
call [_sin_lib]
;; Clean up shadow space (_sin_lib)
add rsp, 20h
;; Move return value (xmm0) to storage location (xmm6)
movsd xmm6, xmm0
;; <-- 12: sin(rad) ---

movsd [__tmp_location_0], xmm6
push qword [__tmp_location_0]
;; Move arguments to argument passing registers (_printf_lib)
;; 12: _fmt_Str_I64_Str_F64_Str_I64_Str_F64
mov rcx, __fmt_Str_I64_Str_F64_Str_I64_Str_F64
;; 12: "sin("
mov rdx, __string_2
;; 12: angle
mov r8, [_angle]
;; 12: ")="
mov r9, __string_0
;; Allocate shadow space (_printf_lib)
sub rsp, 20h
call [_printf_lib]
;; Clean up shadow space (_printf_lib)
add rsp, 20h
;; Ignore return value
;; Clean up 5 pushed argument(s)
add rsp, 28h
;; <-- 12: PRINT "sin(", angle, ")=", sin(rad), ", cos(", ang... ---

;; 13: angle
mov rbx, [_angle]
;; 13: 30
mov rdi, 30
;; 13: angle + 30
add rbx, rdi
;; 13: angle = angle + 30
mov [_angle], rbx

jmp _before_while_0
_after_while_1:

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

