;;; JCC version: 0.8.2-SNAPSHOT
;;; Date & time: 2024-03-16T18:00:14.590484
;;; Source file: ansi_colors.bas
format PE64 console
entry __main
include 'win64a.inc'

section '.idata' import data readable

library msvcrt,'msvcrt.dll'

import msvcrt,\
_exit_lib,'exit',\
_free_lib,'free',\
_malloc_lib,'malloc',\
_printf_lib,'printf',\
_strcat_lib,'strcat',\
_strcpy_lib,'strcpy',\
_strlen_lib,'strlen'

section '.data' data readable writeable

__empty db "",0
__err_function_chr db "Error: Illegal function call: chr$",0
__fmt_Str_Str_Str db "%s%s%s",10,0
__gc_allocation_count dq 0
__gc_allocation_limit dq 100
__gc_allocation_list dq 0
__gc_count_msg db "GC: Allocation count reached limit: %d - collecting",10,0
__gc_limit_msg db "GC: Collection finished with new limit: %d",10,0
__gc_marked_msg db "GC: Marking memory: %x",10,0
__gc_register_msg db "GC: Registering new memory: %x",10,0
__gc_sweeping_msg db "GC: Sweeping memory: %x",10,0
__string_0 db "[30m",0
__string_1 db "[31m",0
__string_10 db "BLACK",0
__string_11 db "RED",0
__string_12 db "GREEN",0
__string_13 db "YELLOW",0
__string_14 db "BLUE",0
__string_15 db "MAGENTA",0
__string_16 db "CYAN",0
__string_17 db "WHITE",0
__string_18 db "INVERSE",0
__string_2 db "[32m",0
__string_3 db "[33m",0
__string_4 db "[34m",0
__string_5 db "[35m",0
__string_6 db "[36m",0
__string_7 db "[37m",0
__string_8 db "[7m",0
__string_9 db "[0m",0
_black dq __empty
_blue dq __empty
_cyan dq __empty
_esc dq __empty
_green dq __empty
_inverse dq __empty
_magenta dq __empty
_normal dq __empty
_red dq __empty
_white dq __empty
_yellow dq __empty

;; --- Dynamic memory type pointers -->
__gc_type_pointers_start dq 0h
__black_type dq 0h
__blue_type dq 0h
__cyan_type dq 0h
__esc_type dq 0h
__green_type dq 0h
__inverse_type dq 0h
__magenta_type dq 0h
__normal_type dq 0h
__red_type dq 0h
__white_type dq 0h
__yellow_type dq 0h
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






;; --- 8: chr$(27) -->
;; Evaluate arguments (_chr$_I64)
;; Defer evaluation of argument 0: 27
;; Move arguments to argument passing registers (_chr$_I64)
;; 8: 27
mov rcx, 27
;; Allocate shadow space (_chr$_I64)
sub rsp, 20h
call __chr$_I64
;; Clean up shadow space (_chr$_I64)
add rsp, 20h
;; Move return value (rax) to storage location (rbx)
mov rbx, rax
;; <-- 8: chr$(27) ---

;; 8: esc = chr$(27)
mov [_esc], rbx

;; Register dynamic memory assigned to esc
lea rcx, [_esc]
lea rdx, [__esc_type]
sub rsp, 20h
call __memory_register_I64_I64
add rsp, 20h


;; --- esc + "[30m" -->
;; 10: esc
mov rbx, [_esc]
;; 10: "[30m"
mov rdi, __string_0
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
;; <-- esc + "[30m" ---

;; 10: black = esc + "[30m"
mov [_black], rbx

;; Register dynamic memory assigned to black
lea rcx, [_black]
lea rdx, [__black_type]
sub rsp, 20h
call __memory_register_I64_I64
add rsp, 20h


;; --- esc + "[31m" -->
;; 11: esc
mov rbx, [_esc]
;; 11: "[31m"
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
;; <-- esc + "[31m" ---

;; 11: red = esc + "[31m"
mov [_red], rbx

;; Register dynamic memory assigned to red
lea rcx, [_red]
lea rdx, [__red_type]
sub rsp, 20h
call __memory_register_I64_I64
add rsp, 20h


