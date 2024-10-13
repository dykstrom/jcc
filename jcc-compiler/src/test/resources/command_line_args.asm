;;; JCC version: 0.8.3-SNAPSHOT
;;; Date & time: 2024-10-13T15:48:01.934849
;;; Source file: command_line_args.bas
format PE64 console
entry __main
include 'win64a.inc'

section '.idata' import data readable

library jccbasic,'jccbasic.dll',\
msvcrt,'msvcrt.dll'

import jccbasic,\
_command$_lib,'command$',\
_ltrim_lib,'ltrim',\
_rtrim_lib,'rtrim'

import msvcrt,\
_exit_lib,'exit',\
_free_lib,'free',\
_malloc_lib,'malloc',\
_printf_lib,'printf',\
_strcmp_lib,'strcmp',\
_strlen_lib,'strlen',\
_strncpy_lib,'strncpy',\
_strstr_lib,'strstr'

section '.data' data readable writeable

__empty db "",0
__err_function_mid$ db "Error: Illegal function call: mid$",0
__err_function_right$ db "Error: Illegal function call: right$",0
__fmt_ db "",10,0
__fmt_Str db "%s",10,0
__fmt_Str_I64_Str_Str_Str db "%s%lld%s%s%s",10,0
__gc_allocation_count dq 0
__gc_allocation_limit dq 100
__gc_allocation_list dq 0
__gc_count_msg db "GC: Allocation count reached limit: %d - collecting",10,0
__gc_limit_msg db "GC: Collection finished with new limit: %d",10,0
__gc_marked_msg db "GC: Marking memory: %x",10,0
__gc_register_msg db "GC: Registering new memory: %x",10,0
__gc_sweeping_msg db "GC: Sweeping memory: %x",10,0
__string_0 db " ",0
__string_1 db "'",0
__string_2 db "Arg ",0
__string_3 db " = '",0
_argc dq 0
_args dq __empty
_first dq 0
_i dq 0
_last dq 0
_argv_arr_dim_0 dq 101
_argv_arr_num_dims dq 1
_argv_arr dq 101 dup __empty

;; --- Dynamic memory type pointers -->
__gc_type_pointers_start dq 0h
__args_type dq 0h
__argv_arr_type dq 101 dup 0h
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
;; Align stack
sub rsp, 8h

;; 1: REM 

;; 2: REM 





;; 9: REM 


;; --- 10: ltrim$(rtrim$(command$())) -->
;; Evaluate arguments (_ltrim_lib)

;; --- 10: rtrim$(command$()) -->
;; Evaluate arguments (_rtrim_lib)

;; --- 10: command$() -->
;; Allocate shadow space (_command$_lib)
sub rsp, 20h
call [_command$_lib]
;; Clean up shadow space (_command$_lib)
add rsp, 20h
;; Move return value (rax) to storage location (rsi)
mov rsi, rax
;; <-- 10: command$() ---

;; Move arguments to argument passing registers (_rtrim_lib)
mov rcx, rsi
;; Allocate shadow space (_rtrim_lib)
sub rsp, 20h
call [_rtrim_lib]
;; Clean up shadow space (_rtrim_lib)
add rsp, 20h
;; Move return value (rax) to storage location (rdi)
mov rdi, rax
;; Free dynamic memory in rsi
mov rcx, rsi
;; free address already in rcx
sub rsp, 20h
call [_free_lib]
add rsp, 20h
;; <-- 10: rtrim$(command$()) ---

;; Move arguments to argument passing registers (_ltrim_lib)
mov rcx, rdi
;; Allocate shadow space (_ltrim_lib)
sub rsp, 20h
call [_ltrim_lib]
;; Clean up shadow space (_ltrim_lib)
add rsp, 20h
;; Move return value (rax) to storage location (rbx)
mov rbx, rax
;; Free dynamic memory in rdi
mov rcx, rdi
;; free address already in rcx
sub rsp, 20h
call [_free_lib]
add rsp, 20h
;; <-- 10: ltrim$(rtrim$(command$())) ---

