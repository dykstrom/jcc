;;; JCC version: 0.10.0
;;; Date & time: 2025-08-09T14:06:44.74245
;;; Source file: game_of_life.bas
format PE64 console
entry __main
include 'win64a.inc'

section '.idata' import data readable

library jccbasic,'jccbasic.dll',\
msvcrt,'msvcrt.dll'

import jccbasic,\
_chr$_lib,'chr$',\
_randomize_lib,'randomize',\
_rnd_lib,'rnd',\
_sleep_F64_lib,'sleep_F64',\
_string$_Str_lib,'string$_Str',\
_timer_lib,'timer'

import msvcrt,\
_exit_lib,'exit',\
_free_lib,'free',\
_malloc_lib,'malloc',\
_printf_lib,'printf',\
_strcat_lib,'strcat',\
_strcpy_lib,'strcpy',\
_strlen_lib,'strlen'

section '.data' data readable writeable

_HEIGHT dq 30
_WIDTH dq 60
__cls_ansi_codes db 27,"[2J",27,"[H",0
__empty db "",0
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
;; Save base pointer
push rbp
mov rbp, rsp
;; Save g.p. registers
push rbx
push rdi
push rsi
push r12
;; Save float registers
sub rsp, 10h
movdqu [rsp], xmm6
sub rsp, 10h
movdqu [rsp], xmm7

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
mov rdx, __string_6
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

;; 2: REM 

;; 4: CONST WIDTH : I64 = 60, HEIGHT : I64 = 30







;; --- 12: string$(WIDTH, "-") -->
;; Evaluate arguments (_string$_Str_lib)
;; Defer evaluation of argument 0: WIDTH
;; Defer evaluation of argument 1: "-"
;; Move arguments to argument passing registers (_string$_Str_lib)
;; 12: WIDTH
mov rcx, [_WIDTH]
;; 12: "-"
mov rdx, __string_0
;; Allocate shadow space (_string$_Str_lib)
sub rsp, 20h
call [_string$_Str_lib]
;; Clean up shadow space (_string$_Str_lib)
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
;; Defer evaluation of argument 0: _cls_ansi_codes
;; Move arguments to argument passing registers (_printf_lib)
;; 14: _cls_ansi_codes
mov rcx, __cls_ansi_codes
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
;; Defer evaluation of argument 0: _fmt_Str_Str

;; --- 27: chr$(27) -->
;; Evaluate arguments (_chr$_lib)
;; Defer evaluation of argument 0: 27
;; Move arguments to argument passing registers (_chr$_lib)
;; 27: 27
mov rcx, 27
;; Allocate shadow space (_chr$_lib)
sub rsp, 20h
call [_chr$_lib]
;; Clean up shadow space (_chr$_lib)
add rsp, 20h
;; Move return value (rax) to storage location (rbx)
mov rbx, rax
;; <-- 27: chr$(27) ---

;; Defer evaluation of argument 2: "[H"
;; Move arguments to argument passing registers (_printf_lib)
;; 27: _fmt_Str_Str
mov rcx, __fmt_Str_Str
mov rdx, rbx
;; 27: "[H"
mov r8, __string_1
;; Allocate shadow space (_printf_lib)
sub rsp, 20h
call [_printf_lib]
;; Clean up shadow space (_printf_lib)
add rsp, 20h
;; Ignore return value
;; Free dynamic memory in rbx
mov rcx, rbx
;; free address already in rcx
sub rsp, 20h
call [_free_lib]
add rsp, 20h
;; <-- 27: PRINT chr$(27), "[H" ---

;; --- 28: PRINT "Generation ", generation -->
;; Evaluate arguments (_printf_lib)
;; Defer evaluation of argument 0: _fmt_Str_I64
;; Defer evaluation of argument 1: "Generation "
;; Defer evaluation of argument 2: generation
;; Move arguments to argument passing registers (_printf_lib)
;; 28: _fmt_Str_I64
mov rcx, __fmt_Str_I64
;; 28: "Generation "
mov rdx, __string_2
;; 28: generation
mov r8, [_generation]
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

