#!/usr/bin/env bash
#
# Runs the become/GC spike cases (ticket 004 of the col-strings epic). Each case
# is hand-written LLVM IR linked against the real libjcccol.a: once plain, once
# with AddressSanitizer. See become-strings-and-gc.md for what each case proves.
#
#   ./run.sh                 all cases
#   ./run.sh e1 e3a          named cases only
#   ./run.sh --scale         also run the two scaling comparisons: E4 at 10x
#                            iterations, and E3a vs E3b at 10x calls (both are
#                            about frame accounting, and both need two runs to
#                            mean anything)
#
# Deliberately outside Maven: libjcccol carries no instrumentation and ASan is a
# manual, on-demand procedure (docs/GarbageCollection.md, "Debugging with
# AddressSanitizer"). Needs clang and a built libjcccol.a in jcc-compiler/target.

set -uo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO="$(cd "$HERE/../../.." && pwd)"
LIBDIR="$REPO/jcc-compiler/target"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

CLANG="${CLANG:-clang}"
OPT="${OPT:-$(command -v opt || echo /opt/homebrew/opt/llvm/bin/opt)}"

# case:file:expected-stdout:verdict(clean|must-fail):args
CASES=(
  "e1:e1-arg-across-pop.ll:123:clean:"
  "e2:e2-negative-control.ll:123:must-fail:"
  "e3a:e3a-tail-if-no-pop.ll:yyyy:clean:200"
  "e3b:e3b-tail-if-with-pop.ll:yyyy:clean:200"
  "e4:e4-deep-chain.ll:1:clean:100000"
  "e5:e5-string-return.ll:0yyyyy:clean:"
  "e6:e6-lambda-callee.ll:123:clean:"
  "e7:e7-mutual-mismatched-sigs.ll:pqpqpqpq:clean:"
)

SCALE=0
WANTED=()
for arg in "$@"; do
  case "$arg" in
    --scale) SCALE=1 ;;
    *) WANTED+=("$arg") ;;
  esac
done

[[ -f "$LIBDIR/libjcccol.a" ]] || { echo "missing $LIBDIR/libjcccol.a - run mvn install first"; exit 1; }

failures=0

# link_and_run <label> <ll> <extra-clang-flags> <args...>
link_and_run() {
  local label="$1" ll="$2" flags="$3"; shift 3
  local bin="$WORK/$label"
  if ! "$CLANG" $flags "$HERE/$ll" -L "$LIBDIR" -ljcccol -o "$bin" 2>"$WORK/$label.cc"; then
    echo "    compile FAILED:"; sed 's/^/      /' "$WORK/$label.cc"
    return 99
  fi
  JCC_GC_DEBUG=1 "$bin" "$@" >"$WORK/$label.out" 2>&1
  return $?
}

for entry in "${CASES[@]}"; do
  IFS=':' read -r name file expected verdict cargs <<<"$entry"
  if [[ ${#WANTED[@]} -gt 0 ]] && [[ ! " ${WANTED[*]} " == *" $name "* ]]; then continue; fi

  echo "=== $name ($file) - expect: $verdict"

  # IR legality on its own: LLVM rejects a musttail not immediately followed by
  # a matching ret, so the verifier is what proves the GC calls fit.
  if [[ -x "$OPT" ]]; then
    if "$OPT" -passes=verify -disable-output "$HERE/$file" 2>"$WORK/$name.verify"; then
      echo "  verifier: ok"
    else
      echo "  verifier: FAILED"; sed 's/^/    /' "$WORK/$name.verify"; failures=$((failures + 1))
    fi
  else
    echo "  verifier: skipped (no opt; clang parses and verifies below)"
  fi

  link_and_run "$name.plain" "$file" "" $cargs; plain_status=$?
  plain_out="$(cat "$WORK/$name.plain.out" 2>/dev/null)"
  echo "  plain:    exit=$plain_status"
  sed 's/^/    | /' <<<"$plain_out"

  link_and_run "$name.asan" "$file" "-fsanitize=address" $cargs; asan_status=$?
  asan_out="$(cat "$WORK/$name.asan.out" 2>/dev/null)"
  echo "  asan:     exit=$asan_status"
  sed 's/^/    | /' <<<"$asan_out"

  asan_report=0
  grep -q "ERROR: AddressSanitizer" <<<"$asan_out" && asan_report=1
  output_ok=0
  grep -qx "$expected" <<<"$plain_out" && grep -qx "$expected" <<<"$asan_out" && output_ok=1

  if [[ "$verdict" == clean ]]; then
    if [[ $output_ok == 1 && $asan_report == 0 && $plain_status == 0 && $asan_status == 0 ]]; then
      echo "  => PASS (output '$expected', no ASan report)"
    else
      echo "  => FAIL (expected '$expected', clean ASan)"; failures=$((failures + 1))
    fi
  else
    # The negative control must be caught: an ASan report, a crash, or wrong output.
    if [[ $asan_report == 1 || $asan_status != 0 || $output_ok == 0 ]]; then
      echo "  => PASS as negative control (the harness detects the bug)"
    else
      echo "  => FAIL: control ran clean, so the harness cannot see a use-after-free"
      failures=$((failures + 1))
    fi
  fi
  echo
done

# scaled <label> <ll> <n>: one run, reporting the exit stats and max RSS in KB
scaled() {
  local label="$1" ll="$2" n="$3"
  local bin="$WORK/$label.scaled"
  [[ -x "$bin" ]] || "$CLANG" "$HERE/$ll" -L "$LIBDIR" -ljcccol -o "$bin" 2>/dev/null || return 1
  local out stats rss
  out=$(JCC_GC_DEBUG=1 /usr/bin/time -l "$bin" "$n" 2>&1)
  stats=$(grep -o "registered=.*" <<<"$out" | tail -1)
  rss=$(grep "maximum resident set size" <<<"$out" | tr -dc '0-9')
  printf "  n=%-9s rss=%6s KB  %s\n" "$n" "$((rss / 1024))" "$stats"
}

if [[ $SCALE == 1 ]]; then
  echo "=== e4 deep chain: live and RSS must not grow with the iteration count"
  scaled e4 e4-deep-chain.ll 100000
  scaled e4 e4-deep-chain.ll 1000000
  echo
  echo "=== e3a vs e3b: the missing pop leaks one frame + two roots per call, so"
  echo "    e3a's RSS grows with the call count and its collections get slower"
  echo "    (every collection walks every stale root); e3b stays flat and linear."
  echo "    Kept at 10k/100k because e3a is quadratic - 1e6 does not terminate."
  for n in 10000 100000; do
    echo "  e3a (no pop)"; scaled e3a e3a-tail-if-no-pop.ll "$n"
    echo "  e3b (pop)   "; scaled e3b e3b-tail-if-with-pop.ll "$n"
  done
  echo
fi

echo "failures: $failures"
exit $((failures > 0 ? 1 : 0))
