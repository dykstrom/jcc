; E3b - E3a with the one missing pop restored on the non-become tail leaf, i.e.
; what ColFunDefCodeGenerator would emit if generateTail routed its leaves
; through the GC's exitFunction the way ReturnCodeGenerator does.
;
; Identical to e3a in every other respect - same argv[1] call count - so any
; difference in max RSS is attributable to that single instruction.
;
; Expected output: yyyy

source_filename = "e3b-tail-if-with-pop"

@_.printf.fmt.Str.nl = private constant [4 x i8] c"%s\0A\00"
@_.str.empty = private constant [1 x i8] c"\00"
@_.str.y = private constant [2 x i8] c"y\00"
@jcc.gc.global.roots = private global [1 x { ptr, i64 }] [{ ptr, i64 } { ptr null, i64 0 }]

declare ptr @col_concat_str_str(ptr, ptr)
declare void @jcc_gc_add_root(ptr)
declare void @jcc_gc_init(i64, i64)
declare void @jcc_gc_pop_frame()
declare void @jcc_gc_push_frame()
declare ptr @jcc_gc_register(ptr)
declare void @jcc_gc_set_global_roots(ptr)
declare i32 @printf(ptr, ...)
declare i64 @strtol(ptr, ptr, i32)

; build(ptr, i64) -> ptr
define tailcc ptr @build_Str_I64(ptr %0, i64 %1) {
entry:
  call void @jcc_gc_push_frame()
  %_acc = alloca ptr
  store ptr %0, ptr %_acc
  %_n = alloca i64
  store i64 %1, ptr %_n
  %_.gc.slot.0 = alloca ptr
  call void @jcc_gc_add_root(ptr %_acc)
  store ptr null, ptr %_.gc.slot.0
  call void @jcc_gc_add_root(ptr %_.gc.slot.0)
  %2 = load i64, ptr %_n
  %3 = icmp eq i64 %2, 0
  br i1 %3, label %L0, label %L1

L0:
  %4 = load ptr, ptr %_acc
  call void @jcc_gc_pop_frame()                   ; the pop e3a is missing
  ret ptr %4

L1:
  ; become build(concat(acc, "y"), n - 1)
  %5 = load ptr, ptr %_acc
  %6 = call ptr @col_concat_str_str(ptr %5, ptr @_.str.y)
  %7 = call ptr @jcc_gc_register(ptr %6)
  store ptr %7, ptr %_.gc.slot.0
  %8 = load i64, ptr %_n
  %9 = sub i64 %8, 1
  call void @jcc_gc_pop_frame()
  %10 = musttail call tailcc ptr @build_Str_I64(ptr %7, i64 %9)
  ret ptr %10
}

define i32 @main(i32 %0, ptr %1) {
entry:
  ; calls = strtol(argv[1], NULL, 10)
  %2 = getelementptr inbounds ptr, ptr %1, i64 1
  %3 = load ptr, ptr %2
  %calls = call i64 @strtol(ptr %3, ptr null, i32 10)
  call void @jcc_gc_init(i64 1, i64 0)
  call void @jcc_gc_set_global_roots(ptr @jcc.gc.global.roots)
  call void @jcc_gc_push_frame()
  %_.gc.slot.0 = alloca ptr
  store ptr null, ptr %_.gc.slot.0
  call void @jcc_gc_add_root(ptr %_.gc.slot.0)
  br label %loop

loop:
  %i = phi i64 [ 0, %entry ], [ %inext, %loop ]
  %r = call tailcc ptr @build_Str_I64(ptr @_.str.empty, i64 4)
  store ptr %r, ptr %_.gc.slot.0
  %inext = add i64 %i, 1
  %done = icmp eq i64 %inext, %calls
  br i1 %done, label %exit, label %loop

exit:
  %last = load ptr, ptr %_.gc.slot.0
  %p = call i32 (ptr, ...) @printf(ptr @_.printf.fmt.Str.nl, ptr %last)
  call void @jcc_gc_pop_frame()
  ret i32 0
}