;; --- esc + "[32m" -->
;; 12: esc
mov rbx, [_esc]
;; 12: "[32m"
mov rdi, __string_2
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
;; <-- esc + "[32m" ---

;; 12: green = esc + "[32m"
mov [_green], rbx

;; Register dynamic memory assigned to green
lea rcx, [_green]
lea rdx, [__green_type]
sub rsp, 20h
call __memory_register_I64_I64
add rsp, 20h


;; --- esc + "[33m" -->
;; 13: esc
mov rbx, [_esc]
;; 13: "[33m"
mov rdi, __string_3
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
;; <-- esc + "[33m" ---

;; 13: yellow = esc + "[33m"
mov [_yellow], rbx

;; Register dynamic memory assigned to yellow
lea rcx, [_yellow]
lea rdx, [__yellow_type]
sub rsp, 20h
call __memory_register_I64_I64
add rsp, 20h


;; --- esc + "[34m" -->
;; 14: esc
mov rbx, [_esc]
;; 14: "[34m"
mov rdi, __string_4
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
;; <-- esc + "[34m" ---

;; 14: blue = esc + "[34m"
mov [_blue], rbx

;; Register dynamic memory assigned to blue
lea rcx, [_blue]
lea rdx, [__blue_type]
sub rsp, 20h
call __memory_register_I64_I64
add rsp, 20h


;; --- esc + "[35m" -->
;; 15: esc
mov rbx, [_esc]
;; 15: "[35m"
mov rdi, __string_5
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
;; <-- esc + "[35m" ---

;; 15: magenta = esc + "[35m"
mov [_magenta], rbx

;; Register dynamic memory assigned to magenta
lea rcx, [_magenta]
lea rdx, [__magenta_type]
sub rsp, 20h
call __memory_register_I64_I64
add rsp, 20h


;; --- esc + "[36m" -->
;; 16: esc
mov rbx, [_esc]
;; 16: "[36m"
mov rdi, __string_6
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
;; <-- esc + "[36m" ---

;; 16: cyan = esc + "[36m"
mov [_cyan], rbx

;; Register dynamic memory assigned to cyan
lea rcx, [_cyan]
lea rdx, [__cyan_type]
sub rsp, 20h
call __memory_register_I64_I64
add rsp, 20h


;; --- esc + "[37m" -->
;; 17: esc
mov rbx, [_esc]
;; 17: "[37m"
mov rdi, __string_7
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
;; <-- esc + "[37m" ---

;; 17: white = esc + "[37m"
mov [_white], rbx

;; Register dynamic memory assigned to white
lea rcx, [_white]
lea rdx, [__white_type]
sub rsp, 20h
call __memory_register_I64_I64
add rsp, 20h


;; --- esc + "[7m" -->
;; 18: esc
mov rbx, [_esc]
;; 18: "[7m"
mov rdi, __string_8
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
;; <-- esc + "[7m" ---

;; 18: inverse = esc + "[7m"
mov [_inverse], rbx

;; Register dynamic memory assigned to inverse
lea rcx, [_inverse]
lea rdx, [__inverse_type]
sub rsp, 20h
call __memory_register_I64_I64
add rsp, 20h


;; --- esc + "[0m" -->
;; 19: esc
mov rbx, [_esc]
;; 19: "[0m"
mov rdi, __string_9
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
;; <-- esc + "[0m" ---

;; 19: normal = esc + "[0m"
mov [_normal], rbx

;; Register dynamic memory assigned to normal
lea rcx, [_normal]
lea rdx, [__normal_type]
sub rsp, 20h
call __memory_register_I64_I64
add rsp, 20h

;; --- 21: PRINT black, "BLACK", normal -->
;; Evaluate arguments (_printf_lib)
;; Defer evaluation of argument 0: _fmt_Str_Str_Str
;; Defer evaluation of argument 1: black
;; Defer evaluation of argument 2: "BLACK"
;; Defer evaluation of argument 3: normal
;; Move arguments to argument passing registers (_printf_lib)
;; 21: _fmt_Str_Str_Str
mov rcx, __fmt_Str_Str_Str
;; 21: black
mov rdx, [_black]
;; 21: "BLACK"
mov r8, __string_10
;; 21: normal
mov r9, [_normal]
;; Allocate shadow space (_printf_lib)
sub rsp, 20h
call [_printf_lib]
;; Clean up shadow space (_printf_lib)
add rsp, 20h
;; Ignore return value
;; <-- 21: PRINT black, "BLACK", normal ---

