;;; JCC version: 0.8.1
;;; Date & time: 2023-12-02T15:00:48.708206
;;; Source file: hello.bas
format PE64 console
entry __main
include 'win64a.inc'

section '.idata' import data readable

library msvcrt,'msvcrt.dll'

import msvcrt,\
_exit_lib,'exit',\
_printf_lib,'printf'

section '.data' data readable writeable

__empty db "",0
__fmt_Str db "%s",10,0
__string_0 db "Hello, world!",0

;; --- Dynamic memory type pointers -->
__gc_type_pointers_start dq 0h
__gc_type_pointers_stop dq 0h
;; <-- Dynamic memory type pointers ---

section '.code' code readable executable

__main:
;; Save used non-volatile registers
push rbx
push rdi
;; Align stack
sub rsp, 8

;; --- 1: PRINT "Hello, world!" -->
;; Evaluate arguments (_printf_lib)
;; 1: _fmt_Str
mov rbx, __fmt_Str
;; 1: "Hello, world!"
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
;; <-- 1: PRINT "Hello, world!" ---

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

