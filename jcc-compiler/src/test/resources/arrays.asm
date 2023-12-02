;;; JCC version: 0.8.1
;;; Date & time: 2023-12-02T15:00:46.299689
;;; Source file: arrays.bas
format PE64 console
entry __main
include 'win64a.inc'

section '.idata' import data readable

library jccbasic,'jccbasic.dll',\
msvcrt,'msvcrt.dll'

import jccbasic,\
_randomize_lib,'randomize',\
_rnd_lib,'rnd',\
_timer_lib,'timer'

import msvcrt,\
_exit_lib,'exit',\
_printf_lib,'printf'

section '.data' data readable writeable

_N dq 10000
__empty db "",0
__float_0 dq 1.0
__float_1 dq 0.0
__fmt_Str_F64 db "%s%f",10,0
__string_0 db "Min: ",0
__string_1 db "Max: ",0
__string_2 db "Avg: ",0
__tmp_location_0 dq 0
_index dq 0
_max dq 0.0
_min dq 0.0
_sum dq 0.0
_array_arr_dim_0 dq 10001
_array_arr_num_dims dq 1
_array_arr dq 10001 dup 0.0

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
sub rsp, 16
movdqu [rsp], xmm6
sub rsp, 16
movdqu [rsp], xmm7

;; 1: REM 

;; 3: CONST N : I64 = 10000




;; 9: RANDOMIZE timer()
;; --- randomize(timer()) -->
;; Evaluate arguments (_randomize_lib)

;; --- 9: timer() -->
;; Allocate shadow space (_timer_lib)
sub rsp, 20h
call [_timer_lib]
;; Clean up shadow space (_timer_lib)
add rsp, 20h
;; Move return value (xmm0) to storage location (xmm6)
movsd xmm6, xmm0
;; <-- 9: timer() ---

;; Move arguments to argument passing registers (_randomize_lib)
movsd xmm0, xmm6
;; Allocate shadow space (_randomize_lib)
sub rsp, 20h
call [_randomize_lib]
;; Clean up shadow space (_randomize_lib)
add rsp, 20h
;; Ignore return value
;; <-- randomize(timer()) ---

;; 11: REM 

;; 12: 0
mov rbx, 0
;; 12: index = 0
mov [_index], rbx

_before_while_0:
;; 13: index
mov rbx, [_index]
;; 13: N
mov rdi, [_N]
;; 13: index < N
cmp rbx, rdi
jl @f
mov rbx, 0
jmp _after_cmp_2
@@:
mov rbx, -1
_after_cmp_2:

;; 13: WHILE index < N array(index) = rnd() : index = ind...
cmp rbx, 0
je _after_while_1


;; --- 14: rnd() -->
;; Allocate shadow space (_rnd_lib)
sub rsp, 20h
call [_rnd_lib]
;; Clean up shadow space (_rnd_lib)
add rsp, 20h
;; Move return value (xmm0) to storage location (xmm6)
movsd xmm6, xmm0
;; <-- 14: rnd() ---

;; 14: array(index) = rnd()
;; 14: array(index)
;; 14: index
mov rbx, [_index]
movsd [_array_arr+8*rbx], xmm6

;; 15: index
mov rbx, [_index]
;; 15: 1
mov rdi, 1
;; 15: index + 1
add rbx, rdi
;; 15: index = index + 1
mov [_index], rbx

jmp _before_while_0
_after_while_1:

;; 18: 1.0
movsd xmm6, [__float_0]
;; 18: min = 1.0
movsd [_min], xmm6

;; 19: 0.0
movsd xmm6, [__float_1]
;; 19: max = 0.0
movsd [_max], xmm6

;; 20: 0.0
movsd xmm6, [__float_1]
;; 20: sum = 0.0
movsd [_sum], xmm6

;; 22: 0
mov rbx, 0
;; 22: index = 0
mov [_index], rbx

_before_while_3:
;; 23: index
mov rbx, [_index]
;; 23: N
mov rdi, [_N]
;; 23: index < N
cmp rbx, rdi
jl @f
mov rbx, 0
jmp _after_cmp_5
@@:
mov rbx, -1
_after_cmp_5:

;; 23: WHILE index < N IF array(index) < min THEN min = a...
cmp rbx, 0
je _after_while_4

;; 24: array(index)
;; 24: index
mov rdi, [_index]
movsd xmm6, [_array_arr+8*rdi]
;; 24: min
movsd xmm7, [_min]
;; 24: array(index) < min
ucomisd xmm6, xmm7
jb @f
mov rbx, 0
jmp _after_cmp_8
@@:
mov rbx, -1
_after_cmp_8:

;; 24: IF array(index) < min THEN min = array(index)
cmp rbx, 0
je _after_then_6

;; 24: array(index)
;; 24: index
mov rbx, [_index]
movsd xmm6, [_array_arr+8*rbx]
;; 24: min = array(index)
movsd [_min], xmm6

_after_then_6:

_after_else_7:

;; 25: array(index)
;; 25: index
mov rdi, [_index]
movsd xmm6, [_array_arr+8*rdi]
;; 25: max
movsd xmm7, [_max]
;; 25: array(index) > max
ucomisd xmm6, xmm7
ja @f
mov rbx, 0
jmp _after_cmp_11
@@:
mov rbx, -1
_after_cmp_11:

;; 25: IF array(index) > max THEN max = array(index)
cmp rbx, 0
je _after_then_9

;; 25: array(index)
;; 25: index
mov rbx, [_index]
movsd xmm6, [_array_arr+8*rbx]
;; 25: max = array(index)
movsd [_max], xmm6

_after_then_9:

_after_else_10:

;; 26: sum
movsd xmm6, [_sum]
;; 26: array(index)
;; 26: index
mov rbx, [_index]
movsd xmm7, [_array_arr+8*rbx]
;; 26: sum + array(index)
addsd xmm6, xmm7
;; 26: sum = sum + array(index)
movsd [_sum], xmm6

;; 27: index
mov rbx, [_index]
;; 27: 1
mov rdi, 1
;; 27: index + 1
add rbx, rdi
;; 27: index = index + 1
mov [_index], rbx

jmp _before_while_3
_after_while_4:

;; --- 30: PRINT "Min: ", min -->
;; Evaluate arguments (_printf_lib)
;; 30: _fmt_Str_F64
mov rbx, __fmt_Str_F64
;; 30: "Min: "
mov rdi, __string_0
;; 30: min
movsd xmm6, [_min]
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
;; <-- 30: PRINT "Min: ", min ---

;; --- 31: PRINT "Max: ", max -->
;; Evaluate arguments (_printf_lib)
;; 31: _fmt_Str_F64
mov rbx, __fmt_Str_F64
;; 31: "Max: "
mov rdi, __string_1
;; 31: max
movsd xmm6, [_max]
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
;; <-- 31: PRINT "Max: ", max ---

;; --- 32: PRINT "Avg: ", sum / N -->
;; Evaluate arguments (_printf_lib)
;; 32: _fmt_Str_F64
mov rbx, __fmt_Str_F64
;; 32: "Avg: "
mov rdi, __string_2
;; 32: sum
movsd xmm6, [_sum]
;; 32: N
mov rsi, [_N]
;; 32: sum / N
cvtsi2sd xmm4, rsi
divsd xmm6, xmm4
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
;; <-- 32: PRINT "Avg: ", sum / N ---

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

