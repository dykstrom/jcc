;;; JCC version: 0.8.2-SNAPSHOT
;;; Date & time: 2023-12-28T15:24:13.104402
;;; Source file: game_of_life.bas
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
_free_lib,'free',\
_malloc_lib,'malloc',\
_memset_lib,'memset',\
_printf_lib,'printf',\
_strcat_lib,'strcat',\
_strcpy_lib,'strcpy',\
_strlen_lib,'strlen'

section '.data' data readable writeable

_HEIGHT dq 30
_WIDTH dq 60
__cls_ansi_codes db 27,"[2J",27,"[H",0
__empty db "",0
__err_function_chr db "Error: Illegal function call: chr$",0
__err_function_string$ db "Error: Illegal function call: string$",0
__float_0 dq 0.5
__fmt_Str db "%s",10,0
__fmt_Str_I64 db "%s%lld",10,0
__fmt_Str_Str db "%s%s",10,0
__gc_allocation_count dq 0
__gc_allocation_limit dq 100
__gc_allocation_list dq 0
__gc_count_msg db "GC: Allocation count reached limit: %d - collecting",10,0
__gc_limit_msg db "GC: Collection finished with new limit: %d",10,0
__gc_marked_msg db "GC: Marking memory: %x",10,0
__gc_register_msg db "GC: Registering new memory: %x",10,0
__gc_sweeping_msg db "GC: Sweeping memory: %x",10,0
__string_0 db "-",0
__string_1 db "[H",0
__string_2 db "Generation ",0
__string_3 db "",0
__string_4 db "O",0
__string_5 db " ",0
__string_6 db "Error: RETURN without GOSUB",0
_count dq 0
_generation dq 0
_separator dq __empty
_str dq __empty
_t dq 0.0
_x dq 0
_xx dq 0
_y dq 0
_yy dq 0
_board_arr_dim_1 dq 31
_board_arr_dim_0 dq 61
_board_arr_num_dims dq 2
_board_arr dq 1891 dup 0
_buffer_arr_dim_1 dq 31
_buffer_arr_dim_0 dq 61
_buffer_arr_num_dims dq 2
_buffer_arr dq 1891 dup 0

;; --- Dynamic memory type pointers -->
__gc_type_pointers_start dq 0h
__separator_type dq 0h
__str_type dq 0h
__gc_type_pointers_stop dq 0h
;; <-- Dynamic memory type pointers ---

section '.code' code readable executable

__main:
;; Save used non-volatile registers
push rbx
push rdi
push rsi
push r12
sub rsp, 16
movdqu [rsp], xmm6
sub rsp, 16
movdqu [rsp], xmm7
sub rsp, 16
movdqu [rsp], xmm8
;; Align stack
sub rsp, 8

;; --- RETURN without GOSUB -->
call __after_return_without_gosub_1
;; --- PRINT "Error: RETURN without GOSUB" -->
;; Evaluate arguments (_printf_lib)
;; _fmt_Str
mov rbx, __fmt_Str
;; "Error: RETURN without GOSUB"
mov rdi, __string_6
;; Move arguments to argument passing registers (_printf_lib)
mov rcx, rbx
mov rdx, rdi
;; Allocate shadow space (_printf_lib)
sub rsp, 20h
call [_printf_lib]
;; Clean up shadow space (_printf_lib)
add rsp, 20h
;; Ignore return value
;; <-- PRINT "Error: RETURN without GOSUB" ---
;; --- exit(1) -->
;; Evaluate arguments (_exit_lib)
;; 1
mov rbx, 1
;; Move arguments to argument passing registers (_exit_lib)
mov rcx, rbx
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

;; 2: REM 

;; 4: CONST WIDTH : I64 = 60, HEIGHT : I64 = 30







;; --- 12: string$(WIDTH, "-") -->
;; Evaluate arguments (_string$_I64_Str)
;; 12: WIDTH
mov rdi, [_WIDTH]
;; 12: "-"
mov rsi, __string_0
;; Move arguments to argument passing registers (_string$_I64_Str)
mov rcx, rdi
mov rdx, rsi
;; Allocate shadow space (_string$_I64_Str)
sub rsp, 20h
call __string$_I64_Str
;; Clean up shadow space (_string$_I64_Str)
add rsp, 20h
;; Move return value (rax) to storage location (rbx)
mov rbx, rax
;; <-- 12: string$(WIDTH, "-") ---

;; 12: separator = string$(WIDTH, "-")
mov [_separator], rbx

;; Register dynamic memory assigned to separator
lea rcx, [_separator]
lea rdx, [__separator_type]
sub rsp, 20h
call __memory_register_I64_I64
add rsp, 20h

