00 REM Calculate the faculty of a number N

10 N = 5
20 result = 1
30 i = N
40 IF i = 0 GOTO 100
50 result = result * i
60 i = i - 1
70 GOTO 40
100 PRINT "fac("; N; ")="; result
