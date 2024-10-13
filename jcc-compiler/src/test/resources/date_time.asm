;;; JCC version: 0.8.3-SNAPSHOT
;;; Date & time: 2024-10-13T15:51:51.028967
;;; Source file: date_time.bas
format PE64 console
entry __main
include 'win64a.inc'

section '.idata' import data readable

library jccbasic,'jccbasic.dll',\
msvcrt,'msvcrt.dll'

import jccbasic,\
_date$_lib,'date$',\
_time$_lib,'time$'

import msvcrt,\
_exit_lib,'exit',\
_free_lib,'free',\
_malloc_lib,'malloc',\
_printf_lib,'printf',\
_strcat_lib,'strcat',\
_strcpy_lib,'strcpy',\
_strlen_lib,'strlen',\
_strncpy_lib,'strncpy'

section '.data' data readable writeable

__empty db "",0
__err_function_left$ db "Error: Illegal function call: left$",0
__err_function_mid$ db "Error: Illegal function call: mid$",0
__err_function_right$ db "Error: Illegal function call: right$",0
__fmt_Str db "%s",10,0
__fmt_Str_Str_Str db "%s%s%s",10,0
__gc_allocation_count dq 0
__gc_allocation_limit dq 100
__gc_allocation_list dq 0
__gc_count_msg db "GC: Allocation count reached limit: %d - collecting",10,0
__gc_limit_msg db "GC: Collection finished with new limit: %d",10,0
__gc_marked_msg db "GC: Marking memory: %x",10,0
__gc_register_msg db "GC: Registering new memory: %x",10,0
__gc_sweeping_msg db "GC: Sweeping memory: %x",10,0
__string_0 db " ",0
__string_1 db "-",0
__string_2 db "Error: RETURN without GOSUB",0
_day$ dq __empty
_isoDate$ dq __empty
_isoTime$ dq __empty
_month$ dq __empty
_usDate$ dq __empty
_year$ dq __empty

;; --- Dynamic memory type pointers -->
__gc_type_pointers_start dq 0h
__day$_type dq 0h
__isoDate$_type dq 0h
__isoTime$_type dq 0h
__month$_type dq 0h
__usDate$_type dq 0h
__year$_type dq 0h
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
mov rdx, __string_2
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


;; --- 3: date$() -->
;; Allocate shadow space (_date$_lib)
sub rsp, 20h
call [_date$_lib]
;; Clean up shadow space (_date$_lib)
add rsp, 20h
;; Move return value (rax) to storage location (rbx)
mov rbx, rax
;; <-- 3: date$() ---

;; 3: usDate$ = date$()
mov [_usDate$], rbx

;; Register dynamic memory assigned to usDate$
lea rcx, [_usDate$]
lea rdx, [__usDate$_type]
sub rsp, 20h
call __memory_register_I64_I64
add rsp, 20h


;; --- 4: time$() -->
;; Allocate shadow space (_time$_lib)
sub rsp, 20h
call [_time$_lib]
;; Clean up shadow space (_time$_lib)
add rsp, 20h
;; Move return value (rax) to storage location (rbx)
mov rbx, rax
;; <-- 4: time$() ---

;; 4: isoTime$ = time$()
mov [_isoTime$], rbx

;; Register dynamic memory assigned to isoTime$
lea rcx, [_isoTime$]
lea rdx, [__isoTime$_type]
sub rsp, 20h
call __memory_register_I64_I64
add rsp, 20h

;; 5: GOSUB usDateToIsoDate
call __line_gosub_usDateToIsoDate

;; --- 6: PRINT isoDate$, " ", isoTime$ -->
;; Evaluate arguments (_printf_lib)
;; Defer evaluation of argument 0: _fmt_Str_Str_Str
;; Defer evaluation of argument 1: isoDate$
;; Defer evaluation of argument 2: " "
;; Defer evaluation of argument 3: isoTime$
;; Move arguments to argument passing registers (_printf_lib)
;; 6: _fmt_Str_Str_Str
mov rcx, __fmt_Str_Str_Str
;; 6: isoDate$
mov rdx, [_isoDate$]
;; 6: " "
mov r8, __string_0
;; 6: isoTime$
mov r9, [_isoTime$]
;; Allocate shadow space (_printf_lib)
sub rsp, 20h
call [_printf_lib]
;; Clean up shadow space (_printf_lib)
add rsp, 20h
;; Ignore return value
;; <-- 6: PRINT isoDate$, " ", isoTime$ ---

;; --- 7: END -->
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
;; <-- 7: END ---

;; 9: REM 

;; 10: REM 

;; 11: REM 

;; 12: REM 

;; 13: REM 

;; 14: REM 

;; 15: REM 

;; 16: REM 

__line_usDateToIsoDate:

;; --- 18: mid$(usDate$, 4, 2) -->
;; Evaluate arguments (_mid$_Str_I64_I64)
;; Defer evaluation of argument 0: usDate$
;; Defer evaluation of argument 1: 4
;; Defer evaluation of argument 2: 2
;; Move arguments to argument passing registers (_mid$_Str_I64_I64)
;; 18: usDate$
mov rcx, [_usDate$]
;; 18: 4
mov rdx, 4
;; 18: 2
mov r8, 2
;; Allocate shadow space (_mid$_Str_I64_I64)
sub rsp, 20h
call __mid$_Str_I64_I64
;; Clean up shadow space (_mid$_Str_I64_I64)
add rsp, 20h
;; Move return value (rax) to storage location (rbx)
mov rbx, rax
;; <-- 18: mid$(usDate$, 4, 2) ---

;; 18: day$ = mid$(usDate$, 4, 2)
mov [_day$], rbx

;; Register dynamic memory assigned to day$
lea rcx, [_day$]
lea rdx, [__day$_type]
sub rsp, 20h
call __memory_register_I64_I64
add rsp, 20h



;; --- 19: left$(usDate$, 2) -->
;; Evaluate arguments (_left$_Str_I64)
;; Defer evaluation of argument 0: usDate$
;; Defer evaluation of argument 1: 2
;; Move arguments to argument passing registers (_left$_Str_I64)
;; 19: usDate$
mov rcx, [_usDate$]
;; 19: 2
mov rdx, 2
;; Allocate shadow space (_left$_Str_I64)
sub rsp, 20h
call __left$_Str_I64
;; Clean up shadow space (_left$_Str_I64)
add rsp, 20h
;; Move return value (rax) to storage location (rbx)
mov rbx, rax
;; <-- 19: left$(usDate$, 2) ---

;; 19: month$ = left$(usDate$, 2)
mov [_month$], rbx

;; Register dynamic memory assigned to month$
lea rcx, [_month$]
lea rdx, [__month$_type]
sub rsp, 20h
call __memory_register_I64_I64
add rsp, 20h


;; --- 20: right$(usDate$, 4) -->
;; Evaluate arguments (_right$_Str_I64)
;; Defer evaluation of argument 0: usDate$
;; Defer evaluation of argument 1: 4
;; Move arguments to argument passing registers (_right$_Str_I64)
;; 20: usDate$
mov rcx, [_usDate$]
;; 20: 4
mov rdx, 4
;; Allocate shadow space (_right$_Str_I64)
sub rsp, 20h
call __right$_Str_I64
;; Clean up shadow space (_right$_Str_I64)
add rsp, 20h
;; Move return value (rax) to storage location (rbx)
mov rbx, rax
;; <-- 20: right$(usDate$, 4) ---

;; 20: year$ = right$(usDate$, 4)
mov [_year$], rbx

;; Register dynamic memory assigned to year$
lea rcx, [_year$]
lea rdx, [__year$_type]
sub rsp, 20h
call __memory_register_I64_I64
add rsp, 20h


;; --- year$ + "-" + month$ + "-" + day$ -->

;; --- year$ + "-" + month$ + "-" -->

;; --- year$ + "-" + month$ -->

;; --- year$ + "-" -->
;; 21: year$
mov rbx, [_year$]
;; 21: "-"
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
;; Move result string to expected storage location (rbx)
mov rbx, rsi
;; <-- year$ + "-" ---

;; 21: month$
mov rdi, [_month$]
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
;; <-- year$ + "-" + month$ ---

;; 21: "-"
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
;; <-- year$ + "-" + month$ + "-" ---

;; 21: day$
mov rdi, [_day$]
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
;; <-- year$ + "-" + month$ + "-" + day$ ---

;; 21: isoDate$ = year$ + "-" + month$ + "-" + day$
mov [_isoDate$], rbx

;; Register dynamic memory assigned to isoDate$
lea rcx, [_isoDate$]
lea rdx, [__isoDate$_type]
sub rsp, 20h
call __memory_register_I64_I64
add rsp, 20h

ret


;; --- GOSUB bridge calls -->
__line_gosub_usDateToIsoDate:
call __line_usDateToIsoDate
ret
;; <-- GOSUB bridge calls ---

;; --- Built-in functions -->

;; left$(Str, I64) -> Str
__left$_Str_I64:
;; Save base pointer
push rbp
mov rbp, rsp
;; Save 2 argument(s) in home location(s)
mov [rbp+10h], rcx
mov [rbp+18h], rdx

cmp rdx, 0h
jl __left$_error
;; strlen address already in rcx
sub rsp, 20h
call [_strlen_lib]
add rsp, 20h
mov rdx, [rbp+18h]
cmp rax, rdx
jge __left$_allocate
mov rdx, rax
mov [rbp+18h], rdx
__left$_allocate:
inc rdx
mov rcx, rdx
sub rsp, 20h
call [_malloc_lib]
add rsp, 20h
mov rdx, [rbp+10h]
mov r8, [rbp+18h]
mov rcx, rax
;; strncpy source already in rdx
;; strncpy length already in r8
sub rsp, 20h
call [_strncpy_lib]
add rsp, 20h
mov r11, rax
add r11, [rbp+18h]
mov [r11], byte 0h
jmp __left$_done
__left$_error:
mov rcx, __err_function_left$
sub rsp, 20h
call [_printf_lib]
add rsp, 20h
mov rcx, 1h
sub rsp, 20h
call [_exit_lib]
add rsp, 20h
__left$_done:
;; Restore base pointer
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
