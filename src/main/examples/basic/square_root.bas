' Calculate the square root of a number N using Newton's method
N = 2.0

' Initialize
guess = N
result = N / 2.0

' Repeat while the result is not good enough
while abs(guess - result) > 0.001
    guess = result

    ' Calculate the next guess in the series
    divisor = guess * guess - N
    dividend = 2.0 * guess
    result = guess - divisor / dividend

    print "Guess="; guess; ", next guess="; result
wend

print
print "The square root of "; N; " = "; result
print "Calling sqr("; N; ") returns "; sqr(N)
