;;; JCC version: 0.8.2-SNAPSHOT
;;; Date & time: 2024-03-16T18:00:18.58198
;;; Source file: primes.bas
format PE64 console
entry __main
include 'win64a.inc'

section '.idata' import data readable

library msvcrt,'msvcrt.dll'

import msvcrt,\
_exit_lib,'exit',\
_printf_lib,'printf'

section '.data' data readable writeable

_N dq 100
__empty db "",0
__fmt_I64 db "%lld",10,0
_index dq 0
_isPrime dq 0
_maxIndex dq 0
_number dq 0
_primes_arr_dim_0 dq 101
_primes_arr_num_dims dq 1
_primes_arr dq 101 dup 0

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
push rsi
push r12

;; 1: REM 

;; 3: CONST N : I64 = 100






;; 11: 2
mov rbx, 2
;; 11: number = 2
mov [_number], rbx

_before_while_0:
;; 12: number
mov rbx, [_number]
;; 12: N
mov rdi, [_N]
;; 12: number < N
cmp rbx, rdi
jl @f
mov rbx, 0
jmp _after_cmp_2
@@:
mov rbx, -1
_after_cmp_2:

;; 12: WHILE number < N REM  : isPrime = 1 : index = 0 : ...
cmp rbx, 0
je _after_while_1

;; 14: REM 

;; 15: 1
mov rbx, 1
;; 15: isPrime = 1
mov [_isPrime], rbx

;; 16: 0
mov rbx, 0
;; 16: index = 0
mov [_index], rbx

_before_while_3:
;; 17: isPrime
mov rbx, [_isPrime]
;; 17: index
mov rdi, [_index]
;; 17: maxIndex
mov rsi, [_maxIndex]
;; 17: index < maxIndex
cmp rdi, rsi
jl @f
mov rdi, 0
jmp _after_cmp_5
@@:
mov rdi, -1
_after_cmp_5:
;; 17: (isPrime AND index < maxIndex)
and rbx, rdi

;; 17: WHILE (isPrime AND index < maxIndex) REM  : isPrim...
cmp rbx, 0
je _after_while_4

;; 18: REM 

;; 19: number
mov rbx, [_number]
;; 19: primes(index)
;; 19: index
mov rsi, [_index]
mov rdi, [_primes_arr+8*rsi]
;; 19: number % primes(index)
mov rax, rbx
cqo
idiv rdi
mov rbx, rdx
;; 19: isPrime = number % primes(index)
mov [_isPrime], rbx

;; 20: index
mov rbx, [_index]
;; 20: 1
mov rdi, 1
;; 20: index + 1
add rbx, rdi
;; 20: index = index + 1
mov [_index], rbx

jmp _before_while_3
_after_while_4:

;; 23: REM 

;; 24: isPrime
mov rbx, [_isPrime]

;; 24: IF isPrime THEN PRINT number : primes(maxIndex) = ...
cmp rbx, 0
je _after_then_6

;; --- 25: PRINT number -->
;; Evaluate arguments (_printf_lib)
;; Defer evaluation of argument 0: _fmt_I64
;; Defer evaluation of argument 1: number
;; Move arguments to argument passing registers (_printf_lib)
;; 25: _fmt_I64
mov rcx, __fmt_I64
;; 25: number
mov rdx, [_number]
;; Allocate shadow space (_printf_lib)
sub rsp, 20h
call [_printf_lib]
;; Clean up shadow space (_printf_lib)
add rsp, 20h
;; Ignore return value
;; <-- 25: PRINT number ---

;; 26: number
mov rbx, [_number]
;; 26: primes(maxIndex) = number
;; 26: primes(maxIndex)
;; 26: maxIndex
mov rdi, [_maxIndex]
mov [_primes_arr+8*rdi], rbx

;; 27: maxIndex
mov rbx, [_maxIndex]
;; 27: 1
mov rdi, 1
;; 27: maxIndex + 1
add rbx, rdi
;; 27: maxIndex = maxIndex + 1
mov [_maxIndex], rbx

_after_then_6:

_after_else_7:

;; 30: number
mov rbx, [_number]
;; 30: 1
mov rdi, 1
;; 30: number + 1
add rbx, rdi
;; 30: number = number + 1
mov [_number], rbx

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