;; --- 14: CLS -->
;; Evaluate arguments (_printf_lib)
;; 14: _cls_ansi_codes
mov rbx, __cls_ansi_codes
;; Move arguments to argument passing registers (_printf_lib)
mov rcx, rbx
;; Allocate shadow space (_printf_lib)
sub rsp, 20h
call [_printf_lib]
;; Clean up shadow space (_printf_lib)
add rsp, 20h
;; Ignore return value
;; <-- 14: CLS ---

;; 16: REM 

;; 17: GOSUB initializeRandom
call __line_gosub_initializeRandom

;; 18: REM 

;; 19: REM 

;; 20: REM 

;; 22: REM 

;; 23: REM 

;; 24: 0
mov rbx, 0
;; 24: generation = 0
mov [_generation], rbx

_before_while_0:
;; 25: generation
mov rbx, [_generation]
;; 25: 5000
mov rdi, 5000
;; 25: generation < 5000
cmp rbx, rdi
jl @f
mov rbx, 0
jmp _after_cmp_2
@@:
mov rbx, -1
_after_cmp_2:

;; 25: WHILE generation < 5000 REM  : PRINT chr$(27), "[H...
cmp rbx, 0
je _after_while_1

;; 26: REM 

;; --- 27: PRINT chr$(27), "[H" -->
;; Evaluate arguments (_printf_lib)
;; 27: _fmt_Str_Str
mov rbx, __fmt_Str_Str

;; --- 27: chr$(27) -->
;; Evaluate arguments (_chr$_I64)
;; 27: 27
mov rsi, 27
;; Move arguments to argument passing registers (_chr$_I64)
mov rcx, rsi
;; Allocate shadow space (_chr$_I64)
sub rsp, 20h
call __chr$_I64
;; Clean up shadow space (_chr$_I64)
add rsp, 20h
;; Move return value (rax) to storage location (rdi)
mov rdi, rax
;; <-- 27: chr$(27) ---

;; 27: "[H"
mov rsi, __string_1
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
;; Free dynamic memory in rdi
mov rcx, rdi
;; free address already in rcx
sub rsp, 20h
call [_free_lib]
add rsp, 20h
;; <-- 27: PRINT chr$(27), "[H" ---

;; --- 28: PRINT "Generation ", generation -->
;; Evaluate arguments (_printf_lib)
;; 28: _fmt_Str_I64
mov rbx, __fmt_Str_I64
;; 28: "Generation "
mov rdi, __string_2
;; 28: generation
mov rsi, [_generation]
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
;; <-- 28: PRINT "Generation ", generation ---

;; 29: GOSUB printBoard
call __line_gosub_printBoard

;; 30: GOSUB evolveBoard
call __line_gosub_evolveBoard

;; 31: generation
mov rbx, [_generation]
;; 31: 1
mov rdi, 1
;; 31: generation + 1
add rbx, rdi
;; 31: generation = generation + 1
mov [_generation], rbx

;; 32: REM 


;; --- 33: timer() -->
;; Allocate shadow space (_timer_lib)
sub rsp, 20h
call [_timer_lib]
;; Clean up shadow space (_timer_lib)
add rsp, 20h
;; Move return value (xmm0) to storage location (xmm6)
movsd xmm6, xmm0
;; <-- 33: timer() ---

;; 33: t = timer()
movsd [_t], xmm6

_before_while_3:

;; --- 34: timer() -->
;; Allocate shadow space (_timer_lib)
sub rsp, 20h
call [_timer_lib]
;; Clean up shadow space (_timer_lib)
add rsp, 20h
;; Move return value (xmm0) to storage location (xmm6)
movsd xmm6, xmm0
;; <-- 34: timer() ---

;; 34: t
movsd xmm7, [_t]
;; 34: 0.5
movsd xmm8, [__float_0]
;; 34: t + 0.5
addsd xmm7, xmm8
;; 34: timer() < t + 0.5
ucomisd xmm6, xmm7
jb @f
mov rbx, 0
jmp _after_cmp_5
@@:
mov rbx, -1
_after_cmp_5:

;; 34: WHILE timer() < t + 0.5 
cmp rbx, 0
je _after_while_4

jmp _before_while_3
_after_while_4:

jmp _before_while_0
_after_while_1:

;; 38: REM 

;; --- 39: END -->
;; Evaluate arguments (_exit_lib)
;; 39: 0
mov rbx, 0
;; Move arguments to argument passing registers (_exit_lib)
mov rcx, rbx
;; Allocate shadow space (_exit_lib)
sub rsp, 20h
call [_exit_lib]
;; Clean up shadow space (_exit_lib)
add rsp, 20h
;; Ignore return value
;; <-- 39: END ---

;; 42: REM 

;; 43: REM 

;; 44: REM 

;; 45: REM 

__line_initializeRandom:
;; 48: RANDOMIZE timer()
;; --- randomize(timer()) -->
;; Evaluate arguments (_randomize_lib)

