; E5 - a string produced at the bottom of a become chain, returned upward past
; frames that keep allocating.
;
;   fun make(n as i64) -> string := string(n)
;   fun tail_make(n as i64) -> string := become make(n)
;   fun wrap(n as i64) -> string := if n == 0 then tail_make(0) else concat(wrap(n - 1), "y")
;
; make registers its own result, stores it in a rooted slot, pops, and returns.
; tail_make pops and musttails into make, so the value comes back to wrap with no
; frame of its own left anywhere. wrap then roots it (protectResult) before its
; own concat registers - the register-then-store discipline - and does that at
; every level of a plain recursion, so a collection runs while a returned value
; is live at several depths at once.
;
; Expected output: 0yyyyy

source_filename = "e5-string-return"

@_.printf.fmt.Str.nl = private constant [4 x i8] c"%s\0A\00"
@_.str.y = private constant [2 x i8] c"y\00"
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

; make(i64) -> ptr
define tailcc ptr @make_I64(i64 %0) {
entry:
  call void @jcc_gc_push_frame()
  %_n = alloca i64
  store i64 %0, ptr %_n
  %_.gc.slot.0 = alloca ptr
  store ptr null, ptr %_.gc.slot.0
  call void @jcc_gc_add_root(ptr %_.gc.slot.0)
  %1 = load i64, ptr %_n
  %2 = call ptr @col_string_i64(i64 %1)
  %3 = call ptr @jcc_gc_register(ptr %2)
  store ptr %3, ptr %_.gc.slot.0
  call void @jcc_gc_pop_frame()
  ret ptr %3
}

; tail_make(i64) -> ptr
define tailcc ptr @tail_make_I64(i64 %0) {
entry:
  call void @jcc_gc_push_frame()
  %_n = alloca i64
  store i64 %0, ptr %_n
  ; become make(n)
  %1 = load i64, ptr %_n
  call void @jcc_gc_pop_frame()
  %2 = musttail call tailcc ptr @make_I64(i64 %1)
  ret ptr %2
}

; wrap(i64) -> ptr
define tailcc ptr @wrap_I64(i64 %0) {
entry:
  call void @jcc_gc_push_frame()
  %_n = alloca i64
  store i64 %0, ptr %_n
  %_.gc.slot.0 = alloca ptr
  %_.gc.slot.1 = alloca ptr
  store ptr null, ptr %_.gc.slot.0
  call void @jcc_gc_add_root(ptr %_.gc.slot.0)
  store ptr null, ptr %_.gc.slot.1
  call void @jcc_gc_add_root(ptr %_.gc.slot.1)
  %1 = load i64, ptr %_n
  %2 = icmp eq i64 %1, 0
  br i1 %2, label %L0, label %L1

L0:
  %3 = call tailcc ptr @tail_make_I64(i64 0)
  store ptr %3, ptr %_.gc.slot.0                  ; protectResult: root what came back
  call void @jcc_gc_pop_frame()
  ret ptr %3

L1:
  %4 = load i64, ptr %_n
  %5 = sub i64 %4, 1
  %6 = call tailcc ptr @wrap_I64(i64 %5)
  store ptr %6, ptr %_.gc.slot.0                  ; protectResult before the next register
  %7 = call ptr @col_concat_str_str(ptr %6, ptr @_.str.y)
  %8 = call ptr @jcc_gc_register(ptr %7)
  store ptr %8, ptr %_.gc.slot.1
  call void @jcc_gc_pop_frame()
  ret ptr %8
}

define i32 @main() {
entry:
  call void @jcc_gc_init(i64 1, i64 0)
  call void @jcc_gc_set_global_roots(ptr @jcc.gc.global.roots)
  call void @jcc_gc_push_frame()
  %_.gc.slot.0 = alloca ptr
  store ptr null, ptr %_.gc.slot.0
  call void @jcc_gc_add_root(ptr %_.gc.slot.0)
  %0 = call tailcc ptr @wrap_I64(i64 5)
  store ptr %0, ptr %_.gc.slot.0
  %1 = call i32 (ptr, ...) @printf(ptr @_.printf.fmt.Str.nl, ptr %0)
  call void @jcc_gc_pop_frame()
  ret i32 0
}
