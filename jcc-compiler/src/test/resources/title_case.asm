;;; JCC version: 0.8.1
;;; Date & time: 2023-12-02T15:00:51.012331
;;; Source file: title_case.bas
format PE64 console
entry __main
include 'win64a.inc'

section '.idata' import data readable

library jccbasic,'jccbasic.dll',\
msvcrt,'msvcrt.dll'

import jccbasic,\
_ltrim_lib,'ltrim',\
_rtrim_lib,'rtrim'

import msvcrt,\
_exit_lib,'exit',\
_fflush_lib,'fflush',\
_free_lib,'free',\
_getchar_lib,'getchar',\
_malloc_lib,'malloc',\
_printf_lib,'printf',\
_realloc_lib,'realloc',\
_strcat_lib,'strcat',\
_strcmp_lib,'strcmp',\
_strcpy_lib,'strcpy',\
_strlen_lib,'strlen',\
_strncpy_lib,'strncpy',\
_strstr_lib,'strstr',\
_tolower_lib,'tolower',\
_toupper_lib,'toupper'

section '.data' data readable writeable

__empty db "",0
__err_function_mid$ db "Error: Illegal function call: mid$",0
__err_function_right$ db "Error: Illegal function call: right$",0
__fmt_Str_Str db "%s%s",10,0
__fmt_input_prompt db "%s",0
__gc_allocation_count dq 0
__gc_allocation_limit dq 100
__gc_allocation_list dq 0
__gc_count_msg db "GC: Allocation count reached limit: %d - collecting",10,0
__gc_limit_msg db "GC: Collection finished with new limit: %d",10,0
__gc_marked_msg db "GC: Marking memory: %x",10,0
__gc_register_msg db "GC: Registering new memory: %x",10,0
__gc_sweeping_msg db "GC: Sweeping memory: %x",10,0
__string_0 db "Enter string: ",0
__string_1 db " ",0
__string_2 db "Source: ",0
__string_3 db "Result: ",0
_end% dq 0
_result$ dq __empty
_source$ dq __empty
_start% dq 0
_word$ dq __empty

;; --- Dynamic memory type pointers -->
__gc_type_pointers_start dq 0h
__result$_type dq 0h
__source$_type dq 0h
__word$_type dq 0h
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

;; 1: REM 

;; 2: REM 

;; 4: REM 


;; 7: REM 

;; 8: LINE INPUT "Enter string: "; source$
;; --- printf("Enter string: ") -->
;; Evaluate arguments (_printf_lib)
;; 8: _fmt_input_prompt
mov rbx, __fmt_input_prompt
;; 8: "Enter string: "
mov rdi, __string_0
;; Move arguments to argument passing registers (_printf_lib)
mov rcx, rbx
mov rdx, rdi
;; Allocate shadow space (_printf_lib)
sub rsp, 20h
call [_printf_lib]
;; Clean up shadow space (_printf_lib)
add rsp, 20h
;; Ignore return value
;; <-- printf("Enter string: ") ---

;; --- getline() -->
;; Allocate shadow space (_getline_)
sub rsp, 20h
call __getline_
;; Clean up shadow space (_getline_)
add rsp, 20h
;; Move return value (rax) to storage location (rbx)
mov rbx, rax
;; <-- getline() ---
mov [_source$], rbx

;; Register dynamic memory assigned to source$
lea rcx, [_source$]
lea rdx, [__source$_type]
sub rsp, 20h
call __memory_register_I64_I64
add rsp, 20h

;; 10: REM 


;; --- 11: ltrim$(rtrim$(source$)) -->
;; Evaluate arguments (_ltrim_lib)

;; --- 11: rtrim$(source$) -->
;; Evaluate arguments (_rtrim_lib)
;; 11: source$
mov rsi, [_source$]
;; Move arguments to argument passing registers (_rtrim_lib)
mov rcx, rsi
;; Allocate shadow space (_rtrim_lib)
sub rsp, 20h
call [_rtrim_lib]
;; Clean up shadow space (_rtrim_lib)
add rsp, 20h
;; Move return value (rax) to storage location (rdi)
mov rdi, rax
;; <-- 11: rtrim$(source$) ---

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
;; <-- 11: ltrim$(rtrim$(source$)) ---

