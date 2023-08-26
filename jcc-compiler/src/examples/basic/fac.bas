  10 REM Calculate the faculty of a number N
  20 CONST N = 5
  30 DEFINT i, r
  40 result = 1
  50 i = N
  60 IF i = 0 GOTO 100
  70 result = result * i
  80 i = i - 1
  90 GOTO 60
 100 PRINT "fac("; N; ")="; result
