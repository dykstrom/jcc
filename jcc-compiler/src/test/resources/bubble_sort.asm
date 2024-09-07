;;; JCC version: 0.8.2-SNAPSHOT
;;; Date & time: 2024-09-07T15:38:45.198925
;;; Source file: bubble_sort.bas
format PE64 console
entry __main
include 'win64a.inc'

section '.idata' import data readable

library jccbasic,'jccbasic.dll',\
msvcrt,'msvcrt.dll'

import jccbasic,\
_int_F64_lib,'int_F64',\
_randomize_lib,'randomize',\
_rnd_lib,'rnd',\
_str_I64_lib,'str_I64',\
_timer_lib,'timer',\
_ubound_lib,'ubound'

import msvcrt,\
_exit_lib,'exit',\
_free_lib,'free',\
_malloc_lib,'malloc',\
_printf_lib,'printf',\
_strcat_lib,'strcat',\
_strcpy_lib,'strcpy',\
_strlen_lib,'strlen'

section '.data' data readable writeable

_MAX dq 100
__empty db "",0
__fmt_Str db "%s",10,0
__gc_allocation_count dq 0
__gc_allocation_limit dq 100
__gc_allocation_list dq 0
__gc_count_msg db "GC: Allocation count reached limit: %d - collecting",10,0
__gc_limit_msg db "GC: Collection finished with new limit: %d",10,0
__gc_marked_msg db "GC: Marking memory: %x",10,0
__gc_register_msg db "GC: Registering new memory: %x",10,0
__gc_sweeping_msg db "GC: Sweeping memory: %x",10,0
__string_0 db "Unsorted:",0
__string_1 db "Sorted:",0
__string_2 db "",0
__string_3 db "Error: RETURN without GOSUB",0
_i% dq 0
_j% dq 0
_s$ dq __empty
_x% dq 0
_numbers%_arr_dim_0 dq 101
_numbers%_arr_num_dims dq 1
_numbers%_arr dq 101 dup 0

;; --- Dynamic memory type pointers -->
__gc_type_pointers_start dq 0h
__s$_type dq 0h
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
push r13
push r14
;; Save float registers
sub rsp, 10h
movdqu [rsp], xmm6

;; --- RETURN without GOSUB -->
call __after_return_without_gosub_1
;; --- PRINT "Error: RETURN without GOSUB" -->
;; Evaluate arguments (_printf_lib)
;; Defer evaluation of argument 0: _fmt_Str
;; Defer evaluation of argument 1: "Error: RETURN without GOSUB"
;; Move arguments to argument passing registers (_printf_lib)
;; _fmt_Str
mov rcx, __fmt_Str
;; "Error: RETURN without GOSUB"
mov rdx, __string_3
;; Allocate shadow space (_printf_lib)
sub rsp, 20h
call [_printf_lib]
;; Clean up shadow space (_printf_lib)
add rsp, 20h
;; Ignore return value
;; <-- PRINT "Error: RETURN without GOSUB" ---

;; --- exit(1) -->
;; Evaluate arguments (_exit_lib)
;; Defer evaluation of argument 0: 1
;; Move arguments to argument passing registers (_exit_lib)
;; 1
mov rcx, 1
;; Allocate shadow space (_exit_lib)
sub rsp, 20h
call [_exit_lib]
;; Clean up shadow space (_exit_lib)
add rsp, 20h
;; Ignore return value
;; <-- exit(1) ---

__after_return_without_gosub_1:
;; Align stack by making a second call
call __after_return_without_gosub_2
ret
__after_return_without_gosub_2:
;; <-- RETURN without GOSUB ---

;; 1: REM 

;; 3: CONST MAX : I64 = 100


;; 7: RANDOMIZE timer()
;; --- randomize(timer()) -->
;; Evaluate arguments (_randomize_lib)

;; --- 7: timer() -->
;; Allocate shadow space (_timer_lib)
sub rsp, 20h
call [_timer_lib]
;; Clean up shadow space (_timer_lib)
add rsp, 20h
;; Move return value (xmm0) to storage location (xmm6)
movsd xmm6, xmm0
;; <-- 7: timer() ---