;; 11: source$ = ltrim$(rtrim$(source$))
mov [_source$], rbx

;; Register dynamic memory assigned to source$
lea rcx, [_source$]
lea rdx, [__source$_type]
sub rsp, 20h
call __memory_register_I64_I64
add rsp, 20h

;; 13: 1
mov rbx, 1
;; 13: start% = 1
mov [_start%], rbx

_before_while_0:
;; 14: start%
mov rbx, [_start%]
;; 14: 0
mov rdi, 0
;; 14: start% != 0
cmp rbx, rdi
jne @f
mov rbx, 0
jmp _after_cmp_2
@@:
mov rbx, -1
_after_cmp_2:

;; 14: WHILE start% != 0 REM  : end% = instr(start%, sour...
cmp rbx, 0
je _after_while_1

;; 15: REM 


;; --- 16: instr(start%, source$, " ") -->
;; Evaluate arguments (_instr_I64_Str_Str)
;; 16: start%
mov rdi, [_start%]
;; 16: source$
mov rsi, [_source$]
;; 16: " "
mov r12, __string_1
;; Move arguments to argument passing registers (_instr_I64_Str_Str)
mov rcx, rdi
mov rdx, rsi
mov r8, r12
;; Allocate shadow space (_instr_I64_Str_Str)
sub rsp, 20h
call __instr_I64_Str_Str
;; Clean up shadow space (_instr_I64_Str_Str)
add rsp, 20h
;; Move return value (rax) to storage location (rbx)
mov rbx, rax
;; <-- 16: instr(start%, source$, " ") ---

;; 16: end% = instr(start%, source$, " ")
mov [_end%], rbx

;; 18: end%
mov rbx, [_end%]
;; 18: 0
mov rdi, 0
;; 18: end% == 0
cmp rbx, rdi
je @f
mov rbx, 0
jmp _after_cmp_5
@@:
mov rbx, -1
_after_cmp_5:

;; 18: IF end% == 0 THEN REM  : word$ = mid$(source$, sta...
cmp rbx, 0
je _after_then_3

;; 19: REM 


;; --- 20: mid$(source$, start%) -->
;; Evaluate arguments (_mid$_Str_I64)
;; 20: source$
mov rdi, [_source$]
;; 20: start%
mov rsi, [_start%]
;; Move arguments to argument passing registers (_mid$_Str_I64)
mov rcx, rdi
mov rdx, rsi
;; Allocate shadow space (_mid$_Str_I64)
sub rsp, 20h
call __mid$_Str_I64
;; Clean up shadow space (_mid$_Str_I64)
add rsp, 20h
;; Move return value (rax) to storage location (rbx)
mov rbx, rax
;; <-- 20: mid$(source$, start%) ---

;; 20: word$ = mid$(source$, start%)
mov [_word$], rbx

;; Register dynamic memory assigned to word$
lea rcx, [_word$]
lea rdx, [__word$_type]
sub rsp, 20h
call __memory_register_I64_I64
add rsp, 20h


;; --- result$ + FNtoTitleCase$(word$) -->
;; 21: result$
mov rbx, [_result$]

;; --- 21: FNtoTitleCase$(word$) -->
;; Evaluate arguments (_FNtoTitleCase$_Str)
;; 21: word$
mov r12, [_word$]
;; Move arguments to argument passing registers (_FNtoTitleCase$_Str)
mov rcx, r12
;; Allocate shadow space (_FNtoTitleCase$_Str)
sub rsp, 20h
call __FNtoTitleCase$_Str
;; Clean up shadow space (_FNtoTitleCase$_Str)
add rsp, 20h
;; Move return value (rax) to storage location (rdi)
mov rdi, rax
;; <-- 21: FNtoTitleCase$(word$) ---

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
;; <-- result$ + FNtoTitleCase$(word$) ---

;; 21: result$ = result$ + FNtoTitleCase$(word$)
mov [_result$], rbx

;; Register dynamic memory assigned to result$
lea rcx, [_result$]
lea rdx, [__result$_type]
sub rsp, 20h
call __memory_register_I64_I64
add rsp, 20h

;; 22: end%
mov rbx, [_end%]
;; 22: start% = end%
mov [_start%], rbx

jmp _after_else_4
_after_then_3:

;; 24: REM 


;; --- 25: mid$(source$, start%, end% - start%) -->
;; Evaluate arguments (_mid$_Str_I64_I64)
;; 25: source$
mov rdi, [_source$]
;; 25: start%
mov rsi, [_start%]
;; 25: end%
mov r12, [_end%]
;; 25: start%
mov r13, [_start%]
;; 25: end% - start%
sub r12, r13
;; Move arguments to argument passing registers (_mid$_Str_I64_I64)
mov rcx, rdi
mov rdx, rsi
mov r8, r12
;; Allocate shadow space (_mid$_Str_I64_I64)
sub rsp, 20h
call __mid$_Str_I64_I64
;; Clean up shadow space (_mid$_Str_I64_I64)
add rsp, 20h
;; Move return value (rax) to storage location (rbx)
mov rbx, rax
;; <-- 25: mid$(source$, start%, end% - start%) ---

;; 25: word$ = mid$(source$, start%, end% - start%)
mov [_word$], rbx

;; Register dynamic memory assigned to word$
lea rcx, [_word$]
lea rdx, [__word$_type]
sub rsp, 20h
call __memory_register_I64_I64
add rsp, 20h


;; --- result$ + FNtoTitleCase$(word$) + " " -->

;; --- result$ + FNtoTitleCase$(word$) -->
;; 26: result$
mov rbx, [_result$]

;; --- 26: FNtoTitleCase$(word$) -->
;; Evaluate arguments (_FNtoTitleCase$_Str)
;; 26: word$
mov r12, [_word$]
;; Move arguments to argument passing registers (_FNtoTitleCase$_Str)
mov rcx, r12
;; Allocate shadow space (_FNtoTitleCase$_Str)
sub rsp, 20h
call __FNtoTitleCase$_Str
;; Clean up shadow space (_FNtoTitleCase$_Str)
add rsp, 20h
;; Move return value (rax) to storage location (rdi)
mov rdi, rax
;; <-- 26: FNtoTitleCase$(word$) ---

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
;; <-- result$ + FNtoTitleCase$(word$) ---

;; 26: " "
mov rdi, __string_1
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
;; Free dynamic memory in rbx
mov rcx, rbx
;; free address already in rcx
sub rsp, 20h
call [_free_lib]
add rsp, 20h
;; Move result string to expected storage location (rbx)
mov rbx, rsi
;; <-- result$ + FNtoTitleCase$(word$) + " " ---

;; 26: result$ = result$ + FNtoTitleCase$(word$) + " "
mov [_result$], rbx

;; Register dynamic memory assigned to result$
lea rcx, [_result$]
lea rdx, [__result$_type]
sub rsp, 20h
call __memory_register_I64_I64
add rsp, 20h

;; 27: end%
mov rbx, [_end%]
;; 27: start% = end%
mov [_start%], rbx

;; 29: REM 

_before_while_6:
;; --- 30: mid$(source$, start%, 1) == " " -->
;; Evaluate arguments (_strcmp_lib)

;; --- 30: mid$(source$, start%, 1) -->
;; Evaluate arguments (_mid$_Str_I64_I64)
;; 30: source$
mov rsi, [_source$]
;; 30: start%
mov r12, [_start%]
;; 30: 1
mov r13, 1
;; Move arguments to argument passing registers (_mid$_Str_I64_I64)
mov rcx, rsi
mov rdx, r12
mov r8, r13
;; Allocate shadow space (_mid$_Str_I64_I64)
sub rsp, 20h
call __mid$_Str_I64_I64
;; Clean up shadow space (_mid$_Str_I64_I64)
add rsp, 20h
;; Move return value (rax) to storage location (rdi)
mov rdi, rax
;; <-- 30: mid$(source$, start%, 1) ---

;; 30: " "
mov rsi, __string_1
;; Move arguments to argument passing registers (_strcmp_lib)
mov rcx, rdi
mov rdx, rsi
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
;; <-- 30: mid$(source$, start%, 1) == " " ---
cmp rbx, 0
je @f
mov rbx, 0
jmp _after_cmp_8
@@:
mov rbx, -1
_after_cmp_8:

;; 30: WHILE mid$(source$, start%, 1) == " " start% = sta...
cmp rbx, 0
je _after_while_7

;; 31: start%
mov rbx, [_start%]
;; 31: 1
mov rdi, 1
;; 31: start% + 1
add rbx, rdi
;; 31: start% = start% + 1
mov [_start%], rbx

jmp _before_while_6
_after_while_7:

_after_else_4:

jmp _before_while_0
_after_while_1:

;; --- 36: PRINT "Source: ", source$ -->
;; Evaluate arguments (_printf_lib)
;; 36: _fmt_Str_Str
mov rbx, __fmt_Str_Str
;; 36: "Source: "
mov rdi, __string_2
;; 36: source$
mov rsi, [_source$]
;; Move arguments to argument passing registers (_printf_lib)
mov rcx, rbx
mov rdx, rdi
mov r8, rsi
;; Allocate shadow space (_printf_lib)
sub rsp, 20h
call [_printf_lib]
;; Clean up shadow space (_printf_lib)
add rsp, 20h
;; Ignore return value
;; <-- 36: PRINT "Source: ", source$ ---

;; --- 37: PRINT "Result: ", result$ -->
;; Evaluate arguments (_printf_lib)
;; 37: _fmt_Str_Str
mov rbx, __fmt_Str_Str
;; 37: "Result: "
mov rdi, __string_3
;; 37: result$
mov rsi, [_result$]
;; Move arguments to argument passing registers (_printf_lib)
mov rcx, rbx
mov rdx, rdi
mov r8, rsi
;; Allocate shadow space (_printf_lib)
sub rsp, 20h
call [_printf_lib]
;; Clean up shadow space (_printf_lib)
add rsp, 20h
;; Ignore return value
;; <-- 37: PRINT "Result: ", result$ ---

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

;; ucase$(Str) -> Str
__ucase$_Str:
push rbp
mov rbp, rsp
mov [rbp+10h], rcx
push rdi
push rsi
sub rsp, 20h
call [_strlen_lib]
add rsp, 20h
inc rax
mov rcx, rax
sub rsp, 20h
call [_malloc_lib]
add rsp, 20h
mov [rbp+18h], rax
mov rsi, [rbp+10h]
mov rdi, rax
__ucase$_loop:
cmp [rsi], byte 0h
je __ucase$_done
movzx rcx, byte [rsi]
sub rsp, 20h
call [_toupper_lib]
add rsp, 20h
mov [rdi], al
inc rsi
inc rdi
jmp __ucase$_loop
__ucase$_done:
mov [rdi], byte 0h
mov rax, [rbp+18h]
pop rsi
pop rdi
pop rbp
ret

;; mid$(Str, I64, I64) -> Str
__mid$_Str_I64_I64:
;; Enter function
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
pop rbp
ret

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

;; right$(Str, I64) -> Str
__right$_Str_I64:
;; Enter function
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
pop rbp
ret

;; memory_register(I64, I64) -> I64
__memory_register_I64_I64:
;; Enter function
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
pop rbp
ret

;; lcase$(Str) -> Str
__lcase$_Str:
push rbp
mov rbp, rsp
mov [rbp+10h], rcx
push rdi
push rsi
sub rsp, 20h
call [_strlen_lib]
add rsp, 20h
inc rax
mov rcx, rax
sub rsp, 20h
call [_malloc_lib]
add rsp, 20h
mov [rbp+18h], rax
mov rsi, [rbp+10h]
mov rdi, rax
__lcase$_loop:
cmp [rsi], byte 0h
je __lcase$_done
movzx rcx, byte [rsi]
sub rsp, 20h
call [_tolower_lib]
add rsp, 20h
mov [rdi], al
inc rsi
inc rdi
jmp __lcase$_loop
__lcase$_done:
mov [rdi], byte 0h
mov rax, [rbp+18h]
pop rsi
pop rdi
pop rbp
ret

;; mid$(Str, I64) -> Str
__mid$_Str_I64:
;; Enter function
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

;; <-- Built-in functions ---

;; --- User-defined functions -->

;; Definition of: FNtoTitleCase$(Str) -> Str
__FNtoTitleCase$_Str:
;; Enter function
push rbp
mov rbp, rsp
;; Save 1 argument(s) in home location(s)
mov [rbp+10h], rcx
;; Save used non-volatile registers
push rbx
push rdi
push rsi
push r12
push r13
push r14
;; Align stack
sub rsp, 8


;; --- ucase$(mid$(s$, 1, 1)) + lcase$(mid$(s$, 2)) -->

;; --- 5: ucase$(mid$(s$, 1, 1)) -->
;; Evaluate arguments (_ucase$_Str)

;; --- 5: mid$(s$, 1, 1) -->
;; Evaluate arguments (_mid$_Str_I64_I64)
;; 5: s$
mov rsi, [rbp+10h]
;; 5: 1
mov r12, 1
;; 5: 1
mov r13, 1
;; Move arguments to argument passing registers (_mid$_Str_I64_I64)
mov rcx, rsi
mov rdx, r12
mov r8, r13
;; Allocate shadow space (_mid$_Str_I64_I64)
sub rsp, 20h
call __mid$_Str_I64_I64
;; Clean up shadow space (_mid$_Str_I64_I64)
add rsp, 20h
;; Move return value (rax) to storage location (rdi)
mov rdi, rax
;; <-- 5: mid$(s$, 1, 1) ---

;; Move arguments to argument passing registers (_ucase$_Str)
mov rcx, rdi
;; Allocate shadow space (_ucase$_Str)
sub rsp, 20h
call __ucase$_Str
;; Clean up shadow space (_ucase$_Str)
add rsp, 20h
;; Move return value (rax) to storage location (rbx)
mov rbx, rax
;; Free dynamic memory in rdi
mov rcx, rdi
;; free address already in rcx
sub rsp, 20h
call [_free_lib]
add rsp, 20h
;; <-- 5: ucase$(mid$(s$, 1, 1)) ---


;; --- 5: lcase$(mid$(s$, 2)) -->
;; Evaluate arguments (_lcase$_Str)

;; --- 5: mid$(s$, 2) -->
;; Evaluate arguments (_mid$_Str_I64)
;; 5: s$
mov r13, [rbp+10h]
;; 5: 2
mov r14, 2
;; Move arguments to argument passing registers (_mid$_Str_I64)
mov rcx, r13
mov rdx, r14
;; Allocate shadow space (_mid$_Str_I64)
sub rsp, 20h
call __mid$_Str_I64
;; Clean up shadow space (_mid$_Str_I64)
add rsp, 20h
;; Move return value (rax) to storage location (r12)
mov r12, rax
;; <-- 5: mid$(s$, 2) ---

;; Move arguments to argument passing registers (_lcase$_Str)
mov rcx, r12
;; Allocate shadow space (_lcase$_Str)
sub rsp, 20h
call __lcase$_Str
;; Clean up shadow space (_lcase$_Str)
add rsp, 20h
;; Move return value (rax) to storage location (rdi)
mov rdi, rax
;; Free dynamic memory in r12
mov rcx, r12
;; free address already in rcx
sub rsp, 20h
call [_free_lib]
add rsp, 20h
;; <-- 5: lcase$(mid$(s$, 2)) ---

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
;; Free dynamic memory in rbx
mov rcx, rbx
;; free address already in rcx
sub rsp, 20h
call [_free_lib]
add rsp, 20h
;; Free dynamic memory in rdi
mov rcx, rdi
;; free address already in rcx
sub rsp, 20h
call [_free_lib]
add rsp, 20h
;; Move result string to expected storage location (rbx)
mov rbx, rsi
;; <-- ucase$(mid$(s$, 1, 1)) + lcase$(mid$(s$, 2)) ---

;; Move result (rbx) to return value (rax)
mov rax, rbx

;; Undo align stack
add rsp, 8
;; Restore used non-volatile registers
pop r14
pop r13
pop r12
pop rsi
pop rdi
pop rbx
;; Leave function
pop rbp
ret

;; <-- User-defined functions ---