;; 33: SLEEP 0.5
;; --- _sleep_F64_lib(0.5) -->
;; Evaluate arguments (_sleep_F64_lib)
;; Defer evaluation of argument 0: 0.5
;; Move arguments to argument passing registers (_sleep_F64_lib)
;; 33: 0.5
movsd xmm0, [__float_0]
;; Allocate shadow space (_sleep_F64_lib)
sub rsp, 20h
call [_sleep_F64_lib]
;; Clean up shadow space (_sleep_F64_lib)
add rsp, 20h
;; Ignore return value
;; <-- _sleep_F64_lib(0.5) ---

jmp _before_while_0
_after_while_1:

;; 36: REM 

;; --- 37: END -->
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
;; <-- 37: END ---

;; 40: REM 

;; 41: REM 

;; 42: REM 

;; 43: REM 

__line_initializeRandom:
;; 46: RANDOMIZE timer()
;; --- .randomize(timer()) -->
;; Evaluate arguments (_randomize_lib)

;; --- 46: timer() -->
;; Allocate shadow space (_timer_lib)
sub rsp, 20h
call [_timer_lib]
;; Clean up shadow space (_timer_lib)
add rsp, 20h
;; Move return value (xmm0) to storage location (xmm6)
movsd xmm6, xmm0
;; <-- 46: timer() ---

;; Move arguments to argument passing registers (_randomize_lib)
movsd xmm0, xmm6
;; Allocate shadow space (_randomize_lib)
sub rsp, 20h
call [_randomize_lib]
;; Clean up shadow space (_randomize_lib)
add rsp, 20h
;; Ignore return value
;; <-- .randomize(timer()) ---


;; 48: 0
mov rbx, 0
;; 48: y = 0
mov [_y], rbx

_before_while_3:
;; 49: y
mov rbx, [_y]
;; 49: HEIGHT
mov rdi, [_HEIGHT]
;; 49: y < HEIGHT
cmp rbx, rdi
jl @f
mov rbx, 0
jmp _after_cmp_5
@@:
mov rbx, -1
_after_cmp_5:

;; 49: WHILE y < HEIGHT x = 0 : WHILE x < WIDTH IF rnd() ...
cmp rbx, 0
je _after_while_4

;; 50: 0
mov rbx, 0
;; 50: x = 0
mov [_x], rbx

_before_while_6:
;; 51: x
mov rbx, [_x]
;; 51: WIDTH
mov rdi, [_WIDTH]
;; 51: x < WIDTH
cmp rbx, rdi
jl @f
mov rbx, 0
jmp _after_cmp_8
@@:
mov rbx, -1
_after_cmp_8:

;; 51: WHILE x < WIDTH IF rnd() > 0.5 THEN board(x, y) = ...
cmp rbx, 0
je _after_while_7


;; --- 52: rnd() -->
;; Allocate shadow space (_rnd_lib)
sub rsp, 20h
call [_rnd_lib]
;; Clean up shadow space (_rnd_lib)
add rsp, 20h
;; Move return value (xmm0) to storage location (xmm6)
movsd xmm6, xmm0
;; <-- 52: rnd() ---

;; 52: 0.5
movsd xmm7, [__float_0]
;; 52: rnd() > 0.5
ucomisd xmm6, xmm7
ja @f
mov rbx, 0
jmp _after_cmp_11
@@:
mov rbx, -1
_after_cmp_11:

;; 52: IF rnd() > 0.5 THEN board(x, y) = 1
cmp rbx, 0
je _after_then_9

;; 52: 1
mov rbx, 1
;; 52: board(x, y) = 1
;; 52: board(x, y)
;; 52: x
mov rdi, [_x]
mov rsi, [_board_arr_dim_1]
imul rdi, rsi
;; 52: y
mov rsi, [_y]
add rdi, rsi
mov [_board_arr+8*rdi], rbx

_after_then_9:

_after_else_10:

;; 53: x
mov rbx, [_x]
;; 53: 1
mov rdi, 1
;; 53: x + 1
add rbx, rdi
;; 53: x = x + 1
mov [_x], rbx

jmp _before_while_6
_after_while_7:

;; 55: y
mov rbx, [_y]
;; 55: 1
mov rdi, 1
;; 55: y + 1
add rbx, rdi
;; 55: y = y + 1
mov [_y], rbx

jmp _before_while_3
_after_while_4:

ret

;; 61: REM 

;; 62: REM 

;; 63: REM 

;; 64: REM 

__line_initializeBlinker:
;; 67: 1
mov rbx, 1
;; 67: board(0, 1) = 1
;; 67: board(0, 1)
;; 67: 0
mov rdi, 0
mov rsi, [_board_arr_dim_1]
imul rdi, rsi
;; 67: 1
mov rsi, 1
add rdi, rsi
mov [_board_arr+8*rdi], rbx


;; 68: 1
mov rbx, 1
;; 68: board(1, 1) = 1
;; 68: board(1, 1)
;; 68: 1
mov rdi, 1
mov rsi, [_board_arr_dim_1]
imul rdi, rsi
;; 68: 1
mov rsi, 1
add rdi, rsi
mov [_board_arr+8*rdi], rbx

;; 69: 1
mov rbx, 1
;; 69: board(2, 1) = 1
;; 69: board(2, 1)
;; 69: 2
mov rdi, 2
mov rsi, [_board_arr_dim_1]
imul rdi, rsi
;; 69: 1
mov rsi, 1
add rdi, rsi
mov [_board_arr+8*rdi], rbx

ret

;; 74: REM 

;; 75: REM 

;; 76: REM 

;; 77: REM 

__line_initializeBeacon:
;; 80: 1
mov rbx, 1
;; 80: board(0, 0) = 1
;; 80: board(0, 0)
;; 80: 0
mov rdi, 0
mov rsi, [_board_arr_dim_1]
imul rdi, rsi
;; 80: 0
mov rsi, 0
add rdi, rsi
mov [_board_arr+8*rdi], rbx


;; 81: 1
mov rbx, 1
;; 81: board(1, 0) = 1
;; 81: board(1, 0)
;; 81: 1
mov rdi, 1
mov rsi, [_board_arr_dim_1]
imul rdi, rsi
;; 81: 0
mov rsi, 0
add rdi, rsi
mov [_board_arr+8*rdi], rbx

;; 82: 1
mov rbx, 1
;; 82: board(0, 1) = 1
;; 82: board(0, 1)
;; 82: 0
mov rdi, 0
mov rsi, [_board_arr_dim_1]
imul rdi, rsi
;; 82: 1
mov rsi, 1
add rdi, rsi
mov [_board_arr+8*rdi], rbx

;; 83: 1
mov rbx, 1
;; 83: board(1, 1) = 1
;; 83: board(1, 1)
;; 83: 1
mov rdi, 1
mov rsi, [_board_arr_dim_1]
imul rdi, rsi
;; 83: 1
mov rsi, 1
add rdi, rsi
mov [_board_arr+8*rdi], rbx

;; 85: 1
mov rbx, 1
;; 85: board(2, 2) = 1
;; 85: board(2, 2)
;; 85: 2
mov rdi, 2
mov rsi, [_board_arr_dim_1]
imul rdi, rsi
;; 85: 2
mov rsi, 2
add rdi, rsi
mov [_board_arr+8*rdi], rbx

;; 86: 1
mov rbx, 1
;; 86: board(3, 2) = 1
;; 86: board(3, 2)
;; 86: 3
mov rdi, 3
mov rsi, [_board_arr_dim_1]
imul rdi, rsi
;; 86: 2
mov rsi, 2
add rdi, rsi
mov [_board_arr+8*rdi], rbx