;; Move arguments to argument passing registers (_randomize_lib)
movsd xmm0, xmm6
;; Allocate shadow space (_randomize_lib)
sub rsp, 20h
call [_randomize_lib]
;; Clean up shadow space (_randomize_lib)
add rsp, 20h
;; Ignore return value
;; <-- randomize(timer()) ---

;; 9: REM 

;; 10: 0
mov rbx, 0
;; 10: i% = 0
mov [_i%], rbx

_before_while_0:
;; 11: i%
mov rbx, [_i%]
;; 11: MAX
mov rdi, [_MAX]
;; 11: i% < MAX
cmp rbx, rdi
jl @f
mov rbx, 0
jmp _after_cmp_2
@@:
mov rbx, -1
_after_cmp_2:

;; 11: WHILE i% < MAX numbers%(i%) = int(rnd() * 1000) + ...
cmp rbx, 0
je _after_while_1


;; --- 12: int(rnd() * 1000) -->
;; Evaluate arguments (_int_F64_lib)

;; --- 12: rnd() -->
;; Allocate shadow space (_rnd_lib)
sub rsp, 20h
call [_rnd_lib]
;; Clean up shadow space (_rnd_lib)
add rsp, 20h
;; Move return value (xmm0) to storage location (xmm6)
movsd xmm6, xmm0
;; <-- 12: rnd() ---

;; 12: 1000
mov rdi, 1000
;; 12: rnd() * 1000
cvtsi2sd xmm4, rdi
mulsd xmm6, xmm4
;; Move arguments to argument passing registers (_int_F64_lib)
movsd xmm0, xmm6
;; Allocate shadow space (_int_F64_lib)
sub rsp, 20h
call [_int_F64_lib]
;; Clean up shadow space (_int_F64_lib)
add rsp, 20h
;; Move return value (rax) to storage location (rbx)
mov rbx, rax
;; <-- 12: int(rnd() * 1000) ---

;; 12: 1
mov rdi, 1
;; 12: int(rnd() * 1000) + 1
add rbx, rdi
;; 12: numbers%(i%) = int(rnd() * 1000) + 1
;; 12: numbers%(i%)
;; 12: i%
mov rdi, [_i%]
mov [_numbers%_arr+8*rdi], rbx

;; 13: i%
mov rbx, [_i%]
;; 13: 1
mov rdi, 1
;; 13: i% + 1
add rbx, rdi
;; 13: i% = i% + 1
mov [_i%], rbx

jmp _before_while_0
_after_while_1:

;; --- 16: PRINT "Unsorted:" -->
;; Evaluate arguments (_printf_lib)
;; Defer evaluation of argument 0: _fmt_Str
;; Defer evaluation of argument 1: "Unsorted:"
;; Move arguments to argument passing registers (_printf_lib)
;; 16: _fmt_Str
mov rcx, __fmt_Str
;; 16: "Unsorted:"
mov rdx, __string_0
;; Allocate shadow space (_printf_lib)
sub rsp, 20h
call [_printf_lib]
;; Clean up shadow space (_printf_lib)
add rsp, 20h
;; Ignore return value
;; <-- 16: PRINT "Unsorted:" ---

;; 17: GOSUB printNumbers
call __line_gosub_printNumbers

;; 19: REM 

;; 20: 0
mov rbx, 0
;; 20: i% = 0
mov [_i%], rbx

_before_while_3:
;; 21: i%
mov rbx, [_i%]
;; 21: MAX
mov rdi, [_MAX]
;; 21: i% < MAX
cmp rbx, rdi
jl @f
mov rbx, 0
jmp _after_cmp_5
@@:
mov rbx, -1
_after_cmp_5:

;; 21: WHILE i% < MAX j% = MAX - 1 : WHILE j% > i% IF num...
cmp rbx, 0
je _after_while_4

