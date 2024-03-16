;;; JCC version: 0.8.2-SNAPSHOT
;;; Date & time: 2024-03-16T18:00:17.265375
;;; Source file: guess_number.bas
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
_timer_lib,'timer'

import msvcrt,\
_atof_lib,'atof',\
_exit_lib,'exit',\
_fflush_lib,'fflush',\
_free_lib,'free',\
_getchar_lib,'getchar',\
_malloc_lib,'malloc',\
_printf_lib,'printf',\
_realloc_lib,'realloc'

section '.data' data readable writeable

__cls_ansi_codes db 27,"[2J",27,"[H",0
__empty db "",0
__fmt_Str db "%s",10,0
__fmt_Str_I64_Str db "%s%lld%s",10,0
__fmt_input_prompt db "%s",0
__gc_allocation_count dq 0
__gc_allocation_limit dq 100
__gc_allocation_list dq 0
__gc_count_msg db "GC: Allocation count reached limit: %d - collecting",10,0
__gc_limit_msg db "GC: Collection finished with new limit: %d",10,0
__gc_marked_msg db "GC: Marking memory: %x",10,0
__gc_register_msg db "GC: Registering new memory: %x",10,0
__gc_sweeping_msg db "GC: Sweeping memory: %x",10,0
__string_0 db "Please guess a number between 1 and 100: ",0
__string_1 db "Too low!",0
__string_2 db "Too high!",0
__string_3 db "Right you are!",0
__string_4 db "You needed ",0
__string_5 db " guesses to get it right!",0
_guess dq 0
_numberOfGuesses dq 0
_s dq __empty
_secret dq 0

;; --- Dynamic memory type pointers -->
__gc_type_pointers_start dq 0h
__s_type dq 0h
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

;; 1: REM 





;; 8: RANDOMIZE timer()
;; --- randomize(timer()) -->
;; Evaluate arguments (_randomize_lib)

;; --- 8: timer() -->
;; Allocate shadow space (_timer_lib)
sub rsp, 20h
call [_timer_lib]
;; Clean up shadow space (_timer_lib)
add rsp, 20h
;; Move return value (xmm0) to storage location (xmm6)
movsd xmm6, xmm0
;; <-- 8: timer() ---

;; Move arguments to argument passing registers (_randomize_lib)
movsd xmm0, xmm6
;; Allocate shadow space (_randomize_lib)
sub rsp, 20h
call [_randomize_lib]
;; Clean up shadow space (_randomize_lib)
add rsp, 20h
;; Ignore return value
;; <-- randomize(timer()) ---

;; --- 9: CLS -->
;; Evaluate arguments (_printf_lib)
;; Defer evaluation of argument 0: _cls_ansi_codes
;; Move arguments to argument passing registers (_printf_lib)
;; 9: _cls_ansi_codes
mov rcx, __cls_ansi_codes
;; Allocate shadow space (_printf_lib)
sub rsp, 20h
call [_printf_lib]
;; Clean up shadow space (_printf_lib)
add rsp, 20h
;; Ignore return value
;; <-- 9: CLS ---

;; 11: REM 


;; --- 12: int(rnd() * 100) -->
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

;; 12: 100
mov rdi, 100
;; 12: rnd() * 100
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
;; <-- 12: int(rnd() * 100) ---

;; 12: 1
mov rdi, 1
;; 12: int(rnd() * 100) + 1
add rbx, rdi
;; 12: secret = int(rnd() * 100) + 1
mov [_secret], rbx

;; 13: -1
mov rbx, -1
;; 13: guess = -1
mov [_guess], rbx

;; 14: 0
mov rbx, 0
;; 14: numberOfGuesses = 0
mov [_numberOfGuesses], rbx

;; 16: REM 

_before_while_0:
;; 17: guess
mov rbx, [_guess]
;; 17: secret
mov rdi, [_secret]
;; 17: guess != secret
cmp rbx, rdi
jne @f
mov rbx, 0
jmp _after_cmp_2
@@:
mov rbx, -1
_after_cmp_2:

;; 17: WHILE guess != secret LINE INPUT "Please guess a n...
cmp rbx, 0
je _after_while_1

;; 18: LINE INPUT "Please guess a number between 1 and 10...
;; --- printf("Please guess a number between 1 and 100: ") -->
;; Evaluate arguments (_printf_lib)
;; Defer evaluation of argument 0: _fmt_input_prompt
;; Defer evaluation of argument 1: "Please guess a number between 1 and 100: "
;; Move arguments to argument passing registers (_printf_lib)
;; 18: _fmt_input_prompt
mov rcx, __fmt_input_prompt
;; 18: "Please guess a number between 1 and 100: "
mov rdx, __string_0
;; Allocate shadow space (_printf_lib)
sub rsp, 20h
call [_printf_lib]
;; Clean up shadow space (_printf_lib)
add rsp, 20h
;; Ignore return value
;; <-- printf("Please guess a number between 1 and 100: ") ---

;; --- getline() -->
;; Allocate shadow space (_getline_)
sub rsp, 20h
call __getline_
;; Clean up shadow space (_getline_)
add rsp, 20h
;; Move return value (rax) to storage location (rbx)
mov rbx, rax
;; <-- getline() ---
mov [_s], rbx

;; Register dynamic memory assigned to s
lea rcx, [_s]
lea rdx, [__s_type]
sub rsp, 20h
call __memory_register_I64_I64
add rsp, 20h


;; --- 19: val(s) -->
;; Evaluate arguments (_atof_lib)
;; Defer evaluation of argument 0: s
;; Move arguments to argument passing registers (_atof_lib)
;; 19: s
mov rcx, [_s]
;; Allocate shadow space (_atof_lib)
sub rsp, 20h
call [_atof_lib]
;; Clean up shadow space (_atof_lib)
add rsp, 20h
;; Move return value (xmm0) to storage location (xmm6)
movsd xmm6, xmm0
;; <-- 19: val(s) ---