;; 87: 1
mov rbx, 1
;; 87: board(2, 3) = 1
;; 87: board(2, 3)
;; 87: 2
mov rdi, 2
mov rsi, [_board_arr_dim_1]
imul rdi, rsi
;; 87: 3
mov rsi, 3
add rdi, rsi
mov [_board_arr+8*rdi], rbx

;; 88: 1
mov rbx, 1
;; 88: board(3, 3) = 1
;; 88: board(3, 3)
;; 88: 3
mov rdi, 3
mov rsi, [_board_arr_dim_1]
imul rdi, rsi
;; 88: 3
mov rsi, 3
add rdi, rsi
mov [_board_arr+8*rdi], rbx

ret

;; 93: REM 

;; 94: REM 

;; 95: REM 

;; 96: REM 

__line_initializeGlider:
;; 99: 1
mov rbx, 1
;; 99: board(1, 0) = 1
;; 99: board(1, 0)
;; 99: 1
mov rdi, 1
mov rsi, [_board_arr_dim_1]
imul rdi, rsi
;; 99: 0
mov rsi, 0
add rdi, rsi
mov [_board_arr+8*rdi], rbx


;; 100: 1
mov rbx, 1
;; 100: board(2, 1) = 1
;; 100: board(2, 1)
;; 100: 2
mov rdi, 2
mov rsi, [_board_arr_dim_1]
imul rdi, rsi
;; 100: 1
mov rsi, 1
add rdi, rsi
mov [_board_arr+8*rdi], rbx

;; 101: 1
mov rbx, 1
;; 101: board(2, 2) = 1
;; 101: board(2, 2)
;; 101: 2
mov rdi, 2
mov rsi, [_board_arr_dim_1]
imul rdi, rsi
;; 101: 2
mov rsi, 2
add rdi, rsi
mov [_board_arr+8*rdi], rbx

;; 102: 1
mov rbx, 1
;; 102: board(1, 2) = 1
;; 102: board(1, 2)
;; 102: 1
mov rdi, 1
mov rsi, [_board_arr_dim_1]
imul rdi, rsi
;; 102: 2
mov rsi, 2
add rdi, rsi
mov [_board_arr+8*rdi], rbx

;; 103: 1
mov rbx, 1
;; 103: board(0, 2) = 1
;; 103: board(0, 2)
;; 103: 0
mov rdi, 0
mov rsi, [_board_arr_dim_1]
imul rdi, rsi
;; 103: 2
mov rsi, 2
add rdi, rsi
mov [_board_arr+8*rdi], rbx

ret

;; 108: REM 

;; 109: REM 

;; 110: REM 

;; 111: REM 

__line_evolveBoard:
;; 114: REM 


;; 115: 0
mov rbx, 0
;; 115: y = 0
mov [_y], rbx

_before_while_12:
;; 116: y
mov rbx, [_y]
;; 116: HEIGHT
mov rdi, [_HEIGHT]
;; 116: y < HEIGHT
cmp rbx, rdi
jl @f
mov rbx, 0
jmp _after_cmp_14
@@:
mov rbx, -1
_after_cmp_14:

;; 116: WHILE y < HEIGHT x = 0 : WHILE x < WIDTH GOSUB evo...
cmp rbx, 0
je _after_while_13

;; 117: 0
mov rbx, 0
;; 117: x = 0
mov [_x], rbx

_before_while_15:
;; 118: x
mov rbx, [_x]
;; 118: WIDTH
mov rdi, [_WIDTH]
;; 118: x < WIDTH
cmp rbx, rdi
jl @f
mov rbx, 0
jmp _after_cmp_17
@@:
mov rbx, -1
_after_cmp_17:

;; 118: WHILE x < WIDTH GOSUB evolveCell : x = x + 1
cmp rbx, 0
je _after_while_16

;; 119: GOSUB evolveCell
call __line_gosub_evolveCell

;; 120: x
mov rbx, [_x]
;; 120: 1
mov rdi, 1
;; 120: x + 1
add rbx, rdi
;; 120: x = x + 1
mov [_x], rbx

jmp _before_while_15
_after_while_16:

;; 122: y
mov rbx, [_y]
;; 122: 1
mov rdi, 1
;; 122: y + 1
add rbx, rdi
;; 122: y = y + 1
mov [_y], rbx

jmp _before_while_12
_after_while_13:

;; 125: REM 

;; 126: 0
mov rbx, 0
;; 126: y = 0
mov [_y], rbx

_before_while_18:
;; 127: y
mov rbx, [_y]
;; 127: HEIGHT
mov rdi, [_HEIGHT]
;; 127: y < HEIGHT
cmp rbx, rdi
jl @f
mov rbx, 0
jmp _after_cmp_20
@@:
mov rbx, -1
_after_cmp_20:

;; 127: WHILE y < HEIGHT x = 0 : WHILE x < WIDTH board(x, ...
cmp rbx, 0
je _after_while_19

;; 128: 0
mov rbx, 0
;; 128: x = 0
mov [_x], rbx

_before_while_21:
;; 129: x
mov rbx, [_x]
;; 129: WIDTH
mov rdi, [_WIDTH]
;; 129: x < WIDTH
cmp rbx, rdi
jl @f
mov rbx, 0
jmp _after_cmp_23
@@:
mov rbx, -1
_after_cmp_23:

;; 129: WHILE x < WIDTH board(x, y) = buffer(x, y) : x = x...
cmp rbx, 0
je _after_while_22

;; 130: buffer(x, y)
;; 130: x
mov rdi, [_x]
mov rsi, [_buffer_arr_dim_1]
imul rdi, rsi
;; 130: y
mov rsi, [_y]
add rdi, rsi
mov rbx, [_buffer_arr+8*rdi]
;; 130: board(x, y) = buffer(x, y)
;; 130: board(x, y)
;; 130: x
mov rdi, [_x]
mov rsi, [_board_arr_dim_1]
imul rdi, rsi
;; 130: y
mov rsi, [_y]
add rdi, rsi
mov [_board_arr+8*rdi], rbx

;; 131: x
mov rbx, [_x]
;; 131: 1
mov rdi, 1
;; 131: x + 1
add rbx, rdi
;; 131: x = x + 1
mov [_x], rbx

jmp _before_while_21
_after_while_22:

;; 133: y
mov rbx, [_y]
;; 133: 1
mov rdi, 1
;; 133: y + 1
add rbx, rdi
;; 133: y = y + 1
mov [_y], rbx

jmp _before_while_18
_after_while_19:

ret

;; 139: REM 

;; 140: REM 

;; 141: REM 

;; 142: REM 

__line_evolveCell:
;; 145: 0
mov rbx, 0
;; 145: count = 0
mov [_count], rbx


;; 147: REM 

;; 148: x
mov rbx, [_x]
;; 148: 1
mov rdi, 1
;; 148: x - 1
sub rbx, rdi
;; 148: xx = x - 1
mov [_xx], rbx

;; 149: y
mov rbx, [_y]
;; 149: 1
mov rdi, 1
;; 149: y - 1
sub rbx, rdi
;; 149: yy = y - 1
mov [_yy], rbx

;; 150: GOSUB countIfLive
call __line_gosub_countIfLive

;; 151: x
mov rbx, [_x]
;; 151: xx = x
mov [_xx], rbx

;; 152: y
mov rbx, [_y]
;; 152: 1
mov rdi, 1
;; 152: y - 1
sub rbx, rdi
;; 152: yy = y - 1
mov [_yy], rbx

;; 153: GOSUB countIfLive
call __line_gosub_countIfLive

;; 154: x
mov rbx, [_x]
;; 154: 1
mov rdi, 1
;; 154: x + 1
add rbx, rdi
;; 154: xx = x + 1
mov [_xx], rbx

;; 155: y
mov rbx, [_y]
;; 155: 1
mov rdi, 1
;; 155: y - 1
sub rbx, rdi
;; 155: yy = y - 1
mov [_yy], rbx

;; 156: GOSUB countIfLive
call __line_gosub_countIfLive

;; 158: REM 

;; 159: x
mov rbx, [_x]
;; 159: 1
mov rdi, 1
;; 159: x - 1
sub rbx, rdi
;; 159: xx = x - 1
mov [_xx], rbx

;; 160: y
mov rbx, [_y]
;; 160: yy = y
mov [_yy], rbx

;; 161: GOSUB countIfLive
call __line_gosub_countIfLive

;; 162: REM 

;; 163: x
mov rbx, [_x]
;; 163: 1
mov rdi, 1
;; 163: x + 1
add rbx, rdi
;; 163: xx = x + 1
mov [_xx], rbx

;; 164: y
mov rbx, [_y]
;; 164: yy = y
mov [_yy], rbx

;; 165: GOSUB countIfLive
call __line_gosub_countIfLive

;; 167: REM 

;; 168: x
mov rbx, [_x]
;; 168: 1
mov rdi, 1
;; 168: x - 1
sub rbx, rdi
;; 168: xx = x - 1
mov [_xx], rbx

;; 169: y
mov rbx, [_y]
;; 169: 1
mov rdi, 1
;; 169: y + 1
add rbx, rdi
;; 169: yy = y + 1
mov [_yy], rbx

;; 170: GOSUB countIfLive
call __line_gosub_countIfLive

;; 171: x
mov rbx, [_x]
;; 171: xx = x
mov [_xx], rbx

;; 172: y
mov rbx, [_y]
;; 172: 1
mov rdi, 1
;; 172: y + 1
add rbx, rdi
;; 172: yy = y + 1
mov [_yy], rbx

;; 173: GOSUB countIfLive
call __line_gosub_countIfLive

;; 174: x
mov rbx, [_x]
;; 174: 1
mov rdi, 1
;; 174: x + 1
add rbx, rdi
;; 174: xx = x + 1
mov [_xx], rbx

;; 175: y
mov rbx, [_y]
;; 175: 1
mov rdi, 1
;; 175: y + 1
add rbx, rdi
;; 175: yy = y + 1
mov [_yy], rbx

;; 176: GOSUB countIfLive
call __line_gosub_countIfLive

;; 178: REM 

;; 179: REM 

;; 180: REM 

;; 181: count
mov rbx, [_count]
;; 181: 2
mov rdi, 2
;; 181: count == 2
cmp rbx, rdi
je @f
mov rbx, 0
jmp _after_cmp_26
@@:
mov rbx, -1
_after_cmp_26:
;; 181: board(x, y)
;; 181: x
mov rsi, [_x]
mov r12, [_board_arr_dim_1]
imul rsi, r12
;; 181: y
mov r12, [_y]
add rsi, r12
mov rdi, [_board_arr+8*rsi]
;; 181: 1
mov rsi, 1
;; 181: board(x, y) == 1
cmp rdi, rsi
je @f
mov rdi, 0
jmp _after_cmp_27
@@:
mov rdi, -1
_after_cmp_27:
;; 181: (count == 2 AND board(x, y) == 1)
and rbx, rdi

;; 181: IF (count == 2 AND board(x, y) == 1) THEN buffer(x...
cmp rbx, 0
je _after_then_24

;; 182: 1
mov rbx, 1
;; 182: buffer(x, y) = 1
;; 182: buffer(x, y)
;; 182: x
mov rdi, [_x]
mov rsi, [_buffer_arr_dim_1]
imul rdi, rsi
;; 182: y
mov rsi, [_y]
add rdi, rsi
mov [_buffer_arr+8*rdi], rbx

jmp _after_else_25
_after_then_24:

;; 183: count
mov rbx, [_count]
;; 183: 3
mov rdi, 3
;; 183: count == 3
cmp rbx, rdi
je @f
mov rbx, 0
jmp _after_cmp_30
@@:
mov rbx, -1
_after_cmp_30:

;; 183: IF count == 3 THEN buffer(x, y) = 1 ELSE buffer(x,...
cmp rbx, 0
je _after_then_28

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

jmp _after_else_29
_after_then_28:

;; 186: 0
mov rbx, 0
;; 186: buffer(x, y) = 0
;; 186: buffer(x, y)
;; 186: x
mov rdi, [_x]
mov rsi, [_buffer_arr_dim_1]
imul rdi, rsi
;; 186: y
mov rsi, [_y]
add rdi, rsi
mov [_buffer_arr+8*rdi], rbx

_after_else_29:

_after_else_25:

ret

;; 192: REM 

;; 193: REM 

;; 194: REM 

;; 195: REM 

;; 196: REM 

__line_countIfLive:
;; 199: xx
mov rbx, [_xx]
;; 199: 0
mov rdi, 0
;; 199: xx >= 0
cmp rbx, rdi
jge @f
mov rbx, 0
jmp _after_cmp_33
@@:
mov rbx, -1
_after_cmp_33:
;; 199: xx
mov rdi, [_xx]
;; 199: WIDTH
mov rsi, [_WIDTH]
;; 199: xx < WIDTH
cmp rdi, rsi
jl @f
mov rdi, 0
jmp _after_cmp_34
@@:
mov rdi, -1
_after_cmp_34:
;; 199: (xx >= 0 AND xx < WIDTH)
and rbx, rdi
;; 199: yy
mov rdi, [_yy]
;; 199: 0
mov rsi, 0
;; 199: yy >= 0
cmp rdi, rsi
jge @f
mov rdi, 0
jmp _after_cmp_35
@@:
mov rdi, -1
_after_cmp_35:
;; 199: ((xx >= 0 AND xx < WIDTH) AND yy >= 0)
and rbx, rdi
;; 199: yy
mov rdi, [_yy]
;; 199: HEIGHT
mov rsi, [_HEIGHT]
;; 199: yy < HEIGHT
cmp rdi, rsi
jl @f
mov rdi, 0
jmp _after_cmp_36
@@:
mov rdi, -1
_after_cmp_36:
;; 199: (((xx >= 0 AND xx < WIDTH) AND yy >= 0) AND yy < H...
and rbx, rdi
;; 199: board(xx, yy)
;; 199: xx
mov rsi, [_xx]
mov r12, [_board_arr_dim_1]
imul rsi, r12
;; 199: yy
mov r12, [_yy]
add rsi, r12
mov rdi, [_board_arr+8*rsi]
;; 199: 1
mov rsi, 1
;; 199: board(xx, yy) == 1
cmp rdi, rsi
je @f
mov rdi, 0
jmp _after_cmp_37
@@:
mov rdi, -1
_after_cmp_37:
;; 199: ((((xx >= 0 AND xx < WIDTH) AND yy >= 0) AND yy < ...
and rbx, rdi

;; 199: IF ((((xx >= 0 AND xx < WIDTH) AND yy >= 0) AND yy...
cmp rbx, 0
je _after_then_31

;; 200: count
mov rbx, [_count]
;; 200: 1
mov rdi, 1
;; 200: count + 1
add rbx, rdi
;; 200: count = count + 1
mov [_count], rbx

_after_then_31:

_after_else_32:


ret

;; 206: REM 

;; 207: REM 

;; 208: REM 

;; 209: REM 

__line_printBoard:
;; --- 212: PRINT separator -->
;; Evaluate arguments (_printf_lib)
;; Defer evaluation of argument 0: _fmt_Str
;; Defer evaluation of argument 1: separator
;; Move arguments to argument passing registers (_printf_lib)
;; 212: _fmt_Str
mov rcx, __fmt_Str
;; 212: separator
mov rdx, [_separator]
;; Allocate shadow space (_printf_lib)
sub rsp, 20h
call [_printf_lib]
;; Clean up shadow space (_printf_lib)
add rsp, 20h
;; Ignore return value
;; <-- 212: PRINT separator ---


;; 213: 0
mov rbx, 0
;; 213: y = 0
mov [_y], rbx

_before_while_38:
;; 214: y
mov rbx, [_y]
;; 214: HEIGHT
mov rdi, [_HEIGHT]
;; 214: y < HEIGHT
cmp rbx, rdi
jl @f
mov rbx, 0
jmp _after_cmp_40
@@:
mov rbx, -1
_after_cmp_40:

;; 214: WHILE y < HEIGHT str = "" : x = 0 : WHILE x < WIDT...
cmp rbx, 0
je _after_while_39

;; 215: ""
mov rbx, __string_3
;; 215: str = ""
mov [_str], rbx

;; Make sure str does not refer to dynamic memory
mov rcx, 0h
mov [__str_type], rcx

;; 216: 0
mov rbx, 0
;; 216: x = 0
mov [_x], rbx

_before_while_41:
;; 217: x
mov rbx, [_x]
;; 217: WIDTH
mov rdi, [_WIDTH]
;; 217: x < WIDTH
cmp rbx, rdi
jl @f
mov rbx, 0
jmp _after_cmp_43
@@:
mov rbx, -1
_after_cmp_43:

;; 217: WHILE x < WIDTH IF board(x, y) THEN str = str + "O...
cmp rbx, 0
je _after_while_42

;; 218: board(x, y)
;; 218: x
mov rdi, [_x]
mov rsi, [_board_arr_dim_1]
imul rdi, rsi
;; 218: y
mov rsi, [_y]
add rdi, rsi
mov rbx, [_board_arr+8*rdi]

;; 218: IF board(x, y) THEN str = str + "O" ELSE str = str...
cmp rbx, 0
je _after_then_44


;; --- str + "O" -->
;; 219: str
mov rbx, [_str]
;; 219: "O"
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

;; 219: str = str + "O"
mov [_str], rbx

;; Register dynamic memory assigned to str
lea rcx, [_str]
lea rdx, [__str_type]
sub rsp, 20h
call __memory_register_I64_I64
add rsp, 20h

jmp _after_else_45
_after_then_44:


;; --- str + " " -->
;; 221: str
mov rbx, [_str]
;; 221: " "
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

;; 221: str = str + " "
mov [_str], rbx

;; Register dynamic memory assigned to str
lea rcx, [_str]
lea rdx, [__str_type]
sub rsp, 20h
call __memory_register_I64_I64
add rsp, 20h

_after_else_45:

;; 223: x
mov rbx, [_x]
;; 223: 1
mov rdi, 1
;; 223: x + 1
add rbx, rdi
;; 223: x = x + 1
mov [_x], rbx

jmp _before_while_41
_after_while_42:

;; --- 225: PRINT str -->
;; Evaluate arguments (_printf_lib)
;; Defer evaluation of argument 0: _fmt_Str
;; Defer evaluation of argument 1: str
;; Move arguments to argument passing registers (_printf_lib)
;; 225: _fmt_Str
mov rcx, __fmt_Str
;; 225: str
mov rdx, [_str]
;; Allocate shadow space (_printf_lib)
sub rsp, 20h
call [_printf_lib]
;; Clean up shadow space (_printf_lib)
add rsp, 20h
;; Ignore return value
;; <-- 225: PRINT str ---

;; 226: y
mov rbx, [_y]
;; 226: 1
mov rdi, 1
;; 226: y + 1
add rbx, rdi
;; 226: y = y + 1
mov [_y], rbx

jmp _before_while_38
_after_while_39:

;; --- 228: PRINT separator -->
;; Evaluate arguments (_printf_lib)
;; Defer evaluation of argument 0: _fmt_Str
;; Defer evaluation of argument 1: separator
;; Move arguments to argument passing registers (_printf_lib)
;; 228: _fmt_Str
mov rcx, __fmt_Str
;; 228: separator
mov rdx, [_separator]
;; Allocate shadow space (_printf_lib)
sub rsp, 20h
call [_printf_lib]
;; Clean up shadow space (_printf_lib)
add rsp, 20h
;; Ignore return value
;; <-- 228: PRINT separator ---

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