;; 10: args = ltrim$(rtrim$(command$()))
mov [_args], rbx

;; Register dynamic memory assigned to args
lea rcx, [_args]
lea rdx, [__args_type]
sub rsp, 20h
call __memory_register_I64_I64
add rsp, 20h

;; 11: 0
mov rbx, 0
;; 11: argc = 0
mov [_argc], rbx

;; 13: REM 

;; 14: 1
mov rbx, 1
;; 14: first = 1
mov [_first], rbx

_before_while_0:
;; 15: first
mov rbx, [_first]

;; 15: WHILE first REM  : last = instr(first, args, " ") ...
cmp rbx, 0
je _after_while_1

;; 16: REM 


;; --- 17: instr(first, args, " ") -->
;; Evaluate arguments (_instr_I64_Str_Str)
;; Defer evaluation of argument 0: first
;; Defer evaluation of argument 1: args
;; Defer evaluation of argument 2: " "
;; Move arguments to argument passing registers (_instr_I64_Str_Str)
;; 17: first
mov rcx, [_first]
;; 17: args
mov rdx, [_args]
;; 17: " "
mov r8, __string_0
;; Allocate shadow space (_instr_I64_Str_Str)
sub rsp, 20h
call __instr_I64_Str_Str
;; Clean up shadow space (_instr_I64_Str_Str)
add rsp, 20h
;; Move return value (rax) to storage location (rbx)
mov rbx, rax
;; <-- 17: instr(first, args, " ") ---

;; 17: last = instr(first, args, " ")
mov [_last], rbx

;; 19: last
mov rbx, [_last]
;; 19: 0
mov rdi, 0
;; 19: last == 0
cmp rbx, rdi
je @f
mov rbx, 0
jmp _after_cmp_4
@@:
mov rbx, -1
_after_cmp_4:

