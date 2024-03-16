;;; JCC version: 0.8.2-SNAPSHOT
;;; Date & time: 2024-03-16T18:00:17.575857
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
;; Save base pointer
push rbp
mov rbp, rsp

;; --- 1: PRINT "Hello, world!" -->
;; Evaluate arguments (_printf_lib)
;; Defer evaluation of argument 0: _fmt_Str
;; Defer evaluation of argument 1: "Hello, world!"
;; Move arguments to argument passing registers (_printf_lib)
;; 1: _fmt_Str
mov rcx, __fmt_Str
;; 1: "Hello, world!"
mov rdx, __string_0
;; Allocate shadow space (_printf_lib)
sub rsp, 20h
call [_printf_lib]
;; Clean up shadow space (_printf_lib)
add rsp, 20h
;; Ignore return value
;; <-- 1: PRINT "Hello, world!" ---

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

