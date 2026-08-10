; E4 - frame accounting over a deep become chain. The iteration count comes from
; argv[1] so the same binary can be run at two scales.
;
;   fun loop(s as string, n as i64) -> string :=
;       if n == 0 then s else become loop(string(n), n - 1)
;
; Each iteration produces a fresh small string and drops the previous one, so a
; correct implementation keeps both the shadow stack and the heap flat: `live` in
; the exit-stats line must not depend on the iteration count, and max RSS must
; not grow with it. A leaked frame or root per iteration shows up in both.
;
; Expected output: 1

source_filename = "e4-deep-chain"

@_.printf.fmt.Str.nl = private constant [4 x i8] c"%s\0A\00"
@_.str.empty = private constant [1 x i8] c"\00"
@jcc.gc.global.roots = private global [1 x { ptr, i64 }] [{ ptr, i64 } { ptr null, i64 0 }]

declare ptr @col_string_i64(i64)
declare void @jcc_gc_add_root(ptr)
declare void @jcc_gc_init(i64, i64)
declare void @jcc_gc_pop_frame()
declare void @jcc_gc_push_frame()
declare ptr @jcc_gc_register(ptr)
declare void @jcc_gc_set_global_roots(ptr)
declare i32 @printf(ptr, ...)
declare i64 @strtol(ptr, ptr, i32)

; loop(ptr, i64) -> ptr
define tailcc ptr @loop_Str_I64(ptr %0, i64 %1) {
entry:
  call void @jcc_gc_push_frame()
  %_s = alloca ptr
  store ptr %0, ptr %_s
  %_n = alloca i64
  store i64 %1, ptr %_n
  %_.gc.slot.0 = alloca ptr
  call void @jcc_gc_add_root(ptr %_s)
  store ptr null, ptr %_.gc.slot.0
  call void @jcc_gc_add_root(ptr %_.gc.slot.0)
  %2 = load i64, ptr %_n
  %3 = icmp eq i64 %2, 0
  br i1 %3, label %L0, label %L1

L0:
  %4 = load ptr, ptr %_s
  call void @jcc_gc_pop_frame()
  ret ptr %4

L1:
  ; become loop(string(n), n - 1)
  %5 = load i64, ptr %_n
  %6 = call ptr @col_string_i64(i64 %5)
  %7 = call ptr @jcc_gc_register(ptr %6)
  store ptr %7, ptr %_.gc.slot.0
  %8 = load i64, ptr %_n
  %9 = sub i64 %8, 1
  call void @jcc_gc_pop_frame()
  %10 = musttail call tailcc ptr @loop_Str_I64(ptr %7, i64 %9)
  ret ptr %10
}

define i32 @main(i32 %0, ptr %1) {
entry:
  ; n = strtol(argv[1], NULL, 10)
  %2 = getelementptr inbounds ptr, ptr %1, i64 1
  %3 = load ptr, ptr %2
  %4 = call i64 @strtol(ptr %3, ptr null, i32 10)
  call void @jcc_gc_init(i64 8, i64 0)
  call void @jcc_gc_set_global_roots(ptr @jcc.gc.global.roots)
  call void @jcc_gc_push_frame()
  %_.gc.slot.0 = alloca ptr
  store ptr null, ptr %_.gc.slot.0
  call void @jcc_gc_add_root(ptr %_.gc.slot.0)
  %5 = call tailcc ptr @loop_Str_I64(ptr @_.str.empty, i64 %4)
  store ptr %5, ptr %_.gc.slot.0
  %6 = call i32 (ptr, ...) @printf(ptr @_.printf.fmt.Str.nl, ptr %5)
  call void @jcc_gc_pop_frame()
  ret i32 0
}