;; 19: IF last == 0 THEN REM  : argv(argc) = mid$(args, f...
cmp rbx, 0
je _after_then_2

;; 20: REM 


;; --- 21: mid$(args, first) -->
;; Evaluate arguments (_mid$_Str_I64)
;; Defer evaluation of argument 0: args
;; Defer evaluation of argument 1: first
;; Move arguments to argument passing registers (_mid$_Str_I64)
;; 21: args
mov rcx, [_args]
;; 21: first
mov rdx, [_first]
;; Allocate shadow space (_mid$_Str_I64)
sub rsp, 20h
call __mid$_Str_I64
;; Clean up shadow space (_mid$_Str_I64)
add rsp, 20h
;; Move return value (rax) to storage location (rbx)
mov rbx, rax
;; <-- 21: mid$(args, first) ---

;; 21: argv(argc) = mid$(args, first)
;; 21: argv(argc)
;; 21: argc
mov rdi, [_argc]
mov [_argv_arr+8*rdi], rbx

;; Register dynamic memory assigned to argv(argc)
;; 21: argv(argc)
;; 21: argc
mov rbx, [_argc]
lea rcx, [_argv_arr+8*rbx]
lea rdx, [__argv_arr_type+8*rbx]
sub rsp, 20h
call __memory_register_I64_I64
add rsp, 20h

;; 22: argc
mov rbx, [_argc]
;; 22: 1
mov rdi, 1
;; 22: argc + 1
add rbx, rdi
;; 22: argc = argc + 1
mov [_argc], rbx

;; 23: last
mov rbx, [_last]
;; 23: first = last
mov [_first], rbx

jmp _after_else_3
_after_then_2:

;; 25: REM 


;; --- 26: mid$(args, first, last - first) -->
;; Evaluate arguments (_mid$_Str_I64_I64)
;; Defer evaluation of argument 0: args
;; Defer evaluation of argument 1: first
;; 26: last
mov rdi, [_last]
;; 26: first
mov rsi, [_first]
;; 26: last - first
sub rdi, rsi
;; Move arguments to argument passing registers (_mid$_Str_I64_I64)
;; 26: args
mov rcx, [_args]
;; 26: first
mov rdx, [_first]
mov r8, rdi
;; Allocate shadow space (_mid$_Str_I64_I64)
sub rsp, 20h
call __mid$_Str_I64_I64
;; Clean up shadow space (_mid$_Str_I64_I64)
add rsp, 20h
;; Move return value (rax) to storage location (rbx)
mov rbx, rax
;; <-- 26: mid$(args, first, last - first) ---

;; 26: argv(argc) = mid$(args, first, last - first)
;; 26: argv(argc)
;; 26: argc
mov rdi, [_argc]
mov [_argv_arr+8*rdi], rbx

;; Register dynamic memory assigned to argv(argc)
;; 26: argv(argc)
;; 26: argc
mov rbx, [_argc]
lea rcx, [_argv_arr+8*rbx]
lea rdx, [__argv_arr_type+8*rbx]
sub rsp, 20h
call __memory_register_I64_I64
add rsp, 20h

;; 27: argc
mov rbx, [_argc]
;; 27: 1
mov rdi, 1
;; 27: argc + 1
add rbx, rdi
;; 27: argc = argc + 1
mov [_argc], rbx

;; 28: last
mov rbx, [_last]
;; 28: first = last
mov [_first], rbx

;; 30: REM 

_before_while_5:
;; --- 31: mid$(args, first, 1) == " " -->
;; Evaluate arguments (_strcmp_lib)

;; --- 31: mid$(args, first, 1) -->
;; Evaluate arguments (_mid$_Str_I64_I64)
;; Defer evaluation of argument 0: args
;; Defer evaluation of argument 1: first
;; Defer evaluation of argument 2: 1
;; Move arguments to argument passing registers (_mid$_Str_I64_I64)
;; 31: args
mov rcx, [_args]
;; 31: first
mov rdx, [_first]
;; 31: 1
mov r8, 1
;; Allocate shadow space (_mid$_Str_I64_I64)
sub rsp, 20h
call __mid$_Str_I64_I64
;; Clean up shadow space (_mid$_Str_I64_I64)
add rsp, 20h
;; Move return value (rax) to storage location (rdi)
mov rdi, rax
;; <-- 31: mid$(args, first, 1) ---

;; Defer evaluation of argument 1: " "
;; Move arguments to argument passing registers (_strcmp_lib)
mov rcx, rdi
;; 31: " "
mov rdx, __string_0
;; Allocate shadow space (_strcmp_lib)
sub rsp, 20h
call [_strcmp_lib]
;; Clean up shadow space (_strcmp_lib)
add rsp, 20h
;; Move return value (rax) to storage location (rbx)
mov rbx, rax
;; Free dynamic memory in rdi
mov rcx, rdi
;; free address already in rcx
sub rsp, 20h
call [_free_lib]
add rsp, 20h
;; <-- 31: mid$(args, first, 1) == " " ---
cmp rbx, 0
je @f
mov rbx, 0
jmp _after_cmp_7
@@:
mov rbx, -1
_after_cmp_7:

;; 31: WHILE mid$(args, first, 1) == " " first = first + 1
cmp rbx, 0
je _after_while_6

;; 32: first
mov rbx, [_first]
;; 32: 1
mov rdi, 1
;; 32: first + 1
add rbx, rdi
;; 32: first = first + 1
mov [_first], rbx

jmp _before_while_5
_after_while_6:

_after_else_3:

jmp _before_while_0
_after_while_1:

;; --- 37: PRINT  -->
;; Evaluate arguments (_printf_lib)
;; Defer evaluation of argument 0: _fmt_
;; Move arguments to argument passing registers (_printf_lib)
;; 37: _fmt_
mov rcx, __fmt_
;; Allocate shadow space (_printf_lib)
sub rsp, 20h
call [_printf_lib]
;; Clean up shadow space (_printf_lib)
add rsp, 20h
;; Ignore return value
;; <-- 37: PRINT  ---

;; --- 38: PRINT args -->
;; Evaluate arguments (_printf_lib)
;; Defer evaluation of argument 0: _fmt_Str
;; Defer evaluation of argument 1: args
;; Move arguments to argument passing registers (_printf_lib)
;; 38: _fmt_Str
mov rcx, __fmt_Str
;; 38: args
mov rdx, [_args]
;; Allocate shadow space (_printf_lib)
sub rsp, 20h
call [_printf_lib]
;; Clean up shadow space (_printf_lib)
add rsp, 20h
;; Ignore return value
;; <-- 38: PRINT args ---

;; --- 39: PRINT  -->
;; Evaluate arguments (_printf_lib)
;; Defer evaluation of argument 0: _fmt_
;; Move arguments to argument passing registers (_printf_lib)
;; 39: _fmt_
mov rcx, __fmt_
;; Allocate shadow space (_printf_lib)
sub rsp, 20h
call [_printf_lib]
;; Clean up shadow space (_printf_lib)
add rsp, 20h
;; Ignore return value
;; <-- 39: PRINT  ---

;; 41: REM 

;; 42: 0
mov rbx, 0
;; 42: i = 0
mov [_i], rbx

_before_while_8:
;; 43: i
mov rbx, [_i]
;; 43: argc
mov rdi, [_argc]
;; 43: i < argc
cmp rbx, rdi
jl @f
mov rbx, 0
jmp _after_cmp_10
@@:
mov rbx, -1
_after_cmp_10:

;; 43: WHILE i < argc PRINT "Arg ", i, " = '", argv(i), "...
cmp rbx, 0
je _after_while_9

;; --- 44: PRINT "Arg ", i, " = '", argv(i), "'" -->
;; Evaluate arguments (_printf_lib)
;; Defer evaluation of argument 0: _fmt_Str_I64_Str_Str_Str
;; Defer evaluation of argument 1: "Arg "
;; Defer evaluation of argument 2: i
;; Defer evaluation of argument 3: " = '"
;; Push 2 additional argument(s) to stack
;; 44: argv(i)
;; 44: i
mov rdi, [_i]
mov rbx, [_argv_arr+8*rdi]
;; Defer evaluation of argument 5: "'"
;; 44: "'"
mov rdi, __string_1
push rdi
push rbx
;; Move arguments to argument passing registers (_printf_lib)
;; 44: _fmt_Str_I64_Str_Str_Str
mov rcx, __fmt_Str_I64_Str_Str_Str
;; 44: "Arg "
mov rdx, __string_2
;; 44: i
mov r8, [_i]
;; 44: " = '"
mov r9, __string_3
;; Allocate shadow space (_printf_lib)
sub rsp, 20h
call [_printf_lib]
;; Clean up shadow space (_printf_lib)
add rsp, 20h
;; Ignore return value
;; Clean up 2 pushed argument(s)
add rsp, 10h
;; <-- 44: PRINT "Arg ", i, " = '", argv(i), "'" ---

;; 45: i
mov rbx, [_i]
;; 45: 1
mov rdi, 1
;; 45: i + 1
add rbx, rdi
;; 45: i = i + 1
mov [_i], rbx

jmp _before_while_8
_after_while_9:

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

;; instr(I64, Str, Str) -> I64
__instr_I64_Str_Str:
push rbp
mov rbp, rsp
dec rcx
cmp rcx, 0
jl __instr3_index_underflow
jmp __instr3_index_valid
__instr3_index_underflow:
mov rax, 0
jmp __instr3_done
__instr3_index_valid:
mov [rbp+10h], rcx
mov [rbp+18h], rdx
mov [rbp+20h], r8
mov rcx, rdx
sub rsp, 20h
call [_strlen_lib]
add rsp, 20h
cmp rax, [rbp+10h]
jl __instr3_index_overflow
jmp __instr3_index_ok
__instr3_index_overflow:
mov rax, 0
jmp __instr3_done
__instr3_index_ok:
mov rcx, [rbp+18h]
add rcx, [rbp+10h]
mov rdx, [rbp+20h]
sub rsp, 20h
call [_strstr_lib]
add rsp, 20h
cmp rax, 0
je __instr3_done
sub rax, [rbp+18h]
inc rax

__instr3_done:
pop rbp
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

;; mid$(Str, I64) -> Str
__mid$_Str_I64:
;; Save base pointer
push rbp
mov rbp, rsp
;; Save 2 argument(s) in home location(s)
mov [rbp+10h], rcx
mov [rbp+18h], rdx

cmp rdx, 1h
jl __mid2$_error
;; strlen address already in rcx
sub rsp, 20h
call [_strlen_lib]
add rsp, 20h
sub rax, [rbp+18h]
inc rax
cmp rax, 0
jge __mid2$_right
xor rax, rax
__mid2$_right:
mov rcx, [rbp+10h]
mov rdx, rax
sub rsp, 20h
call __right$_Str_I64
add rsp, 20h
jmp __mid2$_done
__mid2$_error:
mov rcx, __err_function_mid$
sub rsp, 20h
call [_printf_lib]
add rsp, 20h
mov rcx, 1h
sub rsp, 20h
call [_exit_lib]
add rsp, 20h
__mid2$_done:
;; Restore base pointer
pop rbp
ret

;; mid$(Str, I64, I64) -> Str
__mid$_Str_I64_I64:
;; Save base pointer
push rbp
mov rbp, rsp
;; Save 3 argument(s) in home location(s)
mov [rbp+10h], rcx
mov [rbp+18h], rdx
mov [rbp+20h], r8

cmp rdx, 1h
jl __mid3$_error
cmp r8, 0h
jl __mid3$_error
;; strlen address already in rcx
sub rsp, 20h
call [_strlen_lib]
add rsp, 20h
mov [rbp+28h], rax
sub rax, [rbp+18h]
inc rax
cmp rax, 0
jge __mid3$_allocate
xor rax, rax
mov [rbp+20h], rax
__mid3$_allocate:
inc rax
mov rcx, rax
sub rsp, 20h
call [_malloc_lib]
add rsp, 20h
mov rdx, [rbp+10h]
add rdx, [rbp+18h]
dec rdx
mov r8, [rbp+20h]
mov rcx, rax
;; strncpy source already in rdx
;; strncpy length already in r8
sub rsp, 20h
call [_strncpy_lib]
add rsp, 20h
mov r11, rax
add r11, [rbp+20h]
mov [r11], byte 0h
jmp __mid3$_done
__mid3$_error:
mov rcx, __err_function_mid$
sub rsp, 20h
call [_printf_lib]
add rsp, 20h
mov rcx, 1h
sub rsp, 20h
call [_exit_lib]
add rsp, 20h
__mid3$_done:
;; Restore base pointer
pop rbp
ret

;; right$(Str, I64) -> Str
__right$_Str_I64:
;; Save base pointer
push rbp
mov rbp, rsp
;; Save 2 argument(s) in home location(s)
mov [rbp+10h], rcx
mov [rbp+18h], rdx

cmp rdx, 0h
jl __right$_error
;; strlen address already in rcx
sub rsp, 20h
call [_strlen_lib]
add rsp, 20h
mov [rbp+20h], rax
mov rcx, [rbp+18h]
cmp rax, rcx
jge __right$_allocate
mov rcx, rax
mov [rbp+18h], rcx
__right$_allocate:
inc rcx
;; malloc size already in rcx
sub rsp, 20h
call [_malloc_lib]
add rsp, 20h
mov rcx, [rbp+20h]
mov rdx, [rbp+10h]
mov r8, [rbp+18h]
add rdx, rcx
sub rdx, r8
mov rcx, rax
;; strncpy source already in rdx
;; strncpy length already in r8
sub rsp, 20h
call [_strncpy_lib]
add rsp, 20h
mov r11, rax
add r11, [rbp+18h]
mov [r11], byte 0h
jmp __right$_done
__right$_error:
mov rcx, __err_function_right$
sub rsp, 20h
call [_printf_lib]
add rsp, 20h
mov rcx, 1h
sub rsp, 20h
call [_exit_lib]
add rsp, 20h
__right$_done:
;; Restore base pointer
pop rbp
ret

;; <-- Built-in functions ---