;; 22: MAX
mov rbx, [_MAX]
;; 22: 1
mov rdi, 1
;; 22: MAX - 1
sub rbx, rdi
;; 22: j% = MAX - 1
mov [_j%], rbx

_before_while_6:
;; 23: j%
mov rbx, [_j%]
;; 23: i%
mov rdi, [_i%]
;; 23: j% > i%
cmp rbx, rdi
jg @f
mov rbx, 0
jmp _after_cmp_8
@@:
mov rbx, -1
_after_cmp_8:

;; 23: WHILE j% > i% IF numbers%(j%) < numbers%(j% - 1) T...
cmp rbx, 0
je _after_while_7

;; 24: numbers%(j%)
;; 24: j%
mov rdi, [_j%]
mov rbx, [_numbers%_arr+8*rdi]
;; 24: numbers%(j% - 1)
;; 24: j%
mov rsi, [_j%]
;; 24: 1
mov r13, 1
;; 24: j% - 1
sub rsi, r13
mov rdi, [_numbers%_arr+8*rsi]
;; 24: numbers%(j%) < numbers%(j% - 1)
cmp rbx, rdi
jl @f
mov rbx, 0
jmp _after_cmp_11
@@:
mov rbx, -1
_after_cmp_11:

;; 24: IF numbers%(j%) < numbers%(j% - 1) THEN SWAP numbe...
cmp rbx, 0
je _after_then_9

;; 25: SWAP numbers%(j%), numbers%(j% - 1)
;; 25: numbers%(j%)
;; 25: j%
mov rbx, [_j%]
;; 25: numbers%(j% - 1)
;; 25: j%
mov rsi, [_j%]
;; 25: 1
mov r13, 1
;; 25: j% - 1
sub rsi, r13
;; Swapping numbers%(j%) and numbers%(j% - 1)
mov rcx, [_numbers%_arr+8*rbx]
mov rdx, [_numbers%_arr+8*rsi]
mov [_numbers%_arr+8*rsi], rcx
mov [_numbers%_arr+8*rbx], rdx

_after_then_9:

_after_else_10:

;; 27: j%
mov rbx, [_j%]
;; 27: 1
mov rdi, 1
;; 27: j% - 1
sub rbx, rdi
;; 27: j% = j% - 1
mov [_j%], rbx

jmp _before_while_6
_after_while_7:

;; 29: i%
mov rbx, [_i%]
;; 29: 1
mov rdi, 1
;; 29: i% + 1
add rbx, rdi
;; 29: i% = i% + 1
mov [_i%], rbx

jmp _before_while_3
_after_while_4:

;; --- 32: PRINT "Sorted:" -->
;; Evaluate arguments (_printf_lib)
;; Defer evaluation of argument 0: _fmt_Str
;; Defer evaluation of argument 1: "Sorted:"
;; Move arguments to argument passing registers (_printf_lib)
;; 32: _fmt_Str
mov rcx, __fmt_Str
;; 32: "Sorted:"
mov rdx, __string_1
;; Allocate shadow space (_printf_lib)
sub rsp, 20h
call [_printf_lib]
;; Clean up shadow space (_printf_lib)
add rsp, 20h
;; Ignore return value
;; <-- 32: PRINT "Sorted:" ---

;; 33: GOSUB printNumbers
call __line_gosub_printNumbers

;; --- 35: END -->
;; Evaluate arguments (_exit_lib)
;; Defer evaluation of argument 0: 0
;; Move arguments to argument passing registers (_exit_lib)
;; 35: 0
mov rcx, 0
;; Allocate shadow space (_exit_lib)
sub rsp, 20h
call [_exit_lib]
;; Clean up shadow space (_exit_lib)
add rsp, 20h
;; Ignore return value
;; <-- 35: END ---

;; 37: REM 

;; 38: REM 

;; 39: REM 

;; 40: REM 

__line_printNumbers:
;; 43: ""
mov rbx, __string_2
;; 43: s$ = ""
mov [_s$], rbx

