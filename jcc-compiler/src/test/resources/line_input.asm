;;; JCC version: 0.8.2-SNAPSHOT
;;; Date & time: 2024-03-16T18:00:18.216363
;;; Source file: line_input.bas
format PE64 console
entry __main
include 'win64a.inc'

section '.idata' import data readable

library msvcrt,'msvcrt.dll'

import msvcrt,\
_exit_lib,'exit',\
_fflush_lib,'fflush',\
_free_lib,'free',\
_getchar_lib,'getchar',\
_malloc_lib,'malloc',\
_printf_lib,'printf',\
_realloc_lib,'realloc'

section '.data' data readable writeable

__empty db "",0
__fmt_Str_Str_Str db "%s%s%s",10,0
__fmt_input_prompt db "%s",0
__gc_allocation_count dq 0
__gc_allocation_limit dq 100
__gc_allocation_list dq 0
__gc_count_msg db "GC: Allocation count reached limit: %d - collecting",10,0
__gc_limit_msg db "GC: Collection finished with new limit: %d",10,0
__gc_marked_msg db "GC: Marking memory: %x",10,0
__gc_register_msg db "GC: Registering new memory: %x",10,0
__gc_sweeping_msg db "GC: Sweeping memory: %x",10,0
__string_0 db "Enter first name: ",0
__string_1 db "Enter last name: ",0
__string_2 db " ",0
_a$ dq __empty
_b$ dq __empty

;; --- Dynamic memory type pointers -->
__gc_type_pointers_start dq 0h
__a$_type dq 0h
__b$_type dq 0h
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

;; 3: LINE INPUT "Enter first name: "; a$
;; --- printf("Enter first name: ") -->
;; Evaluate arguments (_printf_lib)
;; Defer evaluation of argument 0: _fmt_input_prompt
;; Defer evaluation of argument 1: "Enter first name: "
;; Move arguments to argument passing registers (_printf_lib)
;; 3: _fmt_input_prompt
mov rcx, __fmt_input_prompt
;; 3: "Enter first name: "
mov rdx, __string_0
;; Allocate shadow space (_printf_lib)
sub rsp, 20h
call [_printf_lib]
;; Clean up shadow space (_printf_lib)
add rsp, 20h
;; Ignore return value
;; <-- printf("Enter first name: ") ---

;; --- getline() -->
;; Allocate shadow space (_getline_)
sub rsp, 20h
call __getline_
;; Clean up shadow space (_getline_)
add rsp, 20h
;; Move return value (rax) to storage location (rbx)
mov rbx, rax
;; <-- getline() ---
mov [_a$], rbx

;; Register dynamic memory assigned to a$
lea rcx, [_a$]
lea rdx, [__a$_type]
sub rsp, 20h
call __memory_register_I64_I64
add rsp, 20h

;; 4: LINE INPUT "Enter last name: "; b$
;; --- printf("Enter last name: ") -->
;; Evaluate arguments (_printf_lib)
;; Defer evaluation of argument 0: _fmt_input_prompt
;; Defer evaluation of argument 1: "Enter last name: "
;; Move arguments to argument passing registers (_printf_lib)
;; 4: _fmt_input_prompt
mov rcx, __fmt_input_prompt
;; 4: "Enter last name: "
mov rdx, __string_1
;; Allocate shadow space (_printf_lib)
sub rsp, 20h
call [_printf_lib]
;; Clean up shadow space (_printf_lib)
add rsp, 20h
;; Ignore return value
;; <-- printf("Enter last name: ") ---

;; --- getline() -->
;; Allocate shadow space (_getline_)
sub rsp, 20h
call __getline_
;; Clean up shadow space (_getline_)
add rsp, 20h
;; Move return value (rax) to storage location (rbx)
mov rbx, rax
;; <-- getline() ---
mov [_b$], rbx

;; Register dynamic memory assigned to b$
lea rcx, [_b$]
lea rdx, [__b$_type]
sub rsp, 20h
call __memory_register_I64_I64
add rsp, 20h

;; --- 6: PRINT a$, " ", b$ -->
;; Evaluate arguments (_printf_lib)
;; Defer evaluation of argument 0: _fmt_Str_Str_Str
;; Defer evaluation of argument 1: a$
;; Defer evaluation of argument 2: " "
;; Defer evaluation of argument 3: b$
;; Move arguments to argument passing registers (_printf_lib)
;; 6: _fmt_Str_Str_Str
mov rcx, __fmt_Str_Str_Str
;; 6: a$
mov rdx, [_a$]
;; 6: " "
mov r8, __string_2
;; 6: b$
mov r9, [_b$]
;; Allocate shadow space (_printf_lib)
sub rsp, 20h
call [_printf_lib]
;; Clean up shadow space (_printf_lib)
add rsp, 20h
;; Ignore return value
;; <-- 6: PRINT a$, " ", b$ ---

;; 7: SWAP a$, b$
;; Swapping a$ and b$
mov rcx, [_a$]
mov rdx, [_b$]
mov [_b$], rcx
mov [_a$], rdx
;; Swapping variable type pointers __a$_type and __b$_type
mov rcx, [__a$_type]
mov rdx, [__b$_type]
mov [__b$_type], rcx
mov [__a$_type], rdx

;; --- 8: PRINT a$, " ", b$ -->
;; Evaluate arguments (_printf_lib)
;; Defer evaluation of argument 0: _fmt_Str_Str_Str
;; Defer evaluation of argument 1: a$
;; Defer evaluation of argument 2: " "
;; Defer evaluation of argument 3: b$
;; Move arguments to argument passing registers (_printf_lib)
;; 8: _fmt_Str_Str_Str
mov rcx, __fmt_Str_Str_Str
;; 8: a$
mov rdx, [_a$]
;; 8: " "
mov r8, __string_2
;; 8: b$
mov r9, [_b$]
;; Allocate shadow space (_printf_lib)
sub rsp, 20h
call [_printf_lib]
;; Clean up shadow space (_printf_lib)
add rsp, 20h
;; Ignore return value
;; <-- 8: PRINT a$, " ", b$ ---

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
