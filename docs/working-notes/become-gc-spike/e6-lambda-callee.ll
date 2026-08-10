; E6 - the become target is a lifted anonymous function. COL lifts lambdas to
; ordinary top-level tailcc functions named lambda.<n> with the usual mangled
; signature suffix (see the real emitted names in anonymous_functions.col:
; @lambda.0_I64, @lambda.1_I64_I64), so a string-taking lambda is
; @lambda.0_Str_I64.
;
; Same body as E1; only the callee's name differs. If nothing here needs special
; handling, lifted lambdas need nothing special.
;
; Expected output: 123

source_filename = "e6-lambda-callee"

@_.printf.fmt.Str.nl = private constant [4 x i8] c"%s\0A\00"
@_.str.empty = private constant [1 x i8] c"\00"
@jcc.gc.global.roots = private global [1 x { ptr, i64 }] [{ ptr, i64 } { ptr null, i64 0 }]

declare ptr @col_concat_str_str(ptr, ptr)
declare ptr @col_string_i64(i64)
declare void @jcc_gc_add_root(ptr)
declare void @jcc_gc_init(i64, i64)
declare void @jcc_gc_pop_frame()
declare void @jcc_gc_push_frame()
declare ptr @jcc_gc_register(ptr)
declare void @jcc_gc_set_global_roots(ptr)
declare i32 @printf(ptr, ...)

; lambda.0(ptr, i64) -> ptr
define tailcc ptr @lambda.0_Str_I64(ptr %0, i64 %1) {
entry:
  call void @jcc_gc_push_frame()
  %_acc = alloca ptr
  store ptr %0, ptr %_acc
  %_n = alloca i64
  store i64 %1, ptr %_n
  %_.gc.slot.0 = alloca ptr
  %_.gc.slot.1 = alloca ptr
  call void @jcc_gc_add_root(ptr %_acc)
  store ptr null, ptr %_.gc.slot.0
  call void @jcc_gc_add_root(ptr %_.gc.slot.0)
  store ptr null, ptr %_.gc.slot.1
  call void @jcc_gc_add_root(ptr %_.gc.slot.1)
  %2 = load i64, ptr %_n
  %3 = icmp eq i64 %2, 0
  br i1 %3, label %L0, label %L1

L0:
  %4 = load ptr, ptr %_acc
  call void @jcc_gc_pop_frame()
  ret ptr %4

L1:
  ; become lambda.0(concat(string(n), acc), n - 1)
  %5 = load i64, ptr %_n
  %6 = call ptr @col_string_i64(i64 %5)
  %7 = call ptr @jcc_gc_register(ptr %6)
  store ptr %7, ptr %_.gc.slot.0
  %8 = load ptr, ptr %_acc
  %9 = call ptr @col_concat_str_str(ptr %7, ptr %8)
  %10 = call ptr @jcc_gc_register(ptr %9)
  store ptr %10, ptr %_.gc.slot.1
  %11 = load i64, ptr %_n
  %12 = sub i64 %11, 1
  call void @jcc_gc_pop_frame()
  %13 = musttail call tailcc ptr @lambda.0_Str_I64(ptr %10, i64 %12)
  ret ptr %13
}

define i32 @main() {
entry:
  call void @jcc_gc_init(i64 1, i64 0)
  call void @jcc_gc_set_global_roots(ptr @jcc.gc.global.roots)
  call void @jcc_gc_push_frame()
  %_.gc.slot.0 = alloca ptr
  store ptr null, ptr %_.gc.slot.0
  call void @jcc_gc_add_root(ptr %_.gc.slot.0)
  %0 = call tailcc ptr @lambda.0_Str_I64(ptr @_.str.empty, i64 3)
  store ptr %0, ptr %_.gc.slot.0
  %1 = call i32 (ptr, ...) @printf(ptr @_.printf.fmt.Str.nl, ptr %0)
  call void @jcc_gc_pop_frame()
  ret i32 0
}