;; --- 48: timer() -->
;; Allocate shadow space (_timer_lib)
sub rsp, 20h
call [_timer_lib]
;; Clean up shadow space (_timer_lib)
add rsp, 20h
;; Move return value (xmm0) to storage location (xmm6)
movsd xmm6, xmm0
;; <-- 48: timer() ---

;; Move arguments to argument passing registers (_randomize_lib)
movsd xmm0, xmm6
;; Allocate shadow space (_randomize_lib)
sub rsp, 20h
call [_randomize_lib]
;; Clean up shadow space (_randomize_lib)
add rsp, 20h
;; Ignore return value
;; <-- randomize(timer()) ---


;; 50: 0
mov rbx, 0
;; 50: y = 0
mov [_y], rbx

_before_while_6:
;; 51: y
mov rbx, [_y]
;; 51: HEIGHT
mov rdi, [_HEIGHT]
;; 51: y < HEIGHT
cmp rbx, rdi
jl @f
mov rbx, 0
jmp _after_cmp_8
@@:
mov rbx, -1
_after_cmp_8:

;; 51: WHILE y < HEIGHT x = 0 : WHILE x < WIDTH IF rnd() ...
cmp rbx, 0
je _after_while_7

;; 52: 0
mov rbx, 0
;; 52: x = 0
mov [_x], rbx

_before_while_9:
;; 53: x
mov rbx, [_x]
;; 53: WIDTH
mov rdi, [_WIDTH]
;; 53: x < WIDTH
cmp rbx, rdi
jl @f
mov rbx, 0
jmp _after_cmp_11
@@:
mov rbx, -1
_after_cmp_11:

;; 53: WHILE x < WIDTH IF rnd() > 0.5 THEN board(x, y) = ...
cmp rbx, 0
je _after_while_10


;; --- 54: rnd() -->
;; Allocate shadow space (_rnd_lib)
sub rsp, 20h
call [_rnd_lib]
;; Clean up shadow space (_rnd_lib)
add rsp, 20h
;; Move return value (xmm0) to storage location (xmm6)
movsd xmm6, xmm0
;; <-- 54: rnd() ---

;; 54: 0.5
movsd xmm7, [__float_0]
;; 54: rnd() > 0.5
ucomisd xmm6, xmm7
ja @f
mov rbx, 0
jmp _after_cmp_14
@@:
mov rbx, -1
_after_cmp_14:

;; 54: IF rnd() > 0.5 THEN board(x, y) = 1
cmp rbx, 0
je _after_then_12

;; 54: 1
mov rbx, 1
;; 54: board(x, y) = 1
;; 54: board(x, y)
;; 54: x
mov rdi, [_x]
mov rsi, [_board_arr_dim_1]
imul rdi, rsi
;; 54: y
mov rsi, [_y]
add rdi, rsi
mov [_board_arr+8*rdi], rbx

_after_then_12:

_after_else_13:

;; 55: x
mov rbx, [_x]
;; 55: 1
mov rdi, 1
;; 55: x + 1
add rbx, rdi
;; 55: x = x + 1
mov [_x], rbx

jmp _before_while_9
_after_while_10:

;; 57: y
mov rbx, [_y]
;; 57: 1
mov rdi, 1
;; 57: y + 1
add rbx, rdi
;; 57: y = y + 1
mov [_y], rbx

jmp _before_while_6
_after_while_7:

ret

;; 63: REM 

;; 64: REM 

;; 65: REM 

;; 66: REM 

__line_initializeBlinker:
;; 69: 1
mov rbx, 1
;; 69: board(0, 1) = 1
;; 69: board(0, 1)
;; 69: 0
mov rdi, 0
mov rsi, [_board_arr_dim_1]
imul rdi, rsi
;; 69: 1
mov rsi, 1
add rdi, rsi
mov [_board_arr+8*rdi], rbx


;; 70: 1
mov rbx, 1
;; 70: board(1, 1) = 1
;; 70: board(1, 1)
;; 70: 1
mov rdi, 1
mov rsi, [_board_arr_dim_1]
imul rdi, rsi
;; 70: 1
mov rsi, 1
add rdi, rsi
mov [_board_arr+8*rdi], rbx

;; 71: 1
mov rbx, 1
;; 71: board(2, 1) = 1
;; 71: board(2, 1)
;; 71: 2
mov rdi, 2
mov rsi, [_board_arr_dim_1]
imul rdi, rsi
;; 71: 1
mov rsi, 1
add rdi, rsi
mov [_board_arr+8*rdi], rbx

ret

;; 76: REM 

;; 77: REM 

;; 78: REM 

;; 79: REM 

