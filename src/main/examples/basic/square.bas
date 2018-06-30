 10 ' Calculate the square of an integer
 20 let msg1$ = "The square of "
 30 let msg2$ = " is "
 40 let x% = 5
 50 gosub 80
 60 print msg1$; x%; msg2$; square%
 70 end
 80 let square% = x% * x%
 90 return
