' Calculate the square root of a number N using Newton's method
CONST N = 2.0

DIM guess AS DOUBLE, result AS DOUBLE
DIM divisor AS DOUBLE, dividend AS DOUBLE

' Initialize
guess = N
result = N / 2.0

' Repeat while the result is not good enough
WHILE abs(guess - result) > 0.001
    guess = result

    ' Calculate the next guess in the series
    divisor = guess * guess - N
    dividend = 2.0 * guess
    result = guess - divisor / dividend

    PRINT "Guess="; guess; ", next guess="; result
WEND

PRINT
PRINT "The square root of "; N; " = "; result
PRINT "Calling sqr("; N; ") returns "; sqr(N)