__line_initializeBeacon:
;; 82: 1
mov rbx, 1
;; 82: board(0, 0) = 1
;; 82: board(0, 0)
;; 82: 0
mov rdi, 0
mov rsi, [_board_arr_dim_1]
imul rdi, rsi
;; 82: 0
mov rsi, 0
add rdi, rsi
mov [_board_arr+8*rdi], rbx


;; 83: 1
mov rbx, 1
;; 83: board(1, 0) = 1
;; 83: board(1, 0)
;; 83: 1
mov rdi, 1
mov rsi, [_board_arr_dim_1]
imul rdi, rsi
;; 83: 0
mov rsi, 0
add rdi, rsi
mov [_board_arr+8*rdi], rbx

;; 84: 1
mov rbx, 1
;; 84: board(0, 1) = 1
;; 84: board(0, 1)
;; 84: 0
mov rdi, 0
mov rsi, [_board_arr_dim_1]
imul rdi, rsi
;; 84: 1
mov rsi, 1
add rdi, rsi
mov [_board_arr+8*rdi], rbx

;; 85: 1
mov rbx, 1
;; 85: board(1, 1) = 1
;; 85: board(1, 1)
;; 85: 1
mov rdi, 1
mov rsi, [_board_arr_dim_1]
imul rdi, rsi
;; 85: 1
mov rsi, 1
add rdi, rsi
mov [_board_arr+8*rdi], rbx

;; 87: 1
mov rbx, 1
;; 87: board(2, 2) = 1
;; 87: board(2, 2)
;; 87: 2
mov rdi, 2
mov rsi, [_board_arr_dim_1]
imul rdi, rsi
;; 87: 2
mov rsi, 2
add rdi, rsi
mov [_board_arr+8*rdi], rbx

;; 88: 1
mov rbx, 1
;; 88: board(3, 2) = 1
;; 88: board(3, 2)
;; 88: 3
mov rdi, 3
mov rsi, [_board_arr_dim_1]
imul rdi, rsi
;; 88: 2
mov rsi, 2
add rdi, rsi
mov [_board_arr+8*rdi], rbx

;; 89: 1
mov rbx, 1
;; 89: board(2, 3) = 1
;; 89: board(2, 3)
;; 89: 2
mov rdi, 2
mov rsi, [_board_arr_dim_1]
imul rdi, rsi
;; 89: 3
mov rsi, 3
add rdi, rsi
mov [_board_arr+8*rdi], rbx

;; 90: 1
mov rbx, 1
;; 90: board(3, 3) = 1
;; 90: board(3, 3)
;; 90: 3
mov rdi, 3
mov rsi, [_board_arr_dim_1]
imul rdi, rsi
;; 90: 3
mov rsi, 3
add rdi, rsi
mov [_board_arr+8*rdi], rbx

ret

;; 95: REM 

;; 96: REM 

;; 97: REM 

;; 98: REM 

__line_initializeGlider:
;; 101: 1
mov rbx, 1
;; 101: board(1, 0) = 1
;; 101: board(1, 0)
;; 101: 1
mov rdi, 1
mov rsi, [_board_arr_dim_1]
imul rdi, rsi
;; 101: 0
mov rsi, 0
add rdi, rsi
mov [_board_arr+8*rdi], rbx


;; 102: 1
mov rbx, 1
;; 102: board(2, 1) = 1
;; 102: board(2, 1)
;; 102: 2
mov rdi, 2
mov rsi, [_board_arr_dim_1]
imul rdi, rsi
;; 102: 1
mov rsi, 1
add rdi, rsi
mov [_board_arr+8*rdi], rbx

;; 103: 1
mov rbx, 1
;; 103: board(2, 2) = 1
;; 103: board(2, 2)
;; 103: 2
mov rdi, 2
mov rsi, [_board_arr_dim_1]
imul rdi, rsi
;; 103: 2
mov rsi, 2
add rdi, rsi
mov [_board_arr+8*rdi], rbx

;; 104: 1
mov rbx, 1
;; 104: board(1, 2) = 1
;; 104: board(1, 2)
;; 104: 1
mov rdi, 1
mov rsi, [_board_arr_dim_1]
imul rdi, rsi
;; 104: 2
mov rsi, 2
add rdi, rsi
mov [_board_arr+8*rdi], rbx

;; 105: 1
mov rbx, 1
;; 105: board(0, 2) = 1
;; 105: board(0, 2)
;; 105: 0
mov rdi, 0
mov rsi, [_board_arr_dim_1]
imul rdi, rsi
;; 105: 2
mov rsi, 2
add rdi, rsi
mov [_board_arr+8*rdi], rbx

ret

;; 110: REM 

;; 111: REM 

;; 112: REM 

;; 113: REM 

__line_evolveBoard:
;; 116: REM 


;; 117: 0
mov rbx, 0
;; 117: y = 0
mov [_y], rbx