;; --- 22: PRINT red, "RED", normal -->
;; Evaluate arguments (_printf_lib)
;; Defer evaluation of argument 0: _fmt_Str_Str_Str
;; Defer evaluation of argument 1: red
;; Defer evaluation of argument 2: "RED"
;; Defer evaluation of argument 3: normal
;; Move arguments to argument passing registers (_printf_lib)
;; 22: _fmt_Str_Str_Str
mov rcx, __fmt_Str_Str_Str
;; 22: red
mov rdx, [_red]
;; 22: "RED"
mov r8, __string_11
;; 22: normal
mov r9, [_normal]
;; Allocate shadow space (_printf_lib)
sub rsp, 20h
call [_printf_lib]
;; Clean up shadow space (_printf_lib)
add rsp, 20h
;; Ignore return value
;; <-- 22: PRINT red, "RED", normal ---

;; --- 23: PRINT green, "GREEN", normal -->
;; Evaluate arguments (_printf_lib)
;; Defer evaluation of argument 0: _fmt_Str_Str_Str
;; Defer evaluation of argument 1: green
;; Defer evaluation of argument 2: "GREEN"
;; Defer evaluation of argument 3: normal
;; Move arguments to argument passing registers (_printf_lib)
;; 23: _fmt_Str_Str_Str
mov rcx, __fmt_Str_Str_Str
;; 23: green
mov rdx, [_green]
;; 23: "GREEN"
mov r8, __string_12
;; 23: normal
mov r9, [_normal]
;; Allocate shadow space (_printf_lib)
sub rsp, 20h
call [_printf_lib]
;; Clean up shadow space (_printf_lib)
add rsp, 20h
;; Ignore return value
;; <-- 23: PRINT green, "GREEN", normal ---

;; --- 24: PRINT yellow, "YELLOW", normal -->
;; Evaluate arguments (_printf_lib)
;; Defer evaluation of argument 0: _fmt_Str_Str_Str
;; Defer evaluation of argument 1: yellow
;; Defer evaluation of argument 2: "YELLOW"
;; Defer evaluation of argument 3: normal
;; Move arguments to argument passing registers (_printf_lib)
;; 24: _fmt_Str_Str_Str
mov rcx, __fmt_Str_Str_Str
;; 24: yellow
mov rdx, [_yellow]
;; 24: "YELLOW"
mov r8, __string_13
;; 24: normal
mov r9, [_normal]
;; Allocate shadow space (_printf_lib)
sub rsp, 20h
call [_printf_lib]
;; Clean up shadow space (_printf_lib)
add rsp, 20h
;; Ignore return value
;; <-- 24: PRINT yellow, "YELLOW", normal ---

;; --- 25: PRINT blue, "BLUE", normal -->
;; Evaluate arguments (_printf_lib)
;; Defer evaluation of argument 0: _fmt_Str_Str_Str
;; Defer evaluation of argument 1: blue
;; Defer evaluation of argument 2: "BLUE"
;; Defer evaluation of argument 3: normal
;; Move arguments to argument passing registers (_printf_lib)
;; 25: _fmt_Str_Str_Str
mov rcx, __fmt_Str_Str_Str
;; 25: blue
mov rdx, [_blue]
;; 25: "BLUE"
mov r8, __string_14
;; 25: normal
mov r9, [_normal]
;; Allocate shadow space (_printf_lib)
sub rsp, 20h
call [_printf_lib]
;; Clean up shadow space (_printf_lib)
add rsp, 20h
;; Ignore return value
;; <-- 25: PRINT blue, "BLUE", normal ---

