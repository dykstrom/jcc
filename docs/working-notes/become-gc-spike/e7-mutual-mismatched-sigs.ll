; E7 - mutual become between two string functions whose prototypes differ in
; both arity order and parameter types:
;
;   fun ping(acc as string, n as i64) -> string :=
;       if n == 0 then acc else become pong(n - 1, concat(acc, "p"))
;   fun pong(n as i64, acc as string) -> string :=
;       become ping(concat(acc, "q"), n)
;
; ptr,i64 tail-calling i64,ptr is exactly the mismatch that made `tailcc`
; necessary (ADR 0001): musttail under the C convention requires matching
; prototypes. Each side pops its own frame before its musttail, so the two frame
; shapes never overlap.
;
; Expected output: pqpqpqpq

source_filename = "e7-mutual-mismatched-sigs"

@_.printf.fmt.Str.nl = private constant [4 x i8] c"%s\0A\00"
@_.str.empty = private constant [1 x i8] c"\00"
@_.str.p = private constant [2 x i8] c"p\00"
@_.str.q = private constant [2 x i8] c"q\00"
@jcc.gc.global.roots = private global [1 x { ptr, i64 }] [{ ptr, i64 } { ptr null, i64 0 }]

declare ptr @col_concat_str_str(ptr, ptr)
declare void @jcc_gc_add_root(ptr)
declare void @jcc_gc_init(i64, i64)
declare void @jcc_gc_pop_frame()
declare void @jcc_gc_push_frame()
declare ptr @jcc_gc_register(ptr)
declare void @jcc_gc_set_global_roots(ptr)
declare i32 @printf(ptr, ...)

; ping(ptr, i64) -> ptr
define tailcc ptr @ping_Str_I64(ptr %0, i64 %1) {
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
  call void @jcc_gc_pop_frame()
  ret ptr %4

L1:
  ; become pong(n - 1, concat(acc, "p"))
  %5 = load i64, ptr %_n
  %6 = sub i64 %5, 1
  %7 = load ptr, ptr %_acc
  %8 = call ptr @col_concat_str_str(ptr %7, ptr @_.str.p)
  %9 = call ptr @jcc_gc_register(ptr %8)
  store ptr %9, ptr %_.gc.slot.0
  call void @jcc_gc_pop_frame()
  %10 = musttail call tailcc ptr @pong_I64_Str(i64 %6, ptr %9)
  ret ptr %10
}

; pong(i64, ptr) -> ptr
define tailcc ptr @pong_I64_Str(i64 %0, ptr %1) {
entry:
  call void @jcc_gc_push_frame()
  %_n = alloca i64
  store i64 %0, ptr %_n
  %_acc = alloca ptr
  store ptr %1, ptr %_acc
  %_.gc.slot.0 = alloca ptr
  call void @jcc_gc_add_root(ptr %_acc)
  store ptr null, ptr %_.gc.slot.0
  call void @jcc_gc_add_root(ptr %_.gc.slot.0)
  ; become ping(concat(acc, "q"), n)
  %2 = load ptr, ptr %_acc
  %3 = call ptr @col_concat_str_str(ptr %2, ptr @_.str.q)
  %4 = call ptr @jcc_gc_register(ptr %3)
  store ptr %4, ptr %_.gc.slot.0
  %5 = load i64, ptr %_n
  call void @jcc_gc_pop_frame()
  %6 = musttail call tailcc ptr @ping_Str_I64(ptr %4, i64 %5)
  ret ptr %6
}

define i32 @main() {
entry:
  call void @jcc_gc_init(i64 1, i64 0)
  call void @jcc_gc_set_global_roots(ptr @jcc.gc.global.roots)
  call void @jcc_gc_push_frame()
  %_.gc.slot.0 = alloca ptr
  store ptr null, ptr %_.gc.slot.0
  call void @jcc_gc_add_root(ptr %_.gc.slot.0)
  %0 = call tailcc ptr @ping_Str_I64(ptr @_.str.empty, i64 4)
  store ptr %0, ptr %_.gc.slot.0
  %1 = call i32 (ptr, ...) @printf(ptr @_.printf.fmt.Str.nl, ptr %0)
  call void @jcc_gc_pop_frame()
  ret i32 0
}
