;;; JCC version: 0.8.3-SNAPSHOT
;;; Date & time: 2024-10-13T15:48:05.223784
;;; Source file: inkey.bas
format PE64 console
entry __main
include 'win64a.inc'

section '.idata' import data readable

library jccbasic,'jccbasic.dll',\
msvcrt,'msvcrt.dll'

import jccbasic,\
_inkey$_lib,'inkey$',\
_sleep_F64_lib,'sleep_F64'

import msvcrt,\
_exit_lib,'exit',\
_free_lib,'free',\
_malloc_lib,'malloc',\
_printf_lib,'printf',\
_strcmp_lib,'strcmp'

section '.data' data readable writeable

__empty db "",0
__float_0 dq 0.0
__fmt_Str db "%s",10,0
__fmt_Str_Str db "%s%s",10,0
__gc_allocation_count dq 0
__gc_allocation_limit dq 100
__gc_allocation_list dq 0
__gc_count_msg db "GC: Allocation count reached limit: %d - collecting",10,0
__gc_limit_msg db "GC: Collection finished with new limit: %d",10,0
__gc_marked_msg db "GC: Marking memory: %x",10,0
__gc_register_msg db "GC: Registering new memory: %x",10,0
__gc_sweeping_msg db "GC: Sweeping memory: %x",10,0
__string_0 db "Press any key to continue (q to quit)",0
__string_1 db "q",0
__string_2 db "You pressed: ",0
_key$ dq __empty

;; --- Dynamic memory type pointers -->
__gc_type_pointers_start dq 0h
__key$_type dq 0h
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

;; 2: REM 


;; --- 6: PRINT "Press any key to continue (q to quit)" -->
;; Evaluate arguments (_printf_lib)
;; Defer evaluation of argument 0: _fmt_Str
;; Defer evaluation of argument 1: "Press any key to continue (q to quit)"
;; Move arguments to argument passing registers (_printf_lib)
;; 6: _fmt_Str
mov rcx, __fmt_Str
;; 6: "Press any key to continue (q to quit)"
mov rdx, __string_0
;; Allocate shadow space (_printf_lib)
sub rsp, 20h
call [_printf_lib]
;; Clean up shadow space (_printf_lib)
add rsp, 20h
;; Ignore return value
;; <-- 6: PRINT "Press any key to continue (q to quit)" ---

_before_while_0:
;; --- 7: key$ != "q" -->
;; Evaluate arguments (_strcmp_lib)
;; Defer evaluation of argument 0: key$
;; Defer evaluation of argument 1: "q"
;; Move arguments to argument passing registers (_strcmp_lib)
;; 7: key$
mov rcx, [_key$]
;; 7: "q"
mov rdx, __string_1
;; Allocate shadow space (_strcmp_lib)
sub rsp, 20h
call [_strcmp_lib]
;; Clean up shadow space (_strcmp_lib)
add rsp, 20h
;; Move return value (rax) to storage location (rbx)
mov rbx, rax
;; <-- 7: key$ != "q" ---
cmp rbx, 0
jne @f
mov rbx, 0
jmp _after_cmp_2
@@:
mov rbx, -1
_after_cmp_2:

;; 7: WHILE key$ != "q" SLEEP : key$ = inkey$() : PRINT ...
cmp rbx, 0
je _after_while_1

;; 8: SLEEP
;; --- _sleep_F64_lib(0.0) -->
;; Evaluate arguments (_sleep_F64_lib)
;; Defer evaluation of argument 0: 0.0
;; Move arguments to argument passing registers (_sleep_F64_lib)
;; 8: 0.0
movsd xmm0, [__float_0]
;; Allocate shadow space (_sleep_F64_lib)
sub rsp, 20h
call [_sleep_F64_lib]
;; Clean up shadow space (_sleep_F64_lib)
add rsp, 20h
;; Ignore return value
;; <-- _sleep_F64_lib(0.0) ---


;; --- 9: inkey$() -->
;; Allocate shadow space (_inkey$_lib)
sub rsp, 20h
call [_inkey$_lib]
;; Clean up shadow space (_inkey$_lib)
add rsp, 20h
;; Move return value (rax) to storage location (rbx)
mov rbx, rax
;; <-- 9: inkey$() ---

;; 9: key$ = inkey$()
mov [_key$], rbx

;; Register dynamic memory assigned to key$
lea rcx, [_key$]
lea rdx, [__key$_type]
sub rsp, 20h
call __memory_register_I64_I64
add rsp, 20h

;; --- 10: PRINT "You pressed: ", key$ -->
;; Evaluate arguments (_printf_lib)
;; Defer evaluation of argument 0: _fmt_Str_Str
;; Defer evaluation of argument 1: "You pressed: "
;; Defer evaluation of argument 2: key$
;; Move arguments to argument passing registers (_printf_lib)
;; 10: _fmt_Str_Str
mov rcx, __fmt_Str_Str
;; 10: "You pressed: "
mov rdx, __string_2
;; 10: key$
mov r8, [_key$]
;; Allocate shadow space (_printf_lib)
sub rsp, 20h
call [_printf_lib]
;; Clean up shadow space (_printf_lib)
add rsp, 20h
;; Ignore return value
;; <-- 10: PRINT "You pressed: ", key$ ---

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