;; Make sure s$ does not refer to dynamic memory
mov rcx, 0h
mov [__s$_type], rcx


;; 44: 0
mov rbx, 0
;; 44: x% = 0
mov [_x%], rbx

_before_while_12:
;; 45: x%
mov rbx, [_x%]

;; --- 45: ubound(numbers%) -->
;; Evaluate arguments (_ubound_lib)
;; Defer evaluation of argument 0: numbers%
;; Move arguments to argument passing registers (_ubound_lib)
;; 45: numbers%
mov rcx, _numbers%_arr
;; Allocate shadow space (_ubound_lib)
sub rsp, 20h
call [_ubound_lib]
;; Clean up shadow space (_ubound_lib)
add rsp, 20h
;; Move return value (rax) to storage location (rdi)
mov rdi, rax
;; <-- 45: ubound(numbers%) ---

;; 45: x% < ubound(numbers%)
cmp rbx, rdi
jl @f
mov rbx, 0
jmp _after_cmp_14
@@:
mov rbx, -1
_after_cmp_14:

;; 45: WHILE x% < ubound(numbers%) s$ = s$ + str$(numbers...
cmp rbx, 0
je _after_while_13


;; --- s$ + str$(numbers%(x%)) -->
;; 46: s$
mov rbx, [_s$]

;; --- 46: str$(numbers%(x%)) -->
;; Evaluate arguments (_str_I64_lib)
;; 46: numbers%(x%)
;; 46: x%
mov r13, [_x%]
mov r12, [_numbers%_arr+8*r13]
;; Move arguments to argument passing registers (_str_I64_lib)
mov rcx, r12
;; Allocate shadow space (_str_I64_lib)
sub rsp, 20h
call [_str_I64_lib]
;; Clean up shadow space (_str_I64_lib)
add rsp, 20h
;; Move return value (rax) to storage location (rdi)
mov rdi, rax
;; <-- 46: str$(numbers%(x%)) ---

;; Calculate length of strings to add (rbx and rdi)
mov rcx, rbx
;; strlen address already in rcx
sub rsp, 20h
call [_strlen_lib]
add rsp, 20h
;; Move length (rax) to tmp location (rsi)
mov rsi, rax
mov rcx, rdi
;; strlen address already in rcx
sub rsp, 20h
call [_strlen_lib]
add rsp, 20h
;; Add length (rax) to tmp location (rsi)
add rsi, rax
inc rsi
mov rcx, rsi
;; malloc size already in rcx
sub rsp, 20h
call [_malloc_lib]
add rsp, 20h
;; Copy left string (rbx) to result string (rax)
mov rcx, rax
mov rdx, rbx
;; strcpy destination already in rcx
;; strcpy source already in rdx
sub rsp, 20h
call [_strcpy_lib]
add rsp, 20h
;; Copy right string (rdi) to result string (rax)
mov rcx, rax
mov rdx, rdi
;; strcat destination already in rcx
;; strcat source already in rdx
sub rsp, 20h
call [_strcat_lib]
add rsp, 20h
;; Move result string (rax) to tmp location (rsi)
mov rsi, rax
;; Free dynamic memory in rdi
mov rcx, rdi
;; free address already in rcx
sub rsp, 20h
call [_free_lib]
add rsp, 20h
;; Move result string to expected storage location (rbx)
mov rbx, rsi
;; <-- s$ + str$(numbers%(x%)) ---

;; 46: s$ = s$ + str$(numbers%(x%))
mov [_s$], rbx

;; Register dynamic memory assigned to s$
lea rcx, [_s$]
lea rdx, [__s$_type]
sub rsp, 20h
call __memory_register_I64_I64
add rsp, 20h

;; 47: x%
mov rbx, [_x%]
;; 47: 1
mov rdi, 1
;; 47: x% + 1
add rbx, rdi
;; 47: x% = x% + 1
mov [_x%], rbx

jmp _before_while_12
_after_while_13:

;; --- 49: PRINT s$ -->
;; Evaluate arguments (_printf_lib)
;; Defer evaluation of argument 0: _fmt_Str
;; Defer evaluation of argument 1: s$
;; Move arguments to argument passing registers (_printf_lib)
;; 49: _fmt_Str
mov rcx, __fmt_Str
;; 49: s$
mov rdx, [_s$]
;; Allocate shadow space (_printf_lib)
sub rsp, 20h
call [_printf_lib]
;; Clean up shadow space (_printf_lib)
add rsp, 20h
;; Ignore return value
;; <-- 49: PRINT s$ ---

ret


;; --- GOSUB bridge calls -->
__line_gosub_printNumbers:
call __line_printNumbers
ret
;; <-- GOSUB bridge calls ---

;; --- Built-in functions -->

;; memory_mark(I64, I64) -> I64
__memory_mark_I64_I64:
__mem_mark_loop:
sub rdx, 8h
cmp rdx, rcx
je __mem_mark_done
mov rax, [rdx]
cmp rax, 0h
je __mem_mark_loop
mov [rax+10h], byte 1
jmp __mem_mark_loop
__mem_mark_done:
ret

;; memory_register(I64, I64) -> I64
__memory_register_I64_I64:
;; Save base pointer
push rbp
mov rbp, rsp
;; Save 2 argument(s) in home location(s)
mov [rbp+10h], rcx
mov [rbp+18h], rdx

mov rcx, 18h
sub rsp, 20h
call [_malloc_lib]
add rsp, 20h
mov rcx, [rbp+10h]
mov r10, [rcx]
mov [rax+8h], r10
mov rcx, 0
mov [rax+10h], rcx
mov rdx, [rbp+18h]
mov [rdx], rax
mov r10, [__gc_allocation_list]
mov [rax], r10
mov [__gc_allocation_list], rax
inc qword [__gc_allocation_count]
mov r10, [__gc_allocation_count]
cmp r10, [__gc_allocation_limit]
jl __mem_reg_done
mov rcx, __gc_type_pointers_start
mov rdx, __gc_type_pointers_stop
sub rsp, 20h
call __memory_mark_I64_I64
add rsp, 20h
sub rsp, 20h
call __memory_sweep_I64_I64
add rsp, 20h
mov r10, [__gc_allocation_count]
imul r10, 2
mov [__gc_allocation_limit], r10
__mem_reg_done:
;; Restore base pointer
pop rbp
ret

;; memory_sweep(I64, I64) -> I64
__memory_sweep_I64_I64:
push rdi
push rbx
mov rdi, 0
mov rbx, [__gc_allocation_list]
__mem_sweep_loop:
cmp rbx, 0
je __mem_sweep_done
cmp [rbx+10h], byte 1
je __mem_sweep_marked
cmp rdi, 0
je __mem_sweep_unmarked_root
;; previous->next = current->next
mov rcx, [rbx]
mov [rdi], rcx
jmp __mem_sweep_free_node
__mem_sweep_unmarked_root:
;; root->next = current->next
mov rcx, [rbx]
mov [__gc_allocation_list], rcx
__mem_sweep_free_node:
;; Free managed memory
mov rcx, [rbx+8h]
;; free address already in rcx
sub rsp, 20h
call [_free_lib]
add rsp, 20h
;; Free swept node
mov rcx, rbx
sub rsp, 20h
call [_free_lib]
add rsp, 20h
dec qword [__gc_allocation_count]
cmp rdi, 0
je __mem_sweep_root_again
;; Look at next node
mov rbx, [rdi]
jmp __mem_sweep_loop
__mem_sweep_root_again:
;; Look at root again
mov rbx, [__gc_allocation_list]
jmp __mem_sweep_loop
__mem_sweep_marked:
;; Unmark node
mov [rbx+10h], byte 0
;; Look at next node
mov rdi, rbx
mov rbx, [rbx]
jmp __mem_sweep_loop
__mem_sweep_done:
pop rbx
pop rdi
ret

;; <-- Built-in functions ---
