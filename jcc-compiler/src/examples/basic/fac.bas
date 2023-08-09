  0 REM Calculate the faculty of a number N
    
 10 DEFINT i, r
 20 DEFINT N
 30 N = 5
 40 result = 1
 50 i = N
 60 IF i = 0 GOTO 100
 70 result = result * i
 80 i = i - 1
 90 GOTO 60
100 PRINT "fac("; N; ")="; result