;; --- 26: PRINT magenta, "MAGENTA", normal -->
;; Evaluate arguments (_printf_lib)
;; Defer evaluation of argument 0: _fmt_Str_Str_Str
;; Defer evaluation of argument 1: magenta
;; Defer evaluation of argument 2: "MAGENTA"
;; Defer evaluation of argument 3: normal
;; Move arguments to argument passing registers (_printf_lib)
;; 26: _fmt_Str_Str_Str
mov rcx, __fmt_Str_Str_Str
;; 26: magenta
mov rdx, [_magenta]
;; 26: "MAGENTA"
mov r8, __string_15
;; 26: normal
mov r9, [_normal]
;; Allocate shadow space (_printf_lib)
sub rsp, 20h
call [_printf_lib]
;; Clean up shadow space (_printf_lib)
add rsp, 20h
;; Ignore return value
;; <-- 26: PRINT magenta, "MAGENTA", normal ---

;; --- 27: PRINT cyan, "CYAN", normal -->
;; Evaluate arguments (_printf_lib)
;; Defer evaluation of argument 0: _fmt_Str_Str_Str
;; Defer evaluation of argument 1: cyan
;; Defer evaluation of argument 2: "CYAN"
;; Defer evaluation of argument 3: normal
;; Move arguments to argument passing registers (_printf_lib)
;; 27: _fmt_Str_Str_Str
mov rcx, __fmt_Str_Str_Str
;; 27: cyan
mov rdx, [_cyan]
;; 27: "CYAN"
mov r8, __string_16
;; 27: normal
mov r9, [_normal]
;; Allocate shadow space (_printf_lib)
sub rsp, 20h
call [_printf_lib]
;; Clean up shadow space (_printf_lib)
add rsp, 20h
;; Ignore return value
;; <-- 27: PRINT cyan, "CYAN", normal ---

;; --- 28: PRINT white, "WHITE", normal -->
;; Evaluate arguments (_printf_lib)
;; Defer evaluation of argument 0: _fmt_Str_Str_Str
;; Defer evaluation of argument 1: white
;; Defer evaluation of argument 2: "WHITE"
;; Defer evaluation of argument 3: normal
;; Move arguments to argument passing registers (_printf_lib)
;; 28: _fmt_Str_Str_Str
mov rcx, __fmt_Str_Str_Str
;; 28: white
mov rdx, [_white]
;; 28: "WHITE"
mov r8, __string_17
;; 28: normal
mov r9, [_normal]
;; Allocate shadow space (_printf_lib)
sub rsp, 20h
call [_printf_lib]
;; Clean up shadow space (_printf_lib)
add rsp, 20h
;; Ignore return value
;; <-- 28: PRINT white, "WHITE", normal ---

;; --- 29: PRINT inverse, "INVERSE", normal -->
;; Evaluate arguments (_printf_lib)
;; Defer evaluation of argument 0: _fmt_Str_Str_Str
;; Defer evaluation of argument 1: inverse
;; Defer evaluation of argument 2: "INVERSE"
;; Defer evaluation of argument 3: normal
;; Move arguments to argument passing registers (_printf_lib)
;; 29: _fmt_Str_Str_Str
mov rcx, __fmt_Str_Str_Str
;; 29: inverse
mov rdx, [_inverse]
;; 29: "INVERSE"
mov r8, __string_18
;; 29: normal
mov r9, [_normal]
;; Allocate shadow space (_printf_lib)
sub rsp, 20h
call [_printf_lib]
;; Clean up shadow space (_printf_lib)
add rsp, 20h
;; Ignore return value
;; <-- 29: PRINT inverse, "INVERSE", normal ---

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

;; chr$(I64) -> Str
__chr$_I64:
push rbp
mov rbp, rsp
mov [rbp+10h], rcx
cmp rcx, 0
jl __chr$_error
cmp rcx, 255
jg __chr$_error
mov rcx, 2h
sub rsp, 20h
call [_malloc_lib]
add rsp, 20h
mov rcx, [rbp+10h]
mov [rax], cl
mov [rax+1h], byte 0h
jmp __chr$_done
__chr$_error:
mov rcx, __err_function_chr
sub rsp, 20h
call [_printf_lib]
add rsp, 20h
mov rcx, 1h
sub rsp, 20h
call [_exit_lib]
add rsp, 20h
__chr$_done:
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

;; <-- Built-in functions ---
