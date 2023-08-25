 10 ' Calculate the square of an integer
 20 LET msg1$ = "The square of "
 30 LET msg2$ = " is "
 40 LET x% = 5
 50 GOSUB 80
 60 PRINT msg1$; x%; msg2$; square%
 70 END
 80 LET square% = x% * x%
 90 RETURN