;; Cast F64 (xmm6) to I64 (rbx)
cvtsd2si rbx, xmm6
;; 19: guess = val(s)
mov [_guess], rbx

;; 21: guess
mov rbx, [_guess]
;; 21: secret
mov rdi, [_secret]
;; 21: guess < secret
cmp rbx, rdi
jl @f
mov rbx, 0
jmp _after_cmp_5
@@:
mov rbx, -1
_after_cmp_5:

;; 21: IF guess < secret THEN PRINT "Too low!" ELSE IF gu...
cmp rbx, 0
je _after_then_3

;; --- 22: PRINT "Too low!" -->
;; Evaluate arguments (_printf_lib)
;; Defer evaluation of argument 0: _fmt_Str
;; Defer evaluation of argument 1: "Too low!"
;; Move arguments to argument passing registers (_printf_lib)
;; 22: _fmt_Str
mov rcx, __fmt_Str
;; 22: "Too low!"
mov rdx, __string_1
;; Allocate shadow space (_printf_lib)
sub rsp, 20h
call [_printf_lib]
;; Clean up shadow space (_printf_lib)
add rsp, 20h
;; Ignore return value
;; <-- 22: PRINT "Too low!" ---

jmp _after_else_4
_after_then_3:

;; 23: guess
mov rbx, [_guess]
;; 23: secret
mov rdi, [_secret]
;; 23: guess > secret
cmp rbx, rdi
jg @f
mov rbx, 0
jmp _after_cmp_8
@@:
mov rbx, -1
_after_cmp_8:

;; 23: IF guess > secret THEN PRINT "Too high!" ELSE PRIN...
cmp rbx, 0
je _after_then_6

;; --- 24: PRINT "Too high!" -->
;; Evaluate arguments (_printf_lib)
;; Defer evaluation of argument 0: _fmt_Str
;; Defer evaluation of argument 1: "Too high!"
;; Move arguments to argument passing registers (_printf_lib)
;; 24: _fmt_Str
mov rcx, __fmt_Str
;; 24: "Too high!"
mov rdx, __string_2
;; Allocate shadow space (_printf_lib)
sub rsp, 20h
call [_printf_lib]
;; Clean up shadow space (_printf_lib)
add rsp, 20h
;; Ignore return value
;; <-- 24: PRINT "Too high!" ---

jmp _after_else_7
_after_then_6:

;; --- 26: PRINT "Right you are!" -->
;; Evaluate arguments (_printf_lib)
;; Defer evaluation of argument 0: _fmt_Str
;; Defer evaluation of argument 1: "Right you are!"
;; Move arguments to argument passing registers (_printf_lib)
;; 26: _fmt_Str
mov rcx, __fmt_Str
;; 26: "Right you are!"
mov rdx, __string_3
;; Allocate shadow space (_printf_lib)
sub rsp, 20h
call [_printf_lib]
;; Clean up shadow space (_printf_lib)
add rsp, 20h
;; Ignore return value
;; <-- 26: PRINT "Right you are!" ---

_after_else_7:

_after_else_4:

;; 29: numberOfGuesses
mov rbx, [_numberOfGuesses]
;; 29: 1
mov rdi, 1
;; 29: numberOfGuesses + 1
add rbx, rdi
;; 29: numberOfGuesses = numberOfGuesses + 1
mov [_numberOfGuesses], rbx

jmp _before_while_0
_after_while_1:

;; --- 32: PRINT "You needed ", numberOfGuesses, " guesses to... -->
;; Evaluate arguments (_printf_lib)
;; Defer evaluation of argument 0: _fmt_Str_I64_Str
;; Defer evaluation of argument 1: "You needed "
;; Defer evaluation of argument 2: numberOfGuesses
;; Defer evaluation of argument 3: " guesses to get it right!"
;; Move arguments to argument passing registers (_printf_lib)
;; 32: _fmt_Str_I64_Str
mov rcx, __fmt_Str_I64_Str
;; 32: "You needed "
mov rdx, __string_4
;; 32: numberOfGuesses
mov r8, [_numberOfGuesses]
;; 32: " guesses to get it right!"
mov r9, __string_5
;; Allocate shadow space (_printf_lib)
sub rsp, 20h
call [_printf_lib]
;; Clean up shadow space (_printf_lib)
add rsp, 20h
;; Ignore return value
;; <-- 32: PRINT "You needed ", numberOfGuesses, " guesses to... ---

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


;; --- Built-in functions -->

;; getline() -> Str
__getline_:
push rbx
push rdi
push rsi
mov rcx, 0
sub rsp, 20h
call [_fflush_lib]
add rsp, 20h
mov rbx, 0
mov rdi, 64
mov rcx, rdi
sub rsp, 20h
call [_malloc_lib]
add rsp, 20h
mov rsi, rax
__getline_loop:
sub rsp, 20h
call [_getchar_lib]
add rsp, 20h
cmp al, 10
je __getline_done
cmp al, -1
je __getline_done
lea r11, [rsi+rbx]
mov [r11], al
inc rbx
cmp rbx, rdi
jl __getline_loop
sal rdi, 1
mov rcx, rsi
mov rdx, rdi
sub rsp, 20h
call [_realloc_lib]
add rsp, 20h
mov rsi, rax
jmp __getline_loop
__getline_done:
lea r11, [rsi+rbx]
mov [r11], byte 0
mov rax, rsi
pop rsi
pop rdi
pop rbx
ret

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