_before_while_15:
;; 118: y
mov rbx, [_y]
;; 118: HEIGHT
mov rdi, [_HEIGHT]
;; 118: y < HEIGHT
cmp rbx, rdi
jl @f
mov rbx, 0
jmp _after_cmp_17
@@:
mov rbx, -1
_after_cmp_17:

;; 118: WHILE y < HEIGHT x = 0 : WHILE x < WIDTH GOSUB evo...
cmp rbx, 0
je _after_while_16

;; 119: 0
mov rbx, 0
;; 119: x = 0
mov [_x], rbx

_before_while_18:
;; 120: x
mov rbx, [_x]
;; 120: WIDTH
mov rdi, [_WIDTH]
;; 120: x < WIDTH
cmp rbx, rdi
jl @f
mov rbx, 0
jmp _after_cmp_20
@@:
mov rbx, -1
_after_cmp_20:

;; 120: WHILE x < WIDTH GOSUB evolveCell : x = x + 1
cmp rbx, 0
je _after_while_19

;; 121: GOSUB evolveCell
call __line_gosub_evolveCell

;; 122: x
mov rbx, [_x]
;; 122: 1
mov rdi, 1
;; 122: x + 1
add rbx, rdi
;; 122: x = x + 1
mov [_x], rbx

jmp _before_while_18
_after_while_19:

;; 124: y
mov rbx, [_y]
;; 124: 1
mov rdi, 1
;; 124: y + 1
add rbx, rdi
;; 124: y = y + 1
mov [_y], rbx

jmp _before_while_15
_after_while_16:

;; 127: REM 

;; 128: 0
mov rbx, 0
;; 128: y = 0
mov [_y], rbx

_before_while_21:
;; 129: y
mov rbx, [_y]
;; 129: HEIGHT
mov rdi, [_HEIGHT]
;; 129: y < HEIGHT
cmp rbx, rdi
jl @f
mov rbx, 0
jmp _after_cmp_23
@@:
mov rbx, -1
_after_cmp_23:

;; 129: WHILE y < HEIGHT x = 0 : WHILE x < WIDTH board(x, ...
cmp rbx, 0
je _after_while_22

;; 130: 0
mov rbx, 0
;; 130: x = 0
mov [_x], rbx

_before_while_24:
;; 131: x
mov rbx, [_x]
;; 131: WIDTH
mov rdi, [_WIDTH]
;; 131: x < WIDTH
cmp rbx, rdi
jl @f
mov rbx, 0
jmp _after_cmp_26
@@:
mov rbx, -1
_after_cmp_26:

;; 131: WHILE x < WIDTH board(x, y) = buffer(x, y) : x = x...
cmp rbx, 0
je _after_while_25

;; 132: buffer(x, y)
;; 132: x
mov rdi, [_x]
mov rsi, [_buffer_arr_dim_1]
imul rdi, rsi
;; 132: y
mov rsi, [_y]
add rdi, rsi
mov rbx, [_buffer_arr+8*rdi]
;; 132: board(x, y) = buffer(x, y)
;; 132: board(x, y)
;; 132: x
mov rdi, [_x]
mov rsi, [_board_arr_dim_1]
imul rdi, rsi
;; 132: y
mov rsi, [_y]
add rdi, rsi
mov [_board_arr+8*rdi], rbx

;; 133: x
mov rbx, [_x]
;; 133: 1
mov rdi, 1
;; 133: x + 1
add rbx, rdi
;; 133: x = x + 1
mov [_x], rbx

jmp _before_while_24
_after_while_25:

;; 135: y
mov rbx, [_y]
;; 135: 1
mov rdi, 1
;; 135: y + 1
add rbx, rdi
;; 135: y = y + 1
mov [_y], rbx

jmp _before_while_21
_after_while_22:

ret

;; 141: REM 

;; 142: REM 

;; 143: REM 

;; 144: REM 

__line_evolveCell:
;; 147: 0
mov rbx, 0
;; 147: count = 0
mov [_count], rbx


;; 149: REM 

;; 150: x
mov rbx, [_x]
;; 150: 1
mov rdi, 1
;; 150: x - 1
sub rbx, rdi
;; 150: xx = x - 1
mov [_xx], rbx

;; 151: y
mov rbx, [_y]
;; 151: 1
mov rdi, 1
;; 151: y - 1
sub rbx, rdi
;; 151: yy = y - 1
mov [_yy], rbx

;; 152: GOSUB countIfLive
call __line_gosub_countIfLive

;; 153: x
mov rbx, [_x]
;; 153: xx = x
mov [_xx], rbx

;; 154: y
mov rbx, [_y]
;; 154: 1
mov rdi, 1
;; 154: y - 1
sub rbx, rdi
;; 154: yy = y - 1
mov [_yy], rbx

;; 155: GOSUB countIfLive
call __line_gosub_countIfLive

;; 156: x
mov rbx, [_x]
;; 156: 1
mov rdi, 1
;; 156: x + 1
add rbx, rdi
;; 156: xx = x + 1
mov [_xx], rbx

;; 157: y
mov rbx, [_y]
;; 157: 1
mov rdi, 1
;; 157: y - 1
sub rbx, rdi
;; 157: yy = y - 1
mov [_yy], rbx

;; 158: GOSUB countIfLive
call __line_gosub_countIfLive

;; 160: REM 

;; 161: x
mov rbx, [_x]
;; 161: 1
mov rdi, 1
;; 161: x - 1
sub rbx, rdi
;; 161: xx = x - 1
mov [_xx], rbx

;; 162: y
mov rbx, [_y]
;; 162: yy = y
mov [_yy], rbx

;; 163: GOSUB countIfLive
call __line_gosub_countIfLive

;; 164: REM 

;; 165: x
mov rbx, [_x]
;; 165: 1
mov rdi, 1
;; 165: x + 1
add rbx, rdi
;; 165: xx = x + 1
mov [_xx], rbx

;; 166: y
mov rbx, [_y]
;; 166: yy = y
mov [_yy], rbx

;; 167: GOSUB countIfLive
call __line_gosub_countIfLive

;; 169: REM 

;; 170: x
mov rbx, [_x]
;; 170: 1
mov rdi, 1
;; 170: x - 1
sub rbx, rdi
;; 170: xx = x - 1
mov [_xx], rbx

;; 171: y
mov rbx, [_y]
;; 171: 1
mov rdi, 1
;; 171: y + 1
add rbx, rdi
;; 171: yy = y + 1
mov [_yy], rbx

;; 172: GOSUB countIfLive
call __line_gosub_countIfLive

;; 173: x
mov rbx, [_x]
;; 173: xx = x
mov [_xx], rbx

;; 174: y
mov rbx, [_y]
;; 174: 1
mov rdi, 1
;; 174: y + 1
add rbx, rdi
;; 174: yy = y + 1
mov [_yy], rbx

;; 175: GOSUB countIfLive
call __line_gosub_countIfLive

;; 176: x
mov rbx, [_x]
;; 176: 1
mov rdi, 1
;; 176: x + 1
add rbx, rdi
;; 176: xx = x + 1
mov [_xx], rbx

;; 177: y
mov rbx, [_y]
;; 177: 1
mov rdi, 1
;; 177: y + 1
add rbx, rdi
;; 177: yy = y + 1
mov [_yy], rbx

;; 178: GOSUB countIfLive
call __line_gosub_countIfLive

;; 180: REM 

;; 181: REM 

;; 182: REM 

;; 183: count
mov rbx, [_count]
;; 183: 2
mov rdi, 2
;; 183: count == 2
cmp rbx, rdi
je @f
mov rbx, 0
jmp _after_cmp_29
@@:
mov rbx, -1
_after_cmp_29:
;; 183: board(x, y)
;; 183: x
mov rsi, [_x]
mov r12, [_board_arr_dim_1]
imul rsi, r12
;; 183: y
mov r12, [_y]
add rsi, r12
mov rdi, [_board_arr+8*rsi]
;; 183: 1
mov rsi, 1
;; 183: board(x, y) == 1
cmp rdi, rsi
je @f
mov rdi, 0
jmp _after_cmp_30
@@:
mov rdi, -1
_after_cmp_30:
;; 183: (count == 2 AND board(x, y) == 1)
and rbx, rdi

;; 183: IF (count == 2 AND board(x, y) == 1) THEN buffer(x...
cmp rbx, 0
je _after_then_27

;; 184: 1
mov rbx, 1
;; 184: buffer(x, y) = 1
;; 184: buffer(x, y)
;; 184: x
mov rdi, [_x]
mov rsi, [_buffer_arr_dim_1]
imul rdi, rsi
;; 184: y
mov rsi, [_y]
add rdi, rsi
mov [_buffer_arr+8*rdi], rbx

jmp _after_else_28
_after_then_27:

;; 185: count
mov rbx, [_count]
;; 185: 3
mov rdi, 3
;; 185: count == 3
cmp rbx, rdi
je @f
mov rbx, 0
jmp _after_cmp_33
@@:
mov rbx, -1
_after_cmp_33:

;; 185: IF count == 3 THEN buffer(x, y) = 1 ELSE buffer(x,...
cmp rbx, 0
je _after_then_31

;; 186: 1
mov rbx, 1
;; 186: buffer(x, y) = 1
;; 186: buffer(x, y)
;; 186: x
mov rdi, [_x]
mov rsi, [_buffer_arr_dim_1]
imul rdi, rsi
;; 186: y
mov rsi, [_y]
add rdi, rsi
mov [_buffer_arr+8*rdi], rbx

jmp _after_else_32
_after_then_31:

;; 188: 0
mov rbx, 0
;; 188: buffer(x, y) = 0
;; 188: buffer(x, y)
;; 188: x
mov rdi, [_x]
mov rsi, [_buffer_arr_dim_1]
imul rdi, rsi
;; 188: y
mov rsi, [_y]
add rdi, rsi
mov [_buffer_arr+8*rdi], rbx

_after_else_32:

_after_else_28:

ret

;; 194: REM 

;; 195: REM 

;; 196: REM 

;; 197: REM 

;; 198: REM 

__line_countIfLive:
;; 201: xx
mov rbx, [_xx]
;; 201: 0
mov rdi, 0
;; 201: xx >= 0
cmp rbx, rdi
jge @f
mov rbx, 0
jmp _after_cmp_36
@@:
mov rbx, -1
_after_cmp_36:
;; 201: xx
mov rdi, [_xx]
;; 201: WIDTH
mov rsi, [_WIDTH]
;; 201: xx < WIDTH
cmp rdi, rsi
jl @f
mov rdi, 0
jmp _after_cmp_37
@@:
mov rdi, -1
_after_cmp_37:
;; 201: (xx >= 0 AND xx < WIDTH)
and rbx, rdi
;; 201: yy
mov rdi, [_yy]
;; 201: 0
mov rsi, 0
;; 201: yy >= 0
cmp rdi, rsi
jge @f
mov rdi, 0
jmp _after_cmp_38
@@:
mov rdi, -1
_after_cmp_38:
;; 201: ((xx >= 0 AND xx < WIDTH) AND yy >= 0)
and rbx, rdi
;; 201: yy
mov rdi, [_yy]
;; 201: HEIGHT
mov rsi, [_HEIGHT]
;; 201: yy < HEIGHT
cmp rdi, rsi
jl @f
mov rdi, 0
jmp _after_cmp_39
@@:
mov rdi, -1
_after_cmp_39:
;; 201: (((xx >= 0 AND xx < WIDTH) AND yy >= 0) AND yy < H...
and rbx, rdi
;; 201: board(xx, yy)
;; 201: xx
mov rsi, [_xx]
mov r12, [_board_arr_dim_1]
imul rsi, r12
;; 201: yy
mov r12, [_yy]
add rsi, r12
mov rdi, [_board_arr+8*rsi]
;; 201: 1
mov rsi, 1
;; 201: board(xx, yy) == 1
cmp rdi, rsi
je @f
mov rdi, 0
jmp _after_cmp_40
@@:
mov rdi, -1
_after_cmp_40:
;; 201: ((((xx >= 0 AND xx < WIDTH) AND yy >= 0) AND yy < ...
and rbx, rdi

;; 201: IF ((((xx >= 0 AND xx < WIDTH) AND yy >= 0) AND yy...
cmp rbx, 0
je _after_then_34

;; 202: count
mov rbx, [_count]
;; 202: 1
mov rdi, 1
;; 202: count + 1
add rbx, rdi
;; 202: count = count + 1
mov [_count], rbx

_after_then_34:

_after_else_35:


ret

;; 208: REM 

;; 209: REM 

;; 210: REM 

;; 211: REM 

__line_printBoard:
;; --- 214: PRINT separator -->
;; Evaluate arguments (_printf_lib)
;; 214: _fmt_Str
mov rbx, __fmt_Str
;; 214: separator
mov rdi, [_separator]
;; Move arguments to argument passing registers (_printf_lib)
mov rcx, rbx
mov rdx, rdi
;; Allocate shadow space (_printf_lib)
sub rsp, 20h
call [_printf_lib]
;; Clean up shadow space (_printf_lib)
add rsp, 20h
;; Ignore return value
;; <-- 214: PRINT separator ---


;; 215: 0
mov rbx, 0
;; 215: y = 0
mov [_y], rbx

_before_while_41:
;; 216: y
mov rbx, [_y]
;; 216: HEIGHT
mov rdi, [_HEIGHT]
;; 216: y < HEIGHT
cmp rbx, rdi
jl @f
mov rbx, 0
jmp _after_cmp_43
@@:
mov rbx, -1
_after_cmp_43:

;; 216: WHILE y < HEIGHT str = "" : x = 0 : WHILE x < WIDT...
cmp rbx, 0
je _after_while_42

;; 217: ""
mov rbx, __string_3
;; 217: str = ""
mov [_str], rbx

;; Make sure str does not refer to dynamic memory
mov rcx, 0h
mov [__str_type], rcx

;; 218: 0
mov rbx, 0
;; 218: x = 0
mov [_x], rbx

_before_while_44:
;; 219: x
mov rbx, [_x]
;; 219: WIDTH
mov rdi, [_WIDTH]
;; 219: x < WIDTH
cmp rbx, rdi
jl @f
mov rbx, 0
jmp _after_cmp_46
@@:
mov rbx, -1
_after_cmp_46:

;; 219: WHILE x < WIDTH IF board(x, y) THEN str = str + "O...
cmp rbx, 0
je _after_while_45

;; 220: board(x, y)
;; 220: x
mov rdi, [_x]
mov rsi, [_board_arr_dim_1]
imul rdi, rsi
;; 220: y
mov rsi, [_y]
add rdi, rsi
mov rbx, [_board_arr+8*rdi]

;; 220: IF board(x, y) THEN str = str + "O" ELSE str = str...
cmp rbx, 0
je _after_then_47


;; --- str + "O" -->
;; 221: str
mov rbx, [_str]
;; 221: "O"
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
;; <-- str + "O" ---

;; 221: str = str + "O"
mov [_str], rbx

;; Register dynamic memory assigned to str
lea rcx, [_str]
lea rdx, [__str_type]
sub rsp, 20h
call __memory_register_I64_I64
add rsp, 20h

jmp _after_else_48
_after_then_47:


;; --- str + " " -->
;; 223: str
mov rbx, [_str]
;; 223: " "
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
;; <-- str + " " ---

;; 223: str = str + " "
mov [_str], rbx

;; Register dynamic memory assigned to str
lea rcx, [_str]
lea rdx, [__str_type]
sub rsp, 20h
call __memory_register_I64_I64
add rsp, 20h

_after_else_48:

;; 225: x
mov rbx, [_x]
;; 225: 1
mov rdi, 1
;; 225: x + 1
add rbx, rdi
;; 225: x = x + 1
mov [_x], rbx

jmp _before_while_44
_after_while_45:

;; --- 227: PRINT str -->
;; Evaluate arguments (_printf_lib)
;; 227: _fmt_Str
mov rbx, __fmt_Str
;; 227: str
mov rdi, [_str]
;; Move arguments to argument passing registers (_printf_lib)
mov rcx, rbx
mov rdx, rdi
;; Allocate shadow space (_printf_lib)
sub rsp, 20h
call [_printf_lib]
;; Clean up shadow space (_printf_lib)
add rsp, 20h
;; Ignore return value
;; <-- 227: PRINT str ---

;; 228: y
mov rbx, [_y]
;; 228: 1
mov rdi, 1
;; 228: y + 1
add rbx, rdi
;; 228: y = y + 1
mov [_y], rbx

jmp _before_while_41
_after_while_42:

;; --- 230: PRINT separator -->
;; Evaluate arguments (_printf_lib)
;; 230: _fmt_Str
mov rbx, __fmt_Str
;; 230: separator
mov rdi, [_separator]
;; Move arguments to argument passing registers (_printf_lib)
mov rcx, rbx
mov rdx, rdi
;; Allocate shadow space (_printf_lib)
sub rsp, 20h
call [_printf_lib]
;; Clean up shadow space (_printf_lib)
add rsp, 20h
;; Ignore return value
;; <-- 230: PRINT separator ---

ret


;; --- GOSUB bridge calls -->
__line_gosub_countIfLive:
call __line_countIfLive
ret
__line_gosub_evolveBoard:
call __line_evolveBoard
ret
__line_gosub_evolveCell:
call __line_evolveCell
ret
__line_gosub_initializeRandom:
call __line_initializeRandom
ret
__line_gosub_printBoard:
call __line_printBoard
ret
;; <-- GOSUB bridge calls ---

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

;; string$(I64, Str) -> Str
__string$_I64_Str:
;; Enter function
push rbp
mov rbp, rsp
;; Save 2 argument(s) in home location(s)
mov [rbp+10h], rcx
mov [rbp+18h], rdx
cmp rcx, 0h
jl __string_str$_error
cmp [rdx], byte 0h
je __string_str$_error
inc rcx
;; malloc size already in rcx
sub rsp, 20h
call [_malloc_lib]
add rsp, 20h
mov rdx, [rbp+18h]
movzx rdx, byte [rdx]
mov r8, [rbp+10h]
mov rcx, rax
;; memset character already in rdx
;; memset size already in r8
sub rsp, 20h
call [_memset_lib]
add rsp, 20h
mov r11, [rbp+10h]
add r11, rax
mov [r11], byte 0h
jmp __string_str$_done
__string_str$_error:
mov rcx, __err_function_string$
sub rsp, 20h
call [_printf_lib]
add rsp, 20h
mov rcx, 1h
sub rsp, 20h
call [_exit_lib]
add rsp, 20h
__string_str$_done:
pop rbp
ret

;; <-- Built-in functions ---
